package com.harrish.auth.service;

import com.harrish.auth.exception.BlogPostNotFoundException;
import com.harrish.auth.model.BlogPost;
import com.harrish.auth.model.User;
import com.harrish.auth.repository.BlogPostRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service responsible for blog post authorization checks.
 * This separates authorization logic from business logic.
 */
@Service
public class BlogPostAuthorizationService {

    private final BlogPostRepository blogPostRepository;
    private final CurrentUserProvider currentUserProvider;

    public BlogPostAuthorizationService(
            BlogPostRepository blogPostRepository,
            CurrentUserProvider currentUserProvider) {
        this.blogPostRepository = blogPostRepository;
        this.currentUserProvider = currentUserProvider;
    }

    /**
     * Ensures the current user is the owner of the blog post.
     * Throws AccessDeniedException if the user is not the owner.
     *
     * @param blogPostId the ID of the blog post
     * @throws BlogPostNotFoundException if blog post not found
     * @throws AccessDeniedException if user is not the owner
     */
    @Transactional(readOnly = true)
    public void requireBlogPostOwnership(Long blogPostId) {
        if (!isBlogPostOwner(blogPostId)) {
            throw new AccessDeniedException("You do not have permission to modify this blog post");
        }
    }

    /**
     * Checks if the current user is the owner of the blog post.
     *
     * @param blogPostId the ID of the blog post
     * @return true if the current user is the owner, false otherwise
     * @throws BlogPostNotFoundException if blog post not found
     */
    @Transactional(readOnly = true)
    public boolean isBlogPostOwner(Long blogPostId) {
        if (blogPostId == null) {
            throw new IllegalArgumentException("blogPostId must not be null");
        }

        User currentUser = currentUserProvider.getCurrentUser();

        return blogPostRepository.findById(blogPostId)
                .map(BlogPost::getCreatedBy)
                .map(User::getId)
                .map(creatorId -> creatorId.equals(currentUser.getId()))
                .orElseThrow(BlogPostNotFoundException::new);
    }
}
