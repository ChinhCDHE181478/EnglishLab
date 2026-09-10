package fu.sep490.g23.backend.repository;

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

    Boolean existsByEmail(String email);

    @Query("select distinct user from User user join user.roles role where role.code in :roles")
    List<User> findDistinctByRoles_CodeIn(@Param("roles") Collection<String> roles);

    /**
     * Streams only the avatar URLs for orphan-cleanup scans, avoiding loading every User row.
     * Returns URLs that are non-blank; the cleanup service is responsible for filtering nulls.
     */
    @Query("select u.avatarUrl from User u where u.avatarUrl is not null and u.avatarUrl <> ''")
    List<String> findAllNonEmptyAvatarUrls();
}
