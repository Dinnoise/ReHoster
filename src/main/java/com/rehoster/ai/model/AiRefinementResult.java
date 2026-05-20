package com.rehoster.ai.model;

import java.util.ArrayList;
import java.util.List;

public class AiRefinementResult {
    private String dockerfile;
    private String dockerCompose;
    private List<String> summary;
    private List<String> warnings;
    private List<AiChange> appliedChanges;
    private double confidence;
    private boolean shouldApply;

    public AiRefinementResult() {
        this.summary = new ArrayList<>();
        this.warnings = new ArrayList<>();
        this.appliedChanges = new ArrayList<>();
    }

    public String getDockerfile() {
        return dockerfile;
    }

    public void setDockerfile(String dockerfile) {
        this.dockerfile = dockerfile;
    }

    public String getDockerCompose() {
        return dockerCompose;
    }

    public void setDockerCompose(String dockerCompose) {
        this.dockerCompose = dockerCompose;
    }

    public List<String> getSummary() {
        return summary;
    }

    public void setSummary(List<String> summary) {
        this.summary = summary;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    public List<AiChange> getAppliedChanges() {
        return appliedChanges;
    }

    public void setAppliedChanges(List<AiChange> appliedChanges) {
        this.appliedChanges = appliedChanges;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public boolean isShouldApply() {
        return shouldApply;
    }

    public void setShouldApply(boolean shouldApply) {
        this.shouldApply = shouldApply;
    }
}
