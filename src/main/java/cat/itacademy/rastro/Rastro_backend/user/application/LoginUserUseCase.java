package cat.itacademy.rastro.Rastro_backend.user.application;

import cat.itacademy.rastro.Rastro_backend.user.domain.model.AccountStatus;
import cat.itacademy.rastro.Rastro_backend.user.domain.model.Email;
import cat.itacademy.rastro.Rastro_backend.user.domain.model.User;

public class LoginUserUseCase {

    private final UserLookupPort userLookupPort;
    private final PasswordVerifier passwordVerifier;
    private final TokenIssuer tokenIssuer;

    public LoginUserUseCase(
            UserLookupPort userLookupPort,
            PasswordVerifier passwordVerifier,
            TokenIssuer tokenIssuer
    ) {
        this.userLookupPort = userLookupPort;
        this.passwordVerifier = passwordVerifier;
        this.tokenIssuer = tokenIssuer;
    }

    public LoginResult login(Email email, String rawPassword) {
        User user = userLookupPort.findByEmail(email)
                .orElseThrow(AuthenticationFailedException::new);

        if (user.accountStatus() != AccountStatus.ACTIVE) {
            throw new AuthenticationFailedException();
        }

        if (!passwordVerifier.matches(rawPassword, user.passwordHash())) {
            throw new AuthenticationFailedException();
        }

        String accessToken = tokenIssuer.issueAccessToken(user);

        return new LoginResult(accessToken);
    }
}
