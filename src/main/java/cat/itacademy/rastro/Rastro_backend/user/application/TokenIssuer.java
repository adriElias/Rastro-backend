package cat.itacademy.rastro.Rastro_backend.user.application;

import cat.itacademy.rastro.Rastro_backend.user.domain.model.User;

public interface TokenIssuer {

    String issueAccessToken(User user);
}
