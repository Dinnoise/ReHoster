package com.rehoster.model.run;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.rehoster.ai.config.AiMode;

public class RunConfig {
    private List<String> legacyCommand;
    private Path workingDirectory;
    private Map<String, String> envOverrides;
    private int timeoutSeconds;
    private Path outputDirectory;
    private boolean aiEnabled;
    private AiMode aiMode;
    private String aiModel;
    private int aiTimeoutSeconds;
    private boolean aiFallbackEnabled;

    public RunConfig() {
        this.legacyCommand = new ArrayList<>();
        this.envOverrides = new HashMap<>();
        this.timeoutSeconds = 60;
        this.aiMode = AiMode.OFF;
        this.aiTimeoutSeconds = 0; // 0 means use AiConfig default (120s)
        this.aiFallbackEnabled = true;
    }

    public List<String> getLegacyCommand() {
        return legacyCommand;
    }

    public void setLegacyCommand(List<String> legacyCommand) {
        this.legacyCommand = legacyCommand;
    }

    public Path getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(Path workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public Map<String, String> getEnvOverrides() {
        return envOverrides;
    }

    public void setEnvOverrides(Map<String, String> envOverrides) {
        this.envOverrides = envOverrides;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public Path getOutputDirectory() {
        return outputDirectory;
    }

    public void setOutputDirectory(Path outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public boolean isAiEnabled() {
        return aiEnabled;
    }

    public void setAiEnabled(boolean aiEnabled) {
        this.aiEnabled = aiEnabled;
    }

    public AiMode getAiMode() {
        return aiMode;
    }

    public void setAiMode(AiMode aiMode) {
        this.aiMode = aiMode;
    }

    public String getAiModel() {
        return aiModel;
    }

    public void setAiModel(String aiModel) {
        this.aiModel = aiModel;
    }

    public int getAiTimeoutSeconds() {
        return aiTimeoutSeconds;
    }

    public void setAiTimeoutSeconds(int aiTimeoutSeconds) {
        this.aiTimeoutSeconds = aiTimeoutSeconds;
    }

    public boolean isAiFallbackEnabled() {
        return aiFallbackEnabled;
    }

    public void setAiFallbackEnabled(boolean aiFallbackEnabled) {
        this.aiFallbackEnabled = aiFallbackEnabled;
    }
}
