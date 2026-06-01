package fu.sap490.g23.backend.repository.course;

import fu.sap490.g23.backend.entity.course.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {
    @Query("""
            select count(l)
            from Lesson l
            join l.module m
            join m.onlineCourse c
            join c.learningPackage lp
            where lp.deleted = false
            """)
    long countActiveLessons();
}
