package cat.itacademy.rastro.Rastro_backend.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class ApproximateAgeMigrationFailureIntegrationTest {

    private static final String LEGACY_APPROXIMATE_AGE = "about 3 years";

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("approximate_age_migration_failure_test")
                    .withUsername("rastro_integration")
                    .withPassword("rastro_integration");

    @Test
    void shouldAbortV3WithoutPartialChangesWhenLegacyAgeExists() throws Exception {
        Flyway v2Flyway = flywayAtVersion2();
        v2Flyway.migrate();
        insertLegacyPost();

        Flyway latestFlyway = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .load();

        FlywayException exception = assertThrows(FlywayException.class, latestFlyway::migrate);
        assertTrue(exception.getMessage().contains("non-null legacy values require manual review"));

        assertEquals("2", latestFlyway.info().current().getVersion().getVersion());

        try (Connection connection = connection()) {
            assertLegacyColumnAndValueRemainUnchanged(connection);
            assertFalse(columnExists(connection, "age_unit"));
            assertFalse(typeExists(connection, "age_unit_enum"));
            assertFalse(constraintExists(connection, "chk_post_approx_age_positive"));
            assertFalse(constraintExists(connection, "chk_post_approx_age_unit_pair"));
        }
    }

    private Flyway flywayAtVersion2() {
        return Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .target("2")
                .load();
    }

    private void insertLegacyPost() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();

        try (Connection connection = connection()) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO app_user (id, name, last_name, email, password_hash)
                    VALUES (?, 'Test', 'User', ?, 'hash')
                    """)) {
                statement.setObject(1, userId);
                statement.setString(2, userId + "@example.com");
                statement.executeUpdate();
            }

            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO location (id, latitude, longitude)
                    VALUES (?, 41.387400, 2.168600)
                    """)) {
                statement.setObject(1, locationId);
                statement.executeUpdate();
            }

            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO post (
                        id, user_id, title, description, animal_type_id, created_date,
                        publication_status_id, post_type, location_id, search_radius,
                        lost_at, approx_age
                    )
                    VALUES (
                        ?, ?, 'Legacy report', 'Description',
                        (SELECT id FROM animal_type WHERE title = 'DOG'), ?,
                        (SELECT id FROM publication_status WHERE title = 'ACTIVE'),
                        'LOST', ?, 10.00, ?, ?
                    )
                    """)) {
                OffsetDateTime createdAt = OffsetDateTime.of(
                        2026, 11, 17, 12, 0, 0, 0, ZoneOffset.UTC
                );
                statement.setObject(1, postId);
                statement.setObject(2, userId);
                statement.setObject(3, createdAt);
                statement.setObject(4, locationId);
                statement.setObject(5, createdAt.minusHours(2));
                statement.setString(6, LEGACY_APPROXIMATE_AGE);
                statement.executeUpdate();
            }
        }
    }

    private void assertLegacyColumnAndValueRemainUnchanged(Connection connection) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT data_type, character_maximum_length
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'post'
                  AND column_name = 'approx_age'
                """);
             ResultSet result = statement.executeQuery()) {
            assertTrue(result.next());
            assertEquals("character varying", result.getString("data_type"));
            assertEquals(50, result.getInt("character_maximum_length"));
        }

        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT approx_age FROM post")) {
            assertTrue(result.next());
            assertEquals(LEGACY_APPROXIMATE_AGE, result.getString("approx_age"));
        }
    }

    private boolean columnExists(Connection connection, String columnName) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT EXISTS (
                    SELECT 1
                    FROM information_schema.columns
                    WHERE table_schema = 'public'
                      AND table_name = 'post'
                      AND column_name = ?
                )
                """)) {
            statement.setString(1, columnName);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getBoolean(1);
            }
        }
    }

    private boolean typeExists(Connection connection, String typeName) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("SELECT to_regtype(?)")) {
            statement.setString(1, typeName);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getObject(1) != null;
            }
        }
    }

    private boolean constraintExists(Connection connection, String constraintName) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT EXISTS (
                    SELECT 1
                    FROM pg_constraint
                    WHERE conrelid = 'post'::regclass
                      AND conname = ?
                )
                """)) {
            statement.setString(1, constraintName);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getBoolean(1);
            }
        }
    }

    private Connection connection() throws Exception {
        return DriverManager.getConnection(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword()
        );
    }
}
