package com.harrish.auth.event.listener;

import com.harrish.auth.event.BlogPostCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Event listener for blog post-related domain events.
 * Handles cross-cutting concerns like notifications, cache invalidation, 
 * search indexing, and analytics in a decoupled manner using the Observer pattern.
 * 
 * Benefits:
 * - Decouples blog post creation logic from side effects
 * - Makes it easy to add new behaviors without modifying BlogPostService
 * - Async processing prevents blocking the main creation flow
 * - Single Responsibility: each listener method handles one concern
 */
@Slf4j
@Component
public class BlogPostEventListener {

    /**
     * Handles blog post creation events.
     * Logs the creation for audit purposes and could trigger additional actions
     * like sending notifications, updating search indexes, or invalidating caches.
     * 
     * @param event the blog post creation event
     */
    @Async
    @EventListener
    public void handleBlogPostCreated(BlogPostCreatedEvent event) {
        var blogPost = event.getBlogPost();
        var author = event.getAuthor();
        log.info("Blog post created - ID: {}, Title: '{}', Author ID: {}, Timestamp: {}",
                blogPost.getId(),
                blogPost.getTitle(),
                author.getId(),
                event.getCreatedAt());
        
        // TODO: Future enhancements can be added here without modifying BlogPostService:
        // - Send notification to followers/subscribers
        // - Update search index (Elasticsearch/Solr)
        // - Invalidate relevant caches
        // - Generate social media preview cards
        // - Update author's post count/statistics
        // - Trigger content moderation workflow
        // - Send to recommendation engine
    }
    
    /**
     * Handles cache invalidation for blog post listings.
     * Demonstrates how multiple listeners can handle different concerns
     * for the same event without coupling.
     */
    @Async
    @EventListener
    public void invalidateBlogPostCache(BlogPostCreatedEvent event) {
        log.debug("Invalidating blog post cache after new post creation: {}", event.getBlogPost().getId());
        // TODO: Invalidate cache
        // - Clear "recent posts" cache
        // - Clear author's post list cache
        // - Clear tag/category caches if applicable
    }
    
    /**
     * Updates search index when a new blog post is created.
     * Another example of separation of concerns via events.
     */
    @Async
    @EventListener
    public void updateSearchIndex(BlogPostCreatedEvent event) {
        var blogPost = event.getBlogPost();
        log.debug("Updating search index for blog post: {} - '{}'", 
                blogPost.getId(), 
                blogPost.getTitle());
        // TODO: Update search index
        // - Index title, content, author, timestamp
        // - Update relevance scores
        // - Refresh search autocomplete
    }
}
