package com.rehoster.ai.client;

import java.io.IOException;
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

public class LmStudioAiClient implements AiClient {
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final String MODELS_PATH = "/v1/models";
    private static final String CHAT_PATH = "/v1/chat/completions";

    private final Gson gson;

    public LmStudioAiClient() {
        this.gson = new Gson();
    }

    public boolean isAvailable(String baseUrl) {
        String modelsUrl = resolveModelsUrl(baseUrl);
        OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build();
        Request request = new Request.Builder()
            .url(modelsUrl)
            .get()
            .build();
        try (Response response = client.newCall(request).execute()) {
            return response.isSuccessful();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public AiClientResponse generate(String systemPrompt, String userPrompt, AiConfig config) throws IOException {
        AiClientResponse response = new AiClientResponse();
        response.setSuccess(false);

        OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(config.getTimeoutSeconds(), TimeUnit.SECONDS)
            .readTimeout(config.getTimeoutSeconds(), TimeUnit.SECONDS)
            .writeTimeout(config.getTimeoutSeconds(), TimeUnit.SECONDS)
            .build();

        JsonObject body = new JsonObject();
        body.addProperty("model", config.getPrimaryModel());
        body.addProperty("temperature", config.getTemperature());
        body.addProperty("stream", false);
        body.addProperty("max_tokens", 4096);

        JsonArray messages = new JsonArray();
        messages.add(createMessage("system", systemPrompt));
        messages.add(createMessage("user", userPrompt));
        body.add("messages", messages);

        String chatUrl = resolveChatUrl(config.getBaseUrl());
        Request request = new Request.Builder()
            .url(chatUrl)
            .header("Content-Type", "application/json")
            .post(RequestBody.create(gson.toJson(body), JSON))
            .build();

        try (Response httpResponse = client.newCall(request).execute()) {
            if (!httpResponse.isSuccessful()) {
                response.setErrorMessage("LM Studio request failed with status " + httpResponse.code());
                return response;
            }
            if (httpResponse.body() == null) {
                response.setErrorMessage("LM Studio returned empty body");
                return response;
            }

            String rawJson = httpResponse.body().string();
            JsonObject root = JsonParser.parseString(rawJson).getAsJsonObject();
            JsonArray choices = root.getAsJsonArray("choices");
            if (choices == null || choices.size() == 0) {
                response.setErrorMessage("LM Studio returned no choices");
                return response;
            }
            JsonObject first = choices.get(0).getAsJsonObject();
            JsonObject message = first.getAsJsonObject("message");
            if (message == null || !message.has("content")) {
                response.setErrorMessage("LM Studio returned no message content");
                return response;
            }

            response.setSuccess(true);
            response.setUsedModel(config.getPrimaryModel());
            response.setFallbackUsed(false);
            response.setContent(message.get("content").getAsString());
            return response;
        }
    }

    private JsonObject createMessage(String role, String content) {
        JsonObject message = new JsonObject();
        message.addProperty("role", role);
        message.addProperty("content", content);
        return message;
    }

    private String resolveModelsUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            return "http://localhost:1234" + MODELS_PATH;
        }
        String url = baseUrl.trim();
        if (url.endsWith(CHAT_PATH)) {
            return url.substring(0, url.length() - CHAT_PATH.length()) + MODELS_PATH;
        }
        if (url.endsWith("/")) {
            return url.substring(0, url.length() - 1) + MODELS_PATH;
        }
        return url + MODELS_PATH;
    }

    private String resolveChatUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            return "http://localhost:1234" + CHAT_PATH;
        }
        String url = baseUrl.trim();
        if (url.endsWith(CHAT_PATH)) {
            return url;
        }
        if (url.endsWith("/")) {
            return url.substring(0, url.length() - 1) + CHAT_PATH;
        }
        return url + CHAT_PATH;
    }
}
