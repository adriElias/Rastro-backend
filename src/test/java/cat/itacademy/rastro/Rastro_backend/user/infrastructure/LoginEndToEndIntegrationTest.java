package cat.itacademy.rastro.Rastro_backend.user.infrastructure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.sql.init.mode=never",
        "JWT_SECRET=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "JWT_ISSUER=rastro-backend",
        "JWT_ACCESS_TOKEN_TTL_SECONDS=900"
})
@AutoConfigureMockMvc
class LoginEndToEndIntegrationTest {

    private static final UUID ACTIVE_USER_ID =
            UUID.fromString("8b24cc18-f243-4dc6-a809-c755c28d61a4");
    private static final UUID BLOCKED_USER_ID =
            UUID.fromString("bdd88647-e630-46b3-b86e-a5986fd78fc8");
    private static final String BCRYPT_PASSWORD_HASH =
            new BCryptPasswordEncoder().encode("password");

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("rastro_login_end_to_end_test")
                    .withUsername("rastro_integration")
                    .withPassword("rastro_integration");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void prepareUsers() throws Exception {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM app_user")) {
            statement.executeUpdate();
        }

        insertUser(ACTIVE_USER_ID, "active@example.com", "ACTIVE");
        insertUser(BLOCKED_USER_ID, "blocked@example.com", "BLOCKED");
    }

    @Test
    void shouldReturnValidJwtForValidCredentials() throws Exception {
        MvcResult result = performLogin("active@example.com", "password")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        Jwt jwt = jwtDecoder.decode(accessToken(result));

        assertEquals(ACTIVE_USER_ID.toString(), jwt.getSubject());
        assertEquals("USER", jwt.getClaimAsString("role"));
        assertEquals("rastro-backend", jwt.getClaimAsString("iss"));
        assertNotNull(jwt.getIssuedAt());
        assertNotNull(jwt.getExpiresAt());
    }

    @Test
    void shouldRejectIncorrectPassword() throws Exception {
        performLogin("active@example.com", "incorrect-password")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));
    }

    @Test
    void shouldRejectUnknownEmail() throws Exception {
        performLogin("unknown@example.com", "password")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));
    }

    @Test
    void shouldRejectBlockedUser() throws Exception {
        performLogin("blocked@example.com", "password")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));
    }

    private org.springframework.test.web.servlet.ResultActions performLogin(
            String email,
            String password
    ) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "email": "%s",
                          "password": "%s"
                        }
                        """.formatted(email, password)));
    }

    private void insertUser(UUID id, String email, String accountStatus) throws Exception {
        String sql = """
                INSERT INTO app_user (
                    id, name, last_name, email, password_hash, phone_number,
                    registration_date, role, account_status
                ) VALUES (?, ?, ?, ?, ?, ?, ?, 'USER', CAST(? AS account_status_enum))
                """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, id);
            statement.setString(2, "Adriana");
            statement.setString(3, "Elias");
            statement.setString(4, email);
            statement.setString(5, BCRYPT_PASSWORD_HASH);
            statement.setString(6, null);
            statement.setTimestamp(7, Timestamp.from(Instant.now()));
            statement.setString(8, accountStatus);
            statement.executeUpdate();
        }
    }

    private String accessToken(MvcResult result) throws Exception {
        String response = result.getResponse().getContentAsString();
        Matcher matcher = Pattern.compile("\\\"accessToken\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"")
                .matcher(response);
        if (!matcher.find()) {
            throw new AssertionError("Missing accessToken in response");
        }
        return matcher.group(1);
    }
}
