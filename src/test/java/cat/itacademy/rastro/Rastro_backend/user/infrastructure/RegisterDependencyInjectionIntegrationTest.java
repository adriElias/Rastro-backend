package cat.itacademy.rastro.Rastro_backend.user.infrastructure;

import cat.itacademy.rastro.Rastro_backend.user.application.PasswordHasher;
import cat.itacademy.rastro.Rastro_backend.user.application.RegisterUserUseCase;
import cat.itacademy.rastro.Rastro_backend.user.application.UserCreationPort;
import cat.itacademy.rastro.Rastro_backend.user.application.UserLookupPort;
import cat.itacademy.rastro.Rastro_backend.user.infrastructure.persistence.UserPersistenceAdapter;
import cat.itacademy.rastro.Rastro_backend.user.infrastructure.security.BCryptPasswordHasher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.ApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Clock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

@Testcontainers
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.sql.init.mode=never"
})
class RegisterDependencyInjectionIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("rastro_register_dependency_test")
                    .withUsername("rastro_integration")
                    .withPassword("rastro_integration");

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private RegisterUserUseCase registerUserUseCase;

    @Autowired
    private PasswordHasher passwordHasher;

    @Autowired
    private UserLookupPort userLookupPort;

    @Autowired
    private UserCreationPort userCreationPort;

    @Autowired
    private Clock clock;

    @Test
    void shouldWireRegisterFlowWithRealAdapters() {
        assertNotNull(registerUserUseCase);
        assertInstanceOf(BCryptPasswordHasher.class, passwordHasher);
        assertInstanceOf(UserPersistenceAdapter.class, userLookupPort);
        assertSame(userLookupPort, userCreationPort);
        assertNotNull(clock);
        assertEquals(1, applicationContext.getBeansOfType(PasswordHasher.class).size());
        assertEquals(1, applicationContext.getBeansOfType(RegisterUserUseCase.class).size());
    }
}
