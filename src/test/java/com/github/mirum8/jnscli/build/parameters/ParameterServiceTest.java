package com.github.mirum8.jnscli.build.parameters;

import com.github.mirum8.jnscli.jenkins.WorkflowJob;
import com.github.mirum8.jnscli.jenkins.WorkflowJob.Property;
import com.github.mirum8.jnscli.jenkins.WorkflowJob.Property.ParameterDefinition;
import com.github.mirum8.jnscli.jenkins.WorkflowJob.Property.ParameterDefinition.DefaultParameterValue;
import com.github.mirum8.jnscli.shell.ShellPrinter;
import com.github.mirum8.jnscli.shell.TerminalCapabilities;
import com.github.mirum8.jnscli.shell.TestCapabilities;
import com.github.mirum8.jnscli.shell.Theme;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class ParameterServiceTest {

    @Test
    void useDefaultsReturnsDefaultsForAllParameters() {
        // given
        ParameterService service = serviceWithoutPrompters();
        WorkflowJob job = jobWithParameters(
            stringParam("env", "prod"),
            stringParam("region", "eu-west-1")
        );

        // when
        Map<String, String> actualParameters = service.prompt(job, List.of(), true);

        // then
        assertThat(actualParameters).containsExactly(
            Map.entry("env", "prod"),
            Map.entry("region", "eu-west-1")
        );
    }

    @Test
    void useDefaultsHonoursExplicitOverride() {
        // given
        ParameterService service = serviceWithoutPrompters();
        WorkflowJob job = jobWithParameters(
            stringParam("env", "prod"),
            stringParam("region", "eu-west-1")
        );

        // when
        Map<String, String> actualParameters = service.prompt(job, List.of("env=staging"), true);

        // then
        assertThat(actualParameters).containsExactly(
            Map.entry("env", "staging"),
            Map.entry("region", "eu-west-1")
        );
    }

    @Test
    void useDefaultsOmitsParametersWithoutDefault() {
        // given
        ParameterService service = serviceWithoutPrompters();
        WorkflowJob job = jobWithParameters(
            stringParam("env", "prod"),
            paramWithoutDefault("region", "StringParameterDefinition")
        );

        // when
        Map<String, String> actualParameters = service.prompt(job, List.of(), true);

        // then
        assertThat(actualParameters).containsExactly(Map.entry("env", "prod"));
    }

    @Test
    void useDefaultsRejectsFileParameter() {
        // given
        ParameterService service = serviceWithoutPrompters();
        WorkflowJob job = jobWithParameters(
            stringParam("env", "prod"),
            paramWithoutDefault("uploadedFile", "FileParameterDefinition")
        );

        // when / then
        List<String> noOverrides = List.of();
        assertThatThrownBy(() -> service.prompt(job, noOverrides, true))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("uploadedFile")
            .hasMessageContaining("--defaults");
    }

    @Test
    void useDefaultsAllowsFileParameterWhenSuppliedExplicitly() {
        // given
        ParameterService service = serviceWithoutPrompters();
        WorkflowJob job = jobWithParameters(
            stringParam("env", "prod"),
            paramWithoutDefault("uploadedFile", "FileParameterDefinition")
        );

        // when
        Map<String, String> actualParameters = service.prompt(job, List.of("uploadedFile=/tmp/upload.txt"), true);

        // then
        assertThat(actualParameters).containsExactly(
            Map.entry("env", "prod"),
            Map.entry("uploadedFile", "/tmp/upload.txt")
        );
    }

    @Test
    void useDefaultsFalseFallsBackToPrompter() {
        // given
        ParameterService service = new ParameterService(new ParameterPrompterRegistry(
            List.of(new FixedStringPrompter("dev")), List.of()
        ), mock(ShellPrinter.class), new Theme(disabledCaps()));
        WorkflowJob job = jobWithParameters(stringParam("env", "prod"));

        // when
        Map<String, String> actualParameters = service.prompt(job, List.of(), false);

        // then
        assertThat(actualParameters).containsExactly(Map.entry("env", "dev"));
    }

    private ParameterService serviceWithoutPrompters() {
        return new ParameterService(new ParameterPrompterRegistry(List.of(), List.of()), mock(ShellPrinter.class), new Theme(disabledCaps()));
    }

    private TerminalCapabilities disabledCaps() {
        return TestCapabilities.disabled();
    }

    private WorkflowJob jobWithParameters(ParameterDefinition... parameters) {
        return new WorkflowJob(
            "name", "name", "http://example/job", true, "blue",
            List.of(), null, List.of(),
            List.of(new Property(List.of(parameters))),
            1, "desc"
        );
    }

    private ParameterDefinition stringParam(String name, String defaultValue) {
        return new ParameterDefinition(
            new DefaultParameterValue(name, defaultValue),
            "", name, "StringParameterDefinition", List.of()
        );
    }

    private ParameterDefinition paramWithoutDefault(String name, String type) {
        return new ParameterDefinition(null, "", name, type, List.of());
    }

    private record FixedStringPrompter(String value) implements ParameterPrompter {
        @Override
        public String prompt(ParameterDefinition parameterDefinition) {
            return value;
        }

        @Override
        public Set<String> applicableForTypes() {
            return Set.of("StringParameterDefinition");
        }
    }
}
