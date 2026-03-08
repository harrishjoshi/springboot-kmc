package com.harrish.auth.dto

data class UserInfoResponse(
    val message: String,
    val username: String,
    val authorities: Collection<*>
)
