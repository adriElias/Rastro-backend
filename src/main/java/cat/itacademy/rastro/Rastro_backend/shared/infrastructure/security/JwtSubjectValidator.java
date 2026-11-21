package cat.itacademy.rastro.Rastro_backend.shared.infrastructure.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

public final class JwtSubjectValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error INVALID_TOKEN = new OAuth2Error(
            OAuth2ErrorCodes.INVALID_TOKEN,
            "The token is invalid",
            null
    );

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        String subject = token.getSubject();
        if (subject == null || subject.isBlank()) {
            return OAuth2TokenValidatorResult.failure(INVALID_TOKEN);
        }

        try {
            UUID userId = UUID.fromString(subject);
            if (!userId.toString().equalsIgnoreCase(subject)) {
                return OAuth2TokenValidatorResult.failure(INVALID_TOKEN);
            }
        } catch (IllegalArgumentException exception) {
            return OAuth2TokenValidatorResult.failure(INVALID_TOKEN);
        }

        return OAuth2TokenValidatorResult.success();
    }
}
