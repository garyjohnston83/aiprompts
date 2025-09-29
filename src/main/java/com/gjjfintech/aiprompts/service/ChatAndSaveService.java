package com.gjjfintech.aiprompts.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gjjfintech.aiprompts.config.CodegenProperties;
import com.gjjfintech.aiprompts.dto.ChatAndSaveRequest;
import com.gjjfintech.aiprompts.dto.ChatAndSaveResponse;
import com.gjjfintech.aiprompts.dto.SavedArtifact;
import com.gjjfintech.aiprompts.util.FileUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

@Service
public class ChatAndSaveService {

    private final CodegenProperties props;
    private final ConversationStore conversationStore;
    private final InputCodeAggregator inputCodeAggregator;
    private final AzureOpenAIClient azureClient;
    private final ArtifactSaver artifactSaver;
    private final ObjectMapper om = new ObjectMapper();

    public ChatAndSaveService(CodegenProperties props,
                              ConversationStore conversationStore,
                              InputCodeAggregator inputCodeAggregator,
                              AzureOpenAIClient azureClient,
                              ArtifactSaver artifactSaver) {
        this.props = props;
        this.conversationStore = conversationStore;
        this.inputCodeAggregator = inputCodeAggregator;
        this.azureClient = azureClient;
        this.artifactSaver = artifactSaver;
    }

    public Mono<ChatAndSaveResponse> process(ChatAndSaveRequest req) {
        // basic validation
        if (req == null || isBlank(req.project) || isBlank(req.systemRole) || isBlank(req.prompt)) {
            return Mono.just(ChatAndSaveResponse.failed("project, systemRole and prompt are required"));
        }

        // Resolve project directories
        Path projectRoot = FileUtils.projectRoot(props.getFilesBaseDir(), req.project);
        Path imagesDir = projectRoot.resolve("images");
        Path inputCodeDir = projectRoot.resolve("inputcode");
        Path conversationDir = projectRoot.resolve("conversation");
        Path generatedDir = projectRoot.resolve("generatedcode");
        FileUtils.ensureDirs(imagesDir, inputCodeDir, conversationDir, generatedDir);

        // Load prior conversation
        List<Map<String, Object>> priorMessages = conversationStore.load(conversationDir);

        // Build combined input code reference text
        String combinedCode = inputCodeAggregator.combine(inputCodeDir, req.codeFiles);

        // Build multipart user content (brief + prior + refs + current + images)
        String brief = StringUtils.hasText(props.getSystemPrompt())
                ? props.getSystemPrompt()
                : "Context: You will receive (1) prior conversation, (2) referenced input code, and (3) a new prompt. Continue helpfully.";

        List<Object> userParts = new ArrayList<>();
        userParts.add(partText(brief));

        if (!priorMessages.isEmpty()) {
            try {
                String priorJson = om.writerWithDefaultPrettyPrinter().writeValueAsString(priorMessages);
                userParts.add(partText("Prior conversation (JSON):\n" + priorJson));
            } catch (Exception ignored) {}
        }

        if (StringUtils.hasText(combinedCode)) {
            userParts.add(partText("Referenced input code:\n" + combinedCode));
        }

        userParts.add(partText("Current prompt:\n" + req.prompt));

        // Images -> data URLs
        if (req.images != null) {
            req.images.forEach(name -> FileUtils.imageDataUrl(imagesDir.resolve(name))
                    .ifPresent(dataUrl -> userParts.add(partImage(dataUrl))));
        }

        // Build final message list (system + prior + current multipart)
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of(
                "role", "system",
                "content", "Act as: " + req.systemRole + ". Be precise, structured, and include code fences for any code."
        ));
        // Add prior (as simple role/content strings)
        messages.addAll(normalizePrior(priorMessages));
        // Add current
        messages.add(Map.of("role", "user", "content", userParts));

        // Call Azure
        return azureClient.chat(messages)
                .map(resp -> {
                    String assistantText = azureClient.extractAssistantText(resp);
                    if (assistantText == null) assistantText = "(no content)";

                    // Save artifacts
                    List<SavedArtifact> artifacts = artifactSaver.saveAll(assistantText, generatedDir);

                    // Append to conversation and persist
                    conversationStore.append(conversationDir, req.prompt, assistantText);

                    ChatAndSaveResponse out = new ChatAndSaveResponse();
                    out.id = "azure-" + Instant.now().toEpochMilli();
                    out.status = "OK";
                    out.modelUsed = String.valueOf(resp.getOrDefault("model", props.getAzure().getDeployment()));
                    out.messageContent = assistantText;
                    out.savedArtifacts = artifacts;
                    out.notes = "choices=" + Objects.toString(resp.get("choices"), null);
                    return out;
                });
    }

    /* ---------------- helpers ---------------- */

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

    private static Map<String, Object> partText(String text) {
        return Map.of("type", "text", "text", text);
    }
    private static Map<String, Object> partImage(String dataUrl) {
        return Map.of("type", "image_url", "image_url", Map.of("url", dataUrl));
    }

    private static List<Map<String, Object>> normalizePrior(List<Map<String, Object>> prior) {
        if (prior == null || prior.isEmpty()) return List.of();
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map<String, Object> m : prior) {
            String role = String.valueOf(m.getOrDefault("role", "user"));
            Object content = m.get("content");
            String text = (content instanceof String) ? (String) content : Objects.toString(content, "");
            list.add(Map.of("role", role, "content", text));
        }
        return list;
    }
}