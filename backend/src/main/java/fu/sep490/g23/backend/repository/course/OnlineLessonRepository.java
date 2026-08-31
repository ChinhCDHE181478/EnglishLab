package fu.sep490.g23.backend.repository.course;

import fu.sep490.g23.backend.entity.course.OnlineLesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface OnlineLessonRepository extends JpaRepository<OnlineLesson, Long> {
    java.util.Optional<OnlineLesson> findByIdAndModuleOnlineCourseVersionOnlineCourseId(Long lessonId, Long courseId);

    boolean existsByModuleIdAndModuleOnlineCourseVersionOnlineCourseId(Long moduleId, Long courseId);

    @Query("""
            select count(l)
            from OnlineLesson l
            join l.module m
            join m.onlineCourseVersion v
            join v.onlineCourse c
            where c.deleted = false
            """)
    long countActiveLessons();
}
