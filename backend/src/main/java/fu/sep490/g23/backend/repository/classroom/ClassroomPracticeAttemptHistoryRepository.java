package fu.sep490.g23.backend.repository.classroom;

import fu.sep490.g23.backend.entity.classroom.ClassroomPracticeAttemptHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClassroomPracticeAttemptHistoryRepository extends JpaRepository<ClassroomPracticeAttemptHistory, Long> {
    List<ClassroomPracticeAttemptHistory> findByClassroomOfferingIdAndStudentIdAndExerciseIdOrderByCompletedAtDesc(
            Long offeringId,
            Long studentId,
            Long exerciseId
    );

    long countByClassroomOfferingIdAndStudentIdAndExerciseId(Long offeringId, Long studentId, Long exerciseId);
}
