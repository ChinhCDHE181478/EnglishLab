package fu.sep490.g23.backend.repository.classroom;

import fu.sep490.g23.backend.entity.classroom.ClassroomTeacherAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClassroomTeacherAssignmentRepository extends JpaRepository<ClassroomTeacherAssignment, Long> {
    List<ClassroomTeacherAssignment> findByClassSectionId(Long classSectionId);

    List<ClassroomTeacherAssignment> findByTeacherId(Long teacherId);

    List<ClassroomTeacherAssignment> findAllByClassSectionIdAndTeacherId(Long classSectionId, Long teacherId);

    Optional<ClassroomTeacherAssignment> findByClassScheduleId(Long classScheduleId);
}
