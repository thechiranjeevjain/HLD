package com.example.capstone.shortener.link;

import java.security.SecureRandom;
import java.util.function.Predicate;
import org.springframework.stereotype.Component;

@Component
public class UrlCodeGenerator {

    private static final char[] ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
    private static final int CODE_LENGTH = 8;
    private final SecureRandom random = new SecureRandom();

    public String uniqueCode(Predicate<String> exists) {
        for (int attempt = 0; attempt < 20; attempt++) {
            String code = nextCode();
            if (!exists.test(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Unable to allocate a unique short code");
    }

    String nextCode() {
        char[] chars = new char[CODE_LENGTH];
        for (int i = 0; i < CODE_LENGTH; i++) {
            chars[i] = ALPHABET[random.nextInt(ALPHABET.length)];
        }
        return new String(chars);
    }
}
