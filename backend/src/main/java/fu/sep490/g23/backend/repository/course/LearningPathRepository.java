package fu.sap490.g23.backend.repository.course;

import fu.sap490.g23.backend.entity.course.LearningPath;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LearningPathRepository extends JpaRepository<LearningPath, Long> {
    boolean existsByCodeIgnoreCase(String code);
    Optional<LearningPath> findByCodeIgnoreCase(String code);
}
