package com.benji.exception;

public class AssetDoesNotExistException extends RuntimeException {
    public AssetDoesNotExistException(String message) {
        super(message);
    }
}
