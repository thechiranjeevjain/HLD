package com.example.capstone.shortener.link;

public class ShortLinkNotFoundException extends RuntimeException {

    public ShortLinkNotFoundException(String code) {
        super("Short link not found or inactive: " + code);
    }
}
