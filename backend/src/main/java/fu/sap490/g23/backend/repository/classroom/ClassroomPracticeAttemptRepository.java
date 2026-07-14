package fu.sap490.g23.backend.repository.classroom;

import fu.sap490.g23.backend.entity.classroom.ClassroomPracticeAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClassroomPracticeAttemptRepository extends JpaRepository<ClassroomPracticeAttempt, Long> {
    List<ClassroomPracticeAttempt> findByClassroomOfferingIdAndStudentId(Long offeringId, Long studentId);
    Optional<ClassroomPracticeAttempt> findByClassroomOfferingIdAndStudentIdAndExerciseId(
            Long offeringId,
            Long studentId,
            Long exerciseId
    );
}
