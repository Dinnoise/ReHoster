package com.rehoster.ai.config;

public enum AiMode {
    OFF,
    ADVISORY,
    AUTO_APPLY;

    public static AiMode fromCliValue(String value) {
        if (value == null) {
            return AUTO_APPLY;
        }

        String normalized = value.trim().toLowerCase();
        if ("off".equals(normalized)) {
            return OFF;
        }
        if ("advisory".equals(normalized)) {
            return ADVISORY;
        }
        if ("auto".equals(normalized) || "auto_apply".equals(normalized) || "auto-apply".equals(normalized)) {
            return AUTO_APPLY;
        }
        return AUTO_APPLY;
    }
}
