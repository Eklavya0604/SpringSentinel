package com.Vaish.SpringSentinel.repository;

import com.Vaish.SpringSentinel.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment,Long> {
}
