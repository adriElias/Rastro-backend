package cat.itacademy.rastro.Rastro_backend.user.infrastructure.persistence;

import cat.itacademy.rastro.Rastro_backend.user.application.EmailAlreadyRegisteredException;
import cat.itacademy.rastro.Rastro_backend.user.application.UserCreationPort;
import cat.itacademy.rastro.Rastro_backend.user.application.UserLookupPort;
import cat.itacademy.rastro.Rastro_backend.user.domain.model.Email;
import cat.itacademy.rastro.Rastro_backend.user.domain.model.User;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserPersistenceAdapter implements UserLookupPort, UserCreationPort {

    private static final String UNIQUE_VIOLATION_SQL_STATE = "23505";
    private static final String EMAIL_UNIQUE_CONSTRAINT = "app_user_email_key";

    private final UserJpaRepository repository;
    private final UserPersistenceMapper mapper;

    public UserPersistenceAdapter(UserJpaRepository repository, UserPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return repository.findByEmail(email.value()).map(mapper::toDomain);
    }

    @Override
    public void create(User user) {
        try {
            repository.saveAndFlush(mapper.toJpaEntity(user));
        } catch (DataIntegrityViolationException exception) {
            if (isEmailUniquenessViolation(exception)) {
                throw new EmailAlreadyRegisteredException();
            }
            throw exception;
        }
    }

    private boolean isEmailUniquenessViolation(DataIntegrityViolationException exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException violation) {
                return UNIQUE_VIOLATION_SQL_STATE.equals(violation.getSQLState())
                        && EMAIL_UNIQUE_CONSTRAINT.equals(violation.getConstraintName());
            }
            cause = cause.getCause();
        }
        return false;
    }
}
