package fu.sap490.g23.backend.repository.classroom;

import fu.sap490.g23.backend.entity.classroom.ClassroomCampus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClassroomCampusRepository extends JpaRepository<ClassroomCampus, Long> {
    List<ClassroomCampus> findByActiveTrueOrderByNameAsc();
}
