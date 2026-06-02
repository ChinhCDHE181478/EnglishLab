package fu.sap490.g23.backend.repository.course;

import fu.sap490.g23.backend.entity.course.LearningPackage;
import fu.sap490.g23.backend.entity.course.PackageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LearningPackageRepository extends JpaRepository<LearningPackage, Long>, JpaSpecificationExecutor<LearningPackage> {
    boolean existsBySlug(String slug);
    Optional<LearningPackage> findByIdAndDeletedFalse(Long id);
    Optional<LearningPackage> findBySlugAndDeletedFalse(String slug);
    long countByDeletedFalse();
    long countByDeletedFalseAndStatus(PackageStatus status);
}
