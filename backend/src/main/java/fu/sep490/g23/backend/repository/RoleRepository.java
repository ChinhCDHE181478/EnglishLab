package fu.sep490.g23.backend.repository;

import fu.sep490.g23.backend.entity.Role;
import fu.sep490.g23.backend.entity.enums.RoleEnum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByCodeAndActiveTrue(RoleEnum code);
}
