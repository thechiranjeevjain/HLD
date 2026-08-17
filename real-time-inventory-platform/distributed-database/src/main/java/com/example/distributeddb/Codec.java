package com.example.distributeddb;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

final class Codec {
    private Codec() {
    }

    static String encode(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    static String decode(String token) {
        return new String(Base64.getDecoder().decode(token), StandardCharsets.UTF_8);
    }
}
