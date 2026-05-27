package com.trung.orderservice.exception;

public class BadRequestException extends Exception {
    public BadRequestException(String message) {
        super(message);
    }
}

