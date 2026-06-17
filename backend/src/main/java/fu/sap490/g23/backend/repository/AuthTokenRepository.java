package fu.sap490.g23.backend.repository;

import fu.sap490.g23.backend.entity.AuthToken;
import fu.sap490.g23.backend.entity.AuthTokenType;
import fu.sap490.g23.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthTokenRepository extends JpaRepository<AuthToken, Long> {

    Optional<AuthToken> findByTokenAndType(String token, AuthTokenType type);

    void deleteByUserAndType(User user, AuthTokenType type);
}
