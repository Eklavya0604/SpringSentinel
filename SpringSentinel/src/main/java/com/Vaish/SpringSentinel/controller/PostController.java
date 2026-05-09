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

    @Operation(
            summary = "Create a post",
            description = "Creates a new post by a user or bot",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                    {
                      "authorId": 1,
                      "authorType": "USER",
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
    public ResponseEntity<Post> createPost(@Valid @RequestBody Post post) {
        return ResponseEntity.ok(postService.createPost(post));
    }

    @Operation(
            summary = "Add a comment",
            description = """
            Adds a comment to a post. Bot comments trigger Redis guardrails:
            - Horizontal Cap: Max 100 bot replies per post
            - Vertical Cap: Max depth level 20
            - Cooldown Cap: Same bot cannot comment on same user's post within 10 minutes
            """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
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
                              "depthLevel": 1
                            }
                        """
                                    ),
                                    @ExampleObject(
                                            name = "Human Comment",
                                            value = """
                            {
                              "authorId": 1,
                              "authorType": "USER",
                              "content": "Great content!",
                              "depthLevel": 1
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
            @Valid @RequestBody Comment comment) {
        return ResponseEntity.ok(postService.addComment(postId, comment));
    }

    @Operation(
            summary = "Like a post",
            description = "Human like adds +20 virality points. Bot like adds +1 point.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Like registered"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized - JWT required")
            }
    )
    @PostMapping("/{postId}/like")
    public ResponseEntity<String> likePost(
            @Parameter(description = "Post ID", example = "1")
            @PathVariable Long postId,
            @Parameter(description = "Type of liker", example = "USER")
            @RequestParam String likerType) {
        postService.likePost(postId, likerType);
        return ResponseEntity.ok("Like registered");
    }

    @Operation(
            summary = "Get virality score",
            description = """
            Returns the real-time virality score from Redis.
            Score = (Bot replies × 1) + (Human likes × 20) + (Human comments × 50)
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