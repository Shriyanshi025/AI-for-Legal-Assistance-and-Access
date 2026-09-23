package com.legalassist.service.embedding;

import com.legalassist.exception.EmbeddingException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class VectorUtils {

    private VectorUtils() {
    }

    public static List<Float> l2Normalize(List<Float> vector) {
        if (vector == null || vector.isEmpty()) {
            throw new IllegalArgumentException("Vector cannot be null or empty");
        }

        double sumSquares = 0.0;
        for (Float val : vector) {
            if (val == null) {
                throw new IllegalArgumentException("Vector elements cannot be null");
            }
            sumSquares += val * val;
        }

        double norm = Math.sqrt(sumSquares);
        if (norm < 1e-9 || Double.isNaN(norm) || Double.isInfinite(norm)) {
            throw new EmbeddingException("Cannot normalize vector with zero or invalid norm: " + norm);
        }

        List<Float> normalized = new ArrayList<>(vector.size());
        for (Float val : vector) {
            normalized.add((float) (val / norm));
        }

        return normalized;
    }

    public static String formatPgVector(List<Float> vector) {
        if (vector == null || vector.isEmpty()) {
            throw new IllegalArgumentException("Vector cannot be null or empty for formatting");
        }

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(vector.get(i));
        }
        sb.append("]");
        return sb.toString();
    }

    public static List<Float> parsePgVector(String vectorStr) {
        if (vectorStr == null || vectorStr.isBlank()) {
            return List.of();
        }

        String trimmed = vectorStr.trim();
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }

        if (trimmed.isBlank()) {
            return List.of();
        }

        String[] parts = trimmed.split(",");
        List<Float> result = new ArrayList<>(parts.length);
        for (String part : parts) {
            result.add(Float.parseFloat(part.trim()));
        }
        return result;
    }
}
