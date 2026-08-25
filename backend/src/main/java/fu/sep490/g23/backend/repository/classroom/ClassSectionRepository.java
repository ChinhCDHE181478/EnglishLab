package fu.sep490.g23.backend.repository.classroom;

import fu.sep490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import fu.sep490.g23.backend.entity.classroom.ClassSection;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomOfferingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface ClassSectionRepository extends JpaRepository<ClassSection, Long> {

    @Query("SELECT co FROM ClassSection co JOIN FETCH co.learningPackage lp WHERE lp.slug = :slugOrId AND lp.deleted = false")
    Optional<ClassSection> findByLearningPackageSlug(@Param("slugOrId") String slugOrId);

    @Query("SELECT co FROM ClassSection co JOIN FETCH co.learningPackage lp WHERE LOWER(lp.title) = LOWER(:title) AND lp.deleted = false")
    Optional<ClassSection> findByLearningPackageTitleIgnoreCase(@Param("title") String title);

    @Query("SELECT co FROM ClassSection co JOIN FETCH co.learningPackage lp WHERE lp.id = :packageId AND lp.deleted = false")
    Optional<ClassSection> findByLearningPackageId(@Param("packageId") Long packageId);

    @Query("""
            SELECT co
            FROM ClassSection co
            JOIN co.learningPackage lp
            WHERE lp.deleted = false
              AND lp.status = 'PUBLISHED'
              AND co.status = 'UPCOMING'
              AND co.startDate > CURRENT_DATE
              AND (:mode IS NULL OR co.deliveryMode = :mode)
            """)
    Page<ClassSection> findPublished(@Param("mode") ClassroomDeliveryMode mode, Pageable pageable);

    boolean existsByLearningPackage_TitleIgnoreCase(String title);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT co
            FROM ClassSection co
            JOIN FETCH co.learningPackage lp
            WHERE co.id = :id
              AND lp.deleted = false
            """)
    Optional<ClassSection> findByIdForUpdate(@Param("id") Long id);

    List<ClassSection> findByPlannedEndDateBetween(LocalDate from, LocalDate to);

    List<ClassSection> findByStatusIn(Collection<ClassroomOfferingStatus> statuses);
}
