package com.github.mirum8.jnscli.list;

public record JobRow(
    int id,
    String status,
    String name
) {
    static Builder builder() {
        return new Builder();
    }

    static final class Builder {
        private int id;
        private String status = "";
        private String name;

        private Builder() {
        }

        public Builder id(int id) {
            this.id = id;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public JobRow build() {
            return new JobRow(id, status, name);
        }
    }
}
