package cat.itacademy.rastro.Rastro_backend.post.repository;

import cat.itacademy.rastro.Rastro_backend.post.entity.PostEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PostRepository extends JpaRepository<PostEntity, UUID> {
}
