package com.legalassist.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DirectKeyDiagnosticController {

    @GetMapping("/api/test/keys")
    public ResponseEntity<String> status() {
        return ResponseEntity.ok("OK");
    }
}
