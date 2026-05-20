package com.rehoster.ai.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rehoster.ai.config.AiConfig;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OpenRouterAiClient implements AiClient {
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final Gson gson;

    public OpenRouterAiClient() {
        this.gson = new Gson();
    }

    @Override
    public AiClientResponse generate(String systemPrompt, String userPrompt, AiConfig config) throws IOException {
        AiClientResponse response = new AiClientResponse();
        response.setSuccess(false);

        String apiKey = System.getenv(config.getApiKeyEnvVar());
        if (apiKey == null || apiKey.trim().isEmpty()) {
            response.setErrorMessage("Missing API key in environment variable " + config.getApiKeyEnvVar());
            return response;
        }

        List<String> models = new ArrayList<String>();
        if (config.getPrimaryModel() != null && !config.getPrimaryModel().trim().isEmpty()) {
            models.add(config.getPrimaryModel().trim());
        }
        if (config.isFallbackEnabled() && config.getFallbackModel() != null && !config.getFallbackModel().trim().isEmpty()) {
            models.add(config.getFallbackModel().trim());
        }

        IOException lastException = null;
        String lastError = null;

        for (int i = 0; i < models.size(); i++) {
            String model = models.get(i);
            try {
                String content = executeSingleRequest(systemPrompt, userPrompt, config, apiKey, model);
                response.setSuccess(true);
                response.setUsedModel(model);
                response.setFallbackUsed(i > 0);
                response.setContent(content);
                return response;
            } catch (IOException e) {
                lastException = e;
                lastError = e.getMessage();
            }
        }

        response.setErrorMessage(lastError != null ? lastError : "AI request failed");
        if (lastException != null) {
            throw lastException;
        }
        return response;
    }

    private String executeSingleRequest(String systemPrompt, String userPrompt, AiConfig config, String apiKey, String model)
        throws IOException {
        OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(config.getTimeoutSeconds(), TimeUnit.SECONDS)
            .readTimeout(config.getTimeoutSeconds(), TimeUnit.SECONDS)
            .writeTimeout(config.getTimeoutSeconds(), TimeUnit.SECONDS)
            .build();

        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.addProperty("temperature", config.getTemperature());

        JsonArray messages = new JsonArray();
        messages.add(createMessage("system", systemPrompt));
        messages.add(createMessage("user", userPrompt));
        body.add("messages", messages);

        Request request = new Request.Builder()
            .url(config.getBaseUrl())
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .header("HTTP-Referer", "https://rehoster.local")
            .header("X-Title", "ReHoster")
            .post(RequestBody.create(gson.toJson(body), JSON))
            .build();

        Response response = client.newCall(request).execute();
        try {
            if (!response.isSuccessful()) {
                throw new IOException("OpenRouter request failed with status " + response.code());
            }
            if (response.body() == null) {
                throw new IOException("OpenRouter returned empty body");
            }

            String rawJson = response.body().string();
            JsonObject root = JsonParser.parseString(rawJson).getAsJsonObject();
            JsonArray choices = root.getAsJsonArray("choices");
            if (choices == null || choices.size() == 0) {
                throw new IOException("OpenRouter returned no choices");
            }
            JsonObject first = choices.get(0).getAsJsonObject();
            JsonObject message = first.getAsJsonObject("message");
            if (message == null || !message.has("content")) {
                throw new IOException("OpenRouter returned no message content");
            }
            return message.get("content").getAsString();
        } finally {
            response.close();
        }
    }

    private JsonObject createMessage(String role, String content) {
        JsonObject message = new JsonObject();
        message.addProperty("role", role);
        message.addProperty("content", content);
        return message;
    }
}
