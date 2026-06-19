package fu.sap490.g23.backend.repository.classroom;

import fu.sap490.g23.backend.entity.classroom.ClassroomHomeworkSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClassroomHomeworkSubmissionRepository extends JpaRepository<ClassroomHomeworkSubmission, Long> {
    Optional<ClassroomHomeworkSubmission> findByHomeworkIdAndStudentId(Long homeworkId, Long studentId);

    List<ClassroomHomeworkSubmission> findByHomeworkId(Long homeworkId);

    List<ClassroomHomeworkSubmission> findByStudentId(Long studentId);

    boolean existsByHomeworkId(Long homeworkId);
}
