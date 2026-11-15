package cat.itacademy.rastro.Rastro_backend.user.application;

public class AuthenticationFailedException extends RuntimeException {

    public AuthenticationFailedException() {
        super("Authentication failed");
    }
}
