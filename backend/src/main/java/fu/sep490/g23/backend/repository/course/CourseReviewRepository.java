package fu.sep490.g23.backend.repository.course;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.course.CourseReview;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CourseReviewRepository extends JpaRepository<CourseReview, Long> {
    Optional<CourseReview> findByStudentAndCourse(User student, OnlineCourse course);
    long countByCourse(OnlineCourse course);
    @Query("select avg(review.rating) from CourseReview review where review.course = :course")
    Double findAverageRatingByCourse(@Param("course") OnlineCourse course);
}
