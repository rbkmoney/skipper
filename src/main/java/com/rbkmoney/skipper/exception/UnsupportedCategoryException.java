package com.rbkmoney.skipper.exception;

public class UnsupportedCategoryException extends RuntimeException {

    public UnsupportedCategoryException() {
        super();
    }

    public UnsupportedCategoryException(String message) {
        super(message);
    }

    public UnsupportedCategoryException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnsupportedCategoryException(Throwable cause) {
        super(cause);
    }

    protected UnsupportedCategoryException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
