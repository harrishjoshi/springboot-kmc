package com.harrish.auth.service;

import com.harrish.auth.dto.BlogPostResponse;
import com.harrish.auth.dto.CreateBlogPostRequest;
import com.harrish.auth.dto.UpdateBlogPostRequest;
import com.harrish.auth.dto.UserDto;
import com.harrish.auth.exception.BlogPostNotFoundException;
import com.harrish.auth.exception.UserNotFoundException;
import com.harrish.auth.model.BlogPost;
import com.harrish.auth.model.User;
import com.harrish.auth.repository.BlogPostRepository;
import com.harrish.auth.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BlogPostService {

    private final BlogPostRepository blogPostRepository;
    private final UserRepository userRepository;

    public BlogPostService(BlogPostRepository blogPostRepository, UserRepository userRepository) {
        this.blogPostRepository = blogPostRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Page<BlogPostResponse> getAllBlogPosts(Pageable pageable) {
        return blogPostRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public List<BlogPostResponse> getBlogPostsByUser(Long userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        return blogPostRepository.findByCreatedByOrderByCreatedAtDesc(user)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BlogPostResponse getBlogPostById(Long id) {
        var blogPost = blogPostRepository.findById(id)
                .orElseThrow(BlogPostNotFoundException::new);

        return mapToResponse(blogPost);
    }

    @Transactional
    public BlogPostResponse createBlogPost(CreateBlogPostRequest request) {
        var blogPost = BlogPost.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .build();

        var savedBlogPost = blogPostRepository.save(blogPost);

        return mapToResponse(savedBlogPost);
    }

    @Transactional
    public BlogPostResponse updateBlogPost(Long id, UpdateBlogPostRequest request) {
        var blogPost = blogPostRepository.findById(id)
                .orElseThrow(BlogPostNotFoundException::new);

        blogPost = BlogPost.builder()
                .id(blogPost.getId())
                .title(request.getTitle())
                .content(request.getContent())
                .build();

        var updatedBlogPost = blogPostRepository.save(blogPost);

        return mapToResponse(updatedBlogPost);
    }

    @Transactional
    public void deleteBlogPost(Long id) {
        var blogPost = blogPostRepository.findById(id)
                .orElseThrow(BlogPostNotFoundException::new);

        blogPostRepository.delete(blogPost);
    }

    private BlogPostResponse mapToResponse(BlogPost blogPost) {
        return BlogPostResponse.builder()
                .id(blogPost.getId())
                .title(blogPost.getTitle())
                .content(blogPost.getContent())
                .createdAt(blogPost.getCreatedAt())
                .updatedAt(blogPost.getUpdatedAt())
                .createdBy(mapToUserDto(blogPost.getCreatedBy()))
                .updatedBy(mapToUserDto(blogPost.getUpdatedBy()))
                .build();
    }

    private UserDto mapToUserDto(User user) {
        if (user == null) {
            return null;
        }

        return UserDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .build();
    }

    private User getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        var email = authentication.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(UserNotFoundException::new);
    }

    public boolean isBlogPostCreator(Long blogPostId) {
        var currentUser = getCurrentUser();
        var blogPost = blogPostRepository.findById(blogPostId)
                .orElseThrow(BlogPostNotFoundException::new);

        return blogPost.getCreatedBy() != null &&
                blogPost.getCreatedBy().getId().equals(currentUser.getId());
    }
}
