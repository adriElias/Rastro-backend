package cat.itacademy.rastro.Rastro_backend.user.infrastructure.persistence;

import cat.itacademy.rastro.Rastro_backend.user.domain.model.Email;
import cat.itacademy.rastro.Rastro_backend.user.domain.model.PasswordHash;
import cat.itacademy.rastro.Rastro_backend.user.domain.model.User;
import cat.itacademy.rastro.Rastro_backend.user.domain.model.UserId;
import org.springframework.stereotype.Component;

@Component
class UserPersistenceMapper {

    UserJpaEntity toJpaEntity(User user) {
        return new UserJpaEntity(
                user.id().value(),
                user.name(),
                user.lastName(),
                user.email().value(),
                user.passwordHash().value(),
                user.phoneNumber(),
                user.registrationDate(),
                user.role(),
                user.accountStatus()
        );
    }

    User toDomain(UserJpaEntity entity) {
        return new User(
                new UserId(entity.getId()),
                entity.getName(),
                entity.getLastName(),
                new Email(entity.getEmail()),
                new PasswordHash(entity.getPasswordHash()),
                entity.getPhoneNumber(),
                entity.getRegistrationDate(),
                entity.getRole(),
                entity.getAccountStatus()
        );
    }
}
