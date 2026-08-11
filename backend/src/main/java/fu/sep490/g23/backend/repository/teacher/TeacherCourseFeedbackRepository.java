package fu.sep490.g23.backend.repository.teacher;

import fu.sep490.g23.backend.entity.teacher.TeacherCourseFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeacherCourseFeedbackRepository extends JpaRepository<TeacherCourseFeedback, Long> {
    Optional<TeacherCourseFeedback> findByEnrollmentIdAndTeacherId(Long enrollmentId, Long teacherId);
    List<TeacherCourseFeedback> findByTeacherIdOrderBySubmittedAtDesc(Long teacherId);
    List<TeacherCourseFeedback> findByClassroomOfferingIdAndTeacherIdOrderBySubmittedAtDesc(
            Long classroomOfferingId,
            Long teacherId
    );
}
