package com.gjjfintech.aiprompts.service;

import com.gjjfintech.aiprompts.dto.SavedArtifact;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ArtifactSaver {

    private static final Pattern FENCE = Pattern.compile(
            "(?ms)```(?<lang>[a-zA-Z0-9_+\\-\\.]*)(?:\\s+filename=(?<fname>[^\\r\\n`]+))?\\s*\\n(?<code>.*?)\\n```"
    );

    public List<SavedArtifact> saveAll(String assistantText, Path generatedDir) {
        List<SavedArtifact> artifacts = new ArrayList<>();
        if (assistantText == null || assistantText.isBlank()) return artifacts;

        Matcher m = FENCE.matcher(assistantText);
        int idx = 1;
        while (m.find()) {
            String lang = emptyToNull(m.group("lang"));
            String fname = emptyToNull(m.group("fname"));
            String code = m.group("code") != null ? m.group("code") : "";

            String ext = extForLang(lang);
            String fileName = (fname != null) ? sanitizeFilename(fname) : String.format("snippet-%03d%s", idx, ext);

            Path out = generatedDir.resolve(fileName);
            try {
                Files.createDirectories(out.getParent());
                Files.writeString(out, code, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                artifacts.add(new SavedArtifact(fileName, lang, out.toAbsolutePath().toString()));
                idx++;
            } catch (IOException ignored) {}
        }
        return artifacts;
    }

    private static String sanitizeFilename(String name) {
        String n = name.trim().replace("\\", "/");
        n = n.substring(n.lastIndexOf('/') + 1);
        return n.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
    private static String extForLang(String lang) {
        if (lang == null) return ".txt";
        String l = lang.toLowerCase(Locale.ROOT);
        return switch (l) {
            case "java" -> ".java";
            case "kotlin" -> ".kt";
            case "scala" -> ".scala";
            case "c", "h" -> ".c";
            case "cpp", "c++", "hpp", "cc" -> ".cpp";
            case "cs", "csharp" -> ".cs";
            case "py", "python" -> ".py";
            case "js", "javascript" -> ".js";
            case "ts" -> ".ts";
            case "tsx" -> ".tsx";
            case "jsx" -> ".jsx";
            case "json" -> ".json";
            case "yaml", "yml" -> ".yaml";
            case "xml" -> ".xml";
            case "html" -> ".html";
            case "css" -> ".css";
            case "sql" -> ".sql";
            case "sh", "bash" -> ".sh";
            case "ps1", "powershell" -> ".ps1";
            default -> ".txt";
        };
    }
    private static String emptyToNull(String s) { return (s == null || s.isBlank()) ? null : s; }
}