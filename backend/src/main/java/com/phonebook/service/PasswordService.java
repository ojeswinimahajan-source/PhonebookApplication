package com.phonebook.service;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.springframework.stereotype.Service;

@Service
public class PasswordService {

    // ASP.NET Core Identity password hash format markers
    private static final int FORMAT_MARKER_V2 = 0x00;
    private static final int FORMAT_MARKER_V3 = 0x01;

    // ASP.NET Core Identity PRF identifiers
    private static final int PRF_HMACSHA1 = 0;
    private static final int PRF_HMACSHA256 = 1;
    private static final int PRF_HMACSHA512 = 2;

    // ASP.NET Core Identity V3 default settings
    private static final int ITERATION_COUNT = 100_000;
    private static final int SALT_SIZE = 16;
    private static final int SUBKEY_SIZE = 32;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generates a password hash compatible with ASP.NET Core Identity V3.
     */
    public String hashPassword(String password) {

        if (password == null) {
            throw new IllegalArgumentException("Password cannot be null.");
        }

        byte[] salt = new byte[SALT_SIZE];
        secureRandom.nextBytes(salt);

        byte[] subkey = deriveSubkey(
            password,
            salt,
            ITERATION_COUNT,
            SUBKEY_SIZE,
            "PBKDF2WithHmacSHA512"
        );

        ByteBuffer buffer = ByteBuffer.allocate(
            13 + salt.length + subkey.length
        ).order(ByteOrder.BIG_ENDIAN);

        buffer.put((byte) FORMAT_MARKER_V3);
        buffer.putInt(PRF_HMACSHA512);
        buffer.putInt(ITERATION_COUNT);
        buffer.putInt(salt.length);
        buffer.put(salt);
        buffer.put(subkey);

        return Base64.getEncoder().encodeToString(buffer.array());
    }

    /**
     * Verifies a password against an ASP.NET Core Identity V2 or V3 hash.
     */
    public boolean verifyPassword(String password, String encodedHash) {

        if (password == null || encodedHash == null || encodedHash.isBlank()) {
            return false;
        }

        try {
            byte[] decoded = Base64.getDecoder().decode(encodedHash);

            if (decoded.length == 0) {
                return false;
            }

            int formatMarker = decoded[0] & 0xff;

            if (formatMarker == FORMAT_MARKER_V2) {
                return verifyV2(password, decoded);
            }

            if (formatMarker == FORMAT_MARKER_V3) {
                return verifyV3(password, decoded);
            }

            return false;

        } catch (RuntimeException ex) {
            return false;
        }
    }

    /**
     * ASP.NET Identity V2:
     * 1-byte marker + 16-byte salt + 32-byte subkey.
     * Uses PBKDF2-HMAC-SHA1 with 1,000 iterations.
     */
    private boolean verifyV2(String password, byte[] decoded) {

        if (decoded.length != 49) {
            return false;
        }

        byte[] salt = new byte[16];
        byte[] expectedSubkey = new byte[32];

        System.arraycopy(decoded, 1, salt, 0, salt.length);
        System.arraycopy(decoded, 17, expectedSubkey, 0, expectedSubkey.length);

        byte[] actualSubkey = deriveSubkey(
            password,
            salt,
            1_000,
            expectedSubkey.length,
            "PBKDF2WithHmacSHA1"
        );

        return MessageDigest.isEqual(expectedSubkey, actualSubkey);
    }

    /**
     * ASP.NET Core Identity V3:
     * marker + PRF + iteration count + salt length + salt + subkey.
     */
    private boolean verifyV3(String password, byte[] decoded) {

        if (decoded.length < 14) {
            return false;
        }

        ByteBuffer buffer = ByteBuffer.wrap(decoded)
            .order(ByteOrder.BIG_ENDIAN);

        buffer.get(); // Skip format marker.

        int prf = buffer.getInt();
        int iterations = buffer.getInt();
        int saltLength = buffer.getInt();

        if (iterations <= 0 || saltLength <= 0) {
            return false;
        }

        // Require enough remaining bytes for both salt and subkey.
        if (saltLength >= buffer.remaining()) {
            return false;
        }

        String algorithm = switch (prf) {
            case PRF_HMACSHA1 -> "PBKDF2WithHmacSHA1";
            case PRF_HMACSHA256 -> "PBKDF2WithHmacSHA256";
            case PRF_HMACSHA512 -> "PBKDF2WithHmacSHA512";
            default -> null;
        };

        if (algorithm == null) {
            return false;
        }

        byte[] salt = new byte[saltLength];
        buffer.get(salt);

        int subkeyLength = buffer.remaining();

        if (subkeyLength <= 0) {
            return false;
        }

        byte[] expectedSubkey = new byte[subkeyLength];
        buffer.get(expectedSubkey);

        byte[] actualSubkey = deriveSubkey(
            password,
            salt,
            iterations,
            subkeyLength,
            algorithm
        );

        return MessageDigest.isEqual(expectedSubkey, actualSubkey);
    }

    /**
     * Derives a PBKDF2 subkey using the requested algorithm.
     */
    private byte[] deriveSubkey(
            String password,
            byte[] salt,
            int iterations,
            int subkeyLength,
            String algorithm) {

        PBEKeySpec spec = new PBEKeySpec(
            password.toCharArray(),
            salt,
            iterations,
            subkeyLength * 8
        );

        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(algorithm);
            return factory.generateSecret(spec).getEncoded();

        } catch (Exception ex) {
            throw new IllegalStateException(
                "Unable to process the password hash.",
                ex
            );

        } finally {
            spec.clearPassword();
        }
    }
}