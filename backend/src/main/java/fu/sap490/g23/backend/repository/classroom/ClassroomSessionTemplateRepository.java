package fu.sap490.g23.backend.repository.classroom;

import fu.sap490.g23.backend.entity.classroom.ClassroomSessionTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClassroomSessionTemplateRepository extends JpaRepository<ClassroomSessionTemplate, Long> {
    List<ClassroomSessionTemplate> findByActiveTrueOrderByNameAsc();

    Optional<ClassroomSessionTemplate> findByNameIgnoreCase(String name);
}
