package com.harrish.auth.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Objects;

@Entity
@Table(name = "blog_posts", indexes = {
    @Index(name = "idx_blog_post_created_by", columnList = "created_by_id"),
    @Index(name = "idx_blog_post_created_at", columnList = "createdAt")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class BlogPost extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 10000)
    private String content;
    
    // Domain methods for updating blog post
    public void updateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title cannot be null or blank");
        }
        this.title = title;
    }
    
    public void updateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Content cannot be null or blank");
        }
        this.content = content;
    }
    
    // Use only id for equals/hashCode to work properly with JPA proxies
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BlogPost blogPost)) return false;
        return id != null && Objects.equals(id, blogPost.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "BlogPost{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", createdAt=" + getCreatedAt() +
                ", updatedAt=" + getUpdatedAt() +
                '}';
    }
}
