package com.gjjfintech.aiprompts.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "codegen")
@Data
public class CodegenProperties {

    private String outputDir;
    private String systemPrompt;
    private Parsing parsing = new Parsing();
    private String providerDefault;

    private OpenAI openai = new OpenAI();
    private Azure azure = new Azure();

    @Data
    public static class Parsing {
        private String defaultMode = "auto";   // code | text | auto
        private boolean codeFenceRequired = true;
    }

    @Data
    public static class OpenAI {
        private String apiBaseUrl;
        private String apiKeyEnv;
        private String model;
        private int timeoutSeconds = 60;
    }

    @Data
    public static class Azure {
        private String endpoint;
        private String apiVersion;
        private String apiKeyEnv;
        private String deployment;
        private int timeoutSeconds = 60;
    }
}
