package fu.sep490.g23.backend.repository.assessment;

import fu.sep490.g23.backend.entity.assessment.CourseAssessment;
import fu.sep490.g23.backend.entity.course.OnlineCourseModule;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.entity.course.OnlineLesson;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseAssessmentRepository extends JpaRepository<CourseAssessment, Long> {
    List<CourseAssessment> findByOnlineCourseAndActiveTrueOrderByDisplayOrderAscIdAsc(OnlineCourse onlineCourse);
    List<CourseAssessment> findByModuleAndActiveTrueOrderByDisplayOrderAscIdAsc(OnlineCourseModule module);
    List<CourseAssessment> findByModule(OnlineCourseModule module);
    List<CourseAssessment> findByOnlineLesson(OnlineLesson onlineLesson);
    long countByOnlineCourseAndActiveTrue(OnlineCourse onlineCourse);
}
