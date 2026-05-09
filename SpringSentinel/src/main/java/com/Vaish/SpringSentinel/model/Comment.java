package com.Vaish.SpringSentinel.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "comments")
@Data
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Set by server — never from client ────────────────────────
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Column(name = "post_id", nullable = false)
    private Long postId;

    // ── For BOT: client sends botId. For USER: set from JWT ──────
    @Column(name = "author_id", nullable = false)
    private Long authorId;

    // ── CLIENT sends "BOT" or "USER" ─────────────────────────────
    @Column(name = "author_type")
    private String authorType;

    @NotBlank(message = "Content cannot be empty")
    @Size(max = 500,
            message = "Content cannot exceed 500 characters")
    @Column(columnDefinition = "TEXT")
    private String content;

    // ── null = top level, set by client to reply to a comment ────
    @Column(name = "parent_comment_id")
    private Long parentCommentId;

    // ── Calculated server-side — client cannot set this ──────────
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Column(name = "depth_level")
    private int depthLevel = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}