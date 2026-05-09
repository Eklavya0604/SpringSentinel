package com.Vaish.SpringSentinel.service;

import com.Vaish.SpringSentinel.model.Comment;
import com.Vaish.SpringSentinel.model.Post;
import com.Vaish.SpringSentinel.model.User;
import com.Vaish.SpringSentinel.repository.CommentRepository;
import com.Vaish.SpringSentinel.repository.PostRepository;
import com.Vaish.SpringSentinel.repository.UserRepository;
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
    private final UserRepository userRepository;
    private final RedisService redisService;
    private final NotificationService notificationService;

    private static final int MAX_BOT_REPLIES = 100;
    private static final int MAX_DEPTH = 20;

    // ── Resolve user from JWT username ───────────────────────────
    private User resolveUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found: " + username));
    }

    // ── Calculate depth from parent comment ──────────────────────
    private int calculateDepth(Long parentCommentId) {
        if (parentCommentId == null) return 0;
        Comment parent = commentRepository
                .findById(parentCommentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Parent comment not found"));
        return parent.getDepthLevel() + 1;
    }

    // ============================================================
    // CREATE POST
    // ============================================================

    public Post createPost(Post post, String username) {
        User caller = resolveUser(username);
        post.setAuthorId(caller.getId());
        post.setAuthorType("USER");
        return postRepository.save(post);
    }

    // ============================================================
    // ADD COMMENT
    // ============================================================

    @Transactional
    public Comment addComment(
            Long postId,
            Comment comment,
            String username) {

        comment.setPostId(postId);

        // ── Calculate depth server-side ──────────────────────────
        int depth = calculateDepth(
                comment.getParentCommentId()
        );
        comment.setDepthLevel(depth);

        boolean isBot = "BOT".equalsIgnoreCase(
                comment.getAuthorType()
        );

        // ========================================================
        // BOT COMMENT FLOW
        // ========================================================

        if (isBot) {

            // ── 1. Depth Check ───────────────────────────────────
            if (depth > MAX_DEPTH) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Max comment depth of 20 exceeded");
            }

            // ── 2. Fetch Post ────────────────────────────────────
            Post post = postRepository.findById(postId)
                    .orElseThrow(() ->
                            new ResponseStatusException(
                                    HttpStatus.NOT_FOUND,
                                    "Post not found"));

            Long humanId = post.getAuthorId();

            // ── 3. Horizontal Cap (Lua atomic) ───────────────────
            boolean allowed = redisService
                    .tryIncrementBotCount(postId, MAX_BOT_REPLIES);

            if (!allowed) {
                throw new ResponseStatusException(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "Post bot reply limit of 100 reached");
            }

            // ── 4. Cooldown Check (atomic setIfAbsent) ───────────
            boolean cooldownAcquired = redisService
                    .trySetCooldown(comment.getAuthorId(), humanId);

            if (!cooldownAcquired) {
                redisService.decrementBotCount(postId);
                throw new ResponseStatusException(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "Bot cooldown active for this user");
            }

            // ── 5. Save to DB ────────────────────────────────────
            Comment saved;
            try {
                saved = commentRepository.save(comment);
            } catch (Exception e) {
                redisService.decrementBotCount(postId);
                redisService.removeCooldown(
                        comment.getAuthorId(), humanId);
                throw e;
            }

            // ── 6. Update Virality (+1 bot reply) ────────────────
            redisService.incrementVirality(postId, 1);

            // ── 7. Trigger Notification ──────────────────────────
            notificationService.handleBotInteraction(
                    humanId,
                    "Bot " + comment.getAuthorId()
                            + " replied to your post");

            return saved;
        }

        // ========================================================
        // HUMAN COMMENT FLOW
        // ========================================================

        // ── Depth check for humans too ───────────────────────────
        if (depth > MAX_DEPTH) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Max comment depth of 20 exceeded");
        }

        User caller = resolveUser(username);
        comment.setAuthorId(caller.getId());
        comment.setAuthorType("USER");

        Comment saved = commentRepository.save(comment);
        redisService.incrementVirality(postId, 50);
        return saved;
    }

    // ============================================================
    // LIKE POST
    // ============================================================

    public void likePost(Long postId, String username) {
        resolveUser(username);
        redisService.incrementVirality(postId, 20);
    }
}