package com.harrish.auth.exception

import com.harrish.auth.exception.error.UserErrorCode

class UserNotFoundException : BaseException(
    UserErrorCode.USER_NOT_FOUND.messageKey,
    "User not found"
)
