package com.harrish.auth.controller;

import com.harrish.auth.dto.BlogPostResponse;
import com.harrish.auth.dto.CreateBlogPostRequest;
import com.harrish.auth.dto.UpdateBlogPostRequest;
import com.harrish.auth.service.BlogPostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

import static net.logstash.logback.argument.StructuredArguments.kv;

@RestController
@RequestMapping("/api/v1/blog-posts")
@Tag(name = "Blog Posts", description = "Blog post management API")
class BlogPostController {

    private static final Logger log = LoggerFactory.getLogger(BlogPostController.class);

    private final BlogPostService blogPostService;

    BlogPostController(BlogPostService blogPostService) {
        this.blogPostService = blogPostService;
    }

    @Operation(
            summary = "Get all blog posts",
            description = "Returns a paginated list of all blog posts sorted by creation date",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved blog posts",
                    content = @Content(schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token is missing or invalid",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    ResponseEntity<Page<BlogPostResponse>> getAllBlogPosts(
            @PageableDefault(sort = "createdAt") Pageable pageable
    ) {
        log.info("GET /api/v1/blog-posts", 
                kv("method", "GET"),
                kv("path", "/api/v1/blog-posts"),
                kv("page", pageable.getPageNumber()),
                kv("size", pageable.getPageSize()));
        
        Page<BlogPostResponse> response = blogPostService.getAllBlogPosts(pageable);
        
        log.info("Blog posts retrieved successfully", 
                kv("path", "/api/v1/blog-posts"),
                kv("status", 200),
                kv("totalElements", response.getTotalElements()));
        
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get blog posts by user ID",
            description = "Returns all blog posts created by a specific user",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved blog posts",
                    content = @Content(schema = @Schema(implementation = List.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token is missing or invalid",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/user/{userId}")
    ResponseEntity<List<BlogPostResponse>> getBlogPostsByUser(@PathVariable Long userId) {
        log.info("GET /api/v1/blog-posts/user/{userId}", 
                kv("method", "GET"),
                kv("path", "/api/v1/blog-posts/user/" + userId),
                kv("userId", userId));
        
        List<BlogPostResponse> response = blogPostService.getBlogPostsByUser(userId);
        
        log.info("User blog posts retrieved successfully", 
                kv("path", "/api/v1/blog-posts/user/" + userId),
                kv("status", 200),
                kv("count", response.size()));
        
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get a blog post by ID",
            description = "Returns a blog post by its ID",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved blog post",
                    content = @Content(schema = @Schema(implementation = BlogPostResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token is missing or invalid",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Blog post not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{id}")
    ResponseEntity<BlogPostResponse> getBlogPostById(@PathVariable Long id) {
        log.info("GET /api/v1/blog-posts/{id}", 
                kv("method", "GET"),
                kv("path", "/api/v1/blog-posts/" + id),
                kv("blogPostId", id));
        
        BlogPostResponse response = blogPostService.getBlogPostById(id);
        
        log.info("Blog post retrieved successfully", 
                kv("path", "/api/v1/blog-posts/" + id),
                kv("status", 200));
        
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Create a new blog post",
            description = "Creates a new blog post for the authenticated user",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Blog post created successfully",
                    content = @Content(schema = @Schema(implementation = BlogPostResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token is missing or invalid",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    ResponseEntity<BlogPostResponse> createBlogPost(@Valid @RequestBody CreateBlogPostRequest request) {
        log.info("POST /api/v1/blog-posts", 
                kv("method", "POST"),
                kv("path", "/api/v1/blog-posts"));
        
        BlogPostResponse response = blogPostService.createBlogPost(request);
        
        // Build Location header with URI of the created resource (REST best practice)
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.getId())
                .toUri();
        
        log.info("Blog post created successfully", 
                kv("path", "/api/v1/blog-posts"),
                kv("status", 201),
                kv("blogPostId", response.getId()),
                kv("location", location.toString()));
        
        return ResponseEntity.created(location).body(response);
    }

    @Operation(
            summary = "Update a blog post",
            description = "Updates an existing blog post. Only the creator of the post or an admin can update it.",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Blog post updated successfully",
                    content = @Content(schema = @Schema(implementation = BlogPostResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token is missing or invalid",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - User is not the creator or an admin",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Blog post not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping("/{id}")
    @PreAuthorize("@blogPostService.isBlogPostCreator(#id) or hasRole('ADMIN')")
    ResponseEntity<BlogPostResponse> updateBlogPost(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBlogPostRequest request
    ) {
        log.info("PUT /api/v1/blog-posts/{id}", 
                kv("method", "PUT"),
                kv("path", "/api/v1/blog-posts/" + id),
                kv("blogPostId", id));
        
        BlogPostResponse response = blogPostService.updateBlogPost(id, request);
        
        log.info("Blog post updated successfully", 
                kv("path", "/api/v1/blog-posts/" + id),
                kv("status", 200),
                kv("blogPostId", id));
        
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Delete a blog post",
            description = "Deletes an existing blog post. Only the creator of the post or an admin can delete it.",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Blog post deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token is missing or invalid",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - User is not the creator or an admin",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Blog post not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("@blogPostService.isBlogPostCreator(#id) or hasRole('ADMIN')")
    ResponseEntity<Void> deleteBlogPost(@PathVariable Long id) {
        log.info("DELETE /api/v1/blog-posts/{id}", 
                kv("method", "DELETE"),
                kv("path", "/api/v1/blog-posts/" + id),
                kv("blogPostId", id));
        
        blogPostService.deleteBlogPost(id);
        
        log.info("Blog post deleted successfully", 
                kv("path", "/api/v1/blog-posts/" + id),
                kv("status", 204),
                kv("blogPostId", id));
        
        return ResponseEntity.noContent().build();
    }
}
