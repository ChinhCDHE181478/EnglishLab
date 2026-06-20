package fu.sap490.g23.backend.repository.classroom;

import fu.sap490.g23.backend.entity.classroom.ClassroomEnrollment;
import fu.sap490.g23.backend.entity.classroom.enums.ClassroomEnrollmentStatus;
import fu.sap490.g23.backend.entity.classroom.enums.ClassroomRegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ClassroomEnrollmentRepository extends JpaRepository<ClassroomEnrollment, Long> {

    Optional<ClassroomEnrollment> findByStudentIdAndClassroomOfferingId(Long studentId, Long classroomOfferingId);

    boolean existsByStudentIdAndClassroomOfferingIdAndStatusIn(
            Long studentId,
            Long classroomOfferingId,
            Collection<ClassroomEnrollmentStatus> statuses
    );

    boolean existsByStudentIdAndClassroomOfferingIdAndRegistrationStatusIn(
            Long studentId,
            Long classroomOfferingId,
            Collection<ClassroomRegistrationStatus> statuses
    );

    @Query("SELECT COUNT(e) FROM ClassroomEnrollment e WHERE e.classroomOffering.id = :offeringId AND e.status IN :statuses")
    long countByOfferingAndStatuses(@Param("offeringId") Long offeringId, @Param("statuses") Collection<ClassroomEnrollmentStatus> statuses);

    @Query("SELECT COUNT(e) FROM ClassroomEnrollment e WHERE e.classroomOffering.id = :offeringId AND e.registrationStatus IN :statuses")
    long countByOfferingAndRegistrationStatuses(
            @Param("offeringId") Long offeringId,
            @Param("statuses") Collection<ClassroomRegistrationStatus> statuses
    );

    List<ClassroomEnrollment> findByStudentIdAndStatusIn(Long studentId, Collection<ClassroomEnrollmentStatus> statuses);

    List<ClassroomEnrollment> findByStudentIdAndRegistrationStatusIn(
            Long studentId,
            Collection<ClassroomRegistrationStatus> statuses
    );

    List<ClassroomEnrollment> findByClassroomOfferingIdAndStatusIn(Long classroomOfferingId, Collection<ClassroomEnrollmentStatus> statuses);

    List<ClassroomEnrollment> findByClassroomOfferingIdAndRegistrationStatusIn(
            Long classroomOfferingId,
            Collection<ClassroomRegistrationStatus> statuses
    );

    List<ClassroomEnrollment> findByRegistrationStatusIn(Collection<ClassroomRegistrationStatus> statuses);

    List<ClassroomEnrollment> findAllByOrderByEnrolledAtDesc();
}
