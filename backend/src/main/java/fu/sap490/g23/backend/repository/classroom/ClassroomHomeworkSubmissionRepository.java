package fu.sap490.g23.backend.repository.classroom;

import fu.sap490.g23.backend.entity.classroom.ClassroomHomeworkSubmission;
import fu.sap490.g23.backend.entity.classroom.enums.HomeworkSubmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClassroomHomeworkSubmissionRepository extends JpaRepository<ClassroomHomeworkSubmission, Long> {
    Optional<ClassroomHomeworkSubmission> findByHomeworkIdAndStudentId(Long homeworkId, Long studentId);

    List<ClassroomHomeworkSubmission> findByHomeworkIdOrderBySubmittedAtDesc(Long homeworkId);

    List<ClassroomHomeworkSubmission> findByHomeworkId(Long homeworkId);

    long countByHomeworkId(Long homeworkId);

    long countByHomeworkIdAndStatus(Long homeworkId, HomeworkSubmissionStatus status);

    List<ClassroomHomeworkSubmission> findByStudentId(Long studentId);

    boolean existsByHomeworkId(Long homeworkId);
}
