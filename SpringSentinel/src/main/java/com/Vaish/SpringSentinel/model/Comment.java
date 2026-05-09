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

    // set by server — never from client
    @Column(name = "post_id", nullable = false)
    private Long postId;

    // set by server from JWT (USER) or request body (BOT)
    @Column(name = "author_id", nullable = false)
    private Long authorId;

    // USER or BOT — client sends this
    @Column(name = "author_type")
    private String authorType;

    @NotBlank(message = "Content cannot be empty")
    @Size(max = 500, message = "Content cannot exceed 500 characters")
    @Column(columnDefinition = "TEXT")
    private String content;

    // null = top level comment, set by client to reply to a comment
    @Column(name = "parent_comment_id")
    private Long parentCommentId;

    // calculated server-side — client can never overwrite this
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Column(name = "depth_level")
    private int depthLevel = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}