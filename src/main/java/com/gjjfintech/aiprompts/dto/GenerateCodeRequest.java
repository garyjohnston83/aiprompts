package com.gjjfintech.aiprompts.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

/**
 * Request DTO for code generation jobs.
 */
@Data
public class GenerateCodeRequest {

    @NotBlank
    private String id;

    @NotBlank
    private String prompt;

    private ImageRef image;
    private String provider; // "openai" | "azure" | null (use default)

    private Map<String, Object> overrides; // e.g. model, temperature, maxOutputTokens
    private Metadata metadata;
    private Parsing parsing;

    @Data
    public static class ImageRef {
        private String path; // Windows file path OR HTTPS URL
    }

    @Data
    public static class Metadata {
        private String filename;
        private String subdir;
        private String language;
    }

    @Data
    public static class Parsing {
        private String mode; // code | text | auto
        private Boolean codeFenceRequired;
    }
}
