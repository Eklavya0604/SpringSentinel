package com.Vaish.SpringSentinel.service;

import com.Vaish.SpringSentinel.model.Comment;
import com.Vaish.SpringSentinel.model.Post;
import com.Vaish.SpringSentinel.repository.CommentRepository;
import com.Vaish.SpringSentinel.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final RedisService redisService;
    private final NotificationService notificationService;

    private static final int MAX_BOT_REPLIES = 100;
    private static final int MAX_DEPTH = 20;

    // ============================================================
    // CREATE POST
    // ============================================================

    public Post createPost(Post post) {
        return postRepository.save(post);
    }

    // ============================================================
    // ADD COMMENT
    // ============================================================

    @Transactional
    public Comment addComment(Long postId, Comment comment) {

        comment.setPostId(postId);

        boolean isBot = "BOT".equalsIgnoreCase(
                comment.getAuthorType()
        );

        // ========================================================
        // BOT COMMENT FLOW
        // ========================================================

        if (isBot) {

            // ── 1. Depth Check (cheapest, no Redis needed) ──────
            if (comment.getDepthLevel() > MAX_DEPTH) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Max comment depth of 20 exceeded"
                );
            }

            // ── 2. Fetch Post ────────────────────────────────────
            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Post not found"
                    ));

            Long humanId = post.getAuthorId();

            // ── 3. Horizontal Cap FIRST (Lua atomic) ─────────────
            // Check cap before cooldown to avoid unnecessary
            // cooldown key creation when post is already full
            boolean allowed = redisService.tryIncrementBotCount(
                    postId, MAX_BOT_REPLIES
            );

            if (!allowed) {
                throw new ResponseStatusException(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "Post bot reply limit of 100 reached"
                );
            }

            // ── 4. Cooldown Check (setIfAbsent atomic) ───────────
            boolean cooldownAcquired = redisService.trySetCooldown(
                    comment.getAuthorId(), humanId
            );

            if (!cooldownAcquired) {
                // Rollback bot count since cooldown blocked
                redisService.decrementBotCount(postId);
                throw new ResponseStatusException(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "Bot cooldown active for this user"
                );
            }

            // ── 5. Save to DB ────────────────────────────────────
            Comment saved;
            try {
                saved = commentRepository.save(comment);
            } catch (Exception e) {
                // Rollback both Redis keys on DB failure
                redisService.decrementBotCount(postId);
                redisService.removeCooldown(
                        comment.getAuthorId(), humanId
                );
                throw e;
            }

            // ── 6. Update Virality (+1 for bot reply) ────────────
            redisService.incrementVirality(postId, 1);

            // ── 7. Trigger Notification ──────────────────────────
            notificationService.handleBotInteraction(
                    humanId,
                    "Bot " + comment.getAuthorId()
                            + " replied to your post"
            );

            return saved;
        }

        // ========================================================
        // HUMAN COMMENT FLOW (no guardrails)
        // ========================================================

        Comment saved = commentRepository.save(comment);
        redisService.incrementVirality(postId, 50);
        return saved;
    }

    // ============================================================
    // LIKE POST
    // ============================================================

    public void likePost(Long postId, String likerType) {
        if ("USER".equalsIgnoreCase(likerType)) {
            redisService.incrementVirality(postId, 20);
        }
    }
}