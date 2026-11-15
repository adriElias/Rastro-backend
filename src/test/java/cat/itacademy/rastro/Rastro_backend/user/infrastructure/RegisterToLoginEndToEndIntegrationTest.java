package cat.itacademy.rastro.Rastro_backend.user.infrastructure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
@Import(RegisterToLoginEndToEndIntegrationTest.ProtectedTestController.class)
class RegisterToLoginEndToEndIntegrationTest {

    private static final String NORMALIZED_EMAIL = "new.person@example.com";
    private static final String VALID_PASSWORD = "Password123";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("rastro_register_to_login_test")
                    .withUsername("rastro_integration")
                    .withPassword("rastro_integration");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void clearUsers() throws Exception {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM app_user")) {
            statement.executeUpdate();
        }
    }

    @Test
    void shouldRegisterPersistLoginAndAccessProtectedEndpoint() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRegistrationRequest("  NEW.PERSON@EXAMPLE.COM  ")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Example"))
                .andExpect(jsonPath("$.lastName").value("Person"))
                .andExpect(jsonPath("$.email").value(NORMALIZED_EMAIL))
                .andExpect(jsonPath("$.phoneNumber").doesNotExist())
                .andExpect(jsonPath("$.registrationDate").isNotEmpty())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(jsonPath("$.role").doesNotExist())
                .andExpect(jsonPath("$.accountStatus").doesNotExist());

        StoredUser storedUser = storedUser(NORMALIZED_EMAIL);
        assertNotNull(storedUser.id());
        assertEquals("Example", storedUser.name());
        assertEquals("Person", storedUser.lastName());
        assertEquals(NORMALIZED_EMAIL, storedUser.email());
        assertNull(storedUser.phoneNumber());
        assertNotNull(storedUser.registrationDate());
        assertEquals("USER", storedUser.role());
        assertEquals("ACTIVE", storedUser.accountStatus());
        assertTrue(storedUser.passwordHash().startsWith("$2"));
        assertTrue(new BCryptPasswordEncoder().matches(VALID_PASSWORD, storedUser.passwordHash()));

        MvcResult loginResult = performLogin(NORMALIZED_EMAIL, VALID_PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        mockMvc.perform(get("/test/protected")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken(loginResult))))
                .andExpect(status().isOk())
                .andExpect(content().string("protected"));

        performLogin(NORMALIZED_EMAIL, "InvalidPassword123")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));
    }

    @Test
    void shouldRejectNormalizedDuplicateEmail() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRegistrationRequest(NORMALIZED_EMAIL)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRegistrationRequest("  NEW.PERSON@EXAMPLE.COM  ")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));
    }

    @Test
    void shouldRejectInvalidRegistration() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Example",
                                  "lastName": "Person",
                                  "email": "invalid@example.com",
                                  "password": "a1",
                                  "phoneNumber": null
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
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

    private String validRegistrationRequest(String email) {
        return """
                {
                  "name": "Example",
                  "lastName": "Person",
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, VALID_PASSWORD);
    }

    private StoredUser storedUser(String email) throws Exception {
        String sql = """
                SELECT id, name, last_name, email, password_hash, phone_number,
                       registration_date, role, account_status
                FROM app_user
                WHERE email = ?
                """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new AssertionError("Registered user was not persisted");
                }

                return new StoredUser(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getString("name"),
                        resultSet.getString("last_name"),
                        resultSet.getString("email"),
                        resultSet.getString("password_hash"),
                        resultSet.getString("phone_number"),
                        resultSet.getTimestamp("registration_date").toInstant(),
                        resultSet.getString("role"),
                        resultSet.getString("account_status")
                );
            }
        }
    }

    private String accessToken(MvcResult result) throws Exception {
        Matcher matcher = Pattern.compile("\\\"accessToken\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"")
                .matcher(result.getResponse().getContentAsString());
        if (!matcher.find()) {
            throw new AssertionError("Missing access token");
        }
        return matcher.group(1);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private record StoredUser(
            UUID id,
            String name,
            String lastName,
            String email,
            String passwordHash,
            String phoneNumber,
            Instant registrationDate,
            String role,
            String accountStatus
    ) {
    }

    @RestController
    static class ProtectedTestController {

        @GetMapping("/test/protected")
        String protectedEndpoint() {
            return "protected";
        }
    }
}
