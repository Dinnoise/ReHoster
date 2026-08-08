package com.rehoster.ai.prompt;

import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.rehoster.ai.config.AiConfig;
import com.rehoster.ai.model.AiRefinementRequest;

public class AiPromptBuilder {
    private static final String SYSTEM_PROMPT =
        "You are an AI assistant helping to containerize legacy applications with Docker.\n\n"
        + "Your task is to refine a baseline Dockerfile and docker-compose.yml based on the provided "
        + "project analysis and the user's specific requirements.\n\n"
        + "Return ONLY valid JSON (no markdown, no code fences) with these exact fields:\n"
        + "{\n"
        + "  \"dockerfile\": \"<Dockerfile content as string>\",\n"
        + "  \"dockerCompose\": \"<docker-compose.yml content as string>\",\n"
        + "  \"summary\": \"<short description of changes made>\",\n"
        + "  \"appliedChanges\": [\"<change 1>\", \"<change 2>\"],\n"
        + "  \"warnings\": [\"<warning if any>\"],\n"
        + "  \"confidence\": <number between 0.0 and 1.0>,\n"
        + "  \"shouldApply\": <true or false>\n"
        + "}";

    private final Gson compactGson;

    public AiPromptBuilder() {
        this.compactGson = new GsonBuilder().disableHtmlEscaping().create();
    }

    public String buildSystemPrompt() {
        return SYSTEM_PROMPT;
    }

    public String buildUserPrompt(AiRefinementRequest request, AiConfig config, String userRequirement) {
        String requestJson = compactGson.toJson(limitRequestSize(request, config.getMaxInputSize()));

        StringBuilder prompt = new StringBuilder();

        if (userRequirement != null && !userRequirement.trim().isEmpty()) {
            prompt.append("=== MANDATORY USER REQUIREMENT (HIGHEST PRIORITY) ===\n");
            prompt.append(userRequirement.trim()).append("\n");
            prompt.append("You MUST follow this requirement exactly. It overrides any other considerations.\n");
            prompt.append("=====================================================\n\n");
        }

        prompt.append("Refine the baseline Dockerfile and docker-compose.yml for this project.\n\n");
        prompt.append("Project analysis (JSON):\n");
        prompt.append(requestJson);

        if (userRequirement == null || userRequirement.trim().isEmpty()) {
            prompt.append("\n\nNo specific requirements. Improve the baseline if possible while keeping it minimal and correct.");
        } else {
            prompt.append("\n\nRemember: apply the user requirement above first and foremost.");
        }

        prompt.append("\n\nReturn ONLY the JSON response with the exact schema described in the system prompt.");
        return prompt.toString();
    }

    private AiRefinementRequest limitRequestSize(AiRefinementRequest request, int maxInputSize) {
        AiRefinementRequest copy = copyRequest(request);
        String json = compactGson.toJson(copy);
        if (json.length() <= maxInputSize) {
            return copy;
        }

        Map<String, String> files = new LinkedHashMap<String, String>(copy.getProjectFiles());
        while (compactGson.toJson(copy).length() > maxInputSize && !files.isEmpty()) {
            String largestKey = null;
            int largestLength = -1;
            for (Map.Entry<String, String> entry : files.entrySet()) {
                int length = entry.getValue() != null ? entry.getValue().length() : 0;
                if (length > largestLength) {
                    largestLength = length;
                    largestKey = entry.getKey();
                }
            }

            if (largestKey == null) {
                break;
            }

            String value = files.get(largestKey);
            if (value == null || value.length() < 256) {
                files.remove(largestKey);
            } else {
                int newLength = Math.max(128, (int) (value.length() * 0.7d));
                files.put(largestKey, value.substring(0, Math.min(newLength, value.length())) + "\n...[TRUNCATED]...");
            }
            copy.setProjectFiles(new LinkedHashMap<String, String>(files));
        }

        return copy;
    }

    private AiRefinementRequest copyRequest(AiRefinementRequest source) {
        return compactGson.fromJson(compactGson.toJson(source), AiRefinementRequest.class);
    }
}
