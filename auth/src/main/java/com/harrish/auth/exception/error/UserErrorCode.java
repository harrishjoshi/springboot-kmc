package com.harrish.auth.exception.error;

public enum UserErrorCode {

    USER_NOT_FOUND("user.error.not_found"),
    USER_EMAIL_ALREADY_EXISTS("user.error.email_already_exists");

    private final String messageKey;

    UserErrorCode(String messageKey) {
        this.messageKey = messageKey;
    }

    public String getMessageKey() {
        return messageKey;
    }
}
