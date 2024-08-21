package com.github.mirum8.jnscli.build.parameters;

import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ParameterPrompterRegistry {
    private Map<String, ParameterPrompter> promptersByParameterType;
    private Map<String, DynamicReferencedParameterPrompter> dynamicPromptersByParameterType;

    ParameterPrompterRegistry(List<ParameterPrompter> prompters, List<DynamicReferencedParameterPrompter> dynamicPrompters) {
        Map<String, ParameterPrompter> promptersMap = new HashMap<>();
        for (var prompter : prompters) {
            prompter.applicableForTypes().forEach(paramType -> promptersMap.put(paramType, prompter));
        }
        promptersByParameterType = Map.copyOf(promptersMap);

        Map<String, DynamicReferencedParameterPrompter> dynamicPromptersMap = new HashMap<>();
        for (var prompter : dynamicPrompters) {
            prompter.applicableForTypes().forEach(paramType -> dynamicPromptersMap.put(paramType, prompter));
        }
        dynamicPromptersByParameterType = Map.copyOf(dynamicPromptersMap);
    }

    ParameterPrompter getStaticPrompter(String parameterType) {
        return Optional.ofNullable(promptersByParameterType.get(parameterType)).orElseThrow();
    }

    DynamicReferencedParameterPrompter getDynamicPrompter(String parameterType) {
        return Optional.ofNullable(dynamicPromptersByParameterType.get(parameterType)).orElseThrow();
    }

    Set<String> getStaticParameterTypes() {
        return Set.copyOf(promptersByParameterType.keySet());
    }

    Set<String> getDynamicParameterTypes() {
        return Set.copyOf(dynamicPromptersByParameterType.keySet());
    }
}
