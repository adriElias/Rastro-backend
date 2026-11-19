package cat.itacademy.rastro.Rastro_backend.shared.infrastructure.openapi;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.sql.init.mode=never",
        "JWT_SECRET=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "JWT_ISSUER=rastro-backend",
        "JWT_ACCESS_TOKEN_TTL_SECONDS=900"
})
@AutoConfigureMockMvc
class OpenApiIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("rastro_openapi_test")
                    .withUsername("rastro_integration")
                    .withPassword("rastro_integration");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldExposeOpenApiDocumentAndPublicAuthEndpointsToAnonymousUsers() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.paths['/api/v1/auth/login'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/register'].post").exists());
    }

    @Test
    void shouldExposeBearerJwtSecurityScheme() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"));
    }

    @Test
    void shouldExposeOpenApiYamlToAnonymousUsers() throws Exception {
        mockMvc.perform(get("/v3/api-docs.yaml"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("openapi:")));
    }

    @Test
    void shouldExposeSwaggerUiAndItsResourcesToAnonymousUsers() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/swagger-ui/index.html"));

        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("Swagger UI")));

        mockMvc.perform(get("/swagger-ui/swagger-initializer.js"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("javascript")))
                .andExpect(content().string(containsString("/v3/api-docs/swagger-config")));

        mockMvc.perform(get("/swagger-ui/swagger-ui.css"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("text/css")));
    }
}
