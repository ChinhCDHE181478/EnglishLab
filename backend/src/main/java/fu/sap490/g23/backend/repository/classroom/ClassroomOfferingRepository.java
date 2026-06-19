package fu.sap490.g23.backend.repository.classroom;

import fu.sap490.g23.backend.entity.classroom.ClassroomDeliveryMode;
import fu.sap490.g23.backend.entity.classroom.ClassroomOffering;
import fu.sap490.g23.backend.entity.classroom.ClassroomOfferingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ClassroomOfferingRepository extends JpaRepository<ClassroomOffering, Long> {

    @Query("SELECT co FROM ClassroomOffering co JOIN FETCH co.learningPackage lp WHERE lp.slug = :slugOrId AND lp.deleted = false")
    Optional<ClassroomOffering> findByLearningPackageSlug(@Param("slugOrId") String slugOrId);

    @Query("SELECT co FROM ClassroomOffering co JOIN FETCH co.learningPackage lp WHERE lp.id = :packageId AND lp.deleted = false")
    Optional<ClassroomOffering> findByLearningPackageId(@Param("packageId") Long packageId);

    @Query("SELECT co FROM ClassroomOffering co JOIN co.learningPackage lp WHERE lp.deleted = false AND lp.status = 'PUBLISHED' AND (:mode IS NULL OR co.deliveryMode = :mode)")
    Page<ClassroomOffering> findPublished(@Param("mode") ClassroomDeliveryMode mode, Pageable pageable);

    boolean existsByLearningPackage_TitleIgnoreCase(String title);
}
