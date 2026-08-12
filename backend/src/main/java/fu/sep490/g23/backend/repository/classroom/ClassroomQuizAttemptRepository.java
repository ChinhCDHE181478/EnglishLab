package fu.sep490.g23.backend.repository.classroom;

import fu.sep490.g23.backend.entity.classroom.ClassroomQuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClassroomQuizAttemptRepository extends JpaRepository<ClassroomQuizAttempt, Long> {
    Optional<ClassroomQuizAttempt> findByQuizIdAndStudentId(Long quizId, Long studentId);
    List<ClassroomQuizAttempt> findByQuizIdOrderBySubmittedAtDesc(Long quizId);
    List<ClassroomQuizAttempt> findByStudentIdOrderBySubmittedAtDesc(Long studentId);
}
