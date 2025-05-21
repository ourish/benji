package com.benji.exception;

public class InvalidCoinCapApiKeyException extends RuntimeException {
    public InvalidCoinCapApiKeyException(String message) {
        super(message);
    }
}
