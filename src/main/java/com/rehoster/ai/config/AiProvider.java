package com.rehoster.ai.config;

public enum AiProvider {
    OPENROUTER;

    public static AiProvider fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return OPENROUTER;
        }
        return OPENROUTER;
    }
}
