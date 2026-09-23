package com.legalassist.exception;

import java.util.UUID;

public class DocumentNotFoundException extends RuntimeException {

    public DocumentNotFoundException(UUID documentId) {
        super("Document not found with id: " + documentId);
    }

    public DocumentNotFoundException(String message) {
        super(message);
    }
}
