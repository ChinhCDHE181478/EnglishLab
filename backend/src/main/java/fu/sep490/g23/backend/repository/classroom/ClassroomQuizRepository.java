package fu.sep490.g23.backend.repository.classroom;

import fu.sep490.g23.backend.entity.classroom.ClassroomQuiz;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomQuizStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClassroomQuizRepository extends JpaRepository<ClassroomQuiz, Long> {
    List<ClassroomQuiz> findByClassSectionIdOrderByCreatedAtDesc(Long classSectionId);
    List<ClassroomQuiz> findByClassSectionIdAndStatusOrderByCreatedAtDesc(Long classSectionId, ClassroomQuizStatus status);
}
