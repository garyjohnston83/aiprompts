package com.gjjfintech.aiprompts.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.gjjfintech.aiprompts.config.CodegenProperties;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
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

        HttpClient insecureHttpClient = HttpClient.create().secure(ssl -> {
            ssl.sslContext(
                    SslContextBuilder.forClient()
                            .trustManager(InsecureTrustManagerFactory.INSTANCE) // trust all
            );
        });

        WebClient client = webClientBuilder
                .baseUrl(endpoint)
                .clientConnector(new ReactorClientHttpConnector(insecureHttpClient))
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

    private static String basicAuthValue(String user, String pass) {
        String token = user + ":" + pass;
        String base64 = Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
        return "Basic " + base64;
    }
}

/*

package com.gjjfintech.aiprompts.service;

import com.gjjfintech.aiprompts.config.CodegenProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class AzureOpenAIClient {

    private final CodegenProperties props;
    private final WebClient.Builder webClientBuilder;

    public AzureOpenAIClient(CodegenProperties props, WebClient.Builder webClientBuilder) {
        this.props = props;
        this.webClientBuilder = webClientBuilder;
    }

    public Mono<Map<String, Object>> chat(List<Map<String, Object>> messages) {
        String endpoint = trimTrailingSlash(props.getAzure().getEndpoint());
        String deployment = coalesce(props.getAzure().getDeployment(), "");
        String apiVersion = coalesce(props.getAzure().getApiVersion(), "2024-02-15-preview");

        String url = endpoint + "/openai/deployments/" + deployment + "/chat/completions?api-version=" + apiVersion;
        WebClient client = webClientBuilder.baseUrl(url).build();

        Map<String, Object> body = new HashMap<>();
        body.put("messages", messages);
        body.put("temperature", 0.2);
        body.put("top_p", 1);
        body.put("stream", false);

        return client.post()
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .headers(h -> {
                    String apiKey = readEnv(props.getAzure().getApiKeyEnv());
                    if (StringUtils.hasText(apiKey)) {
                        h.set("api-key", apiKey);
                    } else {
                        String u = readEnv(props.getAzure().getUsernameEnv());
                        String p = readEnv(props.getAzure().getPasswordEnv());
                        if (StringUtils.hasText(u) && StringUtils.hasText(p)) {
                            String basic = Base64.getEncoder().encodeToString((u + ":" + p).getBytes(StandardCharsets.UTF_8));
                            h.set("Authorization", "Basic " + basic);
                        }
                    }
                })
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class);
    }

    @SuppressWarnings("unchecked")
    public String extractAssistantText(Map<?, ?> azureResponse) {
        Object choices = azureResponse.get("choices");
        if (!(choices instanceof List<?> list) || list.isEmpty()) return null;
        Object first = list.get(0);
        if (!(first instanceof Map<?, ?> m)) return null;
        Object message = m.get("message");
        if (!(message instanceof Map<?, ?> mm)) return null;

        Object content = mm.get("content");
        if (content instanceof String s) return s;

        if (content instanceof List<?> parts) {
            StringBuilder sb = new StringBuilder();
            for (Object o : parts) {
                if (o instanceof Map<?, ?> pm) {
                    Object type = pm.get("type");
                    if ("text".equals(type)) {
                        Object t = pm.get("text");
                        if (t != null) sb.append(t);
                    }
                }
            }
            return sb.length() > 0 ? sb.toString() : null;
        }
        return null;
    }

    private static String coalesce(String a, String b) { return StringUtils.hasText(a) ? a : b; }
    private static String trimTrailingSlash(String s) { return (s != null && s.endsWith("/")) ? s.substring(0, s.length()-1) : s; }
    private static String readEnv(String name) { return (StringUtils.hasText(name) ? System.getenv(name) : null); }
}


 */