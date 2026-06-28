package fu.sap490.g23.backend.repository.classroom;

import fu.sap490.g23.backend.entity.classroom.ClassroomSession;
import fu.sap490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import fu.sap490.g23.backend.entity.classroom.enums.ClassroomSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ClassroomSessionRepository extends JpaRepository<ClassroomSession, Long> {

    List<ClassroomSession> findByClassroomOfferingIdOrderBySessionDateAscStartTimeAsc(Long classroomOfferingId);

    long countByClassroomOfferingId(Long classroomOfferingId);

    List<ClassroomSession> findByDeliveryModeAndStatusIn(
            ClassroomDeliveryMode deliveryMode,
            Collection<ClassroomSessionStatus> statuses
    );

    Optional<ClassroomSession> findByLarkMeetingId(String larkMeetingId);

    Optional<ClassroomSession> findByLarkMeetingNo(String larkMeetingNo);

    List<ClassroomSession> findByLarkEmptySinceIsNotNullAndLarkEmptySinceBefore(LocalDateTime cutoff);

    @Query("""
            SELECT s FROM ClassroomSession s
            WHERE s.teacher.id = :teacherId
              AND s.status IN :statuses
              AND s.sessionDate = :sessionDate
              AND s.startTime < :endTime AND s.endTime > :startTime
              AND (:excludeSessionId IS NULL OR s.id <> :excludeSessionId)
            """)
    List<ClassroomSession> findTeacherConflicts(
            @Param("teacherId") Long teacherId,
            @Param("sessionDate") LocalDate sessionDate,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("statuses") Collection<ClassroomSessionStatus> statuses,
            @Param("excludeSessionId") Long excludeSessionId
    );

    @Query("""
            SELECT s FROM ClassroomSession s
            WHERE s.room.id = :roomId
              AND s.status IN :statuses
              AND s.sessionDate = :sessionDate
              AND s.startTime < :endTime AND s.endTime > :startTime
              AND (:excludeSessionId IS NULL OR s.id <> :excludeSessionId)
            """)
    List<ClassroomSession> findRoomConflicts(
            @Param("roomId") Long roomId,
            @Param("sessionDate") LocalDate sessionDate,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("statuses") Collection<ClassroomSessionStatus> statuses,
            @Param("excludeSessionId") Long excludeSessionId
    );

    @Query("""
            SELECT s FROM ClassroomSession s
            JOIN ClassroomEnrollment e ON e.classroomOffering = s.classroomOffering
            WHERE e.student.id = :studentId
              AND e.status IN ('ENROLLED', 'WAITING')
              AND s.status IN :statuses
              AND s.sessionDate = :sessionDate
              AND s.startTime < :endTime AND s.endTime > :startTime
              AND (:excludeSessionId IS NULL OR s.id <> :excludeSessionId)
            """)
    List<ClassroomSession> findLearnerConflicts(
            @Param("studentId") Long studentId,
            @Param("sessionDate") LocalDate sessionDate,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("statuses") Collection<ClassroomSessionStatus> statuses,
            @Param("excludeSessionId") Long excludeSessionId
    );

    @Query("""
            SELECT s FROM ClassroomSession s
            JOIN s.classroomOffering co
            JOIN ClassroomTeacherAssignment ta ON ta.classroomOffering = co
            WHERE ta.teacher.id = :teacherId
              AND s.sessionDate >= :fromDate
            ORDER BY s.sessionDate ASC, s.startTime ASC
            """)
    List<ClassroomSession> findTeacherSchedule(
            @Param("teacherId") Long teacherId,
            @Param("fromDate") LocalDate fromDate
    );
}
