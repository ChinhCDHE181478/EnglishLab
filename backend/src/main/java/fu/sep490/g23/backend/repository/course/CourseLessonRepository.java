package fu.sep490.g23.backend.repository.course;

import fu.sep490.g23.backend.entity.course.CourseLesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CourseLessonRepository extends JpaRepository<CourseLesson, Long> {
    List<CourseLesson> findByCourseUnitIdOrderBySequenceNumberAscIdAsc(Long courseUnitId);

    List<CourseLesson> findByCourseUnitInstructorLedCourseIdOrderBySequenceNumberAscIdAsc(Long instructorLedCourseId);

    @Query("""
            select count(lesson) > 0 from CourseLesson lesson
            where lesson.courseUnit.instructorLedCourse.id = :courseId
              and lesson.sequenceNumber = :sequenceNumber
              and (:excludeId is null or lesson.id <> :excludeId)
            """)
    boolean existsDuplicateSequenceNumber(
            @Param("courseId") Long courseId,
            @Param("sequenceNumber") Integer sequenceNumber,
            @Param("excludeId") Long excludeId
    );

    long countByCourseUnitInstructorLedCourseId(Long instructorLedCourseId);
}
