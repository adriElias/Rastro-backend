package cat.itacademy.rastro.Rastro_backend.user.application;

import java.time.Instant;
import java.util.UUID;

public record RegisterResult(
        UUID id,
        String name,
        String lastName,
        String email,
        String phoneNumber,
        Instant registrationDate
) {
}
