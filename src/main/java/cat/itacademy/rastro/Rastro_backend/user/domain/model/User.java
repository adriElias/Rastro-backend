package cat.itacademy.rastro.Rastro_backend.user.domain.model;

import java.time.Instant;

public record User(
        UserId id,
        String name,
        String lastName,
        Email email,
        PasswordHash passwordHash,
        String phoneNumber,
        Instant registrationDate,
        UserRole role,
        AccountStatus accountStatus
) {
    public User {
        name = validateAndTrimName(name, "Name");
        lastName = validateAndTrimName(lastName, "Last name");

        if (phoneNumber != null && phoneNumber.isBlank()) {
            phoneNumber = null;
        } else if (phoneNumber != null && phoneNumber.length() > 20) {
            throw new IllegalArgumentException(
                    "Phone number must contain at most 20 characters"
            );
        }
    }

    private static String validateAndTrimName(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " must contain between 3 and 10 characters"
            );
        }

        String trimmedValue = value.trim();
        if (trimmedValue.length() < 3 || trimmedValue.length() > 10) {
            throw new IllegalArgumentException(
                    fieldName + " must contain between 3 and 10 characters"
            );
        }

        return trimmedValue;
    }
}
