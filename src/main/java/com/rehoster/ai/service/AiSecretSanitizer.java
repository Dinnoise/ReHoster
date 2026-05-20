package com.rehoster.ai.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.rehoster.ai.model.AiRefinementRequest;

public class AiSecretSanitizer {
    private static final String[] SECRET_PATTERNS = new String[] {
        "PASSWORD", "SECRET", "TOKEN", "KEY", "CREDENTIAL", "PRIVATE", "AUTH"
    };

    private final Gson gson;

    public AiSecretSanitizer() {
        this.gson = new GsonBuilder().disableHtmlEscaping().create();
    }

    public AiRefinementRequest sanitize(AiRefinementRequest request) {
        AiRefinementRequest copy = gson.fromJson(gson.toJson(request), AiRefinementRequest.class);

        if (copy.getAnalysis() != null && copy.getAnalysis().getRequiredEnv() != null) {
            for (AiRefinementRequest.RequiredEnvPayload env : copy.getAnalysis().getRequiredEnv()) {
                if (env != null && isSensitiveKey(env.getKey())) {
                    env.setDefaultValue("<REDACTED>");
                }
            }
        }

        if (copy.getServiceDependencies() != null) {
            for (AiRefinementRequest.ServiceDependencyPayload dependency : copy.getServiceDependencies()) {
                if (dependency == null || dependency.getEnvironment() == null) {
                    continue;
                }
                dependency.setEnvironment(sanitizeMap(dependency.getEnvironment()));
            }
        }

        if (copy.getProjectFiles() != null) {
            Map<String, String> sanitizedFiles = new LinkedHashMap<String, String>();
            for (Map.Entry<String, String> entry : copy.getProjectFiles().entrySet()) {
                sanitizedFiles.put(entry.getKey(), sanitizeText(entry.getValue()));
            }
            copy.setProjectFiles(sanitizedFiles);
        }

        return copy;
    }

    private Map<String, String> sanitizeMap(Map<String, String> source) {
        Map<String, String> sanitized = new LinkedHashMap<String, String>();
        for (Map.Entry<String, String> entry : source.entrySet()) {
            if (isSensitiveKey(entry.getKey())) {
                sanitized.put(entry.getKey(), "<REDACTED>");
            } else {
                sanitized.put(entry.getKey(), sanitizeText(entry.getValue()));
            }
        }
        return sanitized;
    }

    private String sanitizeText(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        String sanitized = value;
        String[] lines = value.split("\\r?\\n");
        List<String> updated = new ArrayList<String>();
        for (String line : lines) {
            String current = line;
            int eqIndex = current.indexOf('=');
            if (eqIndex > 0) {
                String key = current.substring(0, eqIndex).trim();
                if (isSensitiveKey(key)) {
                    current = key + "=<REDACTED>";
                }
            }
            int colonIndex = current.indexOf(':');
            if (colonIndex > 0) {
                String key = current.substring(0, colonIndex).trim();
                if (isSensitiveKey(key)) {
                    current = key + ": <REDACTED>";
                }
            }
            updated.add(current);
        }
        sanitized = join(updated);
        return sanitized;
    }

    private String join(List<String> values) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                builder.append('\n');
            }
            builder.append(values.get(i));
        }
        return builder.toString();
    }

    private boolean isSensitiveKey(String key) {
        if (key == null) {
            return false;
        }
        String normalized = key.toUpperCase(Locale.ROOT);
        for (String pattern : SECRET_PATTERNS) {
            if (normalized.contains(pattern)) {
                return true;
            }
        }
        return false;
    }
}
