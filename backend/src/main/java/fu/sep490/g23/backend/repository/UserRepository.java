package fu.sep490.g23.backend.repository;

import fu.sep490.g23.backend.entity.enums.RoleEnum;
import fu.sep490.g23.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("select distinct user from User user join user.roles role where role in :roles")
    List<User> findDistinctByRoles_CodeIn(@Param("roles") Collection<RoleEnum> roles);
}
