package com.example.demo.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SHA256HashingUtil {
    public static void main(String[] args) throws NoSuchAlgorithmException {
        String text = "Hello World!";
        String hashedText = sha256Hash(text);
        System.out.println(hashedText);
    }

    public static String sha256Hash(byte[] bytes) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        messageDigest.update(bytes);
        byte[] digest = messageDigest.digest();
        return bytesToHex(digest);
    }

    public static String sha256Hash(String source) throws NoSuchAlgorithmException {
        return sha256Hash(source.getBytes(StandardCharsets.UTF_8));
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        for (byte b : bytes) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }
}
