package com.harrish.auth.dto

import java.time.LocalDateTime

data class BlogPostResponse(
    val id: Long,
    val title: String,
    val content: String,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
    val createdBy: UserDto?,
    val updatedBy: UserDto?
)
