package com.github.mirum8.jnscli.jenkins;

import java.util.List;
import java.util.Optional;

public record BuildInfo(
    Integer number,
    String displayName,
    Long timestamp,
    Long duration,
    String description,
    List<Action> actions,
    Status result
) implements Build {
    public Optional<String> startedBy() {
        return actions.stream()
            .filter(action -> action.causes != null && !action.causes.isEmpty())
            .findFirst()
            .map(action -> action.causes.getFirst().userId);
    }

    public List<Action.Parameter> parameters() {
        return actions.stream()
            .filter(action -> action.parameters != null)
            .findFirst()
            .map(action -> action.parameters)
            .orElse(List.of());
    }

    @Override
    public Integer id() {
        return number();
    }

    @Override
    public Status status() {
        return result();
    }

    public record Action(
        List<Parameter> parameters,
        List<Cause> causes
    ) {
        public record Parameter(
            String name,
            String value
        ) {
        }

        public record Cause(
            String _class,
            String shortDescription,
            String userId,
            String userName
        ) {
        }

        public record Build(
            int number,
            String url
        ) {
        }
    }
}
