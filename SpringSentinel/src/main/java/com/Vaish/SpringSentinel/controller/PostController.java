package com.Vaish.SpringSentinel.controller;

import com.Vaish.SpringSentinel.model.Comment;
import com.Vaish.SpringSentinel.model.Post;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@Tag(name = "3. Posts", description = "Post management and Redis guardrail endpoints")
@SecurityRequirement(name = "Bearer Authentication")
public class PostController {

    private final PostService postService;
    private final RedisService redisService;

    // ── Create post ─────────────────────────────────────────────
    @Operation(
            summary = "Create a post",
            description = "Creates a new post. authorId and authorType are resolved from JWT automatically.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                    {
                      "content": "Hello World! This is my first post."
                    }
                """)
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Post created successfully"),
                    @ApiResponse(responseCode = "400", description = "Validation failed"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized - JWT required")
            }
    )
    @PostMapping
    public ResponseEntity<Post> createPost(
            @Valid @RequestBody Post post,
            @AuthenticationPrincipal String username) {
        return ResponseEntity.ok(postService.createPost(post, username));
    }

    // ── Add comment ─────────────────────────────────────────────
    @Operation(
            summary = "Add a comment",
            description = """
            Adds a comment to a post.
            - USER comments: authorId resolved from JWT automatically
            - BOT comments: authorId must be provided in body (bot ID)
            - parentCommentId: null for top level, set to a comment ID to reply
            - depthLevel: calculated server-side, do not send
            
            Bot comments trigger Redis guardrails:
            - Horizontal Cap: Max 100 bot replies per post
            - Vertical Cap: Max depth level 20
            - Cooldown Cap: Same bot cannot comment on same user's post within 10 minutes
            """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "Bot Comment (top level)",
                                            value = """
                            {
                              "authorId": 1,
                              "authorType": "BOT",
                              "content": "Nice post!",
                              "parentCommentId": null
                            }
                        """
                                    ),
                                    @ExampleObject(
                                            name = "Human Comment (top level)",
                                            value = """
                            {
                              "authorType": "USER",
                              "content": "Great content!",
                              "parentCommentId": null
                            }
                        """
                                    ),
                                    @ExampleObject(
                                            name = "Human Reply (nested)",
                                            value = """
                            {
                              "authorType": "USER",
                              "content": "Replying to your comment!",
                              "parentCommentId": 1
                            }
                        """
                                    )
                            }
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Comment added successfully"),
                    @ApiResponse(responseCode = "400", description = "Depth level exceeded (> 20)"),
                    @ApiResponse(responseCode = "429", description = "Bot cap or cooldown exceeded"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized - JWT required")
            }
    )
    @PostMapping("/{postId}/comments")
    public ResponseEntity<Comment> addComment(
            @Parameter(description = "Post ID", example = "1")
            @PathVariable Long postId,
            @Valid @RequestBody Comment comment,
            @AuthenticationPrincipal String username) {
        return ResponseEntity.ok(
                postService.addComment(postId, comment, username));
    }

    // ── Like post ────────────────────────────────────────────────
    @Operation(
            summary = "Like a post",
            description = "Like a post as the currently logged in user. Adds +20 virality points.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Like registered"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized - JWT required")
            }
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
            Returns the real-time virality score from Redis.
            Score = (Bot replies x 1) + (Human likes x 20) + (Human comments x 50)
            """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "Virality score returned"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized - JWT required")
            }
    )
    @GetMapping("/{postId}/virality")
    public ResponseEntity<Map<String, String>> getVirality(
            @Parameter(description = "Post ID", example = "1")
            @PathVariable Long postId) {
        Map<String, String> response = new HashMap<>();
        response.put("postId", postId.toString());
        response.put("viralityScore", redisService.getVirality(postId));
        return ResponseEntity.ok(response);
    }
}