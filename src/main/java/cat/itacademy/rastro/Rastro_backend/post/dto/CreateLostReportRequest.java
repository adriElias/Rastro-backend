package cat.itacademy.rastro.Rastro_backend.post.dto;

import cat.itacademy.rastro.Rastro_backend.post.entity.AgeUnit;
import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(
        description = "Information required to create a lost animal report",
        requiredProperties = {
                "title",
                "description",
                "animalType",
                "lostAt",
                "latitude",
                "longitude",
                "searchRadiusKm"
        }
)
public record CreateLostReportRequest(
        @Schema(example = "Lost dog in Eixample")
        String title,

        @Schema(example = "Brown dog wearing a blue collar")
        String description,

        @Schema(example = "DOG", allowableValues = {"DOG", "CAT", "BIRD", "OTHER"})
        String animalType,

        @Schema(example = "2026-11-20T17:30:00Z")
        Instant lostAt,

        @Schema(example = "41.387400")
        BigDecimal latitude,

        @Schema(example = "2.168600")
        BigDecimal longitude,

        @Schema(example = "5.00", minimum = "0.01", maximum = "50")
        BigDecimal searchRadiusKm,

        @Schema(
                example = "3",
                description = "Positive integer; must be provided with ageUnit"
        )
        @JsonDeserialize(using = StrictIntegerDeserializer.class)
        Integer approxAge,

        @Schema(description = "Must be provided with approxAge")
        AgeUnit ageUnit,

        @Schema(example = "https://example.com/animal.jpg")
        String photoUrl
) {
}
