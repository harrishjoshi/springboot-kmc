package com.harrish.auth.exception.error;

public enum GenericErrorCode {

    SOMETHING_WENT_WRONG("generic.error.something_went_wrong");

    private final String messageKey;

    GenericErrorCode(String messageKey) {
        this.messageKey = messageKey;
    }

    public String getMessageKey() {
        return messageKey;
    }
}
