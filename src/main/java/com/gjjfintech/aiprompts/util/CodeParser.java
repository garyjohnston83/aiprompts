package com.gjjfintech.aiprompts.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility to extract fenced code blocks from text responses.
 */
public class CodeParser {

    private static final Pattern FENCED_BLOCK =
            Pattern.compile("(?s)```([a-zA-Z0-9+#._-]*)\\s*(.*?)```");

    public record CodeBlock(String language, String content) {}

    /**
     * Extracts the first fenced code block from text.
     * If no block found, returns null.
     */
    public static CodeBlock extractFirstCodeBlock(String text, String preferredLanguage) {
        if (text == null) return null;
        Matcher m = FENCED_BLOCK.matcher(text);
        if (m.find()) {
            String lang = m.group(1) != null && !m.group(1).isBlank()
                    ? m.group(1).trim()
                    : preferredLanguage;
            String content = m.group(2);
            return new CodeBlock(lang, content);
        }
        return null;
    }
}
