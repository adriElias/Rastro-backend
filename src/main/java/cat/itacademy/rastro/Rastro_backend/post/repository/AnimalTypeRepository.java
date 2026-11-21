package cat.itacademy.rastro.Rastro_backend.post.repository;

import cat.itacademy.rastro.Rastro_backend.post.entity.AnimalTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AnimalTypeRepository extends JpaRepository<AnimalTypeEntity, UUID> {

    Optional<AnimalTypeEntity> findByTitle(String title);
}
