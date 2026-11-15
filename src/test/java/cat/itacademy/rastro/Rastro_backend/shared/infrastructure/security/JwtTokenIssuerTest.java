package cat.itacademy.rastro.Rastro_backend.shared.infrastructure.security;

import cat.itacademy.rastro.Rastro_backend.user.domain.model.AccountStatus;
import cat.itacademy.rastro.Rastro_backend.user.domain.model.Email;
import cat.itacademy.rastro.Rastro_backend.user.domain.model.PasswordHash;
import cat.itacademy.rastro.Rastro_backend.user.domain.model.User;
import cat.itacademy.rastro.Rastro_backend.user.domain.model.UserId;
import cat.itacademy.rastro.Rastro_backend.user.domain.model.UserRole;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JwtTokenIssuerTest {

    private static final byte[] SIGNING_KEY =
            "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8);
    private static final Instant NOW = Instant.parse("2026-01-15T10:30:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private final JwtTokenIssuer tokenIssuer = new JwtTokenIssuer(
            SIGNING_KEY,
            "rastro-backend",
            Duration.ofMinutes(15),
            CLOCK
    );

    @Test
    void shouldIssueTokenWithRequiredClaimsAndFifteenMinuteExpiration() {
        UUID userId = UUID.fromString("8b24cc18-f243-4dc6-a809-c755c28d61a4");
        User user = activeUser(userId, UserRole.ADMIN);

        String token = tokenIssuer.issueAccessToken(user);
        String payload = decode(token.split("\\.")[1]);

        assertEquals(userId.toString(), stringClaim(payload, "sub"));
        assertEquals("ADMIN", stringClaim(payload, "role"));
        assertEquals("rastro-backend", stringClaim(payload, "iss"));
        assertEquals(NOW.getEpochSecond(), numericClaim(payload, "iat"));
        assertEquals(NOW.plus(Duration.ofMinutes(15)).getEpochSecond(), numericClaim(payload, "exp"));
    }

    @Test
    void shouldSignTokenUsingHs256() throws Exception {
        String token = tokenIssuer.issueAccessToken(
                activeUser(UUID.randomUUID(), UserRole.USER)
        );
        String[] sections = token.split("\\.");

        assertEquals("HS256", stringClaim(decode(sections[0]), "alg"));

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SIGNING_KEY, "HmacSHA256"));
        String expectedSignature = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(mac.doFinal(
                        (sections[0] + "." + sections[1]).getBytes(StandardCharsets.US_ASCII)
                ));

        assertEquals(expectedSignature, sections[2]);
    }

    private User activeUser(UUID id, UserRole role) {
        return new User(
                new UserId(id),
                "Adriana",
                "Elias",
                new Email("adriana@example.com"),
                new PasswordHash("stored-password-hash"),
                null,
                NOW,
                role,
                AccountStatus.ACTIVE
        );
    }

    private String decode(String section) {
        return new String(Base64.getUrlDecoder().decode(section), StandardCharsets.UTF_8);
    }

    private String stringClaim(String json, String claim) {
        Matcher matcher = Pattern.compile("\\\"" + claim + "\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"")
                .matcher(json);
        if (!matcher.find()) {
            throw new AssertionError("Missing claim: " + claim);
        }
        return matcher.group(1);
    }

    private long numericClaim(String json, String claim) {
        Matcher matcher = Pattern.compile("\\\"" + claim + "\\\"\\s*:\\s*(\\d+)")
                .matcher(json);
        if (!matcher.find()) {
            throw new AssertionError("Missing claim: " + claim);
        }
        return Long.parseLong(matcher.group(1));
    }
}
