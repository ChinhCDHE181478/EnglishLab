package fu.sap490.g23.backend.repository.course;

import fu.sap490.g23.backend.entity.course.CourseLessonFlashcardRef;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseLessonFlashcardRefRepository extends JpaRepository<CourseLessonFlashcardRef, Long> {
    void deleteByLessonId(Long lessonId);
}
