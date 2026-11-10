package br.com.beca.users.adapters.out.persistence.repository;
import java.util.Optional;
import java.util.UUID;
import br.com.beca.users.adapters.out.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByEmail(String email);

}
