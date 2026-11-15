package cat.itacademy.rastro.Rastro_backend.user.infrastructure.security;

import cat.itacademy.rastro.Rastro_backend.user.application.PasswordVerifier;
import cat.itacademy.rastro.Rastro_backend.user.domain.model.PasswordHash;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class BCryptPasswordVerifier implements PasswordVerifier {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public boolean matches(String rawPassword, PasswordHash passwordHash) {
        return passwordEncoder.matches(rawPassword, passwordHash.value());
    }
}
