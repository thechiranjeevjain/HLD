package com.example.capstone.shortener.link;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class UrlCodeGeneratorTest {

    private final UrlCodeGenerator generator = new UrlCodeGenerator();

    @Test
    void generatedCodesAreEightCharacterBase62Values() {
        String code = generator.nextCode();

        assertThat(code).hasSize(8);
        assertThat(code).matches("[0-9A-Za-z]{8}");
    }

    @Test
    void uniqueCodeSkipsExistingValues() {
        Set<String> seen = new HashSet<>();

        for (int i = 0; i < 100; i++) {
            String code = generator.uniqueCode(seen::contains);
            assertThat(seen).doesNotContain(code);
            seen.add(code);
        }
    }
}
