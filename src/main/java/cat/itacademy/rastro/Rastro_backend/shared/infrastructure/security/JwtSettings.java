package cat.itacademy.rastro.Rastro_backend.shared.infrastructure.security;

import java.time.Duration;
import java.util.Arrays;

public record JwtSettings(
        byte[] signingKey,
        String issuer,
        Duration accessTokenLifetime
) {
    public JwtSettings {
        if (signingKey == null || signingKey.length < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 256 bits");
        }
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalArgumentException("JWT issuer must not be blank");
        }
        if (accessTokenLifetime == null || accessTokenLifetime.isNegative()
                || accessTokenLifetime.isZero()) {
            throw new IllegalArgumentException("JWT access token lifetime must be positive");
        }
        signingKey = Arrays.copyOf(signingKey, signingKey.length);
    }

    @Override
    public byte[] signingKey() {
        return Arrays.copyOf(signingKey, signingKey.length);
    }
}
