package com.harrish.auth.exception.error;

public enum BlogErrorCode {

    BLOG_POST_NOT_FOUND("blog.error.not_found");

    private final String messageKey;

    BlogErrorCode(String messageKey) {
        this.messageKey = messageKey;
    }

    public String getMessageKey() {
        return messageKey;
    }
}
