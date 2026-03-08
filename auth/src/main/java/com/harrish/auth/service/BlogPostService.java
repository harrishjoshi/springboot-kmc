package com.harrish.auth.service;

import com.harrish.auth.dto.BlogPostResponse;
import com.harrish.auth.dto.CreateBlogPostRequest;
import com.harrish.auth.dto.UpdateBlogPostRequest;
import com.harrish.auth.event.BlogPostCreatedEvent;
import com.harrish.auth.exception.BlogPostNotFoundException;
import com.harrish.auth.exception.UserNotFoundException;
import com.harrish.auth.model.BlogPost;
import com.harrish.auth.model.User;
import com.harrish.auth.repository.BlogPostRepository;
import com.harrish.auth.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for managing blog posts.
 * Now focused only on business logic, with mapping and authorization delegated to specialized components.
 */
@Service
public class BlogPostService {

    private final BlogPostRepository blogPostRepository;
    private final UserRepository userRepository;
    private final BlogPostMapper blogPostMapper;
    private final BlogPostAuthorizationService authorizationService;
    private final CurrentUserProvider currentUserProvider;
    private final ApplicationEventPublisher eventPublisher;

    public BlogPostService(
            BlogPostRepository blogPostRepository,
            UserRepository userRepository,
            BlogPostMapper blogPostMapper,
            BlogPostAuthorizationService authorizationService,
            CurrentUserProvider currentUserProvider,
            ApplicationEventPublisher eventPublisher) {
        this.blogPostRepository = blogPostRepository;
        this.userRepository = userRepository;
        this.blogPostMapper = blogPostMapper;
        this.authorizationService = authorizationService;
        this.currentUserProvider = currentUserProvider;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public Page<BlogPostResponse> getAllBlogPosts(Pageable pageable) {
        return blogPostRepository.findAll(pageable)
                .map(blogPostMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<BlogPostResponse> getBlogPostsByUser(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }

        var user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        List<BlogPost> blogPosts = blogPostRepository.findByCreatedByOrderByCreatedAtDesc(user);
        return blogPostMapper.toResponseList(blogPosts);
    }

    @Transactional(readOnly = true)
    public BlogPostResponse getBlogPostById(Long id) {
        var blogPost = blogPostRepository.findById(id)
                .orElseThrow(BlogPostNotFoundException::new);

        return blogPostMapper.toResponse(blogPost);
    }

    @Transactional
    public BlogPostResponse createBlogPost(CreateBlogPostRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }

        var blogPost = BlogPost.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .build();

        var savedBlogPost = blogPostRepository.save(blogPost);

        // Publish blog post created event (Observer pattern)
        User currentUser = currentUserProvider.getCurrentUser();
        eventPublisher.publishEvent(new BlogPostCreatedEvent(this, savedBlogPost, currentUser));

        return blogPostMapper.toResponse(savedBlogPost);
    }

    @Transactional
    public BlogPostResponse updateBlogPost(Long id, UpdateBlogPostRequest request) {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }

        // Check authorization
        authorizationService.requireBlogPostOwnership(id);

        var blogPost = blogPostRepository.findById(id)
                .orElseThrow(BlogPostNotFoundException::new);

        // Use domain methods to update - preserves audit fields
        blogPost.updateTitle(request.getTitle());
        blogPost.updateContent(request.getContent());

        // JPA will automatically persist changes due to @Transactional
        return blogPostMapper.toResponse(blogPost);
    }

    @Transactional
    public void deleteBlogPost(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }

        // Check authorization
        authorizationService.requireBlogPostOwnership(id);

        var blogPost = blogPostRepository.findById(id)
                .orElseThrow(BlogPostNotFoundException::new);

        blogPostRepository.delete(blogPost);
    }

    @Transactional(readOnly = true)
    public List<BlogPostResponse> getCurrentUserBlogPosts() {
        User currentUser = currentUserProvider.getCurrentUser();
        List<BlogPost> blogPosts = blogPostRepository.findByCreatedByOrderByCreatedAtDesc(currentUser);
        return blogPostMapper.toResponseList(blogPosts);
    }

    /**
     * Checks if the current user is the creator of the blog post.
     * Delegates to BlogPostAuthorizationService.
     *
     * @param blogPostId the ID of the blog post
     * @return true if the current user is the creator, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean isBlogPostCreator(Long blogPostId) {
        return authorizationService.isBlogPostOwner(blogPostId);
    }
}
