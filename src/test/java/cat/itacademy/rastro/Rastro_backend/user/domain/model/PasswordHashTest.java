package cat.itacademy.rastro.Rastro_backend.user.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PasswordHashTest {
    @Test
    void shouldCreatePasswordHashWithValidValue() {
        String hashValue = "$2a$10$exampleHashValue";

        PasswordHash passwordHash = new PasswordHash(hashValue);

        assertEquals(hashValue, passwordHash.value());
    }

    @Test
    void shouldRejectNullValue() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new PasswordHash(null)
        );
    }

    @Test
    void shouldRejectBlankValue() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new PasswordHash("   ")
        );
    }
}
