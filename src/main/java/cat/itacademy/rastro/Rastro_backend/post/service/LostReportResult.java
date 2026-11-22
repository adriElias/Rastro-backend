package cat.itacademy.rastro.Rastro_backend.post.service;

import cat.itacademy.rastro.Rastro_backend.post.entity.AgeUnit;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LostReportResult(
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
}
