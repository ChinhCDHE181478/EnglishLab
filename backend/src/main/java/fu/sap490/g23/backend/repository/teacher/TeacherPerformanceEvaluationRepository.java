package fu.sap490.g23.backend.repository.teacher;

import fu.sap490.g23.backend.entity.teacher.TeacherPerformanceEvaluation;
import fu.sap490.g23.backend.entity.teacher.enums.TeacherEvaluationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeacherPerformanceEvaluationRepository extends JpaRepository<TeacherPerformanceEvaluation, Long> {
    List<TeacherPerformanceEvaluation> findByTeacherIdOrderByPeriodEndDescIdDesc(Long teacherId);
    List<TeacherPerformanceEvaluation> findByTeacherIdAndStatusOrderByPeriodEndDescIdDesc(
            Long teacherId,
            TeacherEvaluationStatus status
    );
}
