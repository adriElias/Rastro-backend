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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@Testcontainers
class LostReportMigrationIntegrationTest {

    private static final OffsetDateTime CREATED_AT =
            OffsetDateTime.of(2026, 11, 17, 12, 0, 0, 0, ZoneOffset.UTC);
    private static final UUID EXISTING_USER_ID =
            UUID.fromString("2ebfb1dc-d7b1-4988-ac9a-52f9a427dc95");
    private static final UUID EXISTING_LOCATION_ID =
            UUID.fromString("4974a073-bbe1-40f0-9140-638a88bbd242");
    private static final UUID EXISTING_POST_ID =
            UUID.fromString("1fe919c3-f915-4434-ab1e-636eae16e7bc");

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("lost_report_migration_test")
                    .withUsername("rastro_integration")
                    .withPassword("rastro_integration");

    private static Flyway flyway;
    private static String versionAfterFirstMigration;
    private static String versionBeforeThirdMigration;

    private Connection connection;

    @BeforeAll
    static void migrateFromV1ToLatest() throws SQLException {
        Flyway v1Flyway = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .target("1")
                .load();
        v1Flyway.migrate();
        versionAfterFirstMigration = v1Flyway.info().current().getVersion().getVersion();

        Flyway v2Flyway = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .target("2")
                .load();
        v2Flyway.migrate();
        versionBeforeThirdMigration = v2Flyway.info().current().getVersion().getVersion();
        insertExistingCompatibleReport();

        flyway = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .load();
        flyway.migrate();
    }

