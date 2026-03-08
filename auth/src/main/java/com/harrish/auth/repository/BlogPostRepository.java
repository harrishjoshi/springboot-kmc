package com.harrish.auth.repository;

import com.harrish.auth.model.BlogPost;
import com.harrish.auth.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BlogPostRepository extends JpaRepository<BlogPost, Long> {

    /**
     * Find blog posts by creator with fetching of audit fields to prevent N+1 queries.
     * Uses LEFT JOIN FETCH for createdBy and updatedBy to avoid lazy loading issues.
     */
    @EntityGraph(attributePaths = {"createdBy", "updatedBy"})
    List<BlogPost> findByCreatedByOrderByCreatedAtDesc(User user);

    /**
     * Find all blog posts with audit fields fetched to prevent N+1 queries.
     * This is used for paginated queries.
     */
    @EntityGraph(attributePaths = {"createdBy", "updatedBy"})
    Page<BlogPost> findAll(Pageable pageable);

    /**
     * Find blog post by ID with audit fields fetched to prevent N+1 queries.
     */
    @EntityGraph(attributePaths = {"createdBy", "updatedBy"})
    Optional<BlogPost> findById(Long id);
}