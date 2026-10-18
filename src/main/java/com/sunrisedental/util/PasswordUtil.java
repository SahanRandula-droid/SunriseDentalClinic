package com.sunrisedental.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;

public final class PasswordUtil {
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 65_536;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int SALT_BYTES = 16;

    private PasswordUtil() {
    }

    public static String newSalt() {
        byte[] salt = new byte[SALT_BYTES];
        new SecureRandom().nextBytes(salt);
        return HexFormat.of().formatHex(salt);
    }

    public static String hash(String password, String saltHex) {
        if (password == null || saltHex == null) {
            throw new IllegalArgumentException("Password and salt are required");
        }
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(),
                    HexFormat.of().parseHex(saltHex), ITERATIONS, KEY_LENGTH_BITS);
            byte[] hash = SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
            spec.clearPassword();
            return HexFormat.of().formatHex(hash);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Password hashing is not available", e);
        }
    }

    public static boolean matches(String password, String saltHex, String expectedHashHex) {
        if (password == null || saltHex == null || expectedHashHex == null) {
            return false;
        }
        byte[] actual = hash(password, saltHex).getBytes();

        return MessageDigest.isEqual(actual, expectedHashHex.getBytes());
    }

    public static void main(String[] args) {
        String password = args.length > 0 ? args[0] : "ChangeMe@123";
        String salt = newSalt();
        System.out.println("salt = " + salt);
        System.out.println("hash = " + hash(password, salt));
    }
}
