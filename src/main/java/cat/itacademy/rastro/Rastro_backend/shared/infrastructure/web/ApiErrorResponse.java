package cat.itacademy.rastro.Rastro_backend.shared.infrastructure.web;

import java.util.List;

public record ApiErrorResponse(
        String code,
        String message,
        List<FieldErrorResponse> fieldErrors
) {
}
