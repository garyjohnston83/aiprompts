package com.gjjfintech.aiprompts.dto;

public class SavedArtifact {
    public String filename;
    public String language;
    public String savedPath;

    public SavedArtifact() {}
    public SavedArtifact(String filename, String language, String savedPath) {
        this.filename = filename;
        this.language = language;
        this.savedPath = savedPath;
    }
}