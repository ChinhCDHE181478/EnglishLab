package fu.sap490.g23.backend.repository;

import fu.sap490.g23.backend.entity.Role;
import fu.sap490.g23.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByFacebookId(String facebookId);

    Boolean existsByEmail(String email);

    List<User> findByRoleIn(Collection<Role> roles);
}
