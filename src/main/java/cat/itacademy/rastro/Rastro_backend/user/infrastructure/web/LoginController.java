package cat.itacademy.rastro.Rastro_backend.user.infrastructure.web;

import cat.itacademy.rastro.Rastro_backend.user.application.AuthenticationFailedException;
import cat.itacademy.rastro.Rastro_backend.user.application.LoginResult;
import cat.itacademy.rastro.Rastro_backend.user.application.LoginUserUseCase;
import cat.itacademy.rastro.Rastro_backend.user.domain.model.Email;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class LoginController {

    private final LoginUserUseCase loginUserUseCase;

    public LoginController(LoginUserUseCase loginUserUseCase) {
        this.loginUserUseCase = loginUserUseCase;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        if (request.email() == null || request.email().isBlank()
                || request.password() == null || request.password().isBlank()) {
            throw new InvalidLoginRequestException();
        }

        Email email;
        try {
            email = new Email(request.email());
        } catch (IllegalArgumentException exception) {
            throw new InvalidLoginRequestException();
        }

        LoginResult result = loginUserUseCase.login(email, request.password());
        return new LoginResponse(result.accessToken());
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleAuthenticationFailure(AuthenticationFailedException exception) {
        return new ErrorResponse("AUTHENTICATION_FAILED", "Authentication failed");
    }

    @ExceptionHandler({InvalidLoginRequestException.class, HttpMessageNotReadableException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleInvalidRequest(Exception exception) {
        return new ErrorResponse("INVALID_REQUEST", "The login request is invalid");
    }

    public record LoginRequest(String email, String password) {
    }

    public record LoginResponse(String accessToken) {
    }

    public record ErrorResponse(String code, String message) {
    }

    private static class InvalidLoginRequestException extends RuntimeException {
    }
}
