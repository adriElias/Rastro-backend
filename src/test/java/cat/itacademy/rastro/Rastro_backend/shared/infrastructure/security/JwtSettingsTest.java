package cat.itacademy.rastro.Rastro_backend.shared.infrastructure.security;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class JwtSettingsTest {

    @Test
    void shouldRejectSigningKeyShorterThan256Bits() {
        byte[] shortKey = new byte[31];

        assertThrows(
                IllegalArgumentException.class,
                () -> new JwtSettings(
                        shortKey,
                        "rastro-backend",
                        Duration.ofMinutes(15)
                )
        );
    }

    @Test
    void shouldRejectBlankIssuer() {
        byte[] validKey = new byte[32];

        assertThrows(
                IllegalArgumentException.class,
                () -> new JwtSettings(
                        validKey,
                        " ",
                        Duration.ofMinutes(15)
                )
        );
    }

    @Test
    void shouldRejectZeroAccessTokenLifetime() {
        byte[] validKey = new byte[32];

        assertThrows(
                IllegalArgumentException.class,
                () -> new JwtSettings(
                        validKey,
                        "rastro-backend",
                        Duration.ZERO
                )
        );
    }

    @Test
    void shouldProtectSigningKeyFromExternalModification() {
        byte[] originalKey = new byte[32];
        originalKey[0] = 42;

        JwtSettings settings = new JwtSettings(
                originalKey,
                "rastro-backend",
                Duration.ofMinutes(15)
        );

        // Modify the original array after creating JwtSettings.
        originalKey[0] = 99;

        assertEquals(42, settings.signingKey()[0]);

        // Modify the array returned by the accessor.
        byte[] exposedKey = settings.signingKey();
        exposedKey[0] = 77;

        assertEquals(42, settings.signingKey()[0]);
    }
}