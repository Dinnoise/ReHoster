package com.rehoster.ai.config;

public enum AiProvider {
    OPENROUTER,
    LM_STUDIO;

    public static AiProvider fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return LM_STUDIO;
        }
        if ("lm_studio".equalsIgnoreCase(value.trim()) || "lmstudio".equalsIgnoreCase(value.trim())) {
            return LM_STUDIO;
        }
        return LM_STUDIO;
    }
}
