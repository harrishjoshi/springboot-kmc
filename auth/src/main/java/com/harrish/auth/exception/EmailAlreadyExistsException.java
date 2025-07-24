package com.harrish.auth.exception;

import com.harrish.auth.exception.error.UserErrorCode;

public class EmailAlreadyExistsException extends BaseException {

    public EmailAlreadyExistsException(String email) {
        super(UserErrorCode.USER_EMAIL_ALREADY_EXISTS.getMessageKey(),
                "Email already exists: " + email, email);
    }
}
