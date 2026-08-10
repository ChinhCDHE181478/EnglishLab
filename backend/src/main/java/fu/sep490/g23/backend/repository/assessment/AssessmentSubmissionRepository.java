package fu.sap490.g23.backend.repository.assessment;

import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.assessment.AssessmentSubmission;
import fu.sap490.g23.backend.entity.assessment.CourseAssessment;
import fu.sap490.g23.backend.entity.assessment.enums.SubmissionStatus;
import fu.sap490.g23.backend.entity.course.OnlineCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface AssessmentSubmissionRepository extends JpaRepository<AssessmentSubmission, Long> {
    Optional<AssessmentSubmission> findTopByAssessmentAndStudentOrderBySubmittedAtDesc(CourseAssessment assessment, User student);
    List<AssessmentSubmission> findTop2ByAssessmentAndStudentOrderBySubmittedAtDesc(CourseAssessment assessment, User student);
    Optional<AssessmentSubmission> findTopByAssessmentProgressKeyAndStudentOrderBySubmittedAtDesc(
            String progressKey,
            User student
    );
    List<AssessmentSubmission> findTop2ByAssessmentProgressKeyAndStudentOrderBySubmittedAtDesc(
            String progressKey,
            User student
    );
    boolean existsByAssessmentProgressKeyAndStudentAndStatusIn(
            String progressKey,
            User student,
            Set<SubmissionStatus> statuses
    );
    boolean existsByAssessmentAndStudentAndStatusIn(
            CourseAssessment assessment,
            User student,
            Set<SubmissionStatus> statuses
    );
    List<AssessmentSubmission> findByStudentOrderBySubmittedAtDesc(User student);
    boolean existsByAssessmentId(Long assessmentId);
    boolean existsByAssessmentInAndStudent(List<CourseAssessment> assessments, User student);

    @Query("""
            select count(distinct submission.assessment.id)
            from AssessmentSubmission submission
            where submission.student = :student
              and submission.assessment.onlineCourse = :course
              and submission.assessment.active = true
              and submission.status in :completedStatuses
            """)
    long countCompletedAssessments(User student, OnlineCourse course, Set<SubmissionStatus> completedStatuses);
}
