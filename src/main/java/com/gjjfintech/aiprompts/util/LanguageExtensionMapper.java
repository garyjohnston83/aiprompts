package com.gjjfintech.aiprompts.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps language identifiers (from code fences) to file extensions.
 */
public class LanguageExtensionMapper {

    private static final Map<String, String> MAP = new HashMap<>();

    static {
        MAP.put("java", ".java");
        MAP.put("python", ".py");
        MAP.put("py", ".py");
        MAP.put("javascript", ".js");
        MAP.put("js", ".js");
        MAP.put("typescript", ".ts");
        MAP.put("ts", ".ts");
        MAP.put("csharp", ".cs");
        MAP.put("cs", ".cs");
        MAP.put("cpp", ".cpp");
        MAP.put("c", ".c");
        MAP.put("go", ".go");
        MAP.put("rust", ".rs");
        MAP.put("rs", ".rs");
        MAP.put("kotlin", ".kt");
        MAP.put("kt", ".kt");
        MAP.put("php", ".php");
        MAP.put("ruby", ".rb");
        MAP.put("rb", ".rb");
        MAP.put("swift", ".swift");
        MAP.put("scala", ".scala");
        MAP.put("html", ".html");
        MAP.put("xml", ".xml");
        MAP.put("json", ".json");
        MAP.put("yaml", ".yaml");
        MAP.put("yml", ".yml");
        MAP.put("sql", ".sql");
        MAP.put("sh", ".sh");
        MAP.put("bash", ".sh");
        MAP.put("ps1", ".ps1");
        MAP.put("powershell", ".ps1");
        MAP.put("markdown", ".md");
        MAP.put("md", ".md");
        MAP.put("", ".txt"); // fallback
    }

    public static String extensionFor(String language) {
        if (language == null) return ".txt";
        String key = language.trim().toLowerCase();
        return MAP.getOrDefault(key, ".txt");
    }
}
