package fu.sep490.g23.backend.repository.course;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.entity.course.OnlineCourseEnrollment;
import fu.sep490.g23.backend.entity.course.enums.EnrollmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OnlineCourseEnrollmentRepository
        extends JpaRepository<OnlineCourseEnrollment, Long>, JpaSpecificationExecutor<OnlineCourseEnrollment> {

    @Override
    @EntityGraph(attributePaths = {"student", "onlineCourse"})
    Page<OnlineCourseEnrollment> findAll(Specification<OnlineCourseEnrollment> specification, Pageable pageable);

    boolean existsByStudentAndOnlineCourse(User student, OnlineCourse onlineCourse);

    Optional<OnlineCourseEnrollment> findByStudentAndOnlineCourse(User student, OnlineCourse onlineCourse);

    @EntityGraph(attributePaths = {"onlineCourse"})
    List<OnlineCourseEnrollment> findByStudentOrderByRegisteredAtDesc(User student);

    List<OnlineCourseEnrollment> findByOnlineCourse(OnlineCourse onlineCourse);

    long countByOnlineCourse(OnlineCourse onlineCourse);

    long count();

    List<OnlineCourseEnrollment> findByStatusAndProgressPercentBetweenAndUpdatedAtBefore(
            EnrollmentStatus status,
            Integer minimumProgress,
            Integer maximumProgress,
            LocalDateTime updatedBefore
    );

    long countByOnlineCourseAndReviewRatingIsNotNull(OnlineCourse onlineCourse);

    @Query("select avg(e.reviewRating) from OnlineCourseEnrollment e where e.onlineCourse = :course and e.reviewRating is not null")
    Double findAverageReviewRatingByOnlineCourse(@Param("course") OnlineCourse course);
}
