package com.harrish.auth.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateBlogPostRequest(
    @field:NotBlank(message = "Title is required")
    @field:Size(min = 3, max = 255, message = "Title must be between 3 and 255 characters")
    val title: String,

    @field:NotBlank(message = "Content is required")
    @field:Size(min = 10, max = 10000, message = "Content must be between 10 and 10000 characters")
    val content: String
)
