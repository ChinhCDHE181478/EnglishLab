package fu.sep490.g23.backend.repository.course;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.course.CourseDiscussionPost;
import fu.sep490.g23.backend.entity.course.CourseDiscussionReport;
import fu.sep490.g23.backend.entity.course.enums.CourseDiscussionReportReasonCategory;
import fu.sep490.g23.backend.entity.course.enums.CourseDiscussionReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseDiscussionReportRepository extends JpaRepository<CourseDiscussionReport, Long> {
    Optional<CourseDiscussionReport> findByPostAndReporter(CourseDiscussionPost post, User reporter);

    long countByPost(CourseDiscussionPost post);

    @EntityGraph(attributePaths = {
            "reporter", "reviewedBy", "post", "post.course", "post.lesson", "post.author"
    })
    List<CourseDiscussionReport> findByStatusOrderByCreatedAtDesc(CourseDiscussionReportStatus status);

    @EntityGraph(attributePaths = {
            "reporter", "reviewedBy", "post", "post.course", "post.lesson", "post.author"
    })
    List<CourseDiscussionReport> findByStatusAndReasonCategoryOrderByCreatedAtDesc(
            CourseDiscussionReportStatus status,
            CourseDiscussionReportReasonCategory reasonCategory
    );

    @EntityGraph(attributePaths = {
            "reporter", "reviewedBy", "post", "post.course", "post.lesson", "post.author"
    })
    Page<CourseDiscussionReport> findByStatus(CourseDiscussionReportStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {
            "reporter", "reviewedBy", "post", "post.course", "post.lesson", "post.author"
    })
    Page<CourseDiscussionReport> findByStatusAndReasonCategory(
            CourseDiscussionReportStatus status,
            CourseDiscussionReportReasonCategory reasonCategory,
            Pageable pageable
    );
}
