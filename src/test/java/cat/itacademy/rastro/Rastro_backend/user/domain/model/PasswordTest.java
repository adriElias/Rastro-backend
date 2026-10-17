package cat.itacademy.rastro.Rastro_backend.user.domain.model;
import org.junit.jupiter.api.Test;

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
}
