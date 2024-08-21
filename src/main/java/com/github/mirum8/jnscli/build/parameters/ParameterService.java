package com.github.mirum8.jnscli.build.parameters;

import com.github.mirum8.jnscli.jenkins.WorkflowJob;
import com.github.mirum8.jnscli.jenkins.WorkflowJob.Property.ParameterDefinition;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class ParameterService {
    private final ParameterPrompterRegistry prompterRegistry;

    public ParameterService(ParameterPrompterRegistry prompterRegistry) {
        this.prompterRegistry = prompterRegistry;
    }

    /**
     * Prompts for parameters that are not provided in command line
     *
     * @param job        description from Jenkins
     * @param parameters list of parameters provided in command line in format key=value
     * @return map of parameters
     */
    public Map<String, String> prompt(WorkflowJob job, List<String> parameters) {
        List<ParameterDefinition> parameterDefinitions = job.property().stream()
            .map(WorkflowJob.Property::parameterDefinitions)
            .filter(Objects::nonNull)
            .flatMap(List::stream)
            .toList();

        Map<String, String> parametersFromInput = parameters != null
            ? parameters.stream()
            .map(keyValue -> keyValue.split("="))
            .collect(Collectors.toMap(split -> split[0], split -> split[1]))
            : Map.of();

        Map<String, String> result = new LinkedHashMap<>();

        parameterDefinitions.forEach(param -> {
            if (parametersFromInput.containsKey(param.name())) {
                result.put(param.name(), parametersFromInput.get(param.name()));
            } else if (prompterRegistry.getStaticParameterTypes().contains(param.type())) {
                result.put(param.name(), prompterRegistry.getStaticPrompter(param.type()).prompt(param));
            } else if (prompterRegistry.getDynamicParameterTypes().contains(param.type())) {
                result.put(param.name(), prompterRegistry.getDynamicPrompter(param.type()).prompt(job, param, result));
            } else {
                throw new IllegalArgumentException("The parameter with type " + param.type() + " is not supported");
            }
        });
        return result;
    }
}
