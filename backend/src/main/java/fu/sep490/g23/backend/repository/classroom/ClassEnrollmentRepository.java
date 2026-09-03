package fu.sep490.g23.backend.repository.classroom;

import fu.sep490.g23.backend.entity.classroom.ClassEnrollment;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomRegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ClassEnrollmentRepository extends JpaRepository<ClassEnrollment, Long> {

    boolean existsByStudentIdAndClassSectionId(Long studentId, Long classSectionId);

    Optional<ClassEnrollment> findByStudentIdAndClassSectionId(Long studentId, Long classSectionId);

    boolean existsByStudentIdAndClassSectionIdAndRegistrationStatusIn(
            Long studentId,
            Long classSectionId,
            Collection<ClassroomRegistrationStatus> statuses
    );

    @Query("SELECT COUNT(e) FROM ClassEnrollment e WHERE e.classSection.id = :offeringId AND e.registrationStatus IN :statuses")
    long countByOfferingAndRegistrationStatuses(
            @Param("offeringId") Long offeringId,
            @Param("statuses") Collection<ClassroomRegistrationStatus> statuses
    );

    List<ClassEnrollment> findByStudentIdAndRegistrationStatusIn(
            Long studentId,
            Collection<ClassroomRegistrationStatus> statuses
    );

    List<ClassEnrollment> findAllByClassSectionId(Long classSectionId);

    List<ClassEnrollment> findByClassSectionIdAndRegistrationStatusIn(
            Long classSectionId,
            Collection<ClassroomRegistrationStatus> statuses
    );

    List<ClassEnrollment> findByClassSectionIdAndRegistrationStatusOrderByWaitlistPriorityAscEnrolledAtAscIdAsc(
            Long classSectionId,
            ClassroomRegistrationStatus status
    );

    @Query("""
            SELECT MAX(e.waitlistPriority)
            FROM ClassEnrollment e
            WHERE e.classSection.id = :offeringId
              AND e.registrationStatus = :status
            """)
    Integer findMaxWaitlistPriority(
            @Param("offeringId") Long offeringId,
            @Param("status") ClassroomRegistrationStatus status
    );

    List<ClassEnrollment> findByRegistrationStatusIn(Collection<ClassroomRegistrationStatus> statuses);


    List<ClassEnrollment> findAllByOrderByEnrolledAtDesc();
}
