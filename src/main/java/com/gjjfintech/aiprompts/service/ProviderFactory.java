package com.gjjfintech.aiprompts.service;

import com.gjjfintech.aiprompts.config.CodegenProperties;
import org.springframework.stereotype.Component;

@Component
public class ProviderFactory {

    private final OpenAIClient openAIClient;
    private final AzureOpenAIClient azureOpenAIClient;
    private final CodegenProperties props;

    public ProviderFactory(OpenAIClient openAIClient,
                           AzureOpenAIClient azureOpenAIClient,
                           CodegenProperties props) {
        this.openAIClient = openAIClient;
        this.azureOpenAIClient = azureOpenAIClient;
        this.props = props;
    }

    public LLMClient getClient(String provider) {
        String p = (provider != null) ? provider.toLowerCase() : props.getProviderDefault();
        return switch (p) {
            case "openai" -> openAIClient;
            case "azure" -> azureOpenAIClient;
            default -> null;
        };
    }
}