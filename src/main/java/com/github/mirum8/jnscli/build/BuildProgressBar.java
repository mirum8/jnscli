package com.github.mirum8.jnscli.build;

import com.github.mirum8.jnscli.jenkins.JenkinsAdapter;
import com.github.mirum8.jnscli.jenkins.WorkflowRun;
import com.github.mirum8.jnscli.runner.ProgressBar;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class BuildProgressBar implements ProgressBar {
    private static final long DEFAULT_STAGE_DURATION = 60000L;

    private final JenkinsAdapter jenkinsAdapter;
    private final String jobName;
    private final int buildNumber;
    private List<WorkflowRun.Stage> stagesOfPreviousBuild;

    public BuildProgressBar(JenkinsAdapter jenkinsAdapter, String jobName, int buildNumber) {
        this.jenkinsAdapter = jenkinsAdapter;
        this.jobName = jobName;
        this.buildNumber = buildNumber;
    }

    @Override
    public List<String> runningMessage() {
        if (buildNumber == 1) {
            return runningMessageForFirstBuild();
        }
        return showProgressForStages(jobName, buildNumber);
    }

    @Override
    public int refreshIntervalMillis() {
        return 5000;
    }

    private List<String> runningMessageForFirstBuild() {
        WorkflowRun workflowRun = jenkinsAdapter.getJobBuildDescription(jobName, 1);
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

    private List<WorkflowRun.Stage> getStagesOfPreviousBuild() {
        if (stagesOfPreviousBuild == null) {
            stagesOfPreviousBuild = jenkinsAdapter.getJobBuildDescription(jobName, buildNumber - 1).stages();
        }
        return stagesOfPreviousBuild;
    }

    private String getZeroProgressBar(WorkflowRun.Stage stage) {
        return PercentageBar.of(0, stage.name());
    }

    private List<String> showProgressForStages(String jobName, int buildNumber) {
        List<String> initialProgressBar = getStagesOfPreviousBuild().stream()
            .map(this::getZeroProgressBar)
            .toList();
        boolean reset = false;
        List<String> updatedProgressBars = new ArrayList<>(initialProgressBar);
        var workflowRun = jenkinsAdapter.getJobBuildDescription(jobName, buildNumber);
        for (int i = 0; i < workflowRun.stages().size(); i++) {
            if (stagesOfPreviousBuild.size() <= i) {
                reset = true;
            }
            WorkflowRun.Stage previousBuildStage = stagesOfPreviousBuild.get(i);
            // if the stage name is different, reset the progress bars
            if (!reset && !previousBuildStage.name().equals(workflowRun.stages().get(i).name())) {
                updatedProgressBars = updatedProgressBars.subList(0, i + 1);
                reset = true;
            }
            long duration = !reset ? previousBuildStage.durationMillis() : DEFAULT_STAGE_DURATION;
            if (i < updatedProgressBars.size()) {
                updatedProgressBars.set(i, getProgressBar(workflowRun.stages().get(i), duration));
            } else {
                updatedProgressBars.add(getProgressBar(workflowRun.stages().get(i), duration));
            }
        }
        return updatedProgressBars;
    }

}


