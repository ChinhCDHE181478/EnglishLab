package fu.sap490.g23.backend.repository.classroom;

import fu.sap490.g23.backend.entity.classroom.ClassroomGradebookEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClassroomGradebookEntryRepository extends JpaRepository<ClassroomGradebookEntry, Long> {
    List<ClassroomGradebookEntry> findByClassroomOfferingId(Long classroomOfferingId);

    Optional<ClassroomGradebookEntry> findByClassroomOfferingIdAndStudentId(Long classroomOfferingId, Long studentId);
}
