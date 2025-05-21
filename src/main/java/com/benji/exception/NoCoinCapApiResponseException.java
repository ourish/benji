package com.benji.exception;

public class NoCoinCapApiResponseException extends RuntimeException {
    public NoCoinCapApiResponseException(String message) {
        super(message);
    }
}
