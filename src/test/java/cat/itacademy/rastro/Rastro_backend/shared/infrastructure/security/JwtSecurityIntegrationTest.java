package cat.itacademy.rastro.Rastro_backend.shared.infrastructure.security;

import cat.itacademy.rastro.Rastro_backend.user.application.LoginResult;
import cat.itacademy.rastro.Rastro_backend.user.application.LoginUserUseCase;
import cat.itacademy.rastro.Rastro_backend.user.application.EmailAlreadyRegisteredException;
import cat.itacademy.rastro.Rastro_backend.user.application.RegisterCommand;
import cat.itacademy.rastro.Rastro_backend.user.application.RegisterResult;
import cat.itacademy.rastro.Rastro_backend.user.application.RegisterUserUseCase;
import cat.itacademy.rastro.Rastro_backend.user.domain.model.AccountStatus;
import cat.itacademy.rastro.Rastro_backend.user.domain.model.Email;
import cat.itacademy.rastro.Rastro_backend.user.domain.model.PasswordHash;
import cat.itacademy.rastro.Rastro_backend.user.domain.model.User;
import cat.itacademy.rastro.Rastro_backend.user.domain.model.UserId;
import cat.itacademy.rastro.Rastro_backend.user.domain.model.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.util.Arrays;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

@Testcontainers
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.sql.init.mode=never",
        "JWT_SECRET=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "JWT_ISSUER=rastro-backend",
        "JWT_ACCESS_TOKEN_TTL_SECONDS=900"
})
@AutoConfigureMockMvc
@Import(JwtSecurityIntegrationTest.ProtectedTestController.class)
class JwtSecurityIntegrationTest {

    private static final byte[] SIGNING_KEY =
            "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8);
    private static final byte[] DIFFERENT_SIGNING_KEY =
            "abcdef0123456789abcdef0123456789".getBytes(StandardCharsets.UTF_8);

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("rastro_jwt_security_test")
                    .withUsername("rastro_integration")
                    .withPassword("rastro_integration");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LoginUserUseCase loginUserUseCase;

    @MockitoBean
    private RegisterUserUseCase registerUserUseCase;

