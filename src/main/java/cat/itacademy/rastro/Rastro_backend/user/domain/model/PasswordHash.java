package cat.itacademy.rastro.Rastro_backend.user.domain.model;

public record PasswordHash(String value) {
    public PasswordHash {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Password hash cannot be null or blank"
            );
        }
    }
}
