package cat.itacademy.rastro.Rastro_backend.user.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class EmailTest {
    @Test
    void shouldCreateEmailWhenFormatIsValid() {
        Email email = new Email("adriana@example.com");

        assertEquals("adriana@example.com", email.value());
    }

    @Test
    void shouldAcceptEmailWithUppercaseLetters() {
        Email email = new Email("Adriana@Example.COM");

        assertEquals("adriana@example.com", email.value());
    }

    @Test
    void shouldTrimEmail() {
        Email email = new Email("  adriana@example.com  ");

        assertEquals("adriana@example.com", email.value());
    }

    @Test
    void shouldRejectNullEmail() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Email(null)
        );
    }

    @Test
    void shouldRejectBlankEmail() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Email("   ")
        );
    }

    @Test
    void shouldRejectEmailWithoutAtSymbol() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Email("adriana.example.com")
        );
    }

    @Test
    void shouldRejectEmailWithoutCharactersAfterAt() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Email("adriana@")
        );
    }

    @Test
    void shouldRejectEmailWithoutDotAfterDomain() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Email("adriana@example")
        );
    }

    @Test
    void shouldRejectEmailWithoutCharactersAfterDot() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Email("adriana@example.")
        );
    }
}
