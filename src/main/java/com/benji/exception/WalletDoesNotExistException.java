package com.benji.exception;

public class WalletDoesNotExistException extends RuntimeException {
    public WalletDoesNotExistException(String message) {
        super(message);
    }
}
