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
    long countByPrimaryTeacherIsNotNull();
    List<ClassSection> findByPrimaryTeacherId(Long teacherId);
    Optional<ClassSection> findByCode(String code);

    @Query("SELECT co FROM ClassSection co JOIN FETCH co.instructorLedCourse course WHERE course.slug = :slugOrId OR co.code = :slugOrId")
    Optional<ClassSection> findByInstructorLedCourseSlugOrCode(@Param("slugOrId") String slugOrId);

    @Query("SELECT co FROM ClassSection co WHERE LOWER(co.name) = LOWER(:title)")
    Optional<ClassSection> findByNameIgnoreCase(@Param("title") String title);

    @Query("SELECT co FROM ClassSection co WHERE co.id = :packageId")
    Optional<ClassSection> findByIdAsCatalogItem(@Param("packageId") Long packageId);

    @Query("""
            SELECT co
            FROM ClassSection co
            WHERE co.instructorLedCourse.publicationStatus = 'PUBLISHED'
              AND co.status = 'UPCOMING'
              AND co.startDate > CURRENT_DATE
              AND (:mode IS NULL OR co.deliveryMode = :mode)
            """)
    Page<ClassSection> findPublished(@Param("mode") ClassroomDeliveryMode mode, Pageable pageable);

    boolean existsByNameIgnoreCase(String title);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT co
            FROM ClassSection co
            WHERE co.id = :id
            """)
    Optional<ClassSection> findByIdForUpdate(@Param("id") Long id);

    List<ClassSection> findByPlannedEndDateBetween(LocalDate from, LocalDate to);

    List<ClassSection> findByStatusIn(Collection<ClassroomOfferingStatus> statuses);
}