    @Test
    void shouldAllowPublicLogin() throws Exception {
        when(loginUserUseCase.login(new Email("adriana@example.com"), "Valid123"))
                .thenReturn(new LoginResult("signed-access-token"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "adriana@example.com",
                                  "password": "Valid123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("signed-access-token"));
    }

    @Test
    void shouldAllowAnonymousRegistrationToReachController() throws Exception {
        when(registerUserUseCase.register(any(RegisterCommand.class)))
                .thenReturn(new RegisterResult(
                        UUID.fromString("b5c8610d-93c4-4b82-97e1-1a5873cbaf5a"),
                        "Example",
                        "Person",
                        "person@example.com",
                        null,
                        Instant.parse("2026-11-03T10:00:00Z")
                ));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRegisterRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("person@example.com"))
                .andExpect(jsonPath("$.accessToken").doesNotExist());
    }

    @Test
    void shouldReturnBadRequestForInvalidAnonymousRegistration() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Example",
                                  "lastName": "Person",
                                  "email": "person@example.com"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void shouldReturnConflictForDuplicateAnonymousRegistration() throws Exception {
        when(registerUserUseCase.register(any(RegisterCommand.class)))
                .thenThrow(new EmailAlreadyRegisteredException());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRegisterRequest()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));
    }

    @Test
    void shouldRequireAuthenticationForOtherRegisterMethods() throws Exception {
        mockMvc.perform(get("/api/v1/auth/register"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRegisterRequest()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldNotMakeOtherAuthEndpointsPublic() throws Exception {
        mockMvc.perform(post("/api/v1/auth/other"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectProtectedEndpointWithoutToken() throws Exception {
        mockMvc.perform(get("/test/protected"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Bearer")))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"))
                .andExpect(jsonPath("$.message").value("Authentication failed"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(0));
    }

    @Test
    void shouldAllowProtectedEndpointWithValidHs256Token() throws Exception {
        mockMvc.perform(get("/test/protected")
                        .header(HttpHeaders.AUTHORIZATION, bearer(validToken())))
                .andExpect(status().isOk())
                .andExpect(content().string("protected"));
    }

    @Test
    void shouldRejectMalformedToken() throws Exception {
        assertInvalidTokenResponse("not-a-jwt");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "not-a-uuid", "1-1-1-1-1"})
    void shouldRejectTokenWithInvalidSubject(String subject) throws Exception {
        assertInvalidTokenResponse(issueTokenWithSubject(subject));
    }

    @Test
    void shouldRejectExpiredToken() throws Exception {
        Instant issuedAt = Instant.now().minus(Duration.ofMinutes(30));
        String token = issueToken(
                SIGNING_KEY,
                "rastro-backend",
                Clock.fixed(issuedAt, ZoneOffset.UTC)
        );

        assertInvalidTokenResponse(token);
    }

    @Test
    void shouldRejectTokenSignedWithDifferentKey() throws Exception {
        String token = issueToken(
                DIFFERENT_SIGNING_KEY,
                "rastro-backend",
                Clock.systemUTC()
        );

        assertInvalidTokenResponse(token);
    }

    @Test
    void shouldRejectTokenWithIncorrectIssuer() throws Exception {
        String token = issueToken(
                SIGNING_KEY,
                "another-issuer",
                Clock.systemUTC()
        );

        assertInvalidTokenResponse(token);
    }

    private void assertInvalidTokenResponse(String token) throws Exception {
        ResultActions result = mockMvc.perform(get("/test/protected")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)));

        result.andExpect(status().isUnauthorized())
                .andExpect(header().string(
                        HttpHeaders.WWW_AUTHENTICATE,
                        containsString("error=\"invalid_token\"")
                ))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"))
                .andExpect(jsonPath("$.message").value("Authentication failed"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(0));
    }

    private String validToken() {
        return issueToken(SIGNING_KEY, "rastro-backend", Clock.systemUTC());
    }

    private String issueToken(byte[] signingKey, String issuer, Clock clock) {
        JwtTokenIssuer tokenIssuer = new JwtTokenIssuer(
                signingKey,
                issuer,
                Duration.ofMinutes(15),
                clock
        );
        return tokenIssuer.issueAccessToken(activeUser());
    }

    private String issueTokenWithSubject(String subject) {
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(SIGNING_KEY));
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder()
                .issuer("rastro-backend")
                .issuedAt(now)
                .expiresAt(now.plus(Duration.ofMinutes(15)))
                .claim("role", "USER");
        if (subject != null) {
            claims.claim("sub", subject);
        }

        return encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(),
                claims.build()
        )).getTokenValue();
    }

    private User activeUser() {
        return new User(
                new UserId(UUID.fromString("8b24cc18-f243-4dc6-a809-c755c28d61a4")),
                "Adriana",
                "Elias",
                new Email("adriana@example.com"),
                new PasswordHash("stored-password-hash"),
                null,
                Instant.now(),
                UserRole.USER,
                AccountStatus.ACTIVE
        );
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String validRegisterRequest() {
        return """
                {
                  "name": "Example",
                  "lastName": "Person",
                  "email": "person@example.com",
                  "password": "Password123",
                  "phoneNumber": null
                }
                """;
    }

    @RestController
    static class ProtectedTestController {

        @GetMapping("/test/protected")
        String protectedEndpoint() {
            return "protected";
        }
    }

    @Test
    void shouldRejectHs384TokenEvenWithMatchingSigningKey() {
        byte[] signingKey = new byte[48];
        Arrays.fill(signingKey, (byte) 42);

        JwtSettings settings = new JwtSettings(
                signingKey,
                "rastro-backend",
                Duration.ofMinutes(15)
        );

        JwtDecoder decoder = new SecurityConfiguration()
                .jwtDecoder(settings);

        NimbusJwtEncoder encoder =
                new NimbusJwtEncoder(new ImmutableSecret<>(signingKey));

        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("rastro-backend")
                .subject(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiresAt(now.plus(Duration.ofMinutes(15)))
                .claim("role", "USER")
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS384).build();

        String token = encoder.encode(
                JwtEncoderParameters.from(header, claims)
        ).getTokenValue();

        assertThrows(
                JwtException.class,
                () -> decoder.decode(token)
        );
    }
}
