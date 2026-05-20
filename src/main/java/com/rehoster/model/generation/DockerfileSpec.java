package com.rehoster.model.generation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DockerfileSpec {
    private String baseImage;
    private String workDir;
    private List<String> copyRules;
    private Map<String, String> envVars;
    private List<String> entrypoint;
    private List<String> cmd;
    private List<Integer> exposePorts;
    private List<String> runCommands;
    private List<String> postCopyRunCommands;
    private List<String> healthcheckCmd;
    private String healthcheckInterval;
    private String healthcheckTimeout;
    private String healthcheckStartPeriod;
    private Integer healthcheckRetries;

    public DockerfileSpec() {
        this.copyRules = new ArrayList<>();
        this.envVars = new HashMap<>();
        this.entrypoint = new ArrayList<>();
        this.cmd = new ArrayList<>();
        this.exposePorts = new ArrayList<>();
        this.runCommands = new ArrayList<>();
        this.postCopyRunCommands = new ArrayList<>();
        this.healthcheckCmd = new ArrayList<>();
    }

    public String getBaseImage() {
        return baseImage;
    }

    public void setBaseImage(String baseImage) {
        this.baseImage = baseImage;
    }

    public String getWorkDir() {
        return workDir;
    }

    public void setWorkDir(String workDir) {
        this.workDir = workDir;
    }

    public List<String> getCopyRules() {
        return copyRules;
    }

    public void setCopyRules(List<String> copyRules) {
        this.copyRules = copyRules;
    }

    public Map<String, String> getEnvVars() {
        return envVars;
    }

    public void setEnvVars(Map<String, String> envVars) {
        this.envVars = envVars;
    }

    public List<String> getEntrypoint() {
        return entrypoint;
    }

    public void setEntrypoint(List<String> entrypoint) {
        this.entrypoint = entrypoint;
    }

    public List<String> getCmd() {
        return cmd;
    }

    public void setCmd(List<String> cmd) {
        this.cmd = cmd;
    }

    public List<Integer> getExposePorts() {
        return exposePorts;
    }

    public void setExposePorts(List<Integer> exposePorts) {
        this.exposePorts = exposePorts;
    }

    public List<String> getRunCommands() {
        return runCommands;
    }

    public void setRunCommands(List<String> runCommands) {
        this.runCommands = runCommands;
    }

    public void addRunCommand(String command) {
        this.runCommands.add(command);
    }

    public List<String> getPostCopyRunCommands() {
        return postCopyRunCommands;
    }

    public void setPostCopyRunCommands(List<String> postCopyRunCommands) {
        this.postCopyRunCommands = postCopyRunCommands;
    }

    public void addPostCopyRunCommand(String command) {
        this.postCopyRunCommands.add(command);
    }

    public List<String> getHealthcheckCmd() {
        return healthcheckCmd;
    }

    public void setHealthcheckCmd(List<String> healthcheckCmd) {
        this.healthcheckCmd = healthcheckCmd;
    }

    public String getHealthcheckInterval() {
        return healthcheckInterval;
    }

    public void setHealthcheckInterval(String healthcheckInterval) {
        this.healthcheckInterval = healthcheckInterval;
    }

    public String getHealthcheckTimeout() {
        return healthcheckTimeout;
    }

    public void setHealthcheckTimeout(String healthcheckTimeout) {
        this.healthcheckTimeout = healthcheckTimeout;
    }

    public String getHealthcheckStartPeriod() {
        return healthcheckStartPeriod;
    }

    public void setHealthcheckStartPeriod(String healthcheckStartPeriod) {
        this.healthcheckStartPeriod = healthcheckStartPeriod;
    }

    public Integer getHealthcheckRetries() {
        return healthcheckRetries;
    }

    public void setHealthcheckRetries(Integer healthcheckRetries) {
        this.healthcheckRetries = healthcheckRetries;
    }
}
