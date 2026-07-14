package fu.sap490.g23.backend.repository.curriculum;

import fu.sap490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sap490.g23.backend.entity.assessment.enums.AssessmentType;
import fu.sap490.g23.backend.entity.curriculum.AssessmentBankItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssessmentBankItemRepository extends JpaRepository<AssessmentBankItem, Long> {
    List<AssessmentBankItem> findAllByOrderByUpdatedAtDescIdDesc();
    List<AssessmentBankItem> findBySkillOrderByUpdatedAtDescIdDesc(AssessmentSkill skill);
    List<AssessmentBankItem> findByTypeOrderByUpdatedAtDescIdDesc(AssessmentType type);
    List<AssessmentBankItem> findByTypeAndStatusAndActiveTrueOrderByDisplayOrderAscUpdatedAtDescIdDesc(AssessmentType type, String status);
    Optional<AssessmentBankItem> findByIdAndTypeAndStatusAndActiveTrue(Long id, AssessmentType type, String status);
    List<AssessmentBankItem> findByTypeAndStatusAndActiveTrueAndSkillInOrderByDisplayOrderAscUpdatedAtDescIdDesc(
            AssessmentType type,
            String status,
            List<AssessmentSkill> skills
    );
}
