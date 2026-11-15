package cat.itacademy.rastro.Rastro_backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(properties = {
		"spring.jpa.hibernate.ddl-auto=none",
		"spring.sql.init.mode=never",
		"JWT_SECRET=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
		"JWT_ISSUER=rastro-backend",
		"JWT_ACCESS_TOKEN_TTL_SECONDS=900"
})
class RastroBackendApplicationTests {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer<?> postgres =
			new PostgreSQLContainer<>("postgres:16-alpine")
					.withDatabaseName("rastro_context_test")
					.withUsername("rastro_integration")
					.withPassword("rastro_integration");

	@Test
	void contextLoads() {
	}

}
