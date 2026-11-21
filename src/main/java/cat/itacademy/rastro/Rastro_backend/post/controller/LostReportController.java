package cat.itacademy.rastro.Rastro_backend.post.controller;

import cat.itacademy.rastro.Rastro_backend.post.dto.CreateLostReportRequest;
import cat.itacademy.rastro.Rastro_backend.post.dto.CreateLostReportResponse;
import cat.itacademy.rastro.Rastro_backend.shared.infrastructure.web.ApiErrorResponse;
import cat.itacademy.rastro.Rastro_backend.post.service.CreateLostReportCommand;
import cat.itacademy.rastro.Rastro_backend.post.service.LostReportResult;
import cat.itacademy.rastro.Rastro_backend.post.service.LostReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/lost-reports")
@Tag(name = "Lost Reports", description = "Lost animal report operations")
public class LostReportController {

    private final LostReportService service;

    public LostReportController(LostReportService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create a lost animal report",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Lost report created"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid lost report",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class)
                    )
            )
    })
    public CreateLostReportResponse create(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody CreateLostReportRequest request
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        LostReportResult result = service.create(userId, new CreateLostReportCommand(
                request.title(),
                request.description(),
                request.animalType(),
                request.lostAt(),
                request.latitude(),
                request.longitude(),
                request.searchRadiusKm(),
                request.approxAge(),
                request.ageUnit(),
                request.photoUrl()
        ));
        return CreateLostReportResponse.from(result);
    }
}
