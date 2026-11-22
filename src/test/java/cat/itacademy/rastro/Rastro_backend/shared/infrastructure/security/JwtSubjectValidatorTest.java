package cat.itacademy.rastro.Rastro_backend.shared.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtSubjectValidatorTest {

    private final JwtSubjectValidator validator = new JwtSubjectValidator();

    @Test
    void shouldAcceptCanonicalUuidSubject() {
        OAuth2TokenValidatorResult result = validator.validate(jwtWithSubject(
                UUID.fromString("8b24cc18-f243-4dc6-a809-c755c28d61a4").toString()
        ));

        assertFalse(result.hasErrors());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "not-a-uuid", "1-1-1-1-1"})
    void shouldRejectInvalidSubject(String subject) {
        OAuth2TokenValidatorResult result = validator.validate(jwtWithSubject(subject));

        assertTrue(result.hasErrors());
        assertEquals(1, result.getErrors().size());
        assertEquals(
                OAuth2ErrorCodes.INVALID_TOKEN,
                result.getErrors().iterator().next().getErrorCode()
        );
    }

    private Jwt jwtWithSubject(String subject) {
        var builder = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .issuer("rastro-backend");
        if (subject != null) {
            builder.claim("sub", subject);
        }
        return builder.build();
    }
}
