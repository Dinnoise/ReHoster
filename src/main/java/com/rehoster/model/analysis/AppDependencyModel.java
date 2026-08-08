package com.rehoster.model.analysis;

import java.util.ArrayList;
import java.util.List;

import com.rehoster.model.snapshot.Observation;

public class AppDependencyModel {
    private List<String> entrypoint;
    private List<EnvVarSpec> requiredEnv;
    private List<Integer> exposedPorts;
    private List<VolumeSpec> volumes;
    private List<Observation> notes;
    private String detectedType;

    public AppDependencyModel() {
        this.entrypoint = new ArrayList<>();
        this.requiredEnv = new ArrayList<>();
        this.exposedPorts = new ArrayList<>();
        this.volumes = new ArrayList<>();
        this.notes = new ArrayList<>();
    }

    public List<String> getEntrypoint() {
        return entrypoint;
    }

    public void setEntrypoint(List<String> entrypoint) {
        this.entrypoint = entrypoint;
    }

    public List<EnvVarSpec> getRequiredEnv() {
        return requiredEnv;
    }

    public void setRequiredEnv(List<EnvVarSpec> requiredEnv) {
        this.requiredEnv = requiredEnv;
    }

    public List<Integer> getExposedPorts() {
        return exposedPorts;
    }

    public void setExposedPorts(List<Integer> exposedPorts) {
        this.exposedPorts = exposedPorts;
    }

    public List<VolumeSpec> getVolumes() {
        return volumes;
    }

    public void setVolumes(List<VolumeSpec> volumes) {
        this.volumes = volumes;
    }

    public List<Observation> getNotes() {
        return notes;
    }

    public void setNotes(List<Observation> notes) {
        this.notes = notes;
    }

    public void addNote(Observation note) {
        this.notes.add(note);
    }

    public String getDetectedType() {
        return detectedType;
    }

    public void setDetectedType(String detectedType) {
        this.detectedType = detectedType;
    }
}
