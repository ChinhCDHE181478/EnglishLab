package fu.sep490.g23.backend.repository.classroom;

import fu.sep490.g23.backend.entity.classroom.ClassroomHomework;
import fu.sep490.g23.backend.entity.classroom.enums.HomeworkStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

public interface ClassroomHomeworkRepository extends JpaRepository<ClassroomHomework, Long> {
    List<ClassroomHomework> findByStatusAndDeadlineBetween(
            HomeworkStatus status,
            LocalDateTime from,
            LocalDateTime to
    );
    List<ClassroomHomework> findByClassSectionIdOrderByCreatedAtDesc(Long classSectionId);

    List<ClassroomHomework> findByClassSectionIdAndStatusOrderByDeadlineAsc(Long classSectionId, HomeworkStatus status);

    Optional<ClassroomHomework> findFirstByAttachmentUrlEndingWith(String suffix);

    boolean existsByAttachmentUrlEndingWith(String suffix);
}
