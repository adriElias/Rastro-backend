package cat.itacademy.rastro.Rastro_backend.shared.infrastructure.security;

import cat.itacademy.rastro.Rastro_backend.user.application.TokenIssuer;
import cat.itacademy.rastro.Rastro_backend.user.domain.model.User;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.spec.SecretKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

public class JwtTokenIssuer implements TokenIssuer {

    private final String issuer;
    private final Duration tokenLifetime;
    private final Clock clock;
    private final JwtEncoder jwtEncoder;

    public JwtTokenIssuer(byte[] signingKey, String issuer, Duration tokenLifetime, Clock clock) {
        this.issuer = issuer;
        this.tokenLifetime = tokenLifetime;
        this.clock = clock;
        this.jwtEncoder = NimbusJwtEncoder
                .withSecretKey(new SecretKeySpec(signingKey, "HmacSHA256"))
                .algorithm(MacAlgorithm.HS256)
                .build();
    }

    @Override
    public String issueAccessToken(User user) {
        Instant issuedAt = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(user.id().value().toString())
                .claim("role", user.role().name())
                .issuer(issuer)
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(tokenLifetime))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
