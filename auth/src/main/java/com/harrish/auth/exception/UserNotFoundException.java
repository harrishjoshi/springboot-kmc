package com.harrish.auth.exception;

import com.harrish.auth.exception.error.UserErrorCode;

public class UserNotFoundException extends BaseException {

    public UserNotFoundException() {
        super(UserErrorCode.USER_NOT_FOUND.getMessageKey(),
                "User not found");
    }
}
