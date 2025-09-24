package com.gjjfintech.aiprompts.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.gjjfintech.aiprompts.config.CodegenProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Minimal Azure OpenAI client targeting the Chat Completions API.
 * Uses deployment name instead of model, and api-version query param.
 */
@Component
public class AzureOpenAIClient implements LLMClient {

    private static final Logger log = LoggerFactory.getLogger(AzureOpenAIClient.class);

    private final WebClient.Builder webClientBuilder;
    private final CodegenProperties props;

    public AzureOpenAIClient(WebClient.Builder webClientBuilder, CodegenProperties props) {
        this.webClientBuilder = webClientBuilder;
        this.props = props;
    }

    @Override
    public ProviderResult generate(String modelOverride,
                                   String systemPrompt,
                                   String userPrompt,
                                   ImageInput image,
                                   Double temperature,
                                   Integer maxOutputTokens) {

        String endpoint = props.getAzure().getEndpoint();
        String apiVersion = props.getAzure().getApiVersion();
        String deployment = (modelOverride != null && !modelOverride.isBlank())
                ? modelOverride
                : props.getAzure().getDeployment();

        String apiKey = System.getenv(props.getAzure().getApiKeyEnv());
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("AZURE API key env var (" + props.getAzure().getApiKeyEnv() + ") is not set");
        }

        WebClient client = webClientBuilder
                .baseUrl(endpoint)
                .defaultHeader("api-key", apiKey)
                .build();

        Object system = Map.of(
                "role", "system",
                "content", systemPrompt != null ? systemPrompt : "You are a helpful coding assistant."
        );

        Object userContent;
        if (image != null) {
            userContent = Map.of(
                    "role", "user",
                    "content", List.of(
                            Map.of("type", "text", "text", userPrompt),
                            Map.of("type", "image_url", "image_url", Map.of("url", image.url()))
                    )
            );
        } else {
            userContent = Map.of(
                    "role", "user",
                    "content", userPrompt
            );
        }

        Map<String, Object> payload = Map.of(
                "messages", List.of(system, userContent),
                "temperature", temperature != null ? temperature : 0.2,
                "max_tokens", maxOutputTokens != null ? maxOutputTokens : 4096
        );

        // Azure path format: /openai/deployments/{deployment}/chat/completions?api-version=...
        JsonNode response = client.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/openai/deployments/" + deployment + "/chat/completions")
                        .queryParam("api-version", apiVersion)
                        .build())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .timeout(Duration.ofSeconds(props.getAzure().getTimeoutSeconds()))
                .block();

        if (response == null) throw new IllegalStateException("Azure OpenAI returned null response");

        var choices = response.get("choices");
        if (choices == null || !choices.isArray() || choices.isEmpty()) {
            throw new IllegalStateException("Azure OpenAI returned no choices");
        }

        var first = choices.get(0);
        String content = null;
        var msg = first.get("message");
        if (msg != null) {
            var c = msg.get("content");
            if (c != null && !c.isNull()) content = c.asText();
        }
        String modelUsed = deployment; // Azure typically doesn’t echo a 'model' field

        if (content == null) content = "";

        return new ProviderResult(modelUsed, content);
    }
}