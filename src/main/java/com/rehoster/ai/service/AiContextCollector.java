package com.rehoster.ai.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.rehoster.analysis.ServiceDependencyDetector.ServiceDependency;
import com.rehoster.ai.config.AiConfig;
import com.rehoster.ai.model.AiRefinementRequest;
import com.rehoster.model.analysis.AppDependencyModel;
import com.rehoster.model.analysis.EnvVarSpec;
import com.rehoster.model.snapshot.Observation;

public class AiContextCollector {
    private static final List<String> RELEVANT_FILES = Arrays.asList(
        "pom.xml",
        "build.gradle",
        "package.json",
        "package-lock.json",
        "requirements.txt",
        "pyproject.toml",
        "composer.json",
        ".env.example",
        "application.properties",
        "application.yml",
        "application.yaml",
        "docker-compose.yml",
        "docker-compose.yaml"
    );

    public AiRefinementRequest collect(Path workingDirectory,
                                       String serviceName,
                                       AppDependencyModel model,
                                       List<ServiceDependency> serviceDependencies,
                                       String baselineDockerfile,
                                       String baselineCompose,
                                       AiConfig config) {
        AiRefinementRequest request = new AiRefinementRequest();

        request.getProjectInfo().setServiceName(serviceName);
        request.getProjectInfo().setWorkingDirectoryName(resolveWorkingDirectoryName(workingDirectory));

        request.getAnalysis().setDetectedType(model.getDetectedType());
        request.getAnalysis().setEntrypoint(new ArrayList<String>(model.getEntrypoint()));
        request.getAnalysis().setExposedPorts(new ArrayList<Integer>(model.getExposedPorts()));
        request.getAnalysis().setRequiredEnv(convertRequiredEnv(model.getRequiredEnv()));
        request.getAnalysis().setNotes(convertNotes(model.getNotes()));

        request.setServiceDependencies(convertDependencies(serviceDependencies));
        request.getBaselineArtifacts().setDockerfile(baselineDockerfile);
        request.getBaselineArtifacts().setCompose(baselineCompose);
        request.setProjectFiles(readRelevantProjectFiles(workingDirectory, config.getMaxInputSize()));
        request.getConstraints().setMode(config.getMode().name());

        return request;
    }

    private String resolveWorkingDirectoryName(Path workingDirectory) {
        if (workingDirectory == null || workingDirectory.getFileName() == null) {
            return "unknown";
        }
        return workingDirectory.getFileName().toString();
    }

    private List<AiRefinementRequest.RequiredEnvPayload> convertRequiredEnv(List<EnvVarSpec> envVars) {
        List<AiRefinementRequest.RequiredEnvPayload> result = new ArrayList<AiRefinementRequest.RequiredEnvPayload>();
        if (envVars == null) {
            return result;
        }

        for (EnvVarSpec spec : envVars) {
            if (spec == null) {
                continue;
            }
            AiRefinementRequest.RequiredEnvPayload payload = new AiRefinementRequest.RequiredEnvPayload();
            payload.setKey(spec.getKey());
            payload.setRequired(spec.isRequired());
            payload.setDefaultValue(spec.getDefaultValue());
            payload.setDescription(spec.getDescription());
            result.add(payload);
        }
        return result;
    }

    private List<String> convertNotes(List<Observation> notes) {
        List<String> result = new ArrayList<String>();
        if (notes == null) {
            return result;
        }
        for (Observation note : notes) {
            if (note != null && note.getMessage() != null && !note.getMessage().trim().isEmpty()) {
                result.add(note.getMessage());
            }
        }
        return result;
    }

    private List<AiRefinementRequest.ServiceDependencyPayload> convertDependencies(List<ServiceDependency> dependencies) {
        List<AiRefinementRequest.ServiceDependencyPayload> result = new ArrayList<AiRefinementRequest.ServiceDependencyPayload>();
        if (dependencies == null) {
            return result;
        }

        for (ServiceDependency dependency : dependencies) {
            if (dependency == null) {
                continue;
            }
            AiRefinementRequest.ServiceDependencyPayload payload = new AiRefinementRequest.ServiceDependencyPayload();
            payload.setServiceName(dependency.getServiceName());
            payload.setImage(dependency.getImage());
            payload.setDefaultPort(dependency.getDefaultPort());
            payload.setEnvironment(dependency.getEnvironment());
            payload.setVolumes(dependency.getVolumes());
            payload.setHealthCheck(dependency.getHealthCheck());
            result.add(payload);
        }

        return result;
    }

    private java.util.Map<String, String> readRelevantProjectFiles(Path workingDirectory, int maxInputSize) {
        java.util.Map<String, String> result = new java.util.LinkedHashMap<String, String>();
        if (workingDirectory == null) {
            return result;
        }

        int budgetPerFile = Math.max(512, maxInputSize / Math.max(1, RELEVANT_FILES.size()));
        for (String fileName : RELEVANT_FILES) {
            Path file = resolveRelevantFile(workingDirectory, fileName);
            if (file == null || !Files.exists(file) || Files.isDirectory(file)) {
                continue;
            }
            try {
                String content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
                if (content.length() > budgetPerFile) {
                    content = content.substring(0, budgetPerFile) + "\n...[TRUNCATED]...";
                }
                result.put(workingDirectory.relativize(file).toString().replace('\\', '/'), content);
            } catch (IOException ignored) {
            }
        }
        return result;
    }

    private Path resolveRelevantFile(Path workingDirectory, String fileName) {
        if ("application.properties".equals(fileName) || "application.yml".equals(fileName) || "application.yaml".equals(fileName)) {
            Path inResources = workingDirectory.resolve("src").resolve("main").resolve("resources").resolve(fileName);
            if (Files.exists(inResources)) {
                return inResources;
            }
        }
        Path direct = workingDirectory.resolve(fileName);
        if (Files.exists(direct)) {
            return direct;
        }
        return null;
    }
}
