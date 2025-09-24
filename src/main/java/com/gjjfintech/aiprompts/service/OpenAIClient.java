package com.gjjfintech.aiprompts.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gjjfintech.aiprompts.config.CodegenProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Minimal OpenAI client using Chat Completions.
 * Supports text-only or text+image (via image_url with https or data URI).
 */
@Component
public class OpenAIClient implements LLMClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAIClient.class);

    private final WebClient.Builder webClientBuilder;
    private final CodegenProperties props;
    private final ObjectMapper mapper = new ObjectMapper();

    public OpenAIClient(WebClient.Builder webClientBuilder, CodegenProperties props) {
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

        String apiBase = props.getOpenai().getApiBaseUrl();
        String model = (modelOverride != null && !modelOverride.isBlank()) ? modelOverride : props.getOpenai().getModel();
        String apiKey = System.getenv(props.getOpenai().getApiKeyEnv());
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI API key env var (" + props.getOpenai().getApiKeyEnv() + ") is not set");
        }

        WebClient client = webClientBuilder
                .baseUrl(apiBase)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();

        // Build messages
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
                "model", model,
                "messages", List.of(system, userContent),
                "temperature", temperature != null ? temperature : 0.2,
                "max_tokens", maxOutputTokens != null ? maxOutputTokens : 4096
        );

        JsonNode response = client.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .timeout(Duration.ofSeconds(props.getOpenai().getTimeoutSeconds()))
                .block();

        if (response == null) throw new IllegalStateException("OpenAI returned null response");

        JsonNode choices = response.get("choices");
        if (choices == null || !choices.isArray() || choices.isEmpty()) {
            throw new IllegalStateException("OpenAI returned no choices");
        }

        JsonNode first = choices.get(0);
        String content = null;
        JsonNode msg = first.get("message");
        if (msg != null) {
            JsonNode c = msg.get("content");
            if (c != null && !c.isNull()) content = c.asText();
        }
        String modelUsed = response.hasNonNull("model") ? response.get("model").asText() : model;

        if (content == null) content = "";

        return new ProviderResult(modelUsed, content);
    }
}