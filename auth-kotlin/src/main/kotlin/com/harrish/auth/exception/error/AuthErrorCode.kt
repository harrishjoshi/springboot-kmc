package com.harrish.auth.exception.error

enum class AuthErrorCode(val messageKey: String) {
    AUTH_BAD_CREDENTIALS("auth.error.bad_credentials"),
    AUTH_INVALID_TOKEN("auth.error.invalid_token"),
    AUTH_TOKEN_EXPIRED("auth.error.token_expired"),
    AUTH_ACCESS_DENIED("auth.error.access_denied")
}
