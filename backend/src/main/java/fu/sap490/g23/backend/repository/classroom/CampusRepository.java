package fu.sap490.g23.backend.repository.classroom;

import fu.sap490.g23.backend.entity.classroom.Campus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CampusRepository extends JpaRepository<Campus, Long> {
    Optional<Campus> findByNameIgnoreCase(String name);
}
