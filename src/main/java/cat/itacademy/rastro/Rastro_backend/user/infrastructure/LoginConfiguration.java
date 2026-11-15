package cat.itacademy.rastro.Rastro_backend.user.infrastructure;

import cat.itacademy.rastro.Rastro_backend.shared.infrastructure.security.JwtTokenIssuer;
import cat.itacademy.rastro.Rastro_backend.shared.infrastructure.security.JwtSettings;
import cat.itacademy.rastro.Rastro_backend.user.application.LoginUserUseCase;
import cat.itacademy.rastro.Rastro_backend.user.application.PasswordVerifier;
import cat.itacademy.rastro.Rastro_backend.user.application.TokenIssuer;
import cat.itacademy.rastro.Rastro_backend.user.application.UserLookupPort;
import cat.itacademy.rastro.Rastro_backend.user.infrastructure.security.BCryptPasswordVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Base64;
import java.time.Clock;
import java.time.Duration;

@Configuration(proxyBeanMethods = false)
public class LoginConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    PasswordVerifier passwordVerifier() {
        return new BCryptPasswordVerifier();
    }

    @Bean
    JwtSettings jwtSettings(
            @Value("${JWT_SECRET}") String secret,
            @Value("${JWT_ISSUER}") String issuer,
            @Value("${JWT_ACCESS_TOKEN_TTL_SECONDS}") long accessTokenTtlSeconds
    ) {
        byte[] decodedSecret = Base64.getDecoder().decode(secret);

        return new JwtSettings(
                decodedSecret,
                issuer,
                Duration.ofSeconds(accessTokenTtlSeconds)
        );
    }

    @Bean
    TokenIssuer tokenIssuer(JwtSettings jwtSettings, Clock clock) {
        return new JwtTokenIssuer(
                jwtSettings.signingKey(),
                jwtSettings.issuer(),
                jwtSettings.accessTokenLifetime(),
                clock
        );
    }

    @Bean
    LoginUserUseCase loginUserUseCase(
            UserLookupPort userLookupPort,
            PasswordVerifier passwordVerifier,
            TokenIssuer tokenIssuer
    ) {
        return new LoginUserUseCase(userLookupPort, passwordVerifier, tokenIssuer);
    }
}
