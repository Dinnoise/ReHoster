package com.rehoster.ai.service;

import java.util.ArrayList;
import java.util.List;

import com.rehoster.ai.model.AiRefinementRequest;
import com.rehoster.ai.model.AiRefinementResult;

public class AiResponseValidator {
    public List<String> validate(AiRefinementRequest request, AiRefinementResult result) {
        List<String> errors = new ArrayList<String>();
        if (result == null) {
            errors.add("AI response could not be parsed as valid JSON");
            return errors;
        }

        if (isBlank(result.getDockerfile())) {
            errors.add("dockerfile is empty");
        } else if (!result.getDockerfile().trim().startsWith("FROM")) {
            errors.add("dockerfile must start with FROM");
        }

        if (isBlank(result.getDockerCompose())) {
            errors.add("dockerCompose is empty");
        } else if (!result.getDockerCompose().contains("services:")) {
            errors.add("dockerCompose must contain services:");
        }

        validateRequiredEnv(request, result, errors);
        validatePorts(request, result, errors);
        validateTargetPlatform(result, errors);

        return errors;
    }

    private void validateRequiredEnv(AiRefinementRequest request, AiRefinementResult result, List<String> errors) {
        if (request == null || request.getAnalysis() == null || request.getAnalysis().getRequiredEnv() == null) {
            return;
        }
        String compose = result.getDockerCompose() != null ? result.getDockerCompose() : "";
        for (AiRefinementRequest.RequiredEnvPayload env : request.getAnalysis().getRequiredEnv()) {
            if (env == null || env.getKey() == null || !env.isRequired()) {
                continue;
            }
            if (!compose.contains(env.getKey() + ":") && !compose.contains(env.getKey() + "=")) {
                errors.add("Required environment variable missing from compose: " + env.getKey());
            }
        }
    }

    private void validatePorts(AiRefinementRequest request, AiRefinementResult result, List<String> errors) {
        if (request == null || request.getAnalysis() == null || request.getAnalysis().getExposedPorts() == null) {
            return;
        }
        String dockerfile = result.getDockerfile() != null ? result.getDockerfile() : "";
        String compose = result.getDockerCompose() != null ? result.getDockerCompose() : "";
        for (Integer port : request.getAnalysis().getExposedPorts()) {
            if (port == null) {
                continue;
            }
            String portValue = String.valueOf(port);
            boolean foundInDockerfile = dockerfile.contains("EXPOSE " + portValue);
            boolean foundInCompose = compose.contains("\"" + portValue + ":" + portValue + "\"") || compose.contains(portValue + ":" + portValue);
            if (!foundInDockerfile && !foundInCompose) {
                errors.add("Required port missing from refined artifacts: " + portValue);
            }
        }
    }

    private void validateTargetPlatform(AiRefinementResult result, List<String> errors) {
        if (result.getDockerfile() != null && result.getDockerfile().toLowerCase().contains("cmd.exe")) {
            errors.add("dockerfile contains Windows-specific command cmd.exe");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
