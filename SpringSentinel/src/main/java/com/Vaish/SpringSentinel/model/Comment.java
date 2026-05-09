package com.Vaish.SpringSentinel.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @NotNull(message = "Author ID cannot be null")
    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @NotBlank(message = "Author type cannot be empty")
    @Column(name = "author_type")
    private String authorType;

    @NotBlank(message = "Content cannot be empty")
    @Size(max = 500, message = "Content cannot exceed 500 characters")
    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "depth_level")
    private int depthLevel = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}