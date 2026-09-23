package com.legalassist.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test-errors")
public class TestErrorController {

    public record TestValidationRequest(@NotBlank(message = "must not be blank") String title) {
    }

    @PostMapping("/validation")
    public String triggerValidation(@Valid @RequestBody TestValidationRequest request) {
        return "ok";
    }

    @GetMapping("/illegal-argument")
    public String triggerIllegalArgument() {
        throw new IllegalArgumentException("Invalid ID parameter provided");
    }

    @GetMapping("/document-not-found")
    public String triggerDocumentNotFound() {
        throw new DocumentNotFoundException("Document not found with id: 12345678-1234-1234-1234-1234567890ab");
    }

    @GetMapping("/unexpected")
    public String triggerUnexpected() {
        throw new RuntimeException("Secret database connection password leaked in internal error");
    }
}
