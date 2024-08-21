package com.github.mirum8.jnscli.context;

public enum JobType {
    WORKFLOW("org.jenkinsci.plugins.workflow.job.WorkflowJob"),
    FREESTYLE("hudson.model.FreeStyleProject"),
    FOLDER("com.cloudbees.hudson.plugins.folder.Folder"),
    MULTI_CONF("hudson.matrix.MatrixProject"),
    MULTI_BRANCH("org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject"),
    UNKNOWN("unknown");

    private final String value;

    JobType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static JobType fromName(String name) {
        for (JobType jobType : JobType.values()) {
            if (jobType.getValue().equals(name)) {
                return jobType;
            }
        }
        return UNKNOWN;
    }
}
