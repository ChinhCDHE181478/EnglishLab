package fu.sep490.g23.backend.repository.assessment;

import fu.sep490.g23.backend.entity.assessment.CourseAssessment;
import fu.sep490.g23.backend.entity.course.OnlineCourseModule;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.entity.course.OnlineCourseVersion;
import fu.sep490.g23.backend.entity.course.OnlineLesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CourseAssessmentRepository extends JpaRepository<CourseAssessment, Long> {
    @Query("""
            select assessment from CourseAssessment assessment
            where assessment.onlineCourseVersion.onlineCourse = :onlineCourse
              and assessment.active = true
            order by assessment.displayOrder asc, assessment.id asc
            """)
    List<CourseAssessment> findByOnlineCourseAndActiveTrueOrderByDisplayOrderAscIdAsc(OnlineCourse onlineCourse);
    List<CourseAssessment> findByOnlineCourseVersionAndActiveTrueOrderByDisplayOrderAscIdAsc(OnlineCourseVersion version);
    List<CourseAssessment> findByModuleAndActiveTrueOrderByDisplayOrderAscIdAsc(OnlineCourseModule module);
    List<CourseAssessment> findByModule(OnlineCourseModule module);
    List<CourseAssessment> findByOnlineLesson(OnlineLesson onlineLesson);
    @Query("""
            select count(assessment) from CourseAssessment assessment
            where assessment.onlineCourseVersion.onlineCourse = :onlineCourse
              and assessment.active = true
            """)
    long countByOnlineCourseAndActiveTrue(OnlineCourse onlineCourse);
    long countByOnlineCourseVersionAndActiveTrue(OnlineCourseVersion version);
}
