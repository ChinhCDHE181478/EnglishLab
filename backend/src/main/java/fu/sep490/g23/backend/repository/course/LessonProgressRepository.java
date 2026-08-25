package fu.sep490.g23.backend.repository.course;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.course.Lesson;
import fu.sep490.g23.backend.entity.course.LessonProgress;
import fu.sep490.g23.backend.entity.course.enums.LessonProgressStatus;
import fu.sep490.g23.backend.entity.course.OnlineCourseEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface LessonProgressRepository extends JpaRepository<LessonProgress, Long> {
    Optional<LessonProgress> findByStudentAndLesson(User student, Lesson lesson);
    List<LessonProgress> findByEnrollment(OnlineCourseEnrollment enrollment);
    List<LessonProgress> findByEnrollmentAndStatusOrderByCompletedAtDesc(OnlineCourseEnrollment enrollment, LessonProgressStatus status);
    List<LessonProgress> findByStudentAndLessonIdInAndStatus(User student, Set<Long> lessonIds, LessonProgressStatus status);
    long countByEnrollmentAndStatus(OnlineCourseEnrollment enrollment, LessonProgressStatus status);
    boolean existsByLessonId(Long lessonId);
}
