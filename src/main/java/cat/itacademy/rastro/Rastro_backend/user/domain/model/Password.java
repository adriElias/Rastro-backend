package cat.itacademy.rastro.Rastro_backend.user.domain.model;

import java.nio.charset.StandardCharsets;

public record Password(String value) {
    private static final int MAX_UTF_8_BYTES = 72;

    public Password {
        if (value == null) {
            throw new IllegalArgumentException(
                    "Password cannot be null"
            );
        }

        if (value.length() < 5) {
            throw new IllegalArgumentException(
                    "Password must contain at least 5 characters"
            );
        }

        if (value.getBytes(StandardCharsets.UTF_8).length > MAX_UTF_8_BYTES) {
            throw new IllegalArgumentException(
                    "Password must contain at most 72 UTF-8 bytes"
            );
        }

        if (!value.matches(".*[A-Za-z].*")) {
            throw new IllegalArgumentException(
                    "Password must contain at least one letter"
            );
        }

        if (!value.matches(".*\\d.*")) {
            throw new IllegalArgumentException(
                    "Password must contain at least one number"
            );
        }
    }
}
