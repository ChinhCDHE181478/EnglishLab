package fu.sap490.g23.backend.repository.classroom;

import fu.sap490.g23.backend.entity.classroom.ClassroomHomework;
import fu.sap490.g23.backend.entity.classroom.enums.HomeworkStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.time.LocalDateTime;

public interface ClassroomHomeworkRepository extends JpaRepository<ClassroomHomework, Long> {
    List<ClassroomHomework> findByStatusAndDeadlineBetween(
            HomeworkStatus status,
            LocalDateTime from,
            LocalDateTime to
    );
    List<ClassroomHomework> findByClassroomOfferingIdOrderByCreatedAtDesc(Long classroomOfferingId);

    List<ClassroomHomework> findByClassroomOfferingIdAndStatusOrderByDeadlineAsc(Long classroomOfferingId, HomeworkStatus status);
}
