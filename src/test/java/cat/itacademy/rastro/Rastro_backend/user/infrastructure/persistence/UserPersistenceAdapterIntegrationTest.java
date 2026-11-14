package cat.itacademy.rastro.Rastro_backend.user.infrastructure.persistence;

import cat.itacademy.rastro.Rastro_backend.user.application.EmailAlreadyRegisteredException;
import cat.itacademy.rastro.Rastro_backend.user.domain.model.AccountStatus;
import cat.itacademy.rastro.Rastro_backend.user.domain.model.Email;
import cat.itacademy.rastro.Rastro_backend.user.domain.model.PasswordHash;
import cat.itacademy.rastro.Rastro_backend.user.domain.model.User;
import cat.itacademy.rastro.Rastro_backend.user.domain.model.UserId;
import cat.itacademy.rastro.Rastro_backend.user.domain.model.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.sql.init.mode=never"
})
class UserPersistenceAdapterIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("rastro_user_persistence_test")
                    .withUsername("rastro_integration")
                    .withPassword("rastro_integration");

    @Autowired
    private DataSource dataSource;

    @Autowired
    private UserPersistenceAdapter userPersistenceAdapter;

    @BeforeEach
    void cleanUsers() throws Exception {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM app_user")) {
            statement.executeUpdate();
        }
    }

    @Test
    void shouldFindExistingUserByEmail() throws Exception {
        UUID userId = UUID.randomUUID();
        Instant registrationDate = Instant.parse("2026-01-15T10:30:00Z");
        Email email = new Email("adriana@example.com");

        insertUser(userId, email.value(), registrationDate);

        Optional<User> result = userPersistenceAdapter.findByEmail(email);

        assertTrue(result.isPresent());
        assertEquals(userId, result.orElseThrow().id().value());
        assertEquals(email, result.orElseThrow().email());
    }

    @Test
    void shouldReturnEmptyWhenEmailIsUnknown() {
        Optional<User> result = userPersistenceAdapter.findByEmail(
                new Email("unknown@example.com")
        );

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldSaveAndRetrieveCompleteUser() {
        User user = user(
                UUID.randomUUID(),
                "  ADRIANA@EXAMPLE.COM  ",
                "+34600111222"
        );

        userPersistenceAdapter.create(user);

        Optional<User> result = userPersistenceAdapter.findByEmail(
                new Email("adriana@example.com")
        );

        assertEquals(Optional.of(user), result);
    }

    @Test
    void shouldSaveUserWithoutPhoneNumber() {
        User user = user(UUID.randomUUID(), "no-phone@example.com", null);

        userPersistenceAdapter.create(user);

        User result = userPersistenceAdapter.findByEmail(user.email()).orElseThrow();

        assertEquals(user, result);
        assertNull(result.phoneNumber());
    }

    @Test
    void shouldTranslateDatabaseEmailUniquenessViolation() {
        User existingUser = user(UUID.randomUUID(), "duplicate@example.com", null);
        User duplicateEmailUser = user(UUID.randomUUID(), "duplicate@example.com", null);
        userPersistenceAdapter.create(existingUser);

        assertThrows(
                EmailAlreadyRegisteredException.class,
                () -> userPersistenceAdapter.create(duplicateEmailUser)
        );
    }

    @Test
    void shouldNotTranslateUnrelatedDatabaseConstraintViolation() {
        User userWithoutRequiredRole = new User(
                UserId.generate(),
                "Adriana",
                "Elias",
                new Email("missing-role@example.com"),
                new PasswordHash("$2a$10$persistenceHashValue"),
                null,
                Instant.parse("2026-01-15T10:30:00Z"),
                null,
                AccountStatus.ACTIVE
        );

        assertThrows(
                DataIntegrityViolationException.class,
                () -> userPersistenceAdapter.create(userWithoutRequiredRole)
        );
    }

    private void insertUser(UUID userId, String email, Instant registrationDate) throws Exception {
        String sql = """
                INSERT INTO app_user (
                    id, name, last_name, email, password_hash, phone_number,
                    registration_date, role, account_status
                ) VALUES (?, ?, ?, ?, ?, ?, ?, 'USER', 'ACTIVE')
                """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, userId);
            statement.setString(2, "Adriana");
            statement.setString(3, "Elias");
            statement.setString(4, email);
            statement.setString(5, "$2a$10$exampleHashValue");
            statement.setString(6, null);
            statement.setTimestamp(7, Timestamp.from(registrationDate));
            statement.executeUpdate();
        }
    }

    private User user(UUID id, String email, String phoneNumber) {
        return new User(
                new UserId(id),
                "Adriana",
                "Elias",
                new Email(email),
                new PasswordHash("$2a$10$persistenceHashValue"),
                phoneNumber,
                Instant.parse("2026-01-15T10:30:00Z"),
                UserRole.USER,
                AccountStatus.ACTIVE
        );
    }
}
