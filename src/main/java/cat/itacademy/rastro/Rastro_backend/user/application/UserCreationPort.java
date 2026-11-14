package cat.itacademy.rastro.Rastro_backend.user.application;

import cat.itacademy.rastro.Rastro_backend.user.domain.model.User;

public interface UserCreationPort {

    void create(User user);
}
