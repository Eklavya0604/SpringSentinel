package com.Vaish.SpringSentinel.controller;

import com.Vaish.SpringSentinel.model.Comment;
import com.Vaish.SpringSentinel.model.Post;
import com.Vaish.SpringSentinel.repository.CommentRepository;
import com.Vaish.SpringSentinel.repository.PostRepository;
import com.Vaish.SpringSentinel.service.PostService;
import com.Vaish.SpringSentinel.service.RedisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@Tag(name = "3. Posts",
        description = "Post management and Redis guardrail endpoints")
@SecurityRequirement(name = "Bearer Authentication")
public class PostController {

    private final PostService postService;
    private final RedisService redisService;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    // ── Get all posts ────────────────────────────────────────────
    @Operation(summary = "Get all posts (newest first)")
    @GetMapping
    public ResponseEntity<List<Post>> getAllPosts() {
        return ResponseEntity.ok(
                postRepository.findAllByOrderByCreatedAtDesc()
        );
    }

    // ── Create post ──────────────────────────────────────────────
    @Operation(
            summary = "Create a post",
            description = "authorId and authorType set from JWT automatically.",
            requestBody =
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                    {
                      "content": "Hello World!"
                    }
                """)
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "200",
                            description = "Post created"),
                    @ApiResponse(responseCode = "400",
                            description = "Validation failed"),
                    @ApiResponse(responseCode = "401",
                            description = "Unauthorized")
            }
    )
    @PostMapping
    public ResponseEntity<Post> createPost(
            @Valid @RequestBody Post post,
            @AuthenticationPrincipal String username) {
        return ResponseEntity.ok(
                postService.createPost(post, username)
        );
    }

    // ── Get comments for post ────────────────────────────────────
    @Operation(summary = "Get all comments for a post")
    @GetMapping("/{postId}/comments")
    public ResponseEntity<List<Comment>> getComments(
            @Parameter(description = "Post ID", example = "1")
            @PathVariable Long postId) {
        return ResponseEntity.ok(
                commentRepository.findByPostIdOrderByCreatedAtAsc(postId)
        );
    }

    // ── Add comment ──────────────────────────────────────────────
    @Operation(
            summary = "Add a comment",
            description = """
            Bot comments trigger Redis guardrails:
            - Horizontal Cap: max 100 bot replies/post
            - Vertical Cap: max depth 20
            - Cooldown Cap: same bot cannot hit same user in 10 mins
            """,
            requestBody =
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "Bot Comment",
                                            value = """
                        {
                          "authorId": 1,
                          "authorType": "BOT",
                          "content": "Nice post!",
                          "parentCommentId": null
                        }
                    """),
                                    @ExampleObject(
                                            name = "Human Comment",
                                            value = """
                        {
                          "authorType": "USER",
                          "content": "Amazing!",
                          "parentCommentId": null
                        }
                    """)
                            }
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "200",
                            description = "Comment added"),
                    @ApiResponse(responseCode = "400",
                            description = "Depth exceeded"),
                    @ApiResponse(responseCode = "429",
                            description = "Bot cap/cooldown exceeded"),
                    @ApiResponse(responseCode = "401",
                            description = "Unauthorized")
            }
    )
    @PostMapping("/{postId}/comments")
    public ResponseEntity<Comment> addComment(
            @Parameter(description = "Post ID", example = "1")
            @PathVariable Long postId,
            @Valid @RequestBody Comment comment,
            @AuthenticationPrincipal String username) {
        return ResponseEntity.ok(
                postService.addComment(postId, comment, username)
        );
    }

    // ── Like post ────────────────────────────────────────────────
    @Operation(
            summary = "Like a post",
            description = "Human like adds +20 virality points"
    )
    @PostMapping("/{postId}/like")
    public ResponseEntity<String> likePost(
            @Parameter(description = "Post ID", example = "1")
            @PathVariable Long postId,
            @AuthenticationPrincipal String username) {
        postService.likePost(postId, username);
        return ResponseEntity.ok("Like registered");
    }

    // ── Virality score ───────────────────────────────────────────
    @Operation(
            summary = "Get virality score",
            description = """
            Returns real-time virality score from Redis.
            Formula: (Bot×1) + (Like×20) + (Comment×50)
            """
    )
    @GetMapping("/{postId}/virality")
    public ResponseEntity<Map<String, String>> getVirality(
            @Parameter(description = "Post ID", example = "1")
            @PathVariable Long postId) {

        postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Post not found"));

        String score;
        try {
            score = redisService.getVirality(postId);
        } catch (Exception e) {
            score = "0";
        }

        Map<String, String> response = new HashMap<>();
        response.put("postId", postId.toString());
        response.put("viralityScore",
                score != null ? score : "0");
        return ResponseEntity.ok(response);
    }
}