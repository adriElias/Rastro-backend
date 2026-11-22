package cat.itacademy.rastro.Rastro_backend.post.service;

import cat.itacademy.rastro.Rastro_backend.post.entity.AnimalTypeEntity;
import cat.itacademy.rastro.Rastro_backend.post.entity.LocationEntity;
import cat.itacademy.rastro.Rastro_backend.post.entity.PostEntity;
import cat.itacademy.rastro.Rastro_backend.post.entity.PostType;
import cat.itacademy.rastro.Rastro_backend.post.entity.PublicationStatusEntity;
import cat.itacademy.rastro.Rastro_backend.post.repository.AnimalTypeRepository;
import cat.itacademy.rastro.Rastro_backend.post.repository.LocationRepository;
import cat.itacademy.rastro.Rastro_backend.post.repository.PostRepository;
import cat.itacademy.rastro.Rastro_backend.post.repository.PublicationStatusRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class LostReportService {

    private static final String ACTIVE_STATUS = "ACTIVE";
    private static final BigDecimal MIN_LATITUDE = new BigDecimal("-90");
    private static final BigDecimal MAX_LATITUDE = new BigDecimal("90");
    private static final BigDecimal MIN_LONGITUDE = new BigDecimal("-180");
    private static final BigDecimal MAX_LONGITUDE = new BigDecimal("180");
    private static final BigDecimal MAX_SEARCH_RADIUS = new BigDecimal("50");

    private final PostRepository postRepository;
    private final LocationRepository locationRepository;
    private final AnimalTypeRepository animalTypeRepository;
    private final PublicationStatusRepository publicationStatusRepository;
    private final Clock clock;

    public LostReportService(
            PostRepository postRepository,
            LocationRepository locationRepository,
            AnimalTypeRepository animalTypeRepository,
            PublicationStatusRepository publicationStatusRepository,
            Clock clock
    ) {
        this.postRepository = postRepository;
        this.locationRepository = locationRepository;
        this.animalTypeRepository = animalTypeRepository;
        this.publicationStatusRepository = publicationStatusRepository;
        this.clock = clock;
    }

    @Transactional
    public LostReportResult create(UUID userId, CreateLostReportCommand command) {
        Instant createdAt = clock.instant();
        validate(command, createdAt);

        String animalTypeTitle = command.animalType().trim().toUpperCase(Locale.ROOT);
        AnimalTypeEntity animalType = animalTypeRepository.findByTitle(animalTypeTitle)
                .orElseThrow(() -> invalid("animalType", "unsupported animal type"));
        PublicationStatusEntity status = publicationStatusRepository.findByTitle(ACTIVE_STATUS)
                .orElseThrow(() -> new IllegalStateException("ACTIVE publication status is not configured"));

        String title = command.title().trim();
        String description = command.description().trim();
        String photoUrl = normalizeOptional(command.photoUrl());
        UUID locationId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();

        LocationEntity location = new LocationEntity(
                locationId,
                command.latitude(),
                command.longitude()
        );
        PostEntity post = new PostEntity(
                postId,
                userId,
                title,
                photoUrl,
                description,
                animalType.getId(),
                createdAt,
                createdAt,
                status.getId(),
                false,
                command.approxAge(),
                command.ageUnit(),
                PostType.LOST,
                locationId,
                command.searchRadiusKm(),
                command.lostAt()
        );

        locationRepository.save(location);
        postRepository.save(post);

        return new LostReportResult(
                postId,
                title,
                description,
                animalTypeTitle,
                command.lostAt(),
                command.latitude(),
                command.longitude(),
                command.searchRadiusKm(),
                command.approxAge(),
                command.ageUnit(),
                photoUrl,
                status.getTitle(),
                createdAt
        );
    }

    private void validate(CreateLostReportCommand command, Instant createdAt) {
        Map<String, String> errors = new LinkedHashMap<>();

        validateRequiredText(errors, "title", command.title());
        validateRequiredText(errors, "description", command.description());
        validateRequiredText(errors, "animalType", command.animalType());
        validateRequired(errors, "lostAt", command.lostAt());
        validateRequired(errors, "latitude", command.latitude());
        validateRequired(errors, "longitude", command.longitude());
        validateRequired(errors, "searchRadiusKm", command.searchRadiusKm());

        if (command.title() != null && command.title().trim().length() > 150) {
            errors.put("title", "must not exceed 150 characters");
        }
        if (command.lostAt() != null && command.lostAt().isAfter(createdAt)) {
            errors.put("lostAt", "must not be after the report creation time");
        }
        validateRange(errors, "latitude", command.latitude(), MIN_LATITUDE, MAX_LATITUDE);
        validateRange(errors, "longitude", command.longitude(), MIN_LONGITUDE, MAX_LONGITUDE);
        validateDecimalPlaces(errors, "latitude", command.latitude(), 6);
        validateDecimalPlaces(errors, "longitude", command.longitude(), 6);
        validateSearchRadius(errors, command.searchRadiusKm());
        validateApproximateAge(errors, command);
        validatePhotoUrl(errors, command.photoUrl());

        if (!errors.isEmpty()) {
            throw new InvalidLostReportException(errors);
        }
    }

    private void validateRequiredText(Map<String, String> errors, String field, String value) {
        if (value == null || value.isBlank()) {
            errors.put(field, "must not be blank");
        }
    }

    private void validateRequired(Map<String, String> errors, String field, Object value) {
        if (value == null) {
            errors.put(field, "must not be null");
        }
    }

    private void validateRange(
            Map<String, String> errors,
            String field,
            BigDecimal value,
            BigDecimal minimum,
            BigDecimal maximum
    ) {
        if (value != null && (value.compareTo(minimum) < 0 || value.compareTo(maximum) > 0)) {
            errors.put(field, "must be between " + minimum + " and " + maximum);
        }
    }

    private void validateSearchRadius(Map<String, String> errors, BigDecimal radius) {
        if (radius == null) {
            return;
        }
        if (radius.compareTo(BigDecimal.ZERO) <= 0 || radius.compareTo(MAX_SEARCH_RADIUS) > 0) {
            errors.put("searchRadiusKm", "must be greater than 0 and at most 50");
        } else if (radius.stripTrailingZeros().scale() > 2) {
            errors.put("searchRadiusKm", "must have at most 2 decimal places");
        }
    }

    private void validateDecimalPlaces(
            Map<String, String> errors,
            String field,
            BigDecimal value,
            int maximumDecimalPlaces
    ) {
        if (value != null && value.stripTrailingZeros().scale() > maximumDecimalPlaces) {
            errors.put(field, "must have at most " + maximumDecimalPlaces + " decimal places");
        }
    }

    private void validateApproximateAge(
            Map<String, String> errors,
            CreateLostReportCommand command
    ) {
        if (command.approxAge() != null && command.ageUnit() == null) {
            errors.put("ageUnit", "must be provided together with approxAge");
        }
        if (command.approxAge() == null && command.ageUnit() != null) {
            errors.put("approxAge", "must be provided together with ageUnit");
        }
        if (command.approxAge() != null) {
            if (command.approxAge() <= 0) {
                errors.put("approxAge", "must be a positive integer");
            }
        }
    }

    private void validatePhotoUrl(Map<String, String> errors, String rawPhotoUrl) {
        String photoUrl = normalizeOptional(rawPhotoUrl);
        if (photoUrl == null) {
            return;
        }
        if (photoUrl.length() > 2048) {
            errors.put("photoUrl", "must not exceed 2048 characters");
            return;
        }
        try {
            URI uri = URI.create(photoUrl);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
                errors.put("photoUrl", "must be an absolute HTTPS URL");
            }
        } catch (IllegalArgumentException exception) {
            errors.put("photoUrl", "must be an absolute HTTPS URL");
        }
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private InvalidLostReportException invalid(String field, String message) {
        return new InvalidLostReportException(Map.of(field, message));
    }
}
