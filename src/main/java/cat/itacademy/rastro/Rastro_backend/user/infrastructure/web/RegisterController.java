package cat.itacademy.rastro.Rastro_backend.user.infrastructure.web;

import cat.itacademy.rastro.Rastro_backend.user.application.EmailAlreadyRegisteredException;
import cat.itacademy.rastro.Rastro_backend.user.application.RegisterCommand;
import cat.itacademy.rastro.Rastro_backend.user.application.RegisterResult;
import cat.itacademy.rastro.Rastro_backend.user.application.RegisterUserUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class RegisterController {

    private final RegisterUserUseCase registerUserUseCase;

    public RegisterController(RegisterUserUseCase registerUserUseCase) {
        this.registerUserUseCase = registerUserUseCase;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@RequestBody RegisterRequest request) {
        if (isBlank(request.name())
                || isBlank(request.lastName())
                || isBlank(request.email())
                || isBlank(request.password())) {
            throw new InvalidRegisterRequestException();
        }

        RegisterResult result = registerUserUseCase.register(new RegisterCommand(
                request.name(),
                request.lastName(),
                request.email(),
                request.password(),
                request.phoneNumber()
        ));

        return new RegisterResponse(
                result.id(),
                result.name(),
                result.lastName(),
                result.email(),
                result.phoneNumber(),
                result.registrationDate()
        );
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleEmailAlreadyRegistered(EmailAlreadyRegisteredException exception) {
        return new ErrorResponse("EMAIL_ALREADY_REGISTERED", "Email already registered");
    }

    @ExceptionHandler({
            InvalidRegisterRequestException.class,
            IllegalArgumentException.class,
            HttpMessageNotReadableException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleInvalidRequest(Exception exception) {
        return new ErrorResponse("INVALID_REQUEST", "The registration request is invalid");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record RegisterRequest(
            String name,
            String lastName,
            String email,
            String password,
            String phoneNumber
    ) {
    }

    public record RegisterResponse(
            UUID id,
            String name,
            String lastName,
            String email,
            String phoneNumber,
            Instant registrationDate
    ) {
    }

    public record ErrorResponse(String code, String message) {
    }

    private static class InvalidRegisterRequestException extends RuntimeException {
    }
}
