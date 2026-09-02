package fu.sep490.g23.backend.repository.course;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.course.OnlineLesson;
import fu.sep490.g23.backend.entity.course.LessonProgress;
import fu.sep490.g23.backend.entity.course.enums.LessonProgressStatus;
import fu.sep490.g23.backend.entity.course.OnlineCourseEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface LessonProgressRepository extends JpaRepository<LessonProgress, Long> {
    @Query("""
            select progress from LessonProgress progress
            where progress.enrollment.student = :student and progress.lesson = :lesson
            """)
    Optional<LessonProgress> findByStudentAndLesson(User student, OnlineLesson lesson);
    Optional<LessonProgress> findByEnrollmentAndLesson(OnlineCourseEnrollment enrollment, OnlineLesson lesson);
    List<LessonProgress> findByEnrollment(OnlineCourseEnrollment enrollment);
    List<LessonProgress> findByEnrollmentAndStatusOrderByCompletedAtDesc(OnlineCourseEnrollment enrollment, LessonProgressStatus status);
    @Query("""
            select progress from LessonProgress progress
            where progress.enrollment.student = :student
              and progress.lesson.id in :lessonIds
              and progress.status = :status
            """)
    List<LessonProgress> findByStudentAndLessonIdInAndStatus(User student, Set<Long> lessonIds, LessonProgressStatus status);
    long countByEnrollmentAndStatus(OnlineCourseEnrollment enrollment, LessonProgressStatus status);
    boolean existsByLessonId(Long lessonId);
}
