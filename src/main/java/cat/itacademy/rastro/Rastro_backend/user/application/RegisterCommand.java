package cat.itacademy.rastro.Rastro_backend.user.application;

public record RegisterCommand(
        String name,
        String lastName,
        String email,
        String password,
        String phoneNumber
) {
}
