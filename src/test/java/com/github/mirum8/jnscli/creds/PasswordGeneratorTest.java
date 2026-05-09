package com.github.mirum8.jnscli.creds;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordGeneratorTest {

    private static final Pattern ALLOWED = Pattern.compile("^[A-Za-z0-9!@#$%^&*_\\-]+$");

    private final PasswordGenerator generator = new PasswordGenerator();

    @Test
    void generatesPasswordsOf24CharsWithAllowedAlphabet() {
        for (int i = 0; i < 100; i++) {
            String password = generator.generate();
            assertThat(password).hasSize(24);
            assertThat(ALLOWED.matcher(password).matches())
                .as("password '%s' uses only allowed chars", password)
                .isTrue();
        }
    }

    @Test
    void generatesDistinctPasswordsAcrossInvocations() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 50; i++) {
            seen.add(generator.generate());
        }
        assertThat(seen).hasSize(50);
    }
}
