package com.Vaish.SpringSentinel.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "posts")
@Data
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Set by server from JWT — client cannot set this ──────────
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Column(name = "author_id", nullable = false)
    private Long authorId;

    // ── Set by server — always "USER" for post creation ──────────
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Column(name = "author_type")
    private String authorType;

    @NotBlank(message = "Content cannot be empty")
    @Size(max = 500,
            message = "Content cannot exceed 500 characters")
    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}