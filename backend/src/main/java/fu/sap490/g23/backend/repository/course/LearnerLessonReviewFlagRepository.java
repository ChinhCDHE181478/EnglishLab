package fu.sap490.g23.backend.repository.course;

import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.course.LearnerLessonReviewFlag;
import fu.sap490.g23.backend.entity.course.Lesson;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LearnerLessonReviewFlagRepository extends JpaRepository<LearnerLessonReviewFlag, Long> {
    @EntityGraph(attributePaths = {"course.learningPackage", "lesson"})
    List<LearnerLessonReviewFlag> findByUserOrderByCreatedAtDesc(User user);

    Optional<LearnerLessonReviewFlag> findByUserAndLesson(User user, Lesson lesson);
}
