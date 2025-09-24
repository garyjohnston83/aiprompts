package com.gjjfintech.aiprompts.exception;

/**
 * Thrown when parsing of model responses fails
 * (e.g., no fenced code block found when required).
 */
public class ParseException extends CodegenException {

    public ParseException(String message) {
        super(message);
    }

    public ParseException(String message, Throwable cause) {
        super(message, cause);
    }
}