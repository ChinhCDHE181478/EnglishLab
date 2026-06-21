package fu.sap490.g23.backend.repository.assessment;

import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.assessment.PlacementTestAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlacementTestAttemptRepository extends JpaRepository<PlacementTestAttempt, Long> {
    Optional<PlacementTestAttempt> findTopByStudentAndTestCodeOrderBySubmittedAtDesc(User student, String testCode);

    long countByStudentAndTestCode(User student, String testCode);

    boolean existsByStudentAndTestCode(User student, String testCode);
}
