package com.gjjfintech.aiprompts.controller;

import com.gjjfintech.aiprompts.dto.GenerateCodeResponse;
import com.gjjfintech.aiprompts.exception.CodegenException;
import com.gjjfintech.aiprompts.exception.ParseException;
import com.gjjfintech.aiprompts.exception.ProviderException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Centralized exception handler for REST endpoints.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ParseException.class)
    public ResponseEntity<GenerateCodeResponse> handleParseException(ParseException ex) {
        GenerateCodeResponse response = new GenerateCodeResponse(
                null,
                "NO_CODE_FOUND",
                null,
                null,
                ex.getMessage(),
                "PARSE_ERROR"
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(ProviderException.class)
    public ResponseEntity<GenerateCodeResponse> handleProviderException(ProviderException ex) {
        GenerateCodeResponse response = new GenerateCodeResponse(
                null,
                "FAILED",
                null,
                null,
                ex.getMessage(),
                "PROVIDER_ERROR"
        );
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(response);
    }

    @ExceptionHandler(CodegenException.class)
    public ResponseEntity<GenerateCodeResponse> handleCodegenException(CodegenException ex) {
        GenerateCodeResponse response = new GenerateCodeResponse(
                null,
                "FAILED",
                null,
                null,
                ex.getMessage(),
                "CODEGEN_ERROR"
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GenerateCodeResponse> handleOtherExceptions(Exception ex) {
        GenerateCodeResponse response = new GenerateCodeResponse(
                null,
                "FAILED",
                null,
                null,
                "Unexpected error: " + ex.getMessage(),
                "INTERNAL_ERROR"
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}