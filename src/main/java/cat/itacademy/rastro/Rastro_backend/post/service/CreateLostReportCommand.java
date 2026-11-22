package cat.itacademy.rastro.Rastro_backend.post.service;

import cat.itacademy.rastro.Rastro_backend.post.entity.AgeUnit;

import java.math.BigDecimal;
import java.time.Instant;

public record CreateLostReportCommand(
        String title,
        String description,
        String animalType,
        Instant lostAt,
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal searchRadiusKm,
        Integer approxAge,
        AgeUnit ageUnit,
        String photoUrl
) {
}
