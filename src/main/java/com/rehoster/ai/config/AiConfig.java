package com.rehoster.ai.config;

import com.rehoster.model.run.RunConfig;

public class AiConfig {
    public static final String DEFAULT_BASE_URL = "https://openrouter.ai/api/v1/chat/completions";
    public static final String DEFAULT_API_KEY_ENV = "OPENROUTER_API_KEY";
    public static final String DEFAULT_PRIMARY_MODEL = "qwen/qwen3-coder:free";
    public static final String DEFAULT_FALLBACK_MODEL = "meta-llama/llama-3.3-70b-instruct:free";

    private boolean enabled;
    private AiMode mode;
    private AiProvider provider;
    private String baseUrl;
    private String apiKeyEnvVar;
    private String primaryModel;
    private String fallbackModel;
    private double temperature;
    private int timeoutSeconds;
    private int maxInputSize;
    private double minConfidence;
    private boolean fallbackEnabled;

    public AiConfig() {
        this.enabled = false;
        this.mode = AiMode.OFF;
        this.provider = AiProvider.OPENROUTER;
        this.baseUrl = DEFAULT_BASE_URL;
        this.apiKeyEnvVar = DEFAULT_API_KEY_ENV;
        this.primaryModel = DEFAULT_PRIMARY_MODEL;
        this.fallbackModel = DEFAULT_FALLBACK_MODEL;
        this.temperature = 0.1d;
        this.timeoutSeconds = 30;
        this.maxInputSize = 24000;
        this.minConfidence = 0.65d;
        this.fallbackEnabled = true;
    }

    public static AiConfig fromRunConfig(RunConfig runConfig) {
        AiConfig config = new AiConfig();
        if (runConfig == null) {
            return config;
        }

        config.setEnabled(runConfig.isAiEnabled());
        config.setMode(runConfig.getAiMode() != null ? runConfig.getAiMode() : (runConfig.isAiEnabled() ? AiMode.AUTO_APPLY : AiMode.OFF));
        config.setProvider(AiProvider.OPENROUTER);

        String baseUrl = System.getenv("REHOSTER_AI_BASE_URL");
        if (baseUrl != null && !baseUrl.trim().isEmpty()) {
            config.setBaseUrl(baseUrl.trim());
        }

        String apiKeyEnv = System.getenv("REHOSTER_AI_API_KEY_ENV");
        if (apiKeyEnv != null && !apiKeyEnv.trim().isEmpty()) {
            config.setApiKeyEnvVar(apiKeyEnv.trim());
        }

        if (runConfig.getAiModel() != null && !runConfig.getAiModel().trim().isEmpty()) {
            config.setPrimaryModel(runConfig.getAiModel().trim());
        }

        String fallbackModel = System.getenv("REHOSTER_AI_FALLBACK_MODEL");
        if (fallbackModel != null && !fallbackModel.trim().isEmpty()) {
            config.setFallbackModel(fallbackModel.trim());
        }

        if (runConfig.getAiTimeoutSeconds() > 0) {
            config.setTimeoutSeconds(runConfig.getAiTimeoutSeconds());
        }

        String minConfidence = System.getenv("REHOSTER_AI_MIN_CONFIDENCE");
        if (minConfidence != null && !minConfidence.trim().isEmpty()) {
            try {
                config.setMinConfidence(Double.parseDouble(minConfidence.trim()));
            } catch (NumberFormatException ignored) {
            }
        }

        String maxInputSize = System.getenv("REHOSTER_AI_MAX_INPUT_SIZE");
        if (maxInputSize != null && !maxInputSize.trim().isEmpty()) {
            try {
                config.setMaxInputSize(Integer.parseInt(maxInputSize.trim()));
            } catch (NumberFormatException ignored) {
            }
        }

        String temperature = System.getenv("REHOSTER_AI_TEMPERATURE");
        if (temperature != null && !temperature.trim().isEmpty()) {
            try {
                config.setTemperature(Double.parseDouble(temperature.trim()));
            } catch (NumberFormatException ignored) {
            }
        }

        config.setFallbackEnabled(runConfig.isAiFallbackEnabled());

        if (config.getMode() == AiMode.OFF) {
            config.setEnabled(false);
        }

        return config;
    }

    public boolean isEnabled() {
        return enabled && mode != AiMode.OFF;
    }

    public boolean isConfigured() {
        String apiKey = System.getenv(apiKeyEnvVar);
        return apiKey != null && !apiKey.trim().isEmpty();
    }

    public boolean isEnabledFlag() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public AiMode getMode() {
        return mode;
    }

    public void setMode(AiMode mode) {
        this.mode = mode;
    }

    public AiProvider getProvider() {
        return provider;
    }

    public void setProvider(AiProvider provider) {
        this.provider = provider;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKeyEnvVar() {
        return apiKeyEnvVar;
    }

    public void setApiKeyEnvVar(String apiKeyEnvVar) {
        this.apiKeyEnvVar = apiKeyEnvVar;
    }

    public String getPrimaryModel() {
        return primaryModel;
    }

    public void setPrimaryModel(String primaryModel) {
        this.primaryModel = primaryModel;
    }

    public String getFallbackModel() {
        return fallbackModel;
    }

    public void setFallbackModel(String fallbackModel) {
        this.fallbackModel = fallbackModel;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public int getMaxInputSize() {
        return maxInputSize;
    }

    public void setMaxInputSize(int maxInputSize) {
        this.maxInputSize = maxInputSize;
    }

    public double getMinConfidence() {
        return minConfidence;
    }

    public void setMinConfidence(double minConfidence) {
        this.minConfidence = minConfidence;
    }

    public boolean isFallbackEnabled() {
        return fallbackEnabled;
    }

    public void setFallbackEnabled(boolean fallbackEnabled) {
        this.fallbackEnabled = fallbackEnabled;
    }
}
