package com.github.mirum8.jnscli.build.parameters.activechoises;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


class ActiveChoiceExtractorTest {

    private ActiveChoiceExtractor activeChoiceExtractor;

    @BeforeEach
    void setUp() {
        activeChoiceExtractor = new ActiveChoiceExtractor();
    }

    @Test
    void testGetActiveChoice_v1() throws Exception {
        Path path = Paths.get(getClass().getClassLoader().getResource("html/active_choice_v1.html").toURI());

        String html = Files.readString(path);

        String expectedUid = "2e82e00b-ffda-4594-80c6-388c54eababb";

        ActiveChoice activeChoice = activeChoiceExtractor.getActiveChoice("ACTIVE_CHOICE_2", html);

        Assertions.assertThat(expectedUid).isEqualTo(activeChoice.uid());
        Assertions.assertThat(activeChoice.referencedParameter()).containsExactlyInAnyOrder("MAIN_CHOICE", "MAIN_CHOICE_2");
    }

    @Test
    void testGetActiveChoice_v2() throws Exception {
        Path path = Paths.get(getClass().getClassLoader().getResource("html/active_choice_v2.html").toURI());
        String html = Files.readString(path);

        String expectedUid = "abcdef12-3456-7890-abcd-ef1234567890";

        ActiveChoice activeChoice = activeChoiceExtractor.getActiveChoice("ACTIVE_CHOICE", html);

        Assertions.assertThat(activeChoice.uid()).isEqualTo(expectedUid);
        Assertions.assertThat(activeChoice.referencedParameter()).containsExactlyInAnyOrder("REFERENCE_1", "REFERENCE_2");
    }

}

