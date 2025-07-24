package com.harrish.auth.repository;

import com.harrish.auth.model.BlogPost;
import com.harrish.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BlogPostRepository extends JpaRepository<BlogPost, Long> {

    List<BlogPost> findByCreatedByOrderByCreatedAtDesc(User user);
}