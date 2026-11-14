package cat.itacademy.rastro.Rastro_backend.user.application;

import cat.itacademy.rastro.Rastro_backend.user.domain.model.Email;
import cat.itacademy.rastro.Rastro_backend.user.domain.model.User;

import java.util.Optional;

public interface UserLookupPort {

    Optional<User> findByEmail(Email email);
}
