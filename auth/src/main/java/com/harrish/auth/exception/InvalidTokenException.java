package com.harrish.auth.exception;

import com.harrish.auth.exception.error.AuthErrorCode;

public class InvalidTokenException extends BaseException {

    public InvalidTokenException() {
        super(AuthErrorCode.AUTH_INVALID_TOKEN.getMessageKey(),
                "Invalid token");
    }

    public static InvalidTokenException expired() {
        return new InvalidTokenException(AuthErrorCode.AUTH_TOKEN_EXPIRED.getMessageKey(),
                "Token has expired");
    }

    private InvalidTokenException(String errorCode, String message) {
        super(errorCode, message);
    }
}
