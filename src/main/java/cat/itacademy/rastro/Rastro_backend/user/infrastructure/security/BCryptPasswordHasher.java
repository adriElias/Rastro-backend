package cat.itacademy.rastro.Rastro_backend.user.infrastructure.security;

import cat.itacademy.rastro.Rastro_backend.user.application.PasswordHasher;
import cat.itacademy.rastro.Rastro_backend.user.domain.model.Password;
import cat.itacademy.rastro.Rastro_backend.user.domain.model.PasswordHash;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class BCryptPasswordHasher implements PasswordHasher {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public PasswordHash hash(Password password) {
        return new PasswordHash(passwordEncoder.encode(password.value()));
    }
}
