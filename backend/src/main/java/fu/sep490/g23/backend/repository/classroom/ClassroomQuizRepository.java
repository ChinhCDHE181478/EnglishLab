package fu.sap490.g23.backend.repository.classroom;

import fu.sap490.g23.backend.entity.classroom.ClassroomQuiz;
import fu.sap490.g23.backend.entity.classroom.enums.ClassroomQuizStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClassroomQuizRepository extends JpaRepository<ClassroomQuiz, Long> {
    List<ClassroomQuiz> findByClassroomOfferingIdOrderByCreatedAtDesc(Long classroomOfferingId);
    List<ClassroomQuiz> findByClassroomOfferingIdAndStatusOrderByCreatedAtDesc(Long classroomOfferingId, ClassroomQuizStatus status);
}