    private static void insertExistingCompatibleReport() throws SQLException {
        try (Connection setupConnection = DriverManager.getConnection(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword()
        )) {
            try (PreparedStatement statement = setupConnection.prepareStatement("""
                    INSERT INTO app_user (id, name, last_name, email, password_hash)
                    VALUES (?, 'Legacy', 'Owner', 'legacy@example.com', 'hash')
                    """)) {
                statement.setObject(1, EXISTING_USER_ID);
                statement.executeUpdate();
            }

            try (PreparedStatement statement = setupConnection.prepareStatement("""
                    INSERT INTO location (id, latitude, longitude)
                    VALUES (?, 41.400000, 2.170000)
                    """)) {
                statement.setObject(1, EXISTING_LOCATION_ID);
                statement.executeUpdate();
            }

            try (PreparedStatement statement = setupConnection.prepareStatement("""
                    INSERT INTO post (
                        id, user_id, title, photo, description, animal_type_id,
                        created_date, last_update, publication_status_id,
                        is_safekeeping, approx_age, post_type, location_id,
                        search_radius, lost_at
                    )
                    VALUES (
                        ?, ?, 'Existing lost dog', 'https://example.com/existing.jpg',
                        'Existing report description',
                        (SELECT id FROM animal_type WHERE title = 'DOG'), ?, ?,
                        (SELECT id FROM publication_status WHERE title = 'ACTIVE'),
                        FALSE, NULL, 'LOST', ?, 12.50, ?
                    )
                    """)) {
                statement.setObject(1, EXISTING_POST_ID);
                statement.setObject(2, EXISTING_USER_ID);
                statement.setObject(3, CREATED_AT);
                statement.setObject(4, CREATED_AT.plusHours(1));
                statement.setObject(5, EXISTING_LOCATION_ID);
                statement.setObject(6, CREATED_AT.minusHours(2));
                statement.executeUpdate();
            }
        }
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
    void shouldMigrateFromV1ToLatestWithValidatedConstraints() throws SQLException {
        assertEquals("1", versionAfterFirstMigration);
        assertEquals("3", flyway.info().current().getVersion().getVersion());

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
    void shouldPreserveExistingNullAgePostAndLocationWhenMigratingFromV2ToV3() throws SQLException {
        assertEquals("2", versionBeforeThirdMigration);
        assertEquals("3", flyway.info().current().getVersion().getVersion());

        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT p.id, p.user_id, p.title, p.photo, p.description,
                       p.created_date, p.last_update, p.is_safekeeping,
                       p.approx_age, p.age_unit, p.post_type, p.location_id,
                       p.search_radius, p.lost_at,
                       animal.title AS animal_type,
                       status.title AS publication_status,
                       location.latitude, location.longitude
                FROM post p
                JOIN animal_type animal ON animal.id = p.animal_type_id
                JOIN publication_status status ON status.id = p.publication_status_id
                JOIN location ON location.id = p.location_id
                WHERE p.id = ?
                """)) {
            statement.setObject(1, EXISTING_POST_ID);
            try (ResultSet result = statement.executeQuery()) {
                assertTrue(result.next());
                assertEquals(EXISTING_POST_ID, result.getObject("id", UUID.class));
                assertEquals(EXISTING_USER_ID, result.getObject("user_id", UUID.class));
                assertEquals("Existing lost dog", result.getString("title"));
                assertEquals("https://example.com/existing.jpg", result.getString("photo"));
                assertEquals("Existing report description", result.getString("description"));
                assertEquals(CREATED_AT, result.getObject("created_date", OffsetDateTime.class));
                assertEquals(CREATED_AT.plusHours(1), result.getObject("last_update", OffsetDateTime.class));
                assertFalse(result.getBoolean("is_safekeeping"));
                assertNull(result.getObject("approx_age"));
                assertNull(result.getObject("age_unit"));
                assertEquals("LOST", result.getString("post_type"));
                assertEquals(EXISTING_LOCATION_ID, result.getObject("location_id", UUID.class));
                assertEquals(new BigDecimal("12.50"), result.getBigDecimal("search_radius"));
                assertEquals(CREATED_AT.minusHours(2), result.getObject("lost_at", OffsetDateTime.class));
                assertEquals("DOG", result.getString("animal_type"));
                assertEquals("ACTIVE", result.getString("publication_status"));
                assertEquals(new BigDecimal("41.400000"), result.getBigDecimal("latitude"));
                assertEquals(new BigDecimal("2.170000"), result.getBigDecimal("longitude"));
                assertFalse(result.next());
            }
        }
    }

    @Test
    void shouldNormalizeApproximateAgeSchema() throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT column_name, data_type, udt_name, is_nullable, column_default
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'post'
                  AND column_name IN ('approx_age', 'age_unit')
                ORDER BY column_name
                """);
             ResultSet result = statement.executeQuery()) {
            assertTrue(result.next());
            assertEquals("age_unit", result.getString("column_name"));
            assertEquals("USER-DEFINED", result.getString("data_type"));
            assertEquals("age_unit_enum", result.getString("udt_name"));
            assertEquals("YES", result.getString("is_nullable"));
            assertNull(result.getString("column_default"));

            assertTrue(result.next());
            assertEquals("approx_age", result.getString("column_name"));
            assertEquals("integer", result.getString("data_type"));
            assertEquals("int4", result.getString("udt_name"));
            assertEquals("YES", result.getString("is_nullable"));
            assertNull(result.getString("column_default"));
        }

        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("""
                     SELECT enumlabel
                     FROM pg_enum
                     WHERE enumtypid = 'age_unit_enum'::regtype
                     ORDER BY enumsortorder
                     """)) {
            assertTrue(result.next());
            assertEquals("MONTHS", result.getString("enumlabel"));
            assertTrue(result.next());
            assertEquals("YEARS", result.getString("enumlabel"));
        }

        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT conname, convalidated
                FROM pg_constraint
                WHERE conrelid = 'post'::regclass
                  AND conname IN (
                      'chk_post_approx_age_positive',
                      'chk_post_approx_age_unit_pair'
                  )
                """);
             ResultSet result = statement.executeQuery()) {
            int constraintCount = 0;
            while (result.next()) {
                assertTrue(result.getBoolean("convalidated"));
                constraintCount++;
            }
            assertEquals(2, constraintCount);
        }
    }

    @Test
    void shouldAcceptPositiveApproximateAgeWithMonthsOrYears() throws SQLException {
        assertDoesNotThrow(() -> insertPostWithAge(createReferences(), 2, "MONTHS"));
        assertDoesNotThrow(() -> insertPostWithAge(createReferences(), 3, "YEARS"));
    }

    @Test
    void shouldRejectZeroApproximateAge() throws SQLException {
        assertPostgresConstraintViolation("23514", "chk_post_approx_age_positive", () ->
                insertPostWithAge(createReferences(), 0, "MONTHS"));
    }

    @Test
    void shouldRejectNegativeApproximateAge() throws SQLException {
        assertPostgresConstraintViolation("23514", "chk_post_approx_age_positive", () ->
                insertPostWithAge(createReferences(), -1, "YEARS"));
    }

    @Test
    void shouldRejectApproximateAgeWithoutUnit() throws SQLException {
        assertPostgresConstraintViolation("23514", "chk_post_approx_age_unit_pair", () ->
                insertPostWithAge(createReferences(), 3, null));
    }

    @Test
    void shouldRejectAgeUnitWithoutApproximateAge() throws SQLException {
        assertPostgresConstraintViolation("23514", "chk_post_approx_age_unit_pair", () ->
                insertPostWithAge(createReferences(), null, "YEARS"));
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

    private void insertPostWithAge(
            PostReferences references,
            Integer approximateAge,
            String ageUnit
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
                    lost_at,
                    approx_age,
                    age_unit
                )
                VALUES (?, ?, 'Lost animal report', 'Description', ?, ?, ?, 'LOST', ?, 10.00, ?, ?, ?::age_unit_enum)
                """)) {
            statement.setObject(1, UUID.randomUUID());
            statement.setObject(2, references.userId());
            statement.setObject(3, references.animalTypeId());
            statement.setObject(4, CREATED_AT);
            statement.setObject(5, references.publicationStatusId());
            statement.setObject(6, references.locationId());
            statement.setObject(7, CREATED_AT.minusHours(2));
            statement.setObject(8, approximateAge);
            statement.setString(9, ageUnit);
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
