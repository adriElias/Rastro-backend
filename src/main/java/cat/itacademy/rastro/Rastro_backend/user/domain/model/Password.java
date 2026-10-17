package cat.itacademy.rastro.Rastro_backend.user.domain.model;

public record Password(String value) {
    public Password {
        if (value.length() < 5) {
            throw new IllegalArgumentException(
                    "Password must contain at least 5 characters"
            );
        }

        if (!value.matches(".*[A-Za-z].*")) {
            throw new IllegalArgumentException(
                    "Password must contain at least one letter"
            );
        }
    }
}
