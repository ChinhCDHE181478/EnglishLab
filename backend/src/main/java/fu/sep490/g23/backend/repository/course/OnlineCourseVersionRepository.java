package fu.sep490.g23.backend.repository.course;

import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.entity.course.OnlineCourseVersion;
import fu.sep490.g23.backend.entity.course.enums.CourseVersionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OnlineCourseVersionRepository extends JpaRepository<OnlineCourseVersion, Long> {
    List<OnlineCourseVersion> findByOnlineCourseOrderByVersionNumberDesc(OnlineCourse onlineCourse);

    Optional<OnlineCourseVersion> findFirstByOnlineCourseAndStatusOrderByVersionNumberDesc(
            OnlineCourse onlineCourse,
            CourseVersionStatus status
    );

    Optional<OnlineCourseVersion> findByIdAndOnlineCourseId(Long id, Long onlineCourseId);

    boolean existsByOnlineCourseAndStatusIn(OnlineCourse onlineCourse, List<CourseVersionStatus> statuses);

    List<OnlineCourseVersion> findByStatusOrderBySubmittedAtAsc(CourseVersionStatus status);
}
