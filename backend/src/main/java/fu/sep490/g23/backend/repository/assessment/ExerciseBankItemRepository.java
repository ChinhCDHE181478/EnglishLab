package fu.sep490.g23.backend.repository.assessment;

import fu.sep490.g23.backend.entity.assessment.ExerciseBankItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ExerciseBankItemRepository extends JpaRepository<ExerciseBankItem, Long>, JpaSpecificationExecutor<ExerciseBankItem> {
    List<ExerciseBankItem> findByActiveTrueOrderByUpdatedAtDesc();

    List<ExerciseBankItem> findAllByOrderByUpdatedAtDesc();

    List<ExerciseBankItem> findBySkillAndActiveTrueOrderByUpdatedAtDesc(String skill);
}
