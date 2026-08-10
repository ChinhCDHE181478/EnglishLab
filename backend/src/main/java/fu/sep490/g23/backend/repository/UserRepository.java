package fu.sap490.g23.backend.repository;

import fu.sap490.g23.backend.entity.enums.RoleEnum;
import fu.sap490.g23.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmail(String email);

    Optional<User> findByLarkOpenId(String larkOpenId);

    Optional<User> findByFacebookId(String facebookId);

    Boolean existsByEmail(String email);

    List<User> findDistinctByRoles_CodeIn(Collection<RoleEnum> roles);
}
