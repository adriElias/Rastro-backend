package cat.itacademy.rastro.Rastro_backend.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@Testcontainers
class LostReportMigrationIntegrationTest {

    private static final OffsetDateTime CREATED_AT =
            OffsetDateTime.of(2026, 11, 17, 12, 0, 0, 0, ZoneOffset.UTC);

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("lost_report_migration_test")
                    .withUsername("rastro_integration")
                    .withPassword("rastro_integration");

    private static Flyway flyway;
    private static String versionAfterFirstMigration;

    private Connection connection;

    @BeforeAll
    static void migrateFromV1ToLatest() {
        Flyway v1Flyway = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .target("1")
                .load();
        v1Flyway.migrate();
        versionAfterFirstMigration = v1Flyway.info().current().getVersion().getVersion();

        flyway = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .load();
        flyway.migrate();
    }

    @BeforeEach
    void openTransaction() throws SQLException {
        connection = DriverManager.getConnection(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword()
        );
        connection.setAutoCommit(false);
    }

    @AfterEach
    void rollbackTransaction() throws SQLException {
        if (connection != null) {
            connection.rollback();
            connection.close();
        }
    }

    @Test
    void shouldMigrateFromV1ToV2WithValidatedConstraints() throws SQLException {
        assertEquals("1", versionAfterFirstMigration);
        assertEquals("2", flyway.info().current().getVersion().getVersion());

        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT data_type, is_nullable, column_default
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'post'
                  AND column_name = 'lost_at'
                """);
             ResultSet result = statement.executeQuery()) {
            assertTrue(result.next());
            assertEquals("timestamp with time zone", result.getString("data_type"));
            assertEquals("YES", result.getString("is_nullable"));
            assertNull(result.getString("column_default"));
        }

        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT conname, convalidated
                FROM pg_constraint
                WHERE conrelid = 'post'::regclass
                  AND conname IN (
                      'chk_post_lost_at_required',
                      'chk_post_lost_at_not_after_creation',
                      'chk_post_search_radius_max',
                      'uq_post_location_id'
                  )
                """);
             ResultSet result = statement.executeQuery()) {
            int constraintCount = 0;
            while (result.next()) {
                assertTrue(result.getBoolean("convalidated"));
                constraintCount++;
            }
            assertEquals(4, constraintCount);
        }

        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT col_description('post'::regclass, ordinal_position)
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'post'
                  AND column_name = 'search_radius'
                """);
             ResultSet result = statement.executeQuery()) {
            assertTrue(result.next());
            assertTrue(result.getString(1).contains("kilometers"));
        }
    }

    @Test
    void shouldRejectLostPostWithoutLostAt() throws SQLException {
        PostReferences references = createReferences();

        assertPostgresConstraintViolation("23514", "chk_post_lost_at_required", () -> insertPost(
                references,
                "LOST",
                new BigDecimal("10.00"),
                CREATED_AT,
                null
        ));
    }

    @Test
    void shouldAcceptLostPostWithValidLostAt() throws SQLException {
        PostReferences references = createReferences();

        assertDoesNotThrow(() -> insertPost(
                references,
                "LOST",
                new BigDecimal("10.00"),
                CREATED_AT,
                CREATED_AT.minusHours(2)
        ));
    }

    @Test
    void shouldRejectLostAtAfterCreatedDate() throws SQLException {
        PostReferences references = createReferences();

        assertPostgresConstraintViolation("23514", "chk_post_lost_at_not_after_creation", () -> insertPost(
                references,
                "LOST",
                new BigDecimal("10.00"),
                CREATED_AT,
                CREATED_AT.plusSeconds(1)
        ));
    }

    @Test
    void shouldAcceptFoundPostWithoutLostAt() throws SQLException {
        PostReferences references = createReferences();

        assertDoesNotThrow(() -> insertPost(
                references,
                "FOUND",
                new BigDecimal("10.00"),
                CREATED_AT,
                null
        ));
    }

    @Test
    void shouldRejectZeroSearchRadius() throws SQLException {
        PostReferences references = createReferences();

        assertPostgresConstraintViolation("23514", "chk_post_search_radius", () -> insertPost(
                references,
                "FOUND",
                BigDecimal.ZERO,
                CREATED_AT,
                null
        ));
    }

    @Test
    void shouldRejectNegativeSearchRadius() throws SQLException {
        PostReferences references = createReferences();

        assertPostgresConstraintViolation("23514", "chk_post_search_radius", () -> insertPost(
                references,
                "FOUND",
                new BigDecimal("-0.01"),
                CREATED_AT,
                null
        ));
    }

    @Test
    void shouldAcceptMinimumPositiveSearchRadius() throws SQLException {
        PostReferences references = createReferences();

        assertDoesNotThrow(() -> insertPost(
                references,
                "FOUND",
                new BigDecimal("0.01"),
                CREATED_AT,
                null
        ));
    }

    @Test
    void shouldAcceptMaximumSearchRadius() throws SQLException {
        PostReferences references = createReferences();

        assertDoesNotThrow(() -> insertPost(
                references,
                "FOUND",
                new BigDecimal("50.00"),
                CREATED_AT,
                null
        ));
    }

    @Test
    void shouldRejectSearchRadiusGreaterThanMaximum() throws SQLException {
        PostReferences references = createReferences();

        assertPostgresConstraintViolation("23514", "chk_post_search_radius_max", () -> insertPost(
                references,
                "FOUND",
                new BigDecimal("50.01"),
                CREATED_AT,
                null
        ));
    }

    @Test
    void shouldRejectSharedLocationBetweenPosts() throws SQLException {
        PostReferences firstReferences = createReferences();
        insertPost(firstReferences, "FOUND", new BigDecimal("10.00"), CREATED_AT, null);
        PostReferences secondReferences = createReferences(firstReferences.locationId());

        assertPostgresConstraintViolation("23505", "uq_post_location_id", () -> insertPost(
                secondReferences,
                "FOUND",
                new BigDecimal("10.00"),
                CREATED_AT,
                null
        ));
    }

    @Test
    void shouldPreserveExistingPostConstraints() throws SQLException {
        Set<String> expectedConstraints = Set.of(
                "post_pkey",
                "fk_post_user",
                "fk_post_animal_type",
                "fk_post_publication_status",
                "fk_post_location",
                "chk_post_search_radius"
        );

        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("""
                     SELECT conname, convalidated
                     FROM pg_constraint
                     WHERE conrelid = 'post'::regclass
                     """)) {
            Set<String> actualConstraints = new java.util.HashSet<>();
            while (result.next()) {
                assertTrue(result.getBoolean("convalidated"));
                actualConstraints.add(result.getString("conname"));
            }
            assertTrue(actualConstraints.containsAll(expectedConstraints));
        }
    }

    @Test
    void shouldPreserveLocationForeignKeyBehavior() throws SQLException {
        PostReferences references = createReferences();
        PostReferences invalidReferences = new PostReferences(
                references.userId(),
                references.animalTypeId(),
                references.publicationStatusId(),
                UUID.randomUUID()
        );

        assertPostgresConstraintViolation("23503", "fk_post_location", () -> insertPost(
                invalidReferences,
                "FOUND",
                new BigDecimal("10.00"),
                CREATED_AT,
                null
        ));
    }

    private void assertPostgresConstraintViolation(
            String expectedSqlState,
            String expectedConstraint,
            Executable operation
    ) {
        SQLException exception = assertThrows(SQLException.class, operation);
        PSQLException postgresException = findPostgresException(exception);
        ServerErrorMessage serverErrorMessage = postgresException.getServerErrorMessage();

        assertEquals(expectedSqlState, postgresException.getSQLState());
        assertNotNull(serverErrorMessage);
        assertEquals(expectedConstraint, serverErrorMessage.getConstraint());
    }

    private PSQLException findPostgresException(SQLException exception) {
        SQLException currentException = exception;
        while (currentException != null) {
            Throwable currentCause = currentException;
            while (currentCause != null) {
                if (currentCause instanceof PSQLException postgresException) {
                    return postgresException;
                }
                currentCause = currentCause.getCause();
            }
            currentException = currentException.getNextException();
        }

        return fail("Expected a PostgreSQL exception", exception);
    }

    private PostReferences createReferences() throws SQLException {
        UUID locationId = UUID.randomUUID();
        insertLocation(locationId);
        return createReferences(locationId);
    }

    private PostReferences createReferences(UUID locationId) throws SQLException {
        UUID userId = UUID.randomUUID();
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO app_user (id, name, last_name, email, password_hash)
                VALUES (?, 'Test', 'User', ?, 'hash')
                """)) {
            statement.setObject(1, userId);
            statement.setString(2, userId + "@example.com");
            statement.executeUpdate();
        }

        return new PostReferences(
                userId,
                lookupId("animal_type", "DOG"),
                lookupId("publication_status", "ACTIVE"),
                locationId
        );
    }

    private void insertLocation(UUID locationId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO location (id, latitude, longitude)
                VALUES (?, 41.387400, 2.168600)
                """)) {
            statement.setObject(1, locationId);
            statement.executeUpdate();
        }
    }

    private UUID lookupId(String table, String title) throws SQLException {
        String sql = "SELECT id FROM " + table + " WHERE title = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, title);
            try (ResultSet result = statement.executeQuery()) {
                assertTrue(result.next());
                return result.getObject("id", UUID.class);
            }
        }
    }

    private void insertPost(
            PostReferences references,
            String postType,
            BigDecimal searchRadius,
            OffsetDateTime createdDate,
            OffsetDateTime lostAt
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO post (
                    id,
                    user_id,
                    title,
                    description,
                    animal_type_id,
                    created_date,
                    publication_status_id,
                    post_type,
                    location_id,
                    search_radius,
                    lost_at
                )
                VALUES (?, ?, 'Lost animal report', 'Description', ?, ?, ?, ?::post_type_enum, ?, ?, ?)
                """)) {
            statement.setObject(1, UUID.randomUUID());
            statement.setObject(2, references.userId());
            statement.setObject(3, references.animalTypeId());
            statement.setObject(4, createdDate);
            statement.setObject(5, references.publicationStatusId());
            statement.setString(6, postType);
            statement.setObject(7, references.locationId());
            statement.setBigDecimal(8, searchRadius);
            statement.setObject(9, lostAt);
            statement.executeUpdate();
        }
    }

    private record PostReferences(
            UUID userId,
            UUID animalTypeId,
            UUID publicationStatusId,
            UUID locationId
    ) {
    }
}
