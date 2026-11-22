package cat.itacademy.rastro.Rastro_backend.post.repository;

import cat.itacademy.rastro.Rastro_backend.post.entity.PublicationStatusEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PublicationStatusRepository extends JpaRepository<PublicationStatusEntity, UUID> {

    Optional<PublicationStatusEntity> findByTitle(String title);
}
