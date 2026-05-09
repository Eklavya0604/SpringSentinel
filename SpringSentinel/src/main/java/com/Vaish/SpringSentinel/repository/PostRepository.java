package com.Vaish.SpringSentinel.repository;

import com.Vaish.SpringSentinel.model.Post;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post,Long> {
}
