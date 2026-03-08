package com.harrish.auth.exception.error

enum class UserErrorCode(val messageKey: String) {
    USER_NOT_FOUND("user.error.not_found"),
    USER_EMAIL_ALREADY_EXISTS("user.error.email_already_exists")
}
