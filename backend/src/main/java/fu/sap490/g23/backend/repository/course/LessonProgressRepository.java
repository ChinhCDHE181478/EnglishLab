package fu.sap490.g23.backend.repository.course;

import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.course.Lesson;
import fu.sap490.g23.backend.entity.course.LessonProgress;
import fu.sap490.g23.backend.entity.course.LessonProgressStatus;
import fu.sap490.g23.backend.entity.course.PackageEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LessonProgressRepository extends JpaRepository<LessonProgress, Long> {
    Optional<LessonProgress> findByStudentAndLesson(User student, Lesson lesson);
    List<LessonProgress> findByEnrollment(PackageEnrollment enrollment);
    List<LessonProgress> findByEnrollmentAndStatusOrderByCompletedAtDesc(PackageEnrollment enrollment, LessonProgressStatus status);
    long countByEnrollmentAndStatus(PackageEnrollment enrollment, LessonProgressStatus status);
    boolean existsByLessonId(Long lessonId);
}
