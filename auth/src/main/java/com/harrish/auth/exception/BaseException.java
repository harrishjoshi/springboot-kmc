package com.harrish.auth.exception;

public abstract class BaseException extends RuntimeException {

    private final String errorCode;
    private final Object[] params;

    public BaseException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.params = new Object[0];
    }

    public BaseException(String errorCode, String message, Object... params) {
        super(message);
        this.errorCode = errorCode;
        this.params = params;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Object[] getParams() {
        return params;
    }
}
