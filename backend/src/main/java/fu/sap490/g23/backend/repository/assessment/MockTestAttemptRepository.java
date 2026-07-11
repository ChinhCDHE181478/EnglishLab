package fu.sap490.g23.backend.repository.assessment;

import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.assessment.MockTestAttempt;
import fu.sap490.g23.backend.entity.curriculum.AssessmentBankItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MockTestAttemptRepository extends JpaRepository<MockTestAttempt, Long> {
    Optional<MockTestAttempt> findTopByAssessmentBankItemAndStudentOrderBySubmittedAtDesc(AssessmentBankItem assessmentBankItem, User student);

    List<MockTestAttempt> findByStudentOrderBySubmittedAtDesc(User student);
}
