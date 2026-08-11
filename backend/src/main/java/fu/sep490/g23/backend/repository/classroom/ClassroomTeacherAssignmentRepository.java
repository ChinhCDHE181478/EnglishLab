package fu.sap490.g23.backend.repository.classroom;

import fu.sap490.g23.backend.entity.classroom.ClassroomTeacherAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClassroomTeacherAssignmentRepository extends JpaRepository<ClassroomTeacherAssignment, Long> {
    List<ClassroomTeacherAssignment> findByClassroomOfferingId(Long classroomOfferingId);

    List<ClassroomTeacherAssignment> findByTeacherId(Long teacherId);

    List<ClassroomTeacherAssignment> findAllByClassroomOfferingIdAndTeacherId(Long classroomOfferingId, Long teacherId);

    Optional<ClassroomTeacherAssignment> findByClassroomSessionId(Long classroomSessionId);
}
