package com.gjjfintech.aiprompts.controller;

import com.gjjfintech.aiprompts.dto.GenerateCodeRequest;
import com.gjjfintech.aiprompts.dto.GenerateCodeResponse;
import com.gjjfintech.aiprompts.service.CodegenService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller exposing a simple API to generate code from a prompt
 * (with an optional image reference) and save it to a configured directory.
 */
@RestController
@CrossOrigin(origins = {"http://localhost:5190"})
@RequestMapping
public class CodegenController {

    private final CodegenService codegenService;

    public CodegenController(CodegenService codegenService) {
        this.codegenService = codegenService;
    }

    /**
     * Health endpoint for simple readiness checks.
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

    /**
     * Accepts a single generation job, forwards it to the provider (OpenAI/Azure),
     * parses the response, saves the file, and returns the saved path + status.
     */
    @PostMapping(
            path = "/generate-code",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<GenerateCodeResponse> generateCode(
            @RequestBody @Valid GenerateCodeRequest request
    ) {
        GenerateCodeResponse response = codegenService.process(request);
        return ResponseEntity.ok(response);
    }
}
