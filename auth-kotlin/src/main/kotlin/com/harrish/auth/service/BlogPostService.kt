package com.harrish.auth.service

import com.harrish.auth.dto.BlogPostResponse
import com.harrish.auth.dto.CreateBlogPostRequest
import com.harrish.auth.dto.UpdateBlogPostRequest
import com.harrish.auth.dto.UserDto
import com.harrish.auth.exception.BlogPostNotFoundException
import com.harrish.auth.exception.UserNotFoundException
import com.harrish.auth.model.BlogPost
import com.harrish.auth.model.User
import com.harrish.auth.repository.BlogPostRepository
import com.harrish.auth.repository.UserRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BlogPostService(
    private val blogPostRepository: BlogPostRepository,
    private val userRepository: UserRepository
) {

    @Transactional(readOnly = true)
    fun getAllBlogPosts(pageable: Pageable): Page<BlogPostResponse> {
        return blogPostRepository.findAll(pageable)
            .map { mapToResponse(it) }
    }

    @Transactional(readOnly = true)
    fun getBlogPostsByUser(userId: Long): List<BlogPostResponse> {
        val user = userRepository.findById(userId)
            .orElseThrow { UserNotFoundException() }

        return blogPostRepository.findByCreatedByOrderByCreatedAtDesc(user)
            .map { mapToResponse(it) }
    }

    @Transactional(readOnly = true)
    fun getBlogPostById(id: Long): BlogPostResponse {
        val blogPost = blogPostRepository.findById(id)
            .orElseThrow { BlogPostNotFoundException() }

        return mapToResponse(blogPost)
    }

    @Transactional
    fun createBlogPost(request: CreateBlogPostRequest): BlogPostResponse {
        val blogPost = BlogPost(
            title = request.title,
            content = request.content
        )

        val savedBlogPost = blogPostRepository.save(blogPost)

        return mapToResponse(savedBlogPost)
    }

    @Transactional
    fun updateBlogPost(id: Long, request: UpdateBlogPostRequest): BlogPostResponse {
        val existingBlogPost = blogPostRepository.findById(id)
            .orElseThrow { BlogPostNotFoundException() }

        val updatedBlogPost = BlogPost(
            id = existingBlogPost.id,
            title = request.title,
            content = request.content
        )

        val savedBlogPost = blogPostRepository.save(updatedBlogPost)

        return mapToResponse(savedBlogPost)
    }

    @Transactional
    fun deleteBlogPost(id: Long) {
        val blogPost = blogPostRepository.findById(id)
            .orElseThrow { BlogPostNotFoundException() }

        blogPostRepository.delete(blogPost)
    }

    private fun mapToResponse(blogPost: BlogPost): BlogPostResponse {
        return BlogPostResponse(
            id = blogPost.id,
            title = blogPost.title,
            content = blogPost.content,
            createdAt = blogPost.createdAt,
            updatedAt = blogPost.updatedAt,
            createdBy = mapToUserDto(blogPost.createdBy),
            updatedBy = mapToUserDto(blogPost.updatedBy)
        )
    }

    private fun mapToUserDto(user: User?): UserDto? {
        return user?.let {
            UserDto(
                id = it.id,
                firstName = it.firstName,
                lastName = it.lastName,
                email = it.email
            )
        }
    }

    private fun getCurrentUser(): User {
        val authentication = SecurityContextHolder.getContext().authentication
        val email = authentication.name

        return userRepository.findByEmail(email)
            .orElseThrow { UserNotFoundException() }
    }

    fun isBlogPostCreator(blogPostId: Long): Boolean {
        val currentUser = getCurrentUser()
        val blogPost = blogPostRepository.findById(blogPostId)
            .orElseThrow { BlogPostNotFoundException() }

        return blogPost.createdBy?.id == currentUser.id
    }
}
