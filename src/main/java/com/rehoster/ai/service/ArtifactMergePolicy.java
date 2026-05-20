package com.rehoster.ai.service;

import java.util.List;

import com.rehoster.ai.config.AiConfig;
import com.rehoster.ai.config.AiMode;
import com.rehoster.ai.model.AiRefinementResult;

public class ArtifactMergePolicy {
    public boolean shouldApply(AiConfig config, AiRefinementResult result, List<String> validationErrors) {
        if (config == null || config.getMode() == AiMode.OFF) {
            return false;
        }
        if (config.getMode() == AiMode.ADVISORY) {
            return false;
        }
        if (result == null) {
            return false;
        }
        if (validationErrors != null && !validationErrors.isEmpty()) {
            return false;
        }
        if (!result.isShouldApply()) {
            return false;
        }
        return result.getConfidence() >= config.getMinConfidence();
    }
}
