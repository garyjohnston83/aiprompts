package com.gjjfintech.aiprompts.service;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

@Service
public class InputCodeAggregator {

    public String combine(Path inputCodeDir, List<String> relPaths) {
        if (relPaths == null || relPaths.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        int i = 1;
        for (String rel : relPaths) {
            Path p = inputCodeDir.resolve(rel.replace("/", java.io.File.separator));
            if (!Files.isRegularFile(p)) continue;
            sb.append("//Reference Code File ").append(i).append(" - \"")
                    .append(p.toAbsolutePath()).append("\"\n");
            try {
                sb.append(Files.readString(p)).append("\n");
            } catch (IOException ignored) {}
            i++;
        }
        return sb.toString();
    }
}