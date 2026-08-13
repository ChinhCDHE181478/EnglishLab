package fu.sep490.g23.backend.repository.assessment;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.assessment.PlacementTestAttempt;
import fu.sep490.g23.backend.entity.assessment.enums.PlacementEvaluationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.List;

public interface PlacementTestAttemptRepository extends JpaRepository<PlacementTestAttempt, Long> {
    Optional<PlacementTestAttempt> findTopByStudentAndTestCodeOrderBySubmittedAtDesc(User student, String testCode);

    Optional<PlacementTestAttempt> findTopByStudentOrderBySubmittedAtDesc(User student);

    Optional<PlacementTestAttempt> findTopByStudentAndEvaluationStatusOrderBySubmittedAtDesc(
            User student,
            PlacementEvaluationStatus evaluationStatus
    );

    long countByStudentAndTestCode(User student, String testCode);

    boolean existsByStudentAndTestCode(User student, String testCode);

    List<PlacementTestAttempt> findByTestCodeOrderBySubmittedAtDesc(String testCode);

    List<PlacementTestAttempt> findTop20ByTestCodeOrderBySubmittedAtDesc(String testCode);

    List<PlacementTestAttempt> findByEvaluationStatusInOrderBySubmittedAtAsc(
            List<PlacementEvaluationStatus> statuses
    );

    @Query("select count(distinct attempt.student.id) from PlacementTestAttempt attempt where attempt.testCode = :testCode")
    long countDistinctStudentsByTestCode(String testCode);
}
