package cat.itacademy.rastro.Rastro_backend.post.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "animal_type")
public class AnimalTypeEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String title;

    protected AnimalTypeEntity() {
    }

    public AnimalTypeEntity(UUID id, String title) {
        this.id = id;
        this.title = title;
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }
}
