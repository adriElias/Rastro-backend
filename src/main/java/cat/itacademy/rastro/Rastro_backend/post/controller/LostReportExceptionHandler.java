package cat.itacademy.rastro.Rastro_backend.post.controller;

import cat.itacademy.rastro.Rastro_backend.post.service.InvalidLostReportException;
import cat.itacademy.rastro.Rastro_backend.shared.infrastructure.web.ApiErrorResponse;
import cat.itacademy.rastro.Rastro_backend.shared.infrastructure.web.FieldErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice(assignableTypes = LostReportController.class)
public class LostReportExceptionHandler {

    @ExceptionHandler(InvalidLostReportException.class)
    ResponseEntity<ApiErrorResponse> handleInvalidReport(InvalidLostReportException exception) {
        List<FieldErrorResponse> fieldErrors = exception.fieldErrors().entrySet().stream()
                .map(entry -> new FieldErrorResponse(entry.getKey(), entry.getValue()))
                .toList();
        return ResponseEntity.badRequest().body(new ApiErrorResponse(
                "INVALID_REQUEST",
                exception.getMessage(),
                fieldErrors
        ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiErrorResponse> handleUnreadableRequest() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiErrorResponse(
                "INVALID_REQUEST",
                "The lost report request is invalid",
                List.of()
        ));
    }
}
