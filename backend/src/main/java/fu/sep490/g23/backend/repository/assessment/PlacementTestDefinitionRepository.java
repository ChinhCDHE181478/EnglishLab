package fu.sep490.g23.backend.repository.assessment;

import fu.sep490.g23.backend.entity.assessment.PlacementTestDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlacementTestDefinitionRepository extends JpaRepository<PlacementTestDefinition, Long> {
    Optional<PlacementTestDefinition> findByTestCode(String testCode);
}
