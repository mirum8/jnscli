package com.github.mirum8.jnscli.build.parameters;

import com.github.mirum8.jnscli.jenkins.WorkflowJob;
import com.github.mirum8.jnscli.jenkins.WorkflowJob.Property.ParameterDefinition;
import com.github.mirum8.jnscli.shell.ShellPrinter;
import com.github.mirum8.jnscli.shell.Theme;
import com.github.mirum8.jnscli.util.Strings;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class ParameterService {
    private static final String FILE_PARAMETER_TYPE = "FileParameterDefinition";

    private final ParameterPrompterRegistry prompterRegistry;
    private final ShellPrinter shellPrinter;
    private final Theme theme;

    public ParameterService(ParameterPrompterRegistry prompterRegistry, ShellPrinter shellPrinter, Theme theme) {
        this.prompterRegistry = prompterRegistry;
        this.shellPrinter = shellPrinter;
        this.theme = theme;
    }

    public Map<String, String> prompt(WorkflowJob job, List<String> parameters, boolean useDefaults) {
        List<ParameterDefinition> parameterDefinitions = job.property().stream()
            .map(WorkflowJob.Property::parameterDefinitions)
            .filter(Objects::nonNull)
            .flatMap(List::stream)
            .toList();

        Map<String, String> parametersFromInput = parameters != null
            ? parameters.stream()
            .map(ParameterService::parseKeyValue)
            .collect(Collectors.toMap(kv -> kv[0], kv -> kv[1]))
            : Map.of();

        Map<String, String> result = new LinkedHashMap<>();

        boolean willPrompt = !useDefaults && parameterDefinitions.stream()
            .anyMatch(p -> !parametersFromInput.containsKey(p.name()));
        if (willPrompt) {
            shellPrinter.println(theme.header("Parameters"));
        }

        parameterDefinitions.forEach(param -> resolveParameter(job, param, parametersFromInput, useDefaults, result));
        return result;
    }

    private void resolveParameter(WorkflowJob job, ParameterDefinition param, Map<String, String> parametersFromInput, boolean useDefaults, Map<String, String> result) {
        if (parametersFromInput.containsKey(param.name())) {
            result.put(param.name(), parametersFromInput.get(param.name()));
            return;
        }
        if (useDefaults) {
            applyDefault(param, result);
            return;
        }
        if (prompterRegistry.getStaticParameterTypes().contains(param.type())) {
            result.put(param.name(), prompterRegistry.getStaticPrompter(param.type()).prompt(param));
            return;
        }
        if (prompterRegistry.getDynamicParameterTypes().contains(param.type())) {
            result.put(param.name(), prompterRegistry.getDynamicPrompter(param.type()).prompt(job, param, result));
            return;
        }
        throw new IllegalArgumentException("The parameter with type " + param.type() + " is not supported");
    }

    private static void applyDefault(ParameterDefinition param, Map<String, String> result) {
        if (FILE_PARAMETER_TYPE.equals(param.type())) {
            throw new IllegalArgumentException("--defaults cannot be used with file parameter '" + param.name() + "'; specify -p " + param.name() + "=<path> explicitly");
        }
        var dpv = param.defaultParameterValue();
        if (dpv != null && dpv.value() != null) {
            result.put(param.name(), param.defaultValue());
        }
    }

    private static String[] parseKeyValue(String keyValue) {
        String[] parts = Strings.splitOnFirst(keyValue, '=');
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid parameter '" + keyValue + "', expected key=value");
        }
        return parts;
    }
}
