package com.gjjfintech.aiprompts.service;

public interface LLMClient {
    ProviderResult generate(String modelOverride,
                            String systemPrompt,
                            String userPrompt,
                            ImageInput image,
                            Double temperature,
                            Integer maxOutputTokens);
}