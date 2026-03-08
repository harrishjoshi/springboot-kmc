package com.harrish.auth.event;

import com.harrish.auth.model.BlogPost;
import com.harrish.auth.model.User;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

/**
 * Event published when a new blog post is created.
 * Enables decoupled handling of blog post creation side effects:
 * - Sending notifications to followers
 * - Indexing in search engines
 * - Clearing caches
 * - Tracking analytics
 */
public class BlogPostCreatedEvent extends ApplicationEvent {
    
    private final BlogPost blogPost;
    private final User author;
    private final LocalDateTime createdAt;

    public BlogPostCreatedEvent(Object source, BlogPost blogPost, User author) {
        super(source);
        this.blogPost = blogPost;
        this.author = author;
        this.createdAt = LocalDateTime.now();
    }

    public BlogPost getBlogPost() {
        return blogPost;
    }

    public User getAuthor() {
        return author;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "BlogPostCreatedEvent{" +
                "blogPostId=" + blogPost.getId() +
                ", title='" + blogPost.getTitle() + '\'' +
                ", authorEmail=" + (author != null ? author.getEmail() : "unknown") +
                ", createdAt=" + createdAt +
                '}';
    }
}
