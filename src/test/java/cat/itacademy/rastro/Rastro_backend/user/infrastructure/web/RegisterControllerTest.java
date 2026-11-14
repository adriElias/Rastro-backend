package cat.itacademy.rastro.Rastro_backend.user.infrastructure.web;

import cat.itacademy.rastro.Rastro_backend.user.application.EmailAlreadyRegisteredException;
import cat.itacademy.rastro.Rastro_backend.user.application.RegisterCommand;
import cat.itacademy.rastro.Rastro_backend.user.application.RegisterResult;
import cat.itacademy.rastro.Rastro_backend.user.application.RegisterUserUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RegisterController.class)
@AutoConfigureMockMvc(addFilters = false)
class RegisterControllerTest {

    private static final UUID USER_ID =
            UUID.fromString("b5c8610d-93c4-4b82-97e1-1a5873cbaf5a");
    private static final Instant REGISTRATION_DATE =
            Instant.parse("2026-11-02T15:30:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegisterUserUseCase registerUserUseCase;

    @Test
    void shouldReturnCreatedWithSafePublicRegistrationData() throws Exception {
        RegisterCommand command = new RegisterCommand(
                "Example",
                "Person",
                "person@example.com",
                "Password123",
                null
        );
        when(registerUserUseCase.register(command)).thenReturn(result());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(USER_ID.toString()))
                .andExpect(jsonPath("$.name").value("Example"))
                .andExpect(jsonPath("$.lastName").value("Person"))
                .andExpect(jsonPath("$.email").value("person@example.com"))
                .andExpect(jsonPath("$.phoneNumber").doesNotExist())
                .andExpect(jsonPath("$.registrationDate").value(REGISTRATION_DATE.toString()))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(jsonPath("$.role").doesNotExist())
                .andExpect(jsonPath("$.accountStatus").doesNotExist());
    }

    @Test
    void shouldReturnBadRequestWhenRequiredFieldIsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Example",
                                  "lastName": "Person",
                                  "email": "person@example.com"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("The registration request is invalid"));

        verifyNoInteractions(registerUserUseCase);
    }

    @Test
    void shouldReturnBadRequestForInvalidDomainValues() throws Exception {
        when(registerUserUseCase.register(any(RegisterCommand.class)))
                .thenThrow(new IllegalArgumentException("Internal validation detail"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("The registration request is invalid"));
    }

    @Test
    void shouldReturnConflictWhenEmailIsAlreadyRegistered() throws Exception {
        when(registerUserUseCase.register(any(RegisterCommand.class)))
                .thenThrow(new EmailAlreadyRegisteredException());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"))
                .andExpect(jsonPath("$.message").value("Email already registered"));
    }

    @Test
    void shouldReturnBadRequestForMalformedJson() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("The registration request is invalid"));

        verifyNoInteractions(registerUserUseCase);
    }

    @Test
    void shouldNotAllowRoleOrAccountStatusToBeControlledByPublicInput() throws Exception {
        RegisterCommand expectedCommand = new RegisterCommand(
                "Example",
                "Person",
                "person@example.com",
                "Password123",
                null
        );
        when(registerUserUseCase.register(expectedCommand)).thenReturn(result());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Example",
                                  "lastName": "Person",
                                  "email": "person@example.com",
                                  "password": "Password123",
                                  "phoneNumber": null,
                                  "role": "ADMIN",
                                  "accountStatus": "BLOCKED"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").doesNotExist())
                .andExpect(jsonPath("$.accountStatus").doesNotExist());

        verify(registerUserUseCase).register(expectedCommand);
    }

    private RegisterResult result() {
        return new RegisterResult(
                USER_ID,
                "Example",
                "Person",
                "person@example.com",
                null,
                REGISTRATION_DATE
        );
    }

    private String validRequest() {
        return """
                {
                  "name": "Example",
                  "lastName": "Person",
                  "email": "person@example.com",
                  "password": "Password123",
                  "phoneNumber": null
                }
                """;
    }
}
