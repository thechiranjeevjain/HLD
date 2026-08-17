package com.example.capstone.crud.error;

public class DuplicateSkuException extends RuntimeException {

    public DuplicateSkuException(String sku) {
        super("Product SKU already exists: " + sku);
    }
}
