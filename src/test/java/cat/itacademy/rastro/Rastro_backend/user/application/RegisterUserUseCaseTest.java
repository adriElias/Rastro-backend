package cat.itacademy.rastro.Rastro_backend.user.application;

import cat.itacademy.rastro.Rastro_backend.user.domain.model.AccountStatus;
import cat.itacademy.rastro.Rastro_backend.user.domain.model.Email;
import cat.itacademy.rastro.Rastro_backend.user.domain.model.Password;
import cat.itacademy.rastro.Rastro_backend.user.domain.model.PasswordHash;
import cat.itacademy.rastro.Rastro_backend.user.domain.model.User;
import cat.itacademy.rastro.Rastro_backend.user.domain.model.UserId;
import cat.itacademy.rastro.Rastro_backend.user.domain.model.UserRole;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RegisterUserUseCaseTest {

    private static final Instant REGISTRATION_DATE = Instant.parse("2026-11-02T12:00:00Z");
    private static final PasswordHash HASHED_PASSWORD =
            new PasswordHash("$2a$10$hashedPasswordValue");

    @Test
    void shouldRegisterActiveUserWithUserRoleAndSafePublicData() {
        TestDependencies dependencies = new TestDependencies(Optional.empty());

        RegisterResult result = dependencies.useCase.register(validCommand("+34600111222"));

        User createdUser = dependencies.createdUser.get();
        assertNotNull(createdUser.id().value());
        assertEquals(createdUser.id().value(), result.id());
        assertEquals("Adriana", result.name());
        assertEquals("Elias", result.lastName());
        assertEquals("adriana@example.com", result.email());
        assertEquals("+34600111222", result.phoneNumber());
        assertEquals(REGISTRATION_DATE, result.registrationDate());
        assertEquals(UserRole.USER, createdUser.role());
        assertEquals(AccountStatus.ACTIVE, createdUser.accountStatus());
        assertEquals(REGISTRATION_DATE, createdUser.registrationDate());
    }

    @Test
    void shouldRejectDuplicateNormalizedEmailWithoutHashingOrPersisting() {
        TestDependencies dependencies = new TestDependencies(Optional.of(existingUser()));
        RegisterCommand command = new RegisterCommand(
                "Adriana",
                "Elias",
                "  Adriana@Example.COM  ",
                "Valid1",
                null
        );

        assertThrows(
                EmailAlreadyRegisteredException.class,
                () -> dependencies.useCase.register(command)
        );

        assertEquals(new Email("adriana@example.com"), dependencies.lookedUpEmail.get());
        assertEquals(0, dependencies.hashCount.get());
        assertEquals(0, dependencies.creationCount.get());
        assertNull(dependencies.createdUser.get());
    }

    @Test
    void shouldRejectInvalidInputBeforeHashingOrPersisting() {
        List<RegisterCommand> invalidCommands = List.of(
                new RegisterCommand("Al", "Elias", "adriana@example.com", "Valid1", null),
                new RegisterCommand("Adriana", "Li", "adriana@example.com", "Valid1", null),
                new RegisterCommand("Adriana", "Elias", "invalid-email", "Valid1", null),
                new RegisterCommand("Adriana", "Elias", "adriana@example.com", "invalid", null),
                new RegisterCommand(
                        "Adriana",
                        "Elias",
                        "adriana@example.com",
                        "Valid1",
                        "1".repeat(21)
                )
        );

        for (RegisterCommand command : invalidCommands) {
            TestDependencies dependencies = new TestDependencies(Optional.empty());

            assertThrows(
                    IllegalArgumentException.class,
                    () -> dependencies.useCase.register(command)
            );
            assertEquals(0, dependencies.hashCount.get());
            assertEquals(0, dependencies.creationCount.get());
        }
    }

    @Test
    void shouldGiveRawPasswordOnlyToHasherAndPersistOnlyItsHash() {
        TestDependencies dependencies = new TestDependencies(Optional.empty());

        dependencies.useCase.register(validCommand(null));

        assertEquals("Valid1", dependencies.hashedPassword.get().value());
        assertEquals(HASHED_PASSWORD, dependencies.createdUser.get().passwordHash());
        assertNotEquals(
                dependencies.hashedPassword.get().value(),
                dependencies.createdUser.get().passwordHash().value()
        );
    }

    @Test
    void shouldPreserveOptionalPhoneNumber() {
        TestDependencies withPhone = new TestDependencies(Optional.empty());
        TestDependencies withoutPhone = new TestDependencies(Optional.empty());

        RegisterResult resultWithPhone = withPhone.useCase.register(validCommand("+34600111222"));
        RegisterResult resultWithoutPhone = withoutPhone.useCase.register(validCommand(null));

        assertEquals("+34600111222", withPhone.createdUser.get().phoneNumber());
        assertEquals("+34600111222", resultWithPhone.phoneNumber());
        assertNull(withoutPhone.createdUser.get().phoneNumber());
        assertNull(resultWithoutPhone.phoneNumber());
    }

    @Test
    void shouldReturnNoPasswordHashOrAccessToken() {
        List<String> resultFields = Arrays.stream(RegisterResult.class.getRecordComponents())
                .map(component -> component.getName())
                .toList();

        assertFalse(resultFields.contains("password"));
        assertFalse(resultFields.contains("passwordHash"));
        assertFalse(resultFields.contains("accessToken"));
    }

    private RegisterCommand validCommand(String phoneNumber) {
        return new RegisterCommand(
                "  Adriana  ",
                "  Elias  ",
                "  Adriana@Example.COM  ",
                "Valid1",
                phoneNumber
        );
    }

    private User existingUser() {
        return new User(
                UserId.generate(),
                "Adriana",
                "Elias",
                new Email("adriana@example.com"),
                HASHED_PASSWORD,
                null,
                REGISTRATION_DATE,
                UserRole.USER,
                AccountStatus.ACTIVE
        );
    }

    private static class TestDependencies {
        private final AtomicReference<Email> lookedUpEmail = new AtomicReference<>();
        private final AtomicReference<Password> hashedPassword = new AtomicReference<>();
        private final AtomicReference<User> createdUser = new AtomicReference<>();
        private final AtomicInteger hashCount = new AtomicInteger();
        private final AtomicInteger creationCount = new AtomicInteger();
        private final RegisterUserUseCase useCase;

        private TestDependencies(Optional<User> lookupResult) {
            UserLookupPort userLookupPort = email -> {
                lookedUpEmail.set(email);
                return lookupResult;
            };
            PasswordHasher passwordHasher = password -> {
                hashCount.incrementAndGet();
                hashedPassword.set(password);
                return HASHED_PASSWORD;
            };
            UserCreationPort userCreationPort = user -> {
                creationCount.incrementAndGet();
                createdUser.set(user);
            };
            Clock clock = Clock.fixed(REGISTRATION_DATE, ZoneOffset.UTC);

            useCase = new RegisterUserUseCase(
                    userLookupPort,
                    passwordHasher,
                    userCreationPort,
                    clock
            );
        }
    }
}
