package fu.sep490.g23.backend.repository.course;

import fu.sep490.g23.backend.entity.course.CourseDiscussionPost;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.entity.course.enums.CourseDiscussionPostType;
import fu.sep490.g23.backend.entity.course.enums.CourseDiscussionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CourseDiscussionPostRepository extends JpaRepository<CourseDiscussionPost, Long> {

    Optional<CourseDiscussionPost> findByIdAndPostType(Long id, CourseDiscussionPostType postType);

    Optional<CourseDiscussionPost> findFirstByCourseAndPostTypeAndTitle(
            OnlineCourse course,
            CourseDiscussionPostType postType,
            String title
    );

    @Query("""
            select t from CourseDiscussionPost t left join t.lesson l
            where t.postType = :threadType
              and t.course.id = :courseId and t.status <> :hidden
              and ((:moduleId is null and l is null)
                   or (:moduleId is not null and l.module.id = :moduleId))
              and (
                    :filter = 'ALL'
                    or (:filter = 'MINE' and :authorId is not null and t.author.id = :authorId)
                    or (:filter = 'RESOLVED' and t.status = :resolved)
                    or (:filter = 'UNANSWERED' and t.status <> :resolved
                        and not exists (
                            select r from CourseDiscussionPost r
                            where r.parentPost = t
                              and r.postType = :replyType
                              and r.status <> :hidden
                        ))
                    or :filter = 'HELPFUL'
              )
            """)
    Page<CourseDiscussionPost> findCourseDiscussionPage(
            @Param("courseId") Long courseId,
            @Param("moduleId") Long moduleId,
            @Param("filter") String filter,
            @Param("authorId") Long authorId,
            @Param("hidden") CourseDiscussionStatus hidden,
            @Param("resolved") CourseDiscussionStatus resolved,
            @Param("threadType") CourseDiscussionPostType threadType,
            @Param("replyType") CourseDiscussionPostType replyType,
            Pageable pageable
    );

    @Query("""
            select t from CourseDiscussionPost t
            where t.postType = :threadType
              and t.course.id = :courseId and t.lesson.id = :lessonId and t.status <> :hidden
              and (
                    :filter = 'ALL'
                    or (:filter = 'MINE' and :authorId is not null and t.author.id = :authorId)
                    or (:filter = 'RESOLVED' and t.status = :resolved)
                    or (:filter = 'UNANSWERED' and t.status <> :resolved
                        and not exists (
                            select r from CourseDiscussionPost r
                            where r.parentPost = t
                              and r.postType = :replyType
                              and r.status <> :hidden
                        ))
                    or :filter = 'HELPFUL'
              )
            """)
    Page<CourseDiscussionPost> findLessonDiscussionPage(
            @Param("courseId") Long courseId,
            @Param("lessonId") Long lessonId,
            @Param("filter") String filter,
            @Param("authorId") Long authorId,
            @Param("hidden") CourseDiscussionStatus hidden,
            @Param("resolved") CourseDiscussionStatus resolved,
            @Param("threadType") CourseDiscussionPostType threadType,
            @Param("replyType") CourseDiscussionPostType replyType,
            Pageable pageable
    );
}
