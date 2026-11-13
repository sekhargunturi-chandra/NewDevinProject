package com.capstone.fraudcase.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicInteger;

public final class Hashes {

    private static final AtomicInteger SEQ = new AtomicInteger();

    private Hashes() {
    }

    public static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String shortHash(String value) {
        return sha256(value).substring(0, 12);
    }

    /** Deterministic positive long derived from a stable hash of the input. */
    public static long stableLong(String value) {
        return Long.parseUnsignedLong(sha256(value).substring(0, 15), 16);
    }

    public static String nextId(String prefix) {
        return prefix + "-" + shortHash(prefix + System.nanoTime() + SEQ.incrementAndGet()).toUpperCase();
    }
}
