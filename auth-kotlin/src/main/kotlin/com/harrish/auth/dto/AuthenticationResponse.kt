package com.harrish.auth.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class AuthenticationResponse(
    @JsonProperty("access_token")
    val accessToken: String,

    @JsonProperty("refresh_token")
    val refreshToken: String,

    @JsonProperty("token_type")
    val tokenType: String = TOKEN_TYPE,

    @JsonProperty("expires_in")
    val expiresIn: Long
) {
    companion object {
        private const val TOKEN_TYPE = "Bearer"
    }
}
