package com.mt.aggregator.exception;

public class UpstreamInterruptedException extends RuntimeException {

    public UpstreamInterruptedException(String message, Throwable cause) {
        super(message, cause);
    }
}
