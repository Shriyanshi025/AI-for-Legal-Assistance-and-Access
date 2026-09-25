package com.legalassist.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PublicUserIdGeneratorTest {

    @Test
    @DisplayName("Generated public user ID follows USR-XXXXXX format with valid unambiguous characters")
    void testFormatAndAlphabet() {
        String publicId = PublicUserIdGenerator.generatePublicUserId();
        assertNotNull(publicId);
        assertTrue(publicId.startsWith("USR-"), "Should start with USR- prefix");
        assertEquals(10, publicId.length(), "Should be exactly 10 characters long (USR- + 6 code chars)");

        String code = publicId.substring(4);
        assertFalse(code.contains("0"), "Should not contain 0");
        assertFalse(code.contains("O"), "Should not contain O");
        assertFalse(code.contains("1"), "Should not contain 1");
        assertFalse(code.contains("I"), "Should not contain I");
        assertFalse(code.contains("L"), "Should not contain L");
    }

    @Test
    @DisplayName("Generated public user IDs are random and unique")
    void testUniqueness() {
        Set<String> generated = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            String id = PublicUserIdGenerator.generatePublicUserId();
            assertTrue(generated.add(id), "Duplicate public ID generated: " + id);
        }
    }
}
