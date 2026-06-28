package fu.sap490.g23.backend.repository.assessment;

import fu.sap490.g23.backend.entity.assessment.ExerciseBankItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExerciseBankItemRepository extends JpaRepository<ExerciseBankItem, Long> {
    List<ExerciseBankItem> findByActiveTrueOrderByUpdatedAtDesc();

    List<ExerciseBankItem> findAllByOrderByUpdatedAtDesc();

    List<ExerciseBankItem> findBySkillAndActiveTrueOrderByUpdatedAtDesc(String skill);
}
