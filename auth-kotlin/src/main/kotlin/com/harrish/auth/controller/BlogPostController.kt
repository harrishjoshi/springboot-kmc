package com.harrish.auth.controller

import com.harrish.auth.dto.BlogPostResponse
import com.harrish.auth.dto.CreateBlogPostRequest
import com.harrish.auth.dto.UpdateBlogPostRequest
import com.harrish.auth.service.BlogPostService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/blog-posts")
@Tag(name = "Blog Posts", description = "Blog post management API")
class BlogPostController(
    private val blogPostService: BlogPostService
) {

    @Operation(
        summary = "Get all blog posts",
        description = "Returns a paginated list of all blog posts sorted by creation date",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully retrieved blog posts",
            content = [Content(schema = Schema(implementation = Page::class))]),
        ApiResponse(responseCode = "401", description = "Unauthorized - JWT token is missing or invalid",
            content = [Content()])
    ])
    @GetMapping
    fun getAllBlogPosts(@PageableDefault(sort = "createdAt") pageable: Pageable): ResponseEntity<Page<BlogPostResponse>> {
        return ResponseEntity.ok(blogPostService.getAllBlogPosts(pageable))
    }

    @Operation(
        summary = "Get blog posts by user ID",
        description = "Returns all blog posts created by a specific user",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully retrieved blog posts",
            content = [Content(schema = Schema(implementation = List::class))]),
        ApiResponse(responseCode = "401", description = "Unauthorized - JWT token is missing or invalid",
            content = [Content()]),
        ApiResponse(responseCode = "404", description = "User not found",
            content = [Content()])
    ])
    @GetMapping("/user/{userId}")
    fun getBlogPostsByUser(@PathVariable userId: Long): ResponseEntity<List<BlogPostResponse>> {
        return ResponseEntity.ok(blogPostService.getBlogPostsByUser(userId))
    }

    @Operation(
        summary = "Get a blog post by ID",
        description = "Returns a blog post by its ID",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully retrieved blog post",
            content = [Content(schema = Schema(implementation = BlogPostResponse::class))]),
        ApiResponse(responseCode = "401", description = "Unauthorized - JWT token is missing or invalid",
            content = [Content()]),
        ApiResponse(responseCode = "404", description = "Blog post not found",
            content = [Content()])
    ])
    @GetMapping("/{id}")
    fun getBlogPostById(@PathVariable id: Long): ResponseEntity<BlogPostResponse> {
        return ResponseEntity.ok(blogPostService.getBlogPostById(id))
    }

    @Operation(
        summary = "Create a new blog post",
        description = "Creates a new blog post for the authenticated user",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "201", description = "Blog post created successfully",
            content = [Content(schema = Schema(implementation = BlogPostResponse::class))]),
        ApiResponse(responseCode = "401", description = "Unauthorized - JWT token is missing or invalid",
            content = [Content()]),
        ApiResponse(responseCode = "400", description = "Invalid input",
            content = [Content()])
    ])
    @PostMapping
    fun createBlogPost(@Valid @RequestBody request: CreateBlogPostRequest): ResponseEntity<BlogPostResponse> {
        val response = blogPostService.createBlogPost(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @Operation(
        summary = "Update a blog post",
        description = "Updates an existing blog post. Only the creator of the post or an admin can update it.",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Blog post updated successfully",
            content = [Content(schema = Schema(implementation = BlogPostResponse::class))]),
        ApiResponse(responseCode = "401", description = "Unauthorized - JWT token is missing or invalid",
            content = [Content()]),
        ApiResponse(responseCode = "403", description = "Forbidden - User is not the creator or an admin",
            content = [Content()]),
        ApiResponse(responseCode = "404", description = "Blog post not found",
            content = [Content()])
    ])
    @PutMapping("/{id}")
    @PreAuthorize("@blogPostService.isBlogPostCreator(#id) or hasRole('ADMIN')")
    fun updateBlogPost(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateBlogPostRequest
    ): ResponseEntity<BlogPostResponse> {
        return ResponseEntity.ok(blogPostService.updateBlogPost(id, request))
    }

    @Operation(
        summary = "Delete a blog post",
        description = "Deletes an existing blog post. Only the creator of the post or an admin can delete it.",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "204", description = "Blog post deleted successfully"),
        ApiResponse(responseCode = "401", description = "Unauthorized - JWT token is missing or invalid",
            content = [Content()]),
        ApiResponse(responseCode = "403", description = "Forbidden - User is not the creator or an admin",
            content = [Content()]),
        ApiResponse(responseCode = "404", description = "Blog post not found",
            content = [Content()])
    ])
    @DeleteMapping("/{id}")
    @PreAuthorize("@blogPostService.isBlogPostCreator(#id) or hasRole('ADMIN')")
    fun deleteBlogPost(@PathVariable id: Long): ResponseEntity<Void> {
        blogPostService.deleteBlogPost(id)
        return ResponseEntity.noContent().build()
    }
}
