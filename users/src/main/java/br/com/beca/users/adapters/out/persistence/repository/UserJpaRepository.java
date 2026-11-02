package br.com.beca.users.adapters.out.persistence.repository;
import br.com.beca.users.adapters.out.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {

}
