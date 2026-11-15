package cat.itacademy.rastro.Rastro_backend.user.infrastructure;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertThrows;

class LoginConfigurationTest {

    private final LoginConfiguration configuration =
            new LoginConfiguration();

    @Test
    void shouldRejectInvalidBase64JwtSecret() {
        String invalidSecret = "invalid-base64!";

        assertThrows(
                IllegalArgumentException.class,
                () -> configuration.jwtSettings(
                        invalidSecret,
                        "rastro-backend",
                        900
                )
        );
    }

    @Test
    void shouldRejectBase64JwtSecretShorterThan256Bits() {
        byte[] shortKey = new byte[31];
        String encodedSecret = Base64.getEncoder()
                .encodeToString(shortKey);

        assertThrows(
                IllegalArgumentException.class,
                () -> configuration.jwtSettings(
                        encodedSecret,
                        "rastro-backend",
                        900
                )
        );
    }
}