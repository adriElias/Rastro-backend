package cat.itacademy.rastro.Rastro_backend.user.application;

import org.junit.jupiter.api.Test;

import cat.itacademy.rastro.Rastro_backend.user.domain.model.AccountStatus;
import cat.itacademy.rastro.Rastro_backend.user.domain.model.Email;
import cat.itacademy.rastro.Rastro_backend.user.domain.model.PasswordHash;
import cat.itacademy.rastro.Rastro_backend.user.domain.model.User;
import cat.itacademy.rastro.Rastro_backend.user.domain.model.UserId;
import cat.itacademy.rastro.Rastro_backend.user.domain.model.UserRole;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LoginUserUseCaseTest {

    @Test
    void shouldIssueAccessTokenForActiveUserWithCorrectPassword() {
        Email email = new Email("adriana@example.com");
        PasswordHash storedHash = new PasswordHash("$2a$10$exampleHashValue");

        User activeUser = new User(
                UserId.generate(),
                "Adriana",
                "Elias",
                email,
                storedHash,
                null,
                Instant.now(),
                UserRole.USER,
                AccountStatus.ACTIVE
        );

        UserLookupPort userLookupPort = lookupEmail -> Optional.of(activeUser);

        PasswordVerifier passwordVerifier = (rawPassword, hash) -> true;

        TokenIssuer tokenIssuer = user -> "fake-access-token";

        LoginUserUseCase useCase =
                new LoginUserUseCase(userLookupPort, passwordVerifier, tokenIssuer);

        LoginResult result = useCase.login(email, "Valid123");

        assertEquals("fake-access-token", result.accessToken());
    }

    @Test
    void shouldFailAuthenticationForUnknownEmail() {
        Email email = new Email("unknown@example.com");

        UserLookupPort userLookupPort = lookupEmail -> Optional.empty();

        PasswordVerifier passwordVerifier = (rawPassword, hash) -> true;

        AtomicInteger tokenIssuedCount = new AtomicInteger(0);
        TokenIssuer tokenIssuer = user -> {
            tokenIssuedCount.incrementAndGet();
            return "fake-access-token";
        };

        LoginUserUseCase useCase =
                new LoginUserUseCase(userLookupPort, passwordVerifier, tokenIssuer);

        assertThrows(
                AuthenticationFailedException.class,
                () -> useCase.login(email, "Invalid123")
        );

        assertEquals(0, tokenIssuedCount.get());
    }

    @Test
    void shouldFailAuthenticationForIncorrectPassword() {
        Email email = new Email("adriana@example.com");
        PasswordHash storedHash = new PasswordHash("$2a$10$exampleHashValue");

        User activeUser = new User(
                UserId.generate(),
                "Adriana",
                "Elias",
                email,
                storedHash,
                null,
                Instant.now(),
                UserRole.USER,
                AccountStatus.ACTIVE
        );

        UserLookupPort userLookupPort = lookupEmail -> Optional.of(activeUser);

        PasswordVerifier passwordVerifier = (rawPassword, hash) -> false;

        AtomicInteger tokenIssuedCount = new AtomicInteger(0);
        TokenIssuer tokenIssuer = user -> {
            tokenIssuedCount.incrementAndGet();
            return "fake-access-token";
        };

        LoginUserUseCase useCase =
                new LoginUserUseCase(userLookupPort, passwordVerifier, tokenIssuer);

        assertThrows(
                AuthenticationFailedException.class,
                () -> useCase.login(email, "WrongPassword123")
        );

        assertEquals(0, tokenIssuedCount.get());
    }

    @Test
    void shouldFailAuthenticationForBlockedAccount() {
        Email email = new Email("adriana@example.com");
        PasswordHash storedHash = new PasswordHash("$2a$10$exampleHashValue");

        User blockedUser = new User(
                UserId.generate(),
                "Adriana",
                "Elias",
                email,
                storedHash,
                null,
                Instant.now(),
                UserRole.USER,
                AccountStatus.BLOCKED
        );

        UserLookupPort userLookupPort = lookupEmail -> Optional.of(blockedUser);

        PasswordVerifier passwordVerifier = (rawPassword, hash) -> true;

        AtomicInteger tokenIssuedCount = new AtomicInteger(0);
        TokenIssuer tokenIssuer = user -> {
            tokenIssuedCount.incrementAndGet();
            return "fake-access-token";
        };

        LoginUserUseCase useCase =
                new LoginUserUseCase(userLookupPort, passwordVerifier, tokenIssuer);

        assertThrows(
                AuthenticationFailedException.class,
                () -> useCase.login(email, "Valid123")
        );

        assertEquals(0, tokenIssuedCount.get());
    }
}
