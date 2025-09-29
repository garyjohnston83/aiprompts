package com.gjjfintech.aiprompts.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Ultra-basic mock controller for quick UI testing.
 * Returns hardcoded payloads based on systemRole.
 *
 * THROWAWAY: replace with the real implementation later.
 */
@RestController
@CrossOrigin(origins = {"http://localhost:5190"})
public class MockChatController {

    @PostMapping(
            path = "/mock-chat-and-save",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ChatAndSaveResponse> chatAndSave(@RequestBody ChatAndSaveRequest req) {
        if (req == null || isBlank(req.systemRole) || isBlank(req.project)) {
            return ResponseEntity.badRequest().body(ChatAndSaveResponse.failed("project and systemRole are required"));
        }

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // Decide which mock to return based on the selected role
        if ("UX Design Expert".equalsIgnoreCase(req.systemRole)) {
            return ResponseEntity.ok(mockUxDesigner(req.project));
        } else {
            // Default to Software Engineer/Architect Expert
            return ResponseEntity.ok(mockEngineer(req.project));
        }
    }

    /* -------------------- MOCK PAYLOADS -------------------- */

    private ChatAndSaveResponse mockUxDesigner(String project) {
        String content = """
            **UX Discovery Summary for %s**

            Your goal is to deliver a fast, low-friction flow where users can paste a prompt, optionally reference project assets (images/code), and receive a concise, actionable response.

            **Primary tasks**
            1. Enter *Project* name prominently (users should recognize context instantly).
            2. Select *System Role* with clear copy about how it shapes the response tone.
            3. Add *Images* and *Code file references* as lightweight chips; removal must be one click.
            4. Compose prompt with an *auto-growing* textarea; show **Enter=Send**, **Shift+Enter=Newline**.
            5. Present responses in a *chat timeline* with strong contrast, code blocks, and copy-to-clipboard.

            **Heuristics applied**
            - *Match between system and real world*: labels use the language engineers expect (Project, System Role, Code files).
            - *Visibility of system status*: a subtle “backend: healthy” indicator and a spinner during send prevent uncertainty.
            - *User control and freedom*: each chip is removable; “Clear all” never clears Project/Role (preserved intent).
            - *Recognition over recall*: past messages persist; saved paths are shown above code blocks so users don't hunt for files.

            **Interaction design**
            - Controls are in a compact, sticky bar; the chat remains in view during composition.
            - Code blocks include a filename and **saved path** header to confirm persistence (a key reassurance moment).
            - Errors are inline and specific (e.g., “Project is required”), never modal or blocking beyond the immediate step.

            **Accessibility**
            - Proper labels, logical tab order, visible focus states, and `aria-live="polite"` for incoming assistant messages.
            - Text contrast meets WCAG AA on dark backgrounds.

            **Next steps**
            - Usability test with 3–5 engineers: speed to first successful request, time to find saved file, confidence after response.
            - Track client-side validation triggers: which limits are most often hit (images/code paths).
            """.formatted(project);

        ChatAndSaveResponse resp = new ChatAndSaveResponse();
        resp.id = "mock-ux";
        resp.status = "OK";
        resp.modelUsed = "mock-1";
        resp.messageContent = content;
        resp.savedArtifacts = List.of(); // UX payload has no code blocks to save
        resp.notes = "UX mock payload";
        return resp;
    }

    private ChatAndSaveResponse mockEngineer(String project) {
        // We’ll pretend the backend saved two files in this project:
        String base = "C:\\\\MyConfiguredLocation\\\\" + project + "\\\\";
        String file1 = "PromptController.java";
        String file2 = "client.ts";

        String message = """
            Below are two implementation snippets for project **%s**. The controller receives a JSON payload, calls the provider,
            and returns markdown with code blocks. The client helper wraps a POST to `/chat-and-save`.

            ```java
            package com.example.%s;

            import org.springframework.http.MediaType;
            import org.springframework.http.ResponseEntity;
            import org.springframework.web.bind.annotation.*;

            @RestController
            @RequestMapping("/api")
            public class PromptController {

                @PostMapping(path = "/chat-and-save", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
                public ResponseEntity<String> chatAndSave(@RequestBody String body) {
                    // 1) Parse payload (project, systemRole, prompt, images, codeFiles)
                    // 2) Build Azure request (system + user + vision images)
                    // 3) Parse response, extract first fenced code block
                    // 4) Save to C:\\\\MyConfiguredLocation\\\\%s\\\\<filename>
                    // 5) Return markdown content and savedArtifacts
                    return ResponseEntity.ok("OK");
                }
            }
            ```

            ```ts
            export async function chatAndSave(baseUrl: string, payload: any) {
              const res = await fetch(baseUrl + "/chat-and-save", {
                method: "POST",
                headers: { "Content-Type": "application/json", "Accept": "application/json" },
                body: JSON.stringify(payload),
              });
              if (!res.ok) {
                const text = await res.text();
                throw new Error(text || `HTTP ${res.status}`);
              }
              return res.json();
            }
            ```
            """.formatted(project, project.toLowerCase().replaceAll("[^a-z0-9]+", ""), project);

        ChatAndSaveResponse resp = new ChatAndSaveResponse();
        resp.id = "mock-eng";
        resp.status = "OK";
        resp.modelUsed = "mock-1";
        resp.messageContent = message;

        // Two saved artifacts, aligned with the two code blocks above (index 0 -> java, index 1 -> ts)
        resp.savedArtifacts = List.of(
                new SavedArtifact(file1, "java", base + file1),
                new SavedArtifact(file2, "ts", base + file2)
        );
        resp.notes = "Engineer/Architect mock payload with two code blocks";
        return resp;
    }

    /* -------------------- helpers & inline DTOs -------------------- */

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    // Minimal request
    public static class ChatAndSaveRequest {
        public String project;
        public String systemRole; // "UX Design Expert" | "Software Engineer/Architect Expert"
        public String prompt;     // unused in mock
    }

    // Minimal response (shape expected by the UI)
    public static class ChatAndSaveResponse {
        public String id;
        public String status; // OK | FAILED
        public String modelUsed;
        public String messageContent; // markdown (with or without code blocks)
        public List<SavedArtifact> savedArtifacts;
        public String notes;

        public static ChatAndSaveResponse failed(String message) {
            ChatAndSaveResponse r = new ChatAndSaveResponse();
            r.id = "mock-failed";
            r.status = "FAILED";
            r.modelUsed = "mock-1";
            r.messageContent = message != null ? message : "Bad request";
            r.savedArtifacts = List.of();
            r.notes = "Mock validation error";
            return r;
        }
    }

    public static class SavedArtifact {
        public String filename;
        public String language;
        public String savedPath;

        public SavedArtifact() { }

        public SavedArtifact(String filename, String language, String savedPath) {
            this.filename = filename;
            this.language = language;
            this.savedPath = savedPath;
        }
    }
}
