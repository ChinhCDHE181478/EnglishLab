package fu.sep490.g23.backend.repository.classroom;

import fu.sep490.g23.backend.entity.classroom.ClassroomSyllabusItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClassroomSyllabusItemRepository extends JpaRepository<ClassroomSyllabusItem, Long> {
    List<ClassroomSyllabusItem> findByClassroomOfferingIdOrderByDisplayOrderAsc(Long classroomOfferingId);
}
