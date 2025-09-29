package com.gjjfintech.aiprompts.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.UUID;

/**
 * Utility for safe file operations, Windows-friendly.
 */
public class FileUtils {

    /**
     * Saves content under outputDir[/subdir]/filename.
     * If filename already exists, appends a counter.
     */
    public static Path saveToOutput(String outputDir, String subdir, String filename, String content) throws IOException {
        Path base = Paths.get(outputDir);
        if (subdir != null && !subdir.isBlank()) {
            base = base.resolve(subdir);
        }
        Files.createDirectories(base);

        Path file = base.resolve(filename);
        file = resolveConflict(file);

        Files.writeString(file, content, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        return file;
    }

    /**
     * Reads all bytes from a file path (Windows safe).
     */
    public static byte[] readBytes(String path) {
        try {
            return Files.readAllBytes(Paths.get(path));
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot read file: " + path, e);
        }
    }

    /**
     * Attempts to guess MIME type from file extension.
     */
    public static String guessImageMimeType(String path) {
        String lower = path.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        return "application/octet-stream";
    }

    private static Path resolveConflict(Path file) {
        if (!Files.exists(file)) {
            return file;
        }

        String filename = file.getFileName().toString();
        String baseName;
        String ext;

        int dot = filename.lastIndexOf('.');
        if (dot > 0) {
            baseName = filename.substring(0, dot);
            ext = filename.substring(dot); // includes the dot
        } else {
            baseName = filename;
            ext = "";
        }

        int counter = 1;
        Path candidate;
        do {
            String newName = baseName + " (" + counter + ")" + ext;
            candidate = file.getParent().resolve(newName);
            counter++;
        } while (Files.exists(candidate));

        return candidate;
    }
}

/*
package com.gjjfintech.aiprompts.util;

import org.springframework.util.Base64Utils;

import java.io.IOException;
import java.nio.file.*;
import java.util.Locale;
import java.util.Optional;

public final class FileUtils {
    private FileUtils() {}

    public static Path projectRoot(String baseDir, String project) {
        String base = (baseDir != null && !baseDir.isBlank()) ? baseDir : "C:\\\\MyConfiguredLocation";
        return Paths.get(base).resolve(project);
    }

    public static void ensureDirs(Path... dirs) {
        for (Path p : dirs) {
            try { Files.createDirectories(p); } catch (IOException ignored) {}
        }
    }

    public static Optional<String> imageDataUrl(Path imagePath) {
        try {
            if (!Files.isRegularFile(imagePath)) return Optional.empty();
            byte[] bytes = Files.readAllBytes(imagePath);
            String b64 = Base64Utils.encodeToString(bytes);
            String mime = guessImageMime(imagePath.getFileName().toString());
            return Optional.of("data:" + mime + ";base64," + b64);
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    public static String guessImageMime(String filename) {
        String f = filename.toLowerCase(Locale.ROOT);
        if (f.endsWith(".png")) return "image/png";
        if (f.endsWith(".jpg") || f.endsWith(".jpeg")) return "image/jpeg";
        if (f.endsWith(".gif")) return "image/gif";
        if (f.endsWith(".webp")) return "image/webp";
        if (f.endsWith(".bmp")) return "image/bmp";
        return "application/octet-stream";
    }
}

 */