package com.legalassist.exception;

public class ResearchSessionNotFoundException extends RuntimeException {
    public ResearchSessionNotFoundException(String message) {
        super(message);
    }
}
