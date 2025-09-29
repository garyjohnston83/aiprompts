package com.gjjfintech.aiprompts.dto;

import java.time.Instant;
import java.util.List;

public class ChatAndSaveResponse {
    public String id;
    public String status; // "OK" | "FAILED"
    public String modelUsed;
    public String messageContent;
    public List<SavedArtifact> savedArtifacts;
    public String notes;

    public static ChatAndSaveResponse failed(String message) {
        ChatAndSaveResponse r = new ChatAndSaveResponse();
        r.id = "fail-" + Instant.now().toEpochMilli();
        r.status = "FAILED";
        r.modelUsed = null;
        r.messageContent = message;
        r.savedArtifacts = List.of();
        r.notes = null;
        return r;
    }
}