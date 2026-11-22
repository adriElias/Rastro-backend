package cat.itacademy.rastro.Rastro_backend.post.dto;

import cat.itacademy.rastro.Rastro_backend.post.entity.AgeUnit;
import cat.itacademy.rastro.Rastro_backend.post.service.LostReportResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CreateLostReportResponse(
        UUID id,
        String title,
        String description,
        String animalType,
        Instant lostAt,
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal searchRadiusKm,
        Integer approxAge,
        AgeUnit ageUnit,
        String photoUrl,
        String status,
        Instant createdAt
) {

    public static CreateLostReportResponse from(LostReportResult result) {
        return new CreateLostReportResponse(
                result.id(),
                result.title(),
                result.description(),
                result.animalType(),
                result.lostAt(),
                result.latitude(),
                result.longitude(),
                result.searchRadiusKm(),
                result.approxAge(),
                result.ageUnit(),
                result.photoUrl(),
                result.status(),
                result.createdAt()
        );
    }
}
