package cat.itacademy.rastro.Rastro_backend.user.infrastructure;

import cat.itacademy.rastro.Rastro_backend.shared.infrastructure.security.JwtTokenIssuer;
import cat.itacademy.rastro.Rastro_backend.user.application.LoginUserUseCase;
import cat.itacademy.rastro.Rastro_backend.user.application.PasswordVerifier;
import cat.itacademy.rastro.Rastro_backend.user.application.TokenIssuer;
import cat.itacademy.rastro.Rastro_backend.user.application.UserLookupPort;
import cat.itacademy.rastro.Rastro_backend.user.infrastructure.persistence.UserPersistenceAdapter;
import cat.itacademy.rastro.Rastro_backend.user.infrastructure.security.BCryptPasswordVerifier;
import cat.itacademy.rastro.Rastro_backend.user.infrastructure.web.LoginController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Testcontainers
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.sql.init.mode=never",
        "JWT_SECRET=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "JWT_ISSUER=rastro-backend",
        "JWT_ACCESS_TOKEN_TTL_SECONDS=900"
})
class LoginDependencyInjectionIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("rastro_login_dependency_test")
                    .withUsername("rastro_integration")
                    .withPassword("rastro_integration");

    @Autowired
    private LoginController loginController;

    @Autowired
    private LoginUserUseCase loginUserUseCase;

    @Autowired
    private UserLookupPort userLookupPort;

    @Autowired
    private PasswordVerifier passwordVerifier;

    @Autowired
    private TokenIssuer tokenIssuer;

    @Test
    void shouldWireLoginFlowWithRealAdapters() {
        assertNotNull(loginController);
        assertNotNull(loginUserUseCase);
        assertInstanceOf(UserPersistenceAdapter.class, userLookupPort);
        assertInstanceOf(BCryptPasswordVerifier.class, passwordVerifier);
        assertInstanceOf(JwtTokenIssuer.class, tokenIssuer);
    }
}
