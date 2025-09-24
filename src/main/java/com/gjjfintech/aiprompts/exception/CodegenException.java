package com.gjjfintech.aiprompts.exception;

/**
 * Base exception for code generation related errors.
 */
public class CodegenException extends RuntimeException {

    public CodegenException(String message) {
        super(message);
    }

    public CodegenException(String message, Throwable cause) {
        super(message, cause);
    }
}