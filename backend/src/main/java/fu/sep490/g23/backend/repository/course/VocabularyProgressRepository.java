package fu.sep490.g23.backend.repository.course;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.entity.course.VocabularyProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VocabularyProgressRepository extends JpaRepository<VocabularyProgress, Long> {
    List<VocabularyProgress> findByStudentAndCourse(User student, OnlineCourse course);
    Optional<VocabularyProgress> findByStudentAndCourseAndTermKey(User student, OnlineCourse course, String termKey);
}
