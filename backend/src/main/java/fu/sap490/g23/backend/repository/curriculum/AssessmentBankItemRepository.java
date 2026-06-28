package fu.sap490.g23.backend.repository.curriculum;

import fu.sap490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sap490.g23.backend.entity.assessment.enums.AssessmentType;
import fu.sap490.g23.backend.entity.curriculum.AssessmentBankItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssessmentBankItemRepository extends JpaRepository<AssessmentBankItem, Long> {
    List<AssessmentBankItem> findAllByOrderByUpdatedAtDescIdDesc();
    List<AssessmentBankItem> findBySkillOrderByUpdatedAtDescIdDesc(AssessmentSkill skill);
    List<AssessmentBankItem> findByTypeOrderByUpdatedAtDescIdDesc(AssessmentType type);
}
