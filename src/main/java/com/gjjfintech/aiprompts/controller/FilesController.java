package com.gjjfintech.aiprompts.controller;

import com.gjjfintech.aiprompts.config.CodegenProperties;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Lists available images and code files from configured base directories.
 * Returns relative paths (for code files) and filenames (for images).
 *
 * GET /files
 * Response:
 * {
 *   "images": ["img1.png", "img2.jpg"],
 *   "codeFiles": ["src/components/comp1.tsx", "index.html", ...]
 * }
 */
@RestController
@CrossOrigin(origins = {"http://localhost:5190"})
public class FilesController {

    private static final Set<String> IMAGE_EXT = Set.of("png", "jpg", "jpeg", "gif", "bmp", "webp");
    private static final int MAX_DEPTH = 32;

    private final CodegenProperties props;

    public FilesController(CodegenProperties props) {
        this.props = props;
    }

    @GetMapping(path = "/files", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> listFiles(
            @RequestParam(value = "project", required = false) String project // reserved for future use
    ) {
        String filesBase = coalesce(props.getFilesBaseDir() + "\\\\" + project, props.getFilesBaseDir() + "\\\\temp");
        String imagesBase = filesBase + "\\\\images";
        String codeBase = filesBase + "\\\\code";

        List<String> images = listImages(imagesBase);
        List<String> codeFiles = listCodeFiles(codeBase);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("images", images);
        body.put("codeFiles", codeFiles);
        return ResponseEntity.ok(body);
    }

    /* ---------------------------- helpers ---------------------------- */

    private static String coalesce(String a, String b) {
        return (a != null && !a.isBlank()) ? a : b;
    }

    private static List<String> listImages(String baseDir) {
        Path base = Paths.get(baseDir);
        if (!Files.isDirectory(base)) return List.of();

        try (Stream<Path> s = Files.walk(base, MAX_DEPTH)) {
            return s.filter(Files::isRegularFile)
                    .filter(p -> IMAGE_EXT.contains(ext(p.getFileName().toString())))
                    // return just the filename (not relative path) per spec
                    .map(p -> p.getFileName().toString())
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return List.of();
        }
    }

    private static List<String> listCodeFiles(String baseDir) {
        Path base = Paths.get(baseDir);
        if (!Files.isDirectory(base)) return List.of();

        try (Stream<Path> s = Files.walk(base, MAX_DEPTH)) {
            return s.filter(Files::isRegularFile)
                    // return path relative to codeBaseDir using forward slashes
                    .map(p -> base.relativize(p).toString().replace("\\", "/"))
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return List.of();
        }
    }

    private static String ext(String name) {
        int i = name.lastIndexOf('.');
        return (i > 0 && i < name.length() - 1) ? name.substring(i + 1).toLowerCase(Locale.ROOT) : "";
    }
}