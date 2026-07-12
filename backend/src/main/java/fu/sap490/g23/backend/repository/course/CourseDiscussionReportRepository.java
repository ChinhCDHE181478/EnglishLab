package fu.sap490.g23.backend.repository.course;

import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.course.CourseDiscussionReport;
import fu.sap490.g23.backend.entity.course.enums.CourseDiscussionReportTarget;
import fu.sap490.g23.backend.entity.course.enums.CourseDiscussionReportStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface CourseDiscussionReportRepository extends JpaRepository<CourseDiscussionReport, Long> {
    Optional<CourseDiscussionReport> findByTargetTypeAndTargetIdAndReporter(CourseDiscussionReportTarget targetType, Long targetId, User reporter);

    @EntityGraph(attributePaths = {"reporter", "reviewedBy"})
    List<CourseDiscussionReport> findByStatusOrderByCreatedAtDesc(CourseDiscussionReportStatus status);
}
