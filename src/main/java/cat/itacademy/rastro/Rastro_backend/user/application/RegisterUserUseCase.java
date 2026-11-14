package cat.itacademy.rastro.Rastro_backend.user.application;

import cat.itacademy.rastro.Rastro_backend.user.domain.model.AccountStatus;
import cat.itacademy.rastro.Rastro_backend.user.domain.model.Email;
import cat.itacademy.rastro.Rastro_backend.user.domain.model.Password;
import cat.itacademy.rastro.Rastro_backend.user.domain.model.PasswordHash;
import cat.itacademy.rastro.Rastro_backend.user.domain.model.User;
import cat.itacademy.rastro.Rastro_backend.user.domain.model.UserId;
import cat.itacademy.rastro.Rastro_backend.user.domain.model.UserRole;

import java.time.Clock;

public class RegisterUserUseCase {

    private static final PasswordHash VALIDATION_PASSWORD_HASH =
            new PasswordHash("registration-validation-only");

    private final UserLookupPort userLookupPort;
    private final PasswordHasher passwordHasher;
    private final UserCreationPort userCreationPort;
    private final Clock clock;

    public RegisterUserUseCase(
            UserLookupPort userLookupPort,
            PasswordHasher passwordHasher,
            UserCreationPort userCreationPort,
            Clock clock
    ) {
        this.userLookupPort = userLookupPort;
        this.passwordHasher = passwordHasher;
        this.userCreationPort = userCreationPort;
        this.clock = clock;
    }

    public RegisterResult register(RegisterCommand command) {
        Email email = new Email(command.email());
        Password password = new Password(command.password());

        // Construct the aggregate first so every domain invariant fails before any output port runs.
        User candidate = new User(
                UserId.generate(),
                command.name(),
                command.lastName(),
                email,
                VALIDATION_PASSWORD_HASH,
                command.phoneNumber(),
                clock.instant(),
                UserRole.USER,
                AccountStatus.ACTIVE
        );

        if (userLookupPort.findByEmail(email).isPresent()) {
            throw new EmailAlreadyRegisteredException();
        }

        PasswordHash passwordHash = passwordHasher.hash(password);
        User user = withPasswordHash(candidate, passwordHash);
        userCreationPort.create(user);

        return new RegisterResult(
                user.id().value(),
                user.name(),
                user.lastName(),
                user.email().value(),
                user.phoneNumber(),
                user.registrationDate()
        );
    }

    private User withPasswordHash(User user, PasswordHash passwordHash) {
        return new User(
                user.id(),
                user.name(),
                user.lastName(),
                user.email(),
                passwordHash,
                user.phoneNumber(),
                user.registrationDate(),
                user.role(),
                user.accountStatus()
        );
    }
}
