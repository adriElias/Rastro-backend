package cat.itacademy.rastro.Rastro_backend.post;

import cat.itacademy.rastro.Rastro_backend.shared.infrastructure.security.JwtTokenIssuer;
import cat.itacademy.rastro.Rastro_backend.user.domain.model.AccountStatus;
import cat.itacademy.rastro.Rastro_backend.user.domain.model.Email;
import cat.itacademy.rastro.Rastro_backend.user.domain.model.PasswordHash;
import cat.itacademy.rastro.Rastro_backend.user.domain.model.User;
import cat.itacademy.rastro.Rastro_backend.user.domain.model.UserId;
import cat.itacademy.rastro.Rastro_backend.user.domain.model.UserRole;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.sql.init.mode=never",
        "JWT_SECRET=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "JWT_ISSUER=rastro-backend",
        "JWT_ACCESS_TOKEN_TTL_SECONDS=900"
})
@AutoConfigureMockMvc
class LostReportEndToEndIntegrationTest {

    private static final UUID USER_ID = UUID.fromString("8b24cc18-f243-4dc6-a809-c755c28d61a4");
    private static final UUID MISSING_USER_ID = UUID.fromString("98d4cb4e-e4f0-4a18-83d1-d66822076379");
    private static final byte[] SIGNING_KEY = Base64.getDecoder().decode(
            "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
    );

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("lost_report_e2e_test")
                    .withUsername("rastro_integration")
                    .withPassword("rastro_integration");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void clearReportsAndUsers() throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM post");
            statement.executeUpdate("DELETE FROM location");
            statement.executeUpdate("DELETE FROM app_user");
        }
    }

    @Test
    void shouldRejectAnonymousRequestWithoutPersistingData() throws Exception {
        mockMvc.perform(post("/api/v1/lost-reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestWithAge("2", "MONTHS")))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Bearer")))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"))
                .andExpect(jsonPath("$.message").value("Authentication failed"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(0));

        assertEquals(0, countRows("post"));
        assertEquals(0, countRows("location"));
    }

    @Test
    void shouldCreateAndPersistLostReportForAuthenticatedUser() throws Exception {
        insertUser(USER_ID);

        mockMvc.perform(post("/api/v1/lost-reports")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestWithAge("2", "MONTHS")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.title").value("Lost dog"))
                .andExpect(jsonPath("$.animalType").value("DOG"))
                .andExpect(jsonPath("$.approxAge").value(2))
                .andExpect(jsonPath("$.ageUnit").value("MONTHS"))
                .andExpect(jsonPath("$.photoUrl").value("https://example.com/animal.jpg"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        StoredReport report = storedReport();
        assertEquals(USER_ID, report.userId());
        assertEquals("LOST", report.postType());
        assertEquals("ACTIVE", report.status());
        assertEquals("DOG", report.animalType());
        assertFalse(report.safekeeping());
        assertEquals(2, report.approxAge());
        assertEquals("MONTHS", report.ageUnit());
        assertEquals("https://example.com/animal.jpg", report.photo());
        assertEquals(new BigDecimal("5.00"), report.searchRadius());
        assertEquals(new BigDecimal("41.387400"), report.latitude());
        assertEquals(new BigDecimal("2.168600"), report.longitude());
        assertEquals(1, countRows("location"));
    }

    @Test
    void shouldPersistUnknownAgeAndMissingPhotoAsNull() throws Exception {
        insertUser(USER_ID);

        mockMvc.perform(post("/api/v1/lost-reports")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestWithoutOptionals()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.approxAge").doesNotExist())
                .andExpect(jsonPath("$.ageUnit").doesNotExist())
                .andExpect(jsonPath("$.photoUrl").doesNotExist());

        StoredReport report = storedReport();
        assertNull(report.approxAge());
        assertNull(report.ageUnit());
        assertNull(report.photo());
    }

    @Test
    void shouldReportMissingRequiredFieldsWithoutPersistingData() throws Exception {
        insertUser(USER_ID);

        mockMvc.perform(post("/api/v1/lost-reports")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("The lost report request is invalid"))
                .andExpect(jsonPath("$.fieldErrors[*].field").value(containsInAnyOrder(
                        "title",
                        "description",
                        "animalType",
                        "lostAt",
                        "latitude",
                        "longitude",
                        "searchRadiusKm"
                )));

        assertEquals(0, countRows("post"));
        assertEquals(0, countRows("location"));
    }

    @Test
    void shouldRejectFractionalApproximateAgeWithoutTruncatingOrPersisting() throws Exception {
        insertUser(USER_ID);

        mockMvc.perform(post("/api/v1/lost-reports")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestWithAge("2.5", "YEARS")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(0));

        assertEquals(0, countRows("post"));
        assertEquals(0, countRows("location"));
    }

    @Test
    void shouldRejectIncompleteApproximateAgeWithoutPersistingData() throws Exception {
        insertUser(USER_ID);

        mockMvc.perform(post("/api/v1/lost-reports")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestWithAge("3", "null")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("ageUnit"));

        assertEquals(0, countRows("post"));
        assertEquals(0, countRows("location"));
    }

    @Test
    void shouldRejectUnknownAgeUnitWithoutPersistingData() throws Exception {
        insertUser(USER_ID);

        mockMvc.perform(post("/api/v1/lost-reports")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestWithAge("3", "WEEKS")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(0));

        assertEquals(0, countRows("post"));
        assertEquals(0, countRows("location"));
    }

    @Test
    void shouldRollbackLocationWhenPostPersistenceFails() throws Exception {
        ServletException exception = assertThrows(ServletException.class, () ->
                mockMvc.perform(post("/api/v1/lost-reports")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(MISSING_USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestWithAge("3", "YEARS")))
        );

        assertTrue(
                hasPostUserForeignKeyViolation(exception),
                "Expected fk_post_user or SQLSTATE 23503 in the exception chain"
        );
        assertEquals(0, countRows("post"));
        assertEquals(0, countRows("location"));
    }

    @Test
    void shouldDocumentProtectedLostReportEndpointInOpenApi() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/lost-reports'].post").exists())
                .andExpect(jsonPath(
                        "$.paths['/api/v1/lost-reports'].post.security[0].bearerAuth"
                ).isArray())
                .andExpect(jsonPath(
                        "$.components.schemas.CreateLostReportRequest.properties.ageUnit.enum[0]"
                ).value("MONTHS"))
                .andExpect(jsonPath(
                        "$.components.schemas.CreateLostReportRequest.properties.ageUnit.enum[1]"
                ).value("YEARS"))
                .andExpect(jsonPath(
                        "$.components.schemas.CreateLostReportRequest.properties.approxAge.type"
                ).value("integer"))
                .andExpect(jsonPath(
                        "$.components.schemas.CreateLostReportRequest.required"
                ).value(containsInAnyOrder(
                        "title",
                        "description",
                        "animalType",
                        "lostAt",
                        "latitude",
                        "longitude",
                        "searchRadiusKm"
                )))
                .andExpect(jsonPath(
                        "$.paths['/api/v1/lost-reports'].post.responses['401']"
                                + ".content['application/json'].schema.$ref"
                ).value("#/components/schemas/ApiErrorResponse"));
    }

    private boolean hasPostUserForeignKeyViolation(Throwable error) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (current instanceof SQLException sqlException
                    && "23503".equals(sqlException.getSQLState())) {
                return true;
            }
            if (current.getMessage() != null && current.getMessage().contains("fk_post_user")) {
                return true;
            }
        }
        return false;
    }

    private void insertUser(UUID userId) throws Exception {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO app_user (id, name, last_name, email, password_hash)
                     VALUES (?, 'Adriana', 'Elias', ?, 'hash')
                     """)) {
            statement.setObject(1, userId);
            statement.setString(2, userId + "@example.com");
            statement.executeUpdate();
        }
    }

    private StoredReport storedReport() throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("""
                     SELECT p.user_id, p.post_type, p.is_safekeeping, p.approx_age,
                            p.age_unit, p.photo, p.search_radius,
                            animal.title AS animal_type,
                            status.title AS publication_status,
                            location.latitude, location.longitude
                     FROM post p
                     JOIN animal_type animal ON animal.id = p.animal_type_id
                     JOIN publication_status status ON status.id = p.publication_status_id
                     JOIN location ON location.id = p.location_id
                     """)) {
            if (!result.next()) {
                throw new AssertionError("Lost report was not persisted");
            }
            return new StoredReport(
                    result.getObject("user_id", UUID.class),
                    result.getString("post_type"),
                    result.getBoolean("is_safekeeping"),
                    result.getObject("approx_age", Integer.class),
                    result.getString("age_unit"),
                    result.getString("photo"),
                    result.getBigDecimal("search_radius"),
                    result.getString("animal_type"),
                    result.getString("publication_status"),
                    result.getBigDecimal("latitude"),
                    result.getBigDecimal("longitude")
            );
        }
    }

    private long countRows(String table) throws Exception {
        String sql = "SELECT count(*) FROM " + table;
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getLong(1);
        }
    }

    private String tokenFor(UUID userId) {
        JwtTokenIssuer issuer = new JwtTokenIssuer(
                SIGNING_KEY,
                "rastro-backend",
                Duration.ofMinutes(15),
                Clock.systemUTC()
        );
        return issuer.issueAccessToken(new User(
                new UserId(userId),
                "Adriana",
                "Elias",
                new Email(userId + "@example.com"),
                new PasswordHash("stored-password-hash"),
                null,
                Instant.now(),
                UserRole.USER,
                AccountStatus.ACTIVE
        ));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String validRequestWithAge(String approximateAge, String ageUnit) {
        return """
                {
                  "title": "Lost dog",
                  "description": "Brown dog with a blue collar",
                  "animalType": "DOG",
                  "lostAt": "2026-11-20T17:30:00Z",
                  "latitude": 41.387400,
                  "longitude": 2.168600,
                  "searchRadiusKm": 5.00,
                  "approxAge": %s,
                  "ageUnit": %s,
                  "photoUrl": "https://example.com/animal.jpg"
                }
                """.formatted(approximateAge, jsonStringOrNull(ageUnit));
    }

    private String validRequestWithoutOptionals() {
        return """
                {
                  "title": "Lost dog",
                  "description": "Brown dog with a blue collar",
                  "animalType": "DOG",
                  "lostAt": "2026-11-20T17:30:00Z",
                  "latitude": 41.387400,
                  "longitude": 2.168600,
                  "searchRadiusKm": 5.00
                }
                """;
    }

    private String jsonStringOrNull(String value) {
        return "null".equals(value) ? "null" : "\"" + value + "\"";
    }

    private record StoredReport(
            UUID userId,
            String postType,
            boolean safekeeping,
            Integer approxAge,
            String ageUnit,
            String photo,
            BigDecimal searchRadius,
            String animalType,
            String status,
            BigDecimal latitude,
            BigDecimal longitude
    ) {
    }
}
