package com.financeapp.exception;

import lombok.Getter;

@Getter
public class InvalidCredentialsException extends RuntimeException {

    private final int remainingAttempts;

    public InvalidCredentialsException(int remainingAttempts) {
        super("Invalid credentials.");
        this.remainingAttempts = remainingAttempts;
    }
}
