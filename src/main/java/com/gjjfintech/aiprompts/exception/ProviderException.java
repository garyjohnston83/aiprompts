package com.gjjfintech.aiprompts.exception;

/**
 * Thrown when a provider (OpenAI or Azure) fails or returns an invalid response.
 */
public class ProviderException extends CodegenException {

    public ProviderException(String message) {
        super(message);
    }

    public ProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}