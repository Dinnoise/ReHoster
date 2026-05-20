package com.rehoster.model.snapshot;

import com.rehoster.model.run.LaunchResult;
import com.rehoster.model.run.RunContext;

import java.util.ArrayList;
import java.util.List;

public class RuntimeSnapshot {
    private RunContext runContext;
    private LaunchResult launchResult;
    private List<ProcessInfo> processTree;
    private List<EnvVar> environment;
    private List<String> args;
    private List<Observation> observations;

    public RuntimeSnapshot() {
        this.processTree = new ArrayList<>();
        this.environment = new ArrayList<>();
        this.args = new ArrayList<>();
        this.observations = new ArrayList<>();
    }

    public RunContext getRunContext() {
        return runContext;
    }

    public void setRunContext(RunContext runContext) {
        this.runContext = runContext;
    }

    public LaunchResult getLaunchResult() {
        return launchResult;
    }

    public void setLaunchResult(LaunchResult launchResult) {
        this.launchResult = launchResult;
    }

    public List<ProcessInfo> getProcessTree() {
        return processTree;
    }

    public void setProcessTree(List<ProcessInfo> processTree) {
        this.processTree = processTree;
    }

    public List<EnvVar> getEnvironment() {
        return environment;
    }

    public void setEnvironment(List<EnvVar> environment) {
        this.environment = environment;
    }

    public List<String> getArgs() {
        return args;
    }

    public void setArgs(List<String> args) {
        this.args = args;
    }

    public List<Observation> getObservations() {
        return observations;
    }

    public void setObservations(List<Observation> observations) {
        this.observations = observations;
    }

    public void addObservation(Observation observation) {
        this.observations.add(observation);
    }
}
