package cat.itacademy.rastro.Rastro_backend.user.infrastructure.security;

import cat.itacademy.rastro.Rastro_backend.user.domain.model.Password;
import cat.itacademy.rastro.Rastro_backend.user.domain.model.PasswordHash;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BCryptPasswordHasherTest {

    private static final String RAW_PASSWORD = "password123";
    private static final String BCRYPT_PATTERN =
            "^\\$2[ayb]\\$10\\$[./0-9A-Za-z]{53}$";

    private final BCryptPasswordHasher passwordHasher = new BCryptPasswordHasher();
    private final BCryptPasswordVerifier passwordVerifier = new BCryptPasswordVerifier();

    @Test
    void shouldGenerateHashDifferentFromPlaintext() {
        PasswordHash passwordHash = passwordHasher.hash(new Password(RAW_PASSWORD));

        assertNotEquals(RAW_PASSWORD, passwordHash.value());
    }

    @Test
    void shouldGenerateValidBCryptHashWithCurrentCost() {
        PasswordHash passwordHash = passwordHasher.hash(new Password(RAW_PASSWORD));

        assertTrue(passwordHash.value().matches(BCRYPT_PATTERN));
    }

    @Test
    void shouldGenerateHashAcceptedByExistingPasswordVerifier() {
        PasswordHash passwordHash = passwordHasher.hash(new Password(RAW_PASSWORD));

        assertTrue(passwordVerifier.matches(RAW_PASSWORD, passwordHash));
    }

    @Test
    void shouldGenerateHashThatRejectsIncorrectPassword() {
        PasswordHash passwordHash = passwordHasher.hash(new Password(RAW_PASSWORD));

        assertFalse(passwordVerifier.matches("incorrect-password", passwordHash));
    }

    @Test
    void shouldUseIndependentSaltForEachHash() {
        Password password = new Password(RAW_PASSWORD);

        PasswordHash firstHash = passwordHasher.hash(password);
        PasswordHash secondHash = passwordHasher.hash(password);

        assertNotEquals(firstHash, secondHash);
        assertTrue(passwordVerifier.matches(RAW_PASSWORD, firstHash));
        assertTrue(passwordVerifier.matches(RAW_PASSWORD, secondHash));
    }

    @Test
    void shouldRejectInvalidPasswordsThroughExistingDomainRules() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> new Password(null)),
                () -> assertThrows(IllegalArgumentException.class, () -> new Password("abc1")),
                () -> assertThrows(IllegalArgumentException.class, () -> new Password("12345")),
                () -> assertThrows(IllegalArgumentException.class, () -> new Password("abcde"))
        );
    }

    @Test
    void shouldNeverExposeRawPasswordAsPasswordHash() {
        PasswordHash passwordHash = passwordHasher.hash(new Password(RAW_PASSWORD));

        assertNotEquals(RAW_PASSWORD, passwordHash.value());
        assertFalse(passwordHash.value().contains(RAW_PASSWORD));
    }

    @Test
    void shouldAlignDomainWithBCryptSeventyTwoByteLimit() {
        Password seventyTwoBytePassword = new Password("a".repeat(71) + "1");
        String seventyThreeBytePassword = "a".repeat(72) + "1";

        assertTrue(passwordVerifier.matches(
                seventyTwoBytePassword.value(),
                passwordHasher.hash(seventyTwoBytePassword)
        ));
        assertThrows(
                IllegalArgumentException.class,
                () -> new Password(seventyThreeBytePassword)
        );
    }
}
