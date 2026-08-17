package com.example.capstone.ecommerce.error;

public class DomainException extends RuntimeException {

    public DomainException(String message) {
        super(message);
    }
}
