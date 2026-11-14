package cat.itacademy.rastro.Rastro_backend.user.infrastructure;

import cat.itacademy.rastro.Rastro_backend.user.application.PasswordHasher;
import cat.itacademy.rastro.Rastro_backend.user.application.RegisterUserUseCase;
import cat.itacademy.rastro.Rastro_backend.user.application.UserCreationPort;
import cat.itacademy.rastro.Rastro_backend.user.application.UserLookupPort;
import cat.itacademy.rastro.Rastro_backend.user.infrastructure.security.BCryptPasswordHasher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Clock;

@Configuration(proxyBeanMethods = false)
public class RegisterConfiguration {

    @Bean
    PasswordHasher passwordHasher() {
        return new BCryptPasswordHasher();
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    RegisterUserUseCase registerUserUseCase(
            UserLookupPort userLookupPort,
            PasswordHasher passwordHasher,
            UserCreationPort userCreationPort,
            Clock clock
    ) {
        return new RegisterUserUseCase(
                userLookupPort,
                passwordHasher,
                userCreationPort,
                clock
        );
    }
}
