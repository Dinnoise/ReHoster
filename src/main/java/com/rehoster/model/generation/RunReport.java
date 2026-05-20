package com.rehoster.model.generation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.rehoster.model.snapshot.Observation;

public class RunReport {
    private String runId;
    private Instant generatedAt;
    private boolean success;
    private String legacyCommand;
    private List<String> collectedData;
    private List<String> generatedArtifacts;
    private List<Observation> warnings;
    private String summary;
    private boolean aiEnabled;
    private String aiProvider;
    private String aiPrimaryModel;
    private boolean aiFallbackUsed;
    private boolean aiApplied;
    private double aiConfidence;
    private List<String> aiWarnings;

    public RunReport() {
        this.collectedData = new ArrayList<>();
        this.generatedArtifacts = new ArrayList<>();
        this.warnings = new ArrayList<>();
        this.aiWarnings = new ArrayList<>();
        this.generatedAt = Instant.now();
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(Instant generatedAt) {
        this.generatedAt = generatedAt;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getLegacyCommand() {
        return legacyCommand;
    }

    public void setLegacyCommand(String legacyCommand) {
        this.legacyCommand = legacyCommand;
    }

    public List<String> getCollectedData() {
        return collectedData;
    }

    public void setCollectedData(List<String> collectedData) {
        this.collectedData = collectedData;
    }

    public List<String> getGeneratedArtifacts() {
        return generatedArtifacts;
    }

    public void setGeneratedArtifacts(List<String> generatedArtifacts) {
        this.generatedArtifacts = generatedArtifacts;
    }

    public List<Observation> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<Observation> warnings) {
        this.warnings = warnings;
    }

    public void addWarning(Observation warning) {
        this.warnings.add(warning);
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public void addCollectedData(String data) {
        this.collectedData.add(data);
    }

    public void addGeneratedArtifact(String artifact) {
        this.generatedArtifacts.add(artifact);
    }

    public boolean isAiEnabled() {
        return aiEnabled;
    }

    public void setAiEnabled(boolean aiEnabled) {
        this.aiEnabled = aiEnabled;
    }

    public String getAiProvider() {
        return aiProvider;
    }

    public void setAiProvider(String aiProvider) {
        this.aiProvider = aiProvider;
    }

    public String getAiPrimaryModel() {
        return aiPrimaryModel;
    }

    public void setAiPrimaryModel(String aiPrimaryModel) {
        this.aiPrimaryModel = aiPrimaryModel;
    }

    public boolean isAiFallbackUsed() {
        return aiFallbackUsed;
    }

    public void setAiFallbackUsed(boolean aiFallbackUsed) {
        this.aiFallbackUsed = aiFallbackUsed;
    }

    public boolean isAiApplied() {
        return aiApplied;
    }

    public void setAiApplied(boolean aiApplied) {
        this.aiApplied = aiApplied;
    }

    public double getAiConfidence() {
        return aiConfidence;
    }

    public void setAiConfidence(double aiConfidence) {
        this.aiConfidence = aiConfidence;
    }

    public List<String> getAiWarnings() {
        return aiWarnings;
    }

    public void setAiWarnings(List<String> aiWarnings) {
        this.aiWarnings = aiWarnings;
    }
}
