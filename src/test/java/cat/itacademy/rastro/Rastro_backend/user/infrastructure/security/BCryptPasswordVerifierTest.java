package cat.itacademy.rastro.Rastro_backend.user.infrastructure.security;

import cat.itacademy.rastro.Rastro_backend.user.domain.model.PasswordHash;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BCryptPasswordVerifierTest {

    private static final String RAW_PASSWORD = "password123";

    private static final PasswordHash PASSWORD_HASH = new PasswordHash(
            new BCryptPasswordEncoder().encode(RAW_PASSWORD)
    );

    private final BCryptPasswordVerifier passwordVerifier =
            new BCryptPasswordVerifier();

    @Test
    void shouldMatchCorrectPasswordAgainstBCryptHash() {
        assertTrue(passwordVerifier.matches(RAW_PASSWORD, PASSWORD_HASH));
    }

    @Test
    void shouldNotMatchIncorrectPasswordAgainstBCryptHash() {
        assertFalse(passwordVerifier.matches("incorrect-password", PASSWORD_HASH));
    }
}
