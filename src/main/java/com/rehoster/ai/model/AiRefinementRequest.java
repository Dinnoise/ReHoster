package com.rehoster.ai.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AiRefinementRequest {
    private ProjectInfo projectInfo;
    private Analysis analysis;
    private List<ServiceDependencyPayload> serviceDependencies;
    private BaselineArtifacts baselineArtifacts;
    private Map<String, String> projectFiles;
    private Constraints constraints;

    public AiRefinementRequest() {
        this.projectInfo = new ProjectInfo();
        this.analysis = new Analysis();
        this.serviceDependencies = new ArrayList<>();
        this.baselineArtifacts = new BaselineArtifacts();
        this.projectFiles = new LinkedHashMap<>();
        this.constraints = new Constraints();
    }

    public ProjectInfo getProjectInfo() {
        return projectInfo;
    }

    public void setProjectInfo(ProjectInfo projectInfo) {
        this.projectInfo = projectInfo;
    }

    public Analysis getAnalysis() {
        return analysis;
    }

    public void setAnalysis(Analysis analysis) {
        this.analysis = analysis;
    }

    public List<ServiceDependencyPayload> getServiceDependencies() {
        return serviceDependencies;
    }

    public void setServiceDependencies(List<ServiceDependencyPayload> serviceDependencies) {
        this.serviceDependencies = serviceDependencies;
    }

    public BaselineArtifacts getBaselineArtifacts() {
        return baselineArtifacts;
    }

    public void setBaselineArtifacts(BaselineArtifacts baselineArtifacts) {
        this.baselineArtifacts = baselineArtifacts;
    }

    public Map<String, String> getProjectFiles() {
        return projectFiles;
    }

    public void setProjectFiles(Map<String, String> projectFiles) {
        this.projectFiles = projectFiles;
    }

    public Constraints getConstraints() {
        return constraints;
    }

    public void setConstraints(Constraints constraints) {
        this.constraints = constraints;
    }

    public static class ProjectInfo {
        private String serviceName;
        private String workingDirectoryName;

        public String getServiceName() {
            return serviceName;
        }

        public void setServiceName(String serviceName) {
            this.serviceName = serviceName;
        }

        public String getWorkingDirectoryName() {
            return workingDirectoryName;
        }

        public void setWorkingDirectoryName(String workingDirectoryName) {
            this.workingDirectoryName = workingDirectoryName;
        }
    }

    public static class Analysis {
        private String detectedType;
        private List<String> entrypoint = new ArrayList<>();
        private List<Integer> exposedPorts = new ArrayList<>();
        private List<RequiredEnvPayload> requiredEnv = new ArrayList<>();
        private List<String> notes = new ArrayList<>();

        public String getDetectedType() {
            return detectedType;
        }

        public void setDetectedType(String detectedType) {
            this.detectedType = detectedType;
        }

        public List<String> getEntrypoint() {
            return entrypoint;
        }

        public void setEntrypoint(List<String> entrypoint) {
            this.entrypoint = entrypoint;
        }

        public List<Integer> getExposedPorts() {
            return exposedPorts;
        }

        public void setExposedPorts(List<Integer> exposedPorts) {
            this.exposedPorts = exposedPorts;
        }

        public List<RequiredEnvPayload> getRequiredEnv() {
            return requiredEnv;
        }

        public void setRequiredEnv(List<RequiredEnvPayload> requiredEnv) {
            this.requiredEnv = requiredEnv;
        }

        public List<String> getNotes() {
            return notes;
        }

        public void setNotes(List<String> notes) {
            this.notes = notes;
        }
    }

    public static class RequiredEnvPayload {
        private String key;
        private boolean required;
        private String defaultValue;
        private String description;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public boolean isRequired() {
            return required;
        }

        public void setRequired(boolean required) {
            this.required = required;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public void setDefaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    public static class ServiceDependencyPayload {
        private String serviceName;
        private String image;
        private int defaultPort;
        private Map<String, String> environment = new LinkedHashMap<>();
        private List<String> volumes = new ArrayList<>();
        private String healthCheck;

        public String getServiceName() {
            return serviceName;
        }

        public void setServiceName(String serviceName) {
            this.serviceName = serviceName;
        }

        public String getImage() {
            return image;
        }

        public void setImage(String image) {
            this.image = image;
        }

        public int getDefaultPort() {
            return defaultPort;
        }

        public void setDefaultPort(int defaultPort) {
            this.defaultPort = defaultPort;
        }

        public Map<String, String> getEnvironment() {
            return environment;
        }

        public void setEnvironment(Map<String, String> environment) {
            this.environment = environment;
        }

        public List<String> getVolumes() {
            return volumes;
        }

        public void setVolumes(List<String> volumes) {
            this.volumes = volumes;
        }

        public String getHealthCheck() {
            return healthCheck;
        }

        public void setHealthCheck(String healthCheck) {
            this.healthCheck = healthCheck;
        }
    }

    public static class BaselineArtifacts {
        private String dockerfile;
        private String compose;

        public String getDockerfile() {
            return dockerfile;
        }

        public void setDockerfile(String dockerfile) {
            this.dockerfile = dockerfile;
        }

        public String getCompose() {
            return compose;
        }

        public void setCompose(String compose) {
            this.compose = compose;
        }
    }

    public static class Constraints {
        private String targetPlatform = "linux";
        private boolean allowHealthcheck = true;
        private String mode = "AUTO_APPLY";

        public String getTargetPlatform() {
            return targetPlatform;
        }

        public void setTargetPlatform(String targetPlatform) {
            this.targetPlatform = targetPlatform;
        }

        public boolean isAllowHealthcheck() {
            return allowHealthcheck;
        }

        public void setAllowHealthcheck(boolean allowHealthcheck) {
            this.allowHealthcheck = allowHealthcheck;
        }

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }
    }
}
