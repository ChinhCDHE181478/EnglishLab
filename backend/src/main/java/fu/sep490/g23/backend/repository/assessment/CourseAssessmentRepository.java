package fu.sep490.g23.backend.repository.assessment;

import fu.sep490.g23.backend.entity.assessment.CourseAssessment;
import fu.sep490.g23.backend.entity.course.CourseModule;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseAssessmentRepository extends JpaRepository<CourseAssessment, Long> {
    List<CourseAssessment> findByOnlineCourseAndActiveTrueOrderByDisplayOrderAscIdAsc(OnlineCourse onlineCourse);
    List<CourseAssessment> findByModuleAndActiveTrueOrderByDisplayOrderAscIdAsc(CourseModule module);
    List<CourseAssessment> findByModule(CourseModule module);
    long countByOnlineCourseAndActiveTrue(OnlineCourse onlineCourse);
}
