package com.legalassist.util;

import java.security.SecureRandom;

public class PublicUserIdGenerator {

    // Unambiguous compact alphabet: excludes 0, O, 1, I, L
    private static final String ALPHABET = "23456789ABCDEFGHJKMNPQRSTVWXYZ";
    private static final int CODE_LENGTH = 6;
    private static final String PREFIX = "USR-";
    private static final SecureRandom RANDOM = new SecureRandom();

    private PublicUserIdGenerator() {
        // Utility class constructor
    }

    public static String generatePublicUserId() {
        StringBuilder sb = new StringBuilder(PREFIX.length() + CODE_LENGTH);
        sb.append(PREFIX);
        for (int i = 0; i < CODE_LENGTH; i++) {
            int index = RANDOM.nextInt(ALPHABET.length());
            sb.append(ALPHABET.charAt(index));
        }
        return sb.toString();
    }
}
