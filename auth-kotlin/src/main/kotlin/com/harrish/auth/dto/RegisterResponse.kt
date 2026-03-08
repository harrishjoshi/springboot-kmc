package com.harrish.auth.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class RegisterResponse(
    @JsonProperty("message")
    val message: String = MESSAGE
) {
    companion object {
        const val MESSAGE = "User registered successfully"
    }
}
