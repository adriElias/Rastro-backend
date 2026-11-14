
package cat.itacademy.rastro.Rastro_backend.user.infrastructure.security;

import cat.itacademy.rastro.Rastro_backend.user.domain.model.Password;
import cat.itacademy.rastro.Rastro_backend.user.domain.model.PasswordHash;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCrypt;

import static org.junit.jupiter.api.Assertions.*;

class BCryptPasswordHasherTest {

    private static final String RAW_PASSWORD = "password123";
    private static final String BCRYPT_PATTERN =
            "^\\$2[ayb]\\$10\\$[./0-9A-Za-z]{53}$";

    private final BCryptPasswordHasher passwordHasher = new BCryptPasswordHasher();

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
    void shouldGenerateHashAcceptedByBCrypt() {
        PasswordHash passwordHash = passwordHasher.hash(new Password(RAW_PASSWORD));

        assertTrue(BCrypt.checkpw(RAW_PASSWORD, passwordHash.value()));
    }

    @Test
    void shouldGenerateHashThatRejectsIncorrectPassword() {
        PasswordHash passwordHash = passwordHasher.hash(new Password(RAW_PASSWORD));

        assertFalse(BCrypt.checkpw("incorrect-password", passwordHash.value()));
    }

    @Test
    void shouldUseIndependentSaltForEachHash() {
        Password password = new Password(RAW_PASSWORD);

        PasswordHash firstHash = passwordHasher.hash(password);
        PasswordHash secondHash = passwordHasher.hash(password);

        assertNotEquals(firstHash, secondHash);
        assertTrue(BCrypt.checkpw(RAW_PASSWORD, firstHash.value()));
        assertTrue(BCrypt.checkpw(RAW_PASSWORD, secondHash.value()));
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

        assertTrue(BCrypt.checkpw(
                seventyTwoBytePassword.value(),
                passwordHasher.hash(seventyTwoBytePassword).value()
        ));

        assertThrows(
                IllegalArgumentException.class,
                () -> new Password(seventyThreeBytePassword)
        );
    }
}
