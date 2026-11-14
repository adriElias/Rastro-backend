package cat.itacademy.rastro.Rastro_backend.user.domain.model;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class PasswordTest {

    @Test
    void shouldRejectPasswordWithLessThanFiveCharacters() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Password("abc1")
        );
    }

    @Test
    void shouldRejectPasswordWithoutLetters() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Password("12345")
        );
    }

    @Test
    void shouldRejectPasswordWithoutNumbers() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Password("abcde")
        );
    }

    @Test
    void shouldCreatePasswordWhenValid() {
        Password password = new Password("abc12");

        assertEquals("abc12", password.value());
    }

    @Test
    void shouldRejectNullPassword() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Password(null)
        );
    }

    @Test
    void shouldAcceptPasswordWithExactlySeventyTwoUtf8Bytes() {
        String value = "a".repeat(71) + "1";

        Password password = new Password(value);

        assertEquals(72, value.getBytes(StandardCharsets.UTF_8).length);
        assertEquals(value, password.value());
    }

    @Test
    void shouldRejectPasswordWithSeventyThreeUtf8Bytes() {
        String value = "a".repeat(72) + "1";

        assertEquals(73, value.getBytes(StandardCharsets.UTF_8).length);
        assertThrows(IllegalArgumentException.class, () -> new Password(value));
    }

    @Test
    void shouldAcceptMultibytePasswordWithExactlySeventyTwoUtf8Bytes() {
        String value = "\u00e9".repeat(35) + "a1";

        Password password = new Password(value);

        assertEquals(72, value.getBytes(StandardCharsets.UTF_8).length);
        assertEquals(value, password.value());
    }

    @Test
    void shouldRejectMultibytePasswordWithSeventyThreeUtf8Bytes() {
        String value = "\u00e9".repeat(35) + "ab1";

        assertEquals(73, value.getBytes(StandardCharsets.UTF_8).length);
        assertThrows(IllegalArgumentException.class, () -> new Password(value));
    }
}
