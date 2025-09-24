package com.gjjfintech.aiprompts.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gjjfintech.aiprompts.config.CodegenProperties;
import com.gjjfintech.aiprompts.dto.GenerateCodeRequest;
import com.gjjfintech.aiprompts.dto.GenerateCodeResponse;
import com.gjjfintech.aiprompts.util.CodeParser;
import com.gjjfintech.aiprompts.util.FileUtils;
import com.gjjfintech.aiprompts.util.LanguageExtensionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;

@Service
public class CodegenService {

    private static final Logger log = LoggerFactory.getLogger(CodegenService.class);

    private final ProviderFactory providerFactory;
    private final CodegenProperties props;
    private final ObjectMapper mapper = new ObjectMapper();

    public CodegenService(ProviderFactory providerFactory, CodegenProperties props) {
        this.providerFactory = providerFactory;
        this.props = props;
    }

    public GenerateCodeResponse process(GenerateCodeRequest request) {
        try {
            validate(request);

            String provider = request.getProvider();
            if (provider == null || provider.isBlank()) {
                provider = props.getProviderDefault();
            }

            LLMClient client = providerFactory.getClient(provider);
            if (client == null) {
                return new GenerateCodeResponse(
                        request.getId(), "FAILED", null, null,
                        "Unknown provider: " + provider, "BAD_REQUEST"
                );
            }

            // Build inputs
            String systemPrompt = coalesce(
                    (request.getOverrides() != null) ? (String) request.getOverrides().get("systemPrompt") : null,
                    props.getSystemPrompt(),
                    "You are a helpful coding assistant."
            );

            ImageInput imageInput = buildImageInput(request);

            Double temperature = getDouble(request.getOverrides(), "temperature", 0.2);
            Integer maxOutputTokens = getInt(request.getOverrides(), "maxOutputTokens", 4096);
            String model = getString(request.getOverrides(), "model", null); // for Azure: deployment name

            ProviderResult result = client.generate(
                    model, systemPrompt, request.getPrompt(), imageInput, temperature, maxOutputTokens
            );

            if (result == null || result.content() == null) {
                return new GenerateCodeResponse(
                        request.getId(), "FAILED", null, null,
                        "Empty response from provider", "PROVIDER_ERROR"
                );
            }

            // Decide how to save
            String mode = (request.getParsing() != null && request.getParsing().getMode() != null)
                    ? request.getParsing().getMode()
                    : props.getParsing().getDefaultMode();

            boolean fenceRequired = (request.getParsing() != null && request.getParsing().getCodeFenceRequired() != null)
                    ? request.getParsing().getCodeFenceRequired()
                    : props.getParsing().isCodeFenceRequired();

            String responseText = result.content();

            String preferredLang = (request.getMetadata() != null) ? request.getMetadata().getLanguage() : null;

            CodeParser.CodeBlock block = null;
            if ("text".equalsIgnoreCase(mode)) {
                // Save full text
                block = new CodeParser.CodeBlock(preferredLang != null ? preferredLang : "markdown", responseText);
            } else if ("code".equalsIgnoreCase(mode)) {
                block = CodeParser.extractFirstCodeBlock(responseText, preferredLang);
                if (block == null && fenceRequired) {
                    return new GenerateCodeResponse(
                            request.getId(), "NO_CODE_FOUND", result.modelUsed(), null,
                            "No fenced code block found in response", "PARSE_ERROR"
                    );
                }
                if (block == null) {
                    // fallback to full text
                    block = new CodeParser.CodeBlock(preferredLang != null ? preferredLang : "markdown", responseText);
                }
            } else { // auto
                block = CodeParser.extractFirstCodeBlock(responseText, preferredLang);
                if (block == null) {
                    block = new CodeParser.CodeBlock(preferredLang != null ? preferredLang : "markdown", responseText);
                }
            }

            // Compute filename and save
            String filename = (request.getMetadata() != null) ? request.getMetadata().getFilename() : null;
            String subdir = (request.getMetadata() != null) ? request.getMetadata().getSubdir() : null;

            if (filename == null || filename.isBlank()) {
                String ext = LanguageExtensionMapper.extensionFor(block.language());
                String base = (request.getId() != null && !request.getId().isBlank()) ? request.getId() : "output";
                filename = base + ext;
            }

            Path saved = FileUtils.saveToOutput(
                    props.getOutputDir(), subdir, filename, block.content()
            );

            return new GenerateCodeResponse(
                    request.getId(), "OK", result.modelUsed(),
                    saved.toAbsolutePath().toString(),
                    "Saved " + (block.language() != null ? block.language() : "text") + " to " + saved.getFileName(),
                    null
            );

        } catch (Exception e) {
            log.error("Failed to process job id={}", request.getId(), e);
            return new GenerateCodeResponse(
                    request.getId(), "FAILED", null, null,
                    e.getMessage(), e.getClass().getSimpleName()
            );
        }
    }

    private void validate(GenerateCodeRequest req) {
        if (req == null) throw new IllegalArgumentException("Request cannot be null");
        if (req.getPrompt() == null || req.getPrompt().isBlank()) {
            throw new IllegalArgumentException("Prompt is required");
        }
        if (req.getId() == null || req.getId().isBlank()) {
            throw new IllegalArgumentException("Id is required");
        }
    }

    private ImageInput buildImageInput(GenerateCodeRequest req) {
        if (req.getImage() == null || req.getImage().getPath() == null || req.getImage().getPath().isBlank()) {
            return null;
        }
        String path = req.getImage().getPath().trim();
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return ImageInput.fromUrl(path);
        }
        // local file → data URI
        byte[] bytes = FileUtils.readBytes(path);
        String mime = FileUtils.guessImageMimeType(path);
        String base64 = Base64.getEncoder().encodeToString(bytes);
        String dataUri = "data:" + mime + ";base64," + base64;
        return ImageInput.fromUrl(dataUri);
    }

    private static String coalesce(String... values) {
        for (String v : values) if (v != null && !v.isBlank()) return v;
        return null;
    }

    private static String getString(Map<String, Object> map, String key, String defVal) {
        if (map == null) return defVal;
        Object v = map.get(key);
        return (v instanceof String s && !s.isBlank()) ? s : defVal;
    }

    private static Double getDouble(Map<String, Object> map, String key, Double defVal) {
        if (map == null) return defVal;
        Object v = map.get(key);
        if (v instanceof Number n) return n.doubleValue();
        if (v instanceof String s) try { return Double.parseDouble(s); } catch (Exception ignored) {}
        return defVal;
    }

    private static Integer getInt(Map<String, Object> map, String key, Integer defVal) {
        if (map == null) return defVal;
        Object v = map.get(key);
        if (v instanceof Number n) return n.intValue();
        if (v instanceof String s) try { return Integer.parseInt(s); } catch (Exception ignored) {}
        return defVal;
    }
}