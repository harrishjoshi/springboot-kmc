package com.harrish.auth.exception

import com.harrish.auth.exception.error.UserErrorCode

class EmailAlreadyExistsException(email: String) : BaseException(
    UserErrorCode.USER_EMAIL_ALREADY_EXISTS.messageKey,
    "Email already exists: $email",
    email
)
