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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static net.logstash.logback.argument.StructuredArguments.kv;

/**
 * Service for managing blog posts.
 * Now focused only on business logic, with mapping and authorization delegated to specialized components.
 */
@Service
public class BlogPostService {

    private static final Logger log = LoggerFactory.getLogger(BlogPostService.class);

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
        log.debug("Fetching all blog posts", kv("page", pageable.getPageNumber()), kv("size", pageable.getPageSize()));
        Page<BlogPostResponse> results = blogPostRepository.findAll(pageable)
                .map(blogPostMapper::toResponse);
        log.info("Blog posts fetched", 
                kv("totalElements", results.getTotalElements()),
                kv("totalPages", results.getTotalPages()),
                kv("currentPage", results.getNumber()));
        return results;
    }

    @Transactional(readOnly = true)
    public List<BlogPostResponse> getBlogPostsByUser(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        
        log.debug("Fetching blog posts by user", kv("userId", userId), kv("step", "start"));

        var user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        List<BlogPost> blogPosts = blogPostRepository.findByCreatedByOrderByCreatedAtDesc(user);
        log.info("Blog posts fetched by user", 
                kv("userId", userId), 
                kv("count", blogPosts.size()));
        return blogPostMapper.toResponseList(blogPosts);
    }

    @Transactional(readOnly = true)
    public BlogPostResponse getBlogPostById(Long id) {
        log.debug("Fetching blog post by ID", kv("blogPostId", id));
        var blogPost = blogPostRepository.findById(id)
                .orElseThrow(BlogPostNotFoundException::new);

        log.info("Blog post fetched", kv("blogPostId", id));
        return blogPostMapper.toResponse(blogPost);
    }

    @Transactional
    public BlogPostResponse createBlogPost(CreateBlogPostRequest request) {
        long startTime = System.currentTimeMillis();
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        
        User currentUser = currentUserProvider.getCurrentUser();
        log.info("Creating blog post", 
                kv("userId", currentUser.getId()),
                kv("title", request.getTitle()),
                kv("step", "start"));

        var blogPost = BlogPost.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .build();
        log.debug("Blog post entity created", kv("step", "entity_created"));

        var savedBlogPost = blogPostRepository.save(blogPost);
        long saveTime = System.currentTimeMillis();
        log.debug("Blog post saved to database", 
                kv("blogPostId", savedBlogPost.getId()),
                kv("step", "saved"),
                kv("duration_ms", saveTime - startTime));

        // Publish blog post created event (Observer pattern)
        eventPublisher.publishEvent(new BlogPostCreatedEvent(this, savedBlogPost, currentUser));
        log.debug("Blog post created event published", 
                kv("blogPostId", savedBlogPost.getId()),
                kv("step", "event_published"));

        long totalTime = System.currentTimeMillis();
        log.info("Blog post created successfully", 
                kv("blogPostId", savedBlogPost.getId()),
                kv("userId", currentUser.getId()),
                kv("step", "complete"),
                kv("duration_ms", totalTime - startTime));

        return blogPostMapper.toResponse(savedBlogPost);
    }

    @Transactional
    public BlogPostResponse updateBlogPost(Long id, UpdateBlogPostRequest request) {
        long startTime = System.currentTimeMillis();
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        
        log.info("Updating blog post", 
                kv("blogPostId", id),
                kv("step", "start"));

        // Check authorization
        authorizationService.requireBlogPostOwnership(id);
        log.debug("Authorization check passed", 
                kv("blogPostId", id),
                kv("step", "auth_checked"));

        var blogPost = blogPostRepository.findById(id)
                .orElseThrow(BlogPostNotFoundException::new);

        // Use domain methods to update - preserves audit fields
        blogPost.updateTitle(request.getTitle());
        blogPost.updateContent(request.getContent());
        log.debug("Blog post entity updated", 
                kv("blogPostId", id),
                kv("step", "entity_updated"));

        // JPA will automatically persist changes due to @Transactional
        long totalTime = System.currentTimeMillis();
        log.info("Blog post updated successfully", 
                kv("blogPostId", id),
                kv("step", "complete"),
                kv("duration_ms", totalTime - startTime));
        
        return blogPostMapper.toResponse(blogPost);
    }

    @Transactional
    public void deleteBlogPost(Long id) {
        long startTime = System.currentTimeMillis();
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        
        log.info("Deleting blog post", kv("blogPostId", id), kv("step", "start"));

        // Check authorization
        authorizationService.requireBlogPostOwnership(id);
        log.debug("Authorization check passed", 
                kv("blogPostId", id),
                kv("step", "auth_checked"));

        var blogPost = blogPostRepository.findById(id)
                .orElseThrow(BlogPostNotFoundException::new);

        blogPostRepository.delete(blogPost);
        
        long totalTime = System.currentTimeMillis();
        log.info("Blog post deleted successfully", 
                kv("blogPostId", id),
                kv("step", "complete"),
                kv("duration_ms", totalTime - startTime));
    }

    @Transactional(readOnly = true)
    public List<BlogPostResponse> getCurrentUserBlogPosts() {
        User currentUser = currentUserProvider.getCurrentUser();
        log.debug("Fetching current user's blog posts", kv("userId", currentUser.getId()));
        
        List<BlogPost> blogPosts = blogPostRepository.findByCreatedByOrderByCreatedAtDesc(currentUser);
        log.info("Current user's blog posts fetched", 
                kv("userId", currentUser.getId()),
                kv("count", blogPosts.size()));
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
