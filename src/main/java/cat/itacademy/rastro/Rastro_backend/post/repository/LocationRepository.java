package cat.itacademy.rastro.Rastro_backend.post.repository;

import cat.itacademy.rastro.Rastro_backend.post.entity.LocationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LocationRepository extends JpaRepository<LocationEntity, UUID> {
}
