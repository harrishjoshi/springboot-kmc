package com.harrish.auth.exception

import com.harrish.auth.exception.error.AuthErrorCode

class InvalidTokenException : BaseException(
    AuthErrorCode.AUTH_INVALID_TOKEN.messageKey,
    "Invalid token"
) {
    companion object {
        fun expired(): InvalidTokenException {
            return InvalidTokenException(AuthErrorCode.AUTH_TOKEN_EXPIRED.messageKey, "Token has expired")
        }
    }

    private constructor(errorCode: String, message: String) : super(errorCode, message)
}
