package fu.sap490.g23.backend.repository.course;

import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.course.CourseDiscussionReport;
import fu.sap490.g23.backend.entity.course.CourseDiscussionReportTarget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CourseDiscussionReportRepository extends JpaRepository<CourseDiscussionReport, Long> {
    Optional<CourseDiscussionReport> findByTargetTypeAndTargetIdAndReporter(CourseDiscussionReportTarget targetType, Long targetId, User reporter);
}
