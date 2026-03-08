package com.harrish.auth.dto

data class UserDto(
    val id: Long = 0,
    val firstName: String,
    val lastName: String,
    val email: String
)
