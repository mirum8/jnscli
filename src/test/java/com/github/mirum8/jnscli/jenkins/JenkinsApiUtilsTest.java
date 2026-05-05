package com.github.mirum8.jnscli.jenkins;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class JenkinsApiUtilsTest {

    @ParameterizedTest
    @CsvSource({
        "http://j/job/x,    api/json,   http://j/job/x/api/json",
        "http://j/job/x/,   api/json,   http://j/job/x/api/json",
        "http://j/job/x,    /api/json,  http://j/job/x/api/json",
        "http://j/job/x/,   /api/json,  http://j/job/x/api/json"
    })
    void joinPathNormalizesSeam(String base, String path, String expected) {
        assertThat(JenkinsApiUtils.joinPath(base, path)).isEqualTo(expected);
    }
}
