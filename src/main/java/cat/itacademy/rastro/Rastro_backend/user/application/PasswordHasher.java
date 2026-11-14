package cat.itacademy.rastro.Rastro_backend.user.application;

import cat.itacademy.rastro.Rastro_backend.user.domain.model.Password;
import cat.itacademy.rastro.Rastro_backend.user.domain.model.PasswordHash;

public interface PasswordHasher {

    PasswordHash hash(Password password);
}
