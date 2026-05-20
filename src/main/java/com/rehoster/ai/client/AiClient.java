package com.rehoster.ai.client;

import java.io.IOException;

import com.rehoster.ai.config.AiConfig;

public interface AiClient {
    AiClientResponse generate(String systemPrompt, String userPrompt, AiConfig config) throws IOException;
}
