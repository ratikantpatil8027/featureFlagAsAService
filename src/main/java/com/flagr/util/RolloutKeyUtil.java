package com.flagr.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;

public class RolloutKeyUtil {

    /**
     * Computes a deterministic sticky hash value between 0 and 99 for the given attributes.
     * Identical attribute values always produce the same hash regardless of call order or time,
     * guaranteeing sticky rollout behavior.
     *
     * @param attributes map of attribute names to values
     * @return integer between 0 and 99 inclusive
     */
    public static int computeRolloutHash(Map<String, Object> attributes) {
        TreeMap<String, Object> sorted = new TreeMap<>(attributes);
        StringBuilder concatenated = new StringBuilder();
        for (Map.Entry<String, Object> entry : sorted.entrySet()) {
            concatenated.append(entry.getValue().toString());
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(concatenated.toString().getBytes(StandardCharsets.UTF_8));
            int result = 0;
            for (int i = 0; i < 4; i++) {
                result = (result << 8) | (hash[i] & 0xFF);
            }
            return Math.abs(result) % 100;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Generates a deterministic hex-encoded SHA-256 key for the given attributes.
     * Identical attribute values always produce the same key regardless of call order or time,
     * guaranteeing sticky rollout behavior.
     *
     * @param attributes map of attribute names to values
     * @return hex-encoded SHA-256 hash string
     */
    public static String generateRolloutKey(Map<String, Object> attributes) {
        TreeMap<String, Object> sorted = new TreeMap<>(attributes);
        StringBuilder concatenated = new StringBuilder();
        for (Map.Entry<String, Object> entry : sorted.entrySet()) {
            concatenated.append(entry.getValue().toString());
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(concatenated.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
