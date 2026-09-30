package cat.itacademy.rastro.Rastro_backend.user.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;


public class UserIdTest {
    @Test
    void shouldCreateUserIdWithValidUuid() {
        UUID uuid = UUID.randomUUID();

        UserId userId = new UserId(uuid);

        assertEquals(uuid, userId.value());
    }

    @Test
    void shouldGenerateUserId() {
        UserId userId = UserId.generate();

        assertNotNull(userId);
        assertNotNull(userId.value());
    }

    @Test
    void shouldRejectNullUuid() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new UserId(null)
        );
    }
}
