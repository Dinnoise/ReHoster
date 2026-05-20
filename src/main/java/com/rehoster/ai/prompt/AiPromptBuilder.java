package com.rehoster.ai.prompt;

import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.rehoster.ai.config.AiConfig;
import com.rehoster.ai.model.AiRefinementRequest;

public class AiPromptBuilder {
    private static final String SYSTEM_PROMPT = "You are the AI refinement stage of ReHoster, a Java framework for migrating legacy applications into containers.\n\n"
        + "Your task is to refine an already generated Dockerfile and docker-compose.yml using structured runtime analysis, dependency analysis, and project manifest files.\n\n"
        + "You must behave as a strict infrastructure configuration refiner.\n\n"
        + "Goals:\n"
        + "1. Improve correctness of Dockerfile and docker-compose.yml.\n"
        + "2. Preserve the intent of the baseline configuration.\n"
        + "3. Minimize unnecessary changes and keep the output lean.\n"
        + "4. Prefer fast builds, small images, and simple configurations over bulky convenience installs.\n"
        + "5. Return only valid JSON in the exact schema requested.\n\n"
        + "Hard rules:\n"
        + "1. Do not return markdown.\n"
        + "2. Do not wrap JSON in code fences.\n"
        + "3. Do not explain outside JSON fields.\n"
        + "4. Do not invent files that are not supported by the provided input.\n"
        + "5. Do not assume secrets or credentials.\n"
        + "6. Do not remove required environment variables unless they are clearly invalid duplicates.\n"
        + "7. Do not replace detected service dependencies with unrelated services.\n"
        + "8. Do not convert a working baseline into a riskier configuration unless there is strong evidence in the input.\n"
        + "9. If confidence is low, keep the baseline mostly unchanged and describe the uncertainty.\n"
        + "10. If build-time behavior is unclear, prefer conservative improvements over large rewrites.\n"
        + "11. Keep Dockerfile instructions minimal: do not install packages, Node.js, npm, compilers, build tools, or extra OS libraries unless the input provides strong evidence they are required.\n"
        + "12. Avoid slow and heavy image customizations when a simpler baseline already works.\n"
        + "13. In docker-compose.yml, do not create containerized database, cache, queue, or search services such as mysql, mariadb, postgres, redis, mongodb, rabbitmq, kafka, elasticsearch, or similar infrastructure components.\n"
        + "14. Assume such services already exist outside Docker on the local network or host machine; preserve application connectivity by keeping ports and environment variables, but point the application service to external/local service endpoints instead of provisioning new infrastructure containers.\n"
        + "15. If a baseline compose file includes local infrastructure services, remove them unless the input contains explicit evidence that ReHoster itself must run those services inside Docker.\n"
        + "16. Use exec-form CMD or ENTRYPOINT for the main container process, not shell-form strings. Run the application as PID 1 directly so it receives SIGTERM/SIGINT correctly and stops cleanly when the container stops.\n"
        + "17. Avoid shell wrappers such as sh -c, bash -c, tail -f, sleep infinity, or other keep-alive commands unless the input explicitly requires them.\n\n"
        + "Return JSON with fields: dockerfile, dockerCompose, summary, warnings, appliedChanges, confidence, shouldApply.";

    private final Gson compactGson;

    public AiPromptBuilder() {
        this.compactGson = new GsonBuilder().disableHtmlEscaping().create();
    }

    public String buildSystemPrompt() {
        return SYSTEM_PROMPT;
    }

    public String buildUserPrompt(AiRefinementRequest request, AiConfig config) {
        String requestJson = compactGson.toJson(limitRequestSize(request, config.getMaxInputSize()));
        return "Refine the baseline Dockerfile and docker-compose.yml for this ReHoster run.\n\n"
            + "Input JSON:\n"
            + requestJson
            + "\n\nReturn JSON with the exact schema requested below.\n"
            + "Prioritize minimal Dockerfile changes, fast builds, and compact output.\n"
            + "Do not add database or cache containers to docker-compose; keep only the application-facing services and connect them to already existing external/local infrastructure using environment variables and hostnames.\n"
            + "Use exec-form CMD or ENTRYPOINT and start the real application process directly so container stop signals terminate it cleanly without leaving orphan CLI processes.\n"
            + "If you are uncertain, preserve the baseline and explain the risk in the warnings field.";
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
