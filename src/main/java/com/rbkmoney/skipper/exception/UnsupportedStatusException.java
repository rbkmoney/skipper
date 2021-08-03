package com.rbkmoney.skipper.exception;

public class UnsupportedStatusException extends RuntimeException {

    public UnsupportedStatusException() {
        super();
    }

    public UnsupportedStatusException(String message) {
        super(message);
    }

    public UnsupportedStatusException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnsupportedStatusException(Throwable cause) {
        super(cause);
    }

    protected UnsupportedStatusException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
