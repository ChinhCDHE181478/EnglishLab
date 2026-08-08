package fu.sap490.g23.backend.repository.classroom;

import fu.sap490.g23.backend.entity.classroom.ClassroomHomeworkSubmission;
import fu.sap490.g23.backend.entity.classroom.enums.HomeworkSubmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClassroomHomeworkSubmissionRepository extends JpaRepository<ClassroomHomeworkSubmission, Long> {
    Optional<ClassroomHomeworkSubmission> findByHomeworkIdAndStudentId(Long homeworkId, Long studentId);

    List<ClassroomHomeworkSubmission> findByHomeworkIdOrderBySubmittedAtDesc(Long homeworkId);

    List<ClassroomHomeworkSubmission> findByHomeworkId(Long homeworkId);

    long countByHomeworkId(Long homeworkId);

    long countByHomeworkIdAndStatus(Long homeworkId, HomeworkSubmissionStatus status);

    List<ClassroomHomeworkSubmission> findByStudentId(Long studentId);

    @Query("""
            select submission
            from ClassroomHomeworkSubmission submission
            join fetch submission.homework homework
            join fetch submission.student student
            where homework.classroomOffering.id = :offeringId
            """)
    List<ClassroomHomeworkSubmission> findAllForGradebook(@Param("offeringId") Long offeringId);

    @Query("""
            select submission
            from ClassroomHomeworkSubmission submission
            join fetch submission.homework homework
            where homework.classroomOffering.id = :offeringId
              and submission.student.id = :studentId
            """)
    List<ClassroomHomeworkSubmission> findAllForStudentGradebook(
            @Param("offeringId") Long offeringId,
            @Param("studentId") Long studentId
    );

    boolean existsByHomeworkId(Long homeworkId);

    Optional<ClassroomHomeworkSubmission> findFirstByAttachmentUrlEndingWith(String suffix);

    boolean existsByAttachmentUrlEndingWith(String suffix);
}
