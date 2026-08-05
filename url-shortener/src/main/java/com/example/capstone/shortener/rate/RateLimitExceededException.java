package com.example.capstone.shortener.rate;

import java.time.Duration;

public class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException(int limit, Duration window) {
        super("Rate limit exceeded: " + limit + " requests per " + window.toSeconds() + " seconds");
    }
}
