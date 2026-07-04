package com.financeapp.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * DESIGN PATTERN: Custom Exception Pattern
 * Centralized exception handling across microservices
 */
@Getter
@AllArgsConstructor
public class ServiceException extends RuntimeException {
    private final String errorCode;
    private final int statusCode;

    public ServiceException(String message, String errorCode, int statusCode) {
        super(message);
        this.errorCode = errorCode;
        this.statusCode = statusCode;
    }

    public ServiceException(String message, Throwable cause, String errorCode, int statusCode) {
        super(message, cause);
        this.errorCode = errorCode;
        this.statusCode = statusCode;
    }
}
