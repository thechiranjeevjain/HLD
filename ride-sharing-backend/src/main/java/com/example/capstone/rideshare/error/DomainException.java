package com.example.capstone.rideshare.error;

public class DomainException extends RuntimeException {

    public DomainException(String message) {
        super(message);
    }
}
