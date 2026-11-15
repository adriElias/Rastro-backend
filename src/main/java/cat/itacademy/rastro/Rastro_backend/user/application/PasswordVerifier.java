package cat.itacademy.rastro.Rastro_backend.user.application;

import cat.itacademy.rastro.Rastro_backend.user.domain.model.PasswordHash;

public interface PasswordVerifier {

    boolean matches(String rawPassword, PasswordHash passwordHash);
}
