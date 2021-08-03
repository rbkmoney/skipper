package com.rbkmoney.skipper.exception;

public class UnsupportedStageException extends RuntimeException {

    public UnsupportedStageException() {
        super();
    }

    public UnsupportedStageException(String message) {
        super(message);
    }

    public UnsupportedStageException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnsupportedStageException(Throwable cause) {
        super(cause);
    }

    protected UnsupportedStageException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
