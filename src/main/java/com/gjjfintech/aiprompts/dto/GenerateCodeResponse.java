package com.gjjfintech.aiprompts.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO returned by the /generate-code endpoint.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenerateCodeResponse {

    private String id;
    private String status;     // OK | NO_CODE_FOUND | FAILED
    private String modelUsed;
    private String savedFile;  // Absolute path on disk (Windows style)
    private String message;
    private String error;      // Optional error description if FAILED
}
