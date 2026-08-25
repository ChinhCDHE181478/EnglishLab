package fu.sep490.g23.backend.repository.classroom;

import fu.sep490.g23.backend.entity.classroom.ClassroomGradebookEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;

public interface ClassroomGradebookEntryRepository extends JpaRepository<ClassroomGradebookEntry, Long> {
    @EntityGraph(attributePaths = "student")
    List<ClassroomGradebookEntry> findByClassSectionId(Long classSectionId);

    @EntityGraph(attributePaths = "student")
    Optional<ClassroomGradebookEntry> findByClassSectionIdAndStudentId(Long classSectionId, Long studentId);
}
