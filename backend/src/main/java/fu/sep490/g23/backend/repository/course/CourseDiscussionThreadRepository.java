package fu.sep490.g23.backend.repository.course;

import fu.sep490.g23.backend.entity.course.CourseDiscussionThread;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.entity.course.enums.CourseDiscussionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CourseDiscussionThreadRepository extends JpaRepository<CourseDiscussionThread, Long> {
    @Query("""
            select t from CourseDiscussionThread t left join t.lesson l
            where t.course.id = :courseId and t.status <> :hidden
              and ((:moduleId is null and l is null)
                   or (:moduleId is not null and l.module.id = :moduleId))
              and (
                    :filter = 'ALL'
                    or (:filter = 'MINE' and :authorId is not null and t.author.id = :authorId)
                    or (:filter = 'RESOLVED' and t.status = :resolved)
                    or (:filter = 'UNANSWERED' and t.status <> :resolved
                        and not exists (select r from CourseDiscussionReply r where r.thread = t and r.status <> :hidden))
                    or :filter = 'HELPFUL'
              )
            """)
    Page<CourseDiscussionThread> findCourseDiscussionPage(
            @Param("courseId") Long courseId,
            @Param("moduleId") Long moduleId,
            @Param("filter") String filter,
            @Param("authorId") Long authorId,
            @Param("hidden") CourseDiscussionStatus hidden,
            @Param("resolved") CourseDiscussionStatus resolved,
            Pageable pageable
    );

    @Query("""
            select t from CourseDiscussionThread t
            where t.course.id = :courseId and t.lesson.id = :lessonId and t.status <> :hidden
              and (
                    :filter = 'ALL'
                    or (:filter = 'MINE' and :authorId is not null and t.author.id = :authorId)
                    or (:filter = 'RESOLVED' and t.status = :resolved)
                    or (:filter = 'UNANSWERED' and t.status <> :resolved
                        and not exists (select r from CourseDiscussionReply r where r.thread = t and r.status <> :hidden))
                    or :filter = 'HELPFUL'
              )
            """)
    Page<CourseDiscussionThread> findLessonDiscussionPage(
            @Param("courseId") Long courseId,
            @Param("lessonId") Long lessonId,
            @Param("filter") String filter,
            @Param("authorId") Long authorId,
            @Param("hidden") CourseDiscussionStatus hidden,
            @Param("resolved") CourseDiscussionStatus resolved,
            Pageable pageable
    );

    Optional<CourseDiscussionThread> findFirstByCourseAndTitle(OnlineCourse course, String title);
}
