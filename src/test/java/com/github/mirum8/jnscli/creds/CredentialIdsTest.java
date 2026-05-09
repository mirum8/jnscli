package com.github.mirum8.jnscli.creds;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CredentialIdsTest {

    @ParameterizedTest
    @ValueSource(strings = {"deploy-bot", "ci_bot", "api.key", "ABC123", "x"})
    void acceptsAllowedIds(String id) {
        assertThat(CredentialIds.validate(id)).isEqualTo(id);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "../../etc/passwd",
        "foo/bar",
        "with space",
        "../escape",
        "/absolute",
        "with$dollar",
        "back\\slash"
    })
    void rejectsIdsWithUnsafeCharacters(String id) {
        assertThatThrownBy(() -> CredentialIds.validate(id))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining(id);
    }

    @Test
    void rejectsNullId() {
        assertThatThrownBy(() -> CredentialIds.validate(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsBlankId() {
        assertThatThrownBy(() -> CredentialIds.validate("   "))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
