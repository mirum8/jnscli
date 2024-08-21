package com.github.mirum8.jnscli.list;

public record JobRow(
    int id,
    Symbol color,
    String name
) {
    static Builder builder() {
        return new Builder();
    }

    static final class Builder {
        private int id;
        private Symbol color = new Symbol.Single("N");
        private String name;

        private Builder() {}

        public Builder id(int id) {
            this.id = id;
            return this;
        }

        public Builder color(Symbol color) {
            this.color = color;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public JobRow build() {
            return new JobRow(id, color, name);
        }
    }
}
