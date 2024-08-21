package com.github.mirum8.jnscli.model;

import com.github.mirum8.jnscli.context.JobType;

public record JobDescriptor(
    int id,
    String name,
    String url,
    JobType type,
    String alias
) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int id = 0;
        private String name;
        private String url;
        private JobType type = JobType.UNKNOWN;
        private String alias;

        public Builder id(int id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder type(JobType type) {
            this.type = type;
            return this;
        }

        public Builder alias(String alias) {
            this.alias = alias;
            return this;
        }

        public JobDescriptor build() {
            return new JobDescriptor(id, name, url, type, alias);
        }
    }
}
