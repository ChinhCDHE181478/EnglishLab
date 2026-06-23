package fu.sap490.g23.backend.repository.assessment;

import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.assessment.PlacementTestAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.List;

public interface PlacementTestAttemptRepository extends JpaRepository<PlacementTestAttempt, Long> {
    Optional<PlacementTestAttempt> findTopByStudentAndTestCodeOrderBySubmittedAtDesc(User student, String testCode);

    long countByStudentAndTestCode(User student, String testCode);

    boolean existsByStudentAndTestCode(User student, String testCode);

    List<PlacementTestAttempt> findByTestCodeOrderBySubmittedAtDesc(String testCode);

    List<PlacementTestAttempt> findTop20ByTestCodeOrderBySubmittedAtDesc(String testCode);

    @Query("select count(distinct attempt.student.id) from PlacementTestAttempt attempt where attempt.testCode = :testCode")
    long countDistinctStudentsByTestCode(String testCode);
}
