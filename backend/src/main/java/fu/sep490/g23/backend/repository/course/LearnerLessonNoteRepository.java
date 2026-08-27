package fu.sep490.g23.backend.repository.course;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.course.LearnerLessonNote;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LearnerLessonNoteRepository extends JpaRepository<LearnerLessonNote, Long> {
    @EntityGraph(attributePaths = {"course", "lesson"})
    List<LearnerLessonNote> findByUserOrderByUpdatedAtDesc(User user);

    @EntityGraph(attributePaths = {"course", "lesson"})
    Optional<LearnerLessonNote> findByIdAndUser(Long id, User user);
}
