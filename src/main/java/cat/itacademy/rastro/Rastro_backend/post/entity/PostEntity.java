package cat.itacademy.rastro.Rastro_backend.post.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "post")
public class PostEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 2048)
    private String photo;

    @Column(nullable = false)
    private String description;

    @Column(name = "animal_type_id", nullable = false)
    private UUID animalTypeId;

    @Column(name = "created_date", nullable = false)
    private Instant createdDate;

    @Column(name = "last_update", nullable = false)
    private Instant lastUpdate;

    @Column(name = "publication_status_id", nullable = false)
    private UUID publicationStatusId;

    @Column(name = "is_safekeeping", nullable = false)
    private boolean safekeeping;

    @Column(name = "approx_age")
    private Integer approxAge;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "age_unit", columnDefinition = "age_unit_enum")
    private AgeUnit ageUnit;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "post_type", nullable = false, columnDefinition = "post_type_enum")
    private PostType postType;

    @Column(name = "location_id", nullable = false)
    private UUID locationId;

    @Column(name = "search_radius", nullable = false, precision = 6, scale = 2)
    private BigDecimal searchRadius;

    @Column(name = "lost_at")
    private Instant lostAt;

    protected PostEntity() {
    }

    public PostEntity(
            UUID id,
            UUID userId,
            String title,
            String photo,
            String description,
            UUID animalTypeId,
            Instant createdDate,
            Instant lastUpdate,
            UUID publicationStatusId,
            boolean safekeeping,
            Integer approxAge,
            AgeUnit ageUnit,
            PostType postType,
            UUID locationId,
            BigDecimal searchRadius,
            Instant lostAt
    ) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.photo = photo;
        this.description = description;
        this.animalTypeId = animalTypeId;
        this.createdDate = createdDate;
        this.lastUpdate = lastUpdate;
        this.publicationStatusId = publicationStatusId;
        this.safekeeping = safekeeping;
        this.approxAge = approxAge;
        this.ageUnit = ageUnit;
        this.postType = postType;
        this.locationId = locationId;
        this.searchRadius = searchRadius;
        this.lostAt = lostAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getTitle() {
        return title;
    }

    public String getPhoto() {
        return photo;
    }

    public String getDescription() {
        return description;
    }

    public UUID getAnimalTypeId() {
        return animalTypeId;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public Instant getLastUpdate() {
        return lastUpdate;
    }

    public UUID getPublicationStatusId() {
        return publicationStatusId;
    }

    public boolean isSafekeeping() {
        return safekeeping;
    }

    public Integer getApproxAge() {
        return approxAge;
    }

    public AgeUnit getAgeUnit() {
        return ageUnit;
    }

    public PostType getPostType() {
        return postType;
    }

    public UUID getLocationId() {
        return locationId;
    }

    public BigDecimal getSearchRadius() {
        return searchRadius;
    }

    public Instant getLostAt() {
        return lostAt;
    }
}
