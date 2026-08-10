package fu.sap490.g23.backend.repository.course;

import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.course.LearnerLessonNote;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LearnerLessonNoteRepository extends JpaRepository<LearnerLessonNote, Long> {
    @EntityGraph(attributePaths = {"course.learningPackage", "lesson"})
    List<LearnerLessonNote> findByUserOrderByUpdatedAtDesc(User user);

    @EntityGraph(attributePaths = {"course.learningPackage", "lesson"})
    Optional<LearnerLessonNote> findByIdAndUser(Long id, User user);
}
