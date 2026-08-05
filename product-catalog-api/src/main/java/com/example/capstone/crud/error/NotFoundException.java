package com.example.capstone.crud.error;

import java.util.UUID;

public class NotFoundException extends RuntimeException {

    public NotFoundException(UUID id) {
        super("Product not found: " + id);
    }
}
