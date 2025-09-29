package com.gjjfintech.aiprompts.dto;

import java.util.List;

public class ChatAndSaveRequest {
    public String project;
    public String systemRole;
    public String prompt;
    public List<String> images;
    public List<String> codeFiles;
    public Options options;

    public static class Options {
        public String parsingMode;        // "auto" | "code" | "text" (optional)
        public Boolean codeFenceRequired; // optional
    }
}