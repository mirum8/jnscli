package com.github.mirum8.jnscli.build;

import com.github.mirum8.jnscli.jenkins.PipelineAPI;
import com.github.mirum8.jnscli.jenkins.WorkflowRun;
import com.github.mirum8.jnscli.runner.ProgressBar;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;


public class BuildProgressBar implements ProgressBar {
    private static final long DEFAULT_STAGE_DURATION = 60000L;

    private final PipelineAPI pipelineAPI;
    private final String jobUrl;
    private final int buildNumber;
    private List<WorkflowRun.Stage> previousBuildStages;

    public BuildProgressBar(PipelineAPI pipelineAPI, String jobUrl, int buildNumber) {
        this.pipelineAPI = pipelineAPI;
        this.jobUrl = jobUrl;
        this.buildNumber = buildNumber;
    }

    @Override
    public List<String> running() {
        if (buildNumber == 1) {
            return runningMessageForFirstBuild();
        }
        return showProgressForStages(jobUrl, buildNumber);
    }

    @Override
    public int refreshIntervalMillis() {
        return 5000;
    }

    private List<String> runningMessageForFirstBuild() {
        WorkflowRun workflowRun = pipelineAPI.getJobBuildDescription(jobUrl, 1);
        return workflowRun.stages().stream()
            .map(stage -> getProgressBar(stage, DEFAULT_STAGE_DURATION))
            .toList();
    }

    private String getProgressBar(WorkflowRun.Stage stage, Long previousDuration) {
        if (stage.status().equals("SUCCESS")) {
            return PercentageBar.of(100, stage.name());
        } else {
            long percentage = (new Date().getTime() - stage.startTimeMillis()) * 100 / previousDuration;
            if (percentage > 100) {
                percentage = 99;
            }
            return PercentageBar.of((int) percentage, stage.name());
        }
    }

    private List<WorkflowRun.Stage> getPreviousBuildStages() {
        if (previousBuildStages == null) {
            previousBuildStages = pipelineAPI.getJobBuildDescription(jobUrl, buildNumber - 1).stages();
        }
        return previousBuildStages;
    }

    private String getZeroProgressBar(WorkflowRun.Stage stage) {
        return PercentageBar.of(0, stage.name());
    }

    private List<String> showProgressForStages(String jobName, int buildNumber) {
        List<String> initialProgressBar = getPreviousBuildStages().stream()
            .map(this::getZeroProgressBar)
            .toList();
        boolean reset = false;
        List<String> updatedProgressBars = new ArrayList<>(initialProgressBar);
        var workflowRun = pipelineAPI.getJobBuildDescription(jobName, buildNumber);
        for (int i = 0; i < workflowRun.stages().size(); i++) {
            if (previousBuildStages.size() <= i) {
                reset = true;
            }
            // if the stage name is different, reset the progress bars
            if (!reset && !previousBuildStages.get(i).name().equals(workflowRun.stages().get(i).name())) {
                updatedProgressBars = updatedProgressBars.subList(0, i + 1);
                reset = true;
            }
            long duration = !reset ? previousBuildStages.get(i).durationMillis() : DEFAULT_STAGE_DURATION;
            if (i < updatedProgressBars.size()) {
                updatedProgressBars.set(i, getProgressBar(workflowRun.stages().get(i), duration));
            } else {
                updatedProgressBars.add(getProgressBar(workflowRun.stages().get(i), duration));
            }
        }
        return updatedProgressBars;
    }

    @Override
    public List<String> completed() {
        return pipelineAPI.getJobBuildDescription(jobUrl, buildNumber).stages().stream()
            .map(stage -> PercentageBar.of(100, stage.name()))
            .toList();
    }

    @Override
    public List<String> failed() {
        return pipelineAPI.getJobBuildDescription(jobUrl, buildNumber).stages().stream()
            .map(this::getProgressBarOnError)
            .toList();
    }

    private String getProgressBarOnError(WorkflowRun.Stage stage) {
        return Objects.equals(stage.status(), "SUCCESS") ? PercentageBar.of(100, stage.name()) : PercentageBar.error(99, stage.name());
    }

}


