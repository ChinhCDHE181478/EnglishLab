package fu.sap490.g23.backend.repository.course;

import fu.sap490.g23.backend.entity.course.PackageType;
import fu.sap490.g23.backend.entity.course.PackageTypeCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PackageTypeRepository extends JpaRepository<PackageType, Long> {
    Optional<PackageType> findByCode(PackageTypeCode code);
    boolean existsByCode(PackageTypeCode code);
}
