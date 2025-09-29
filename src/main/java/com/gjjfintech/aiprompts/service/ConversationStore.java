package com.gjjfintech.aiprompts.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

@Service
public class ConversationStore {

    private final ObjectMapper om = new ObjectMapper();

    public List<Map<String, Object>> load(Path conversationDir) {
        try {
            Files.createDirectories(conversationDir);
            Path file = conversationDir.resolve("messages.json");
            if (!Files.exists(file)) return new ArrayList<>();
            byte[] json = Files.readAllBytes(file);
            return om.readValue(json, new TypeReference<List<Map<String, Object>>>(){});
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    public void append(Path conversationDir, String userPrompt, String assistantText) {
        List<Map<String, Object>> convo = load(conversationDir);

        Map<String, Object> userMsg = new LinkedHashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userPrompt);
        userMsg.put("timestamp", Instant.now().toString());
        convo.add(userMsg);

        Map<String, Object> asstMsg = new LinkedHashMap<>();
        asstMsg.put("role", "assistant");
        asstMsg.put("content", assistantText);
        asstMsg.put("timestamp", Instant.now().toString());
        convo.add(asstMsg);

        try {
            Path file = conversationDir.resolve("messages.json");
            byte[] out = om.writerWithDefaultPrettyPrinter().writeValueAsBytes(convo);
            Files.write(file, out, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ignored) {}
    }
}