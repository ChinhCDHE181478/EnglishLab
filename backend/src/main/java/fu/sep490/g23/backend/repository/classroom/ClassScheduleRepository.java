package fu.sep490.g23.backend.repository.classroom;

import fu.sep490.g23.backend.entity.classroom.ClassSchedule;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomSessionStatus;
import fu.sep490.g23.backend.entity.classroom.enums.RecordingSyncStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ClassScheduleRepository extends JpaRepository<ClassSchedule, Long> {

    List<ClassSchedule> findByClassSectionIdOrderBySessionDateAscStartTimeAsc(Long classSectionId);

    long countByClassSectionId(Long classSectionId);

    long countByClassSectionIdAndStatus(Long classSectionId, ClassroomSessionStatus status);

    long countByClassSectionIdAndStatusNot(Long classSectionId, ClassroomSessionStatus status);

    long countByTeacherId(Long teacherId);

    long countByTeacherIdAndStatus(Long teacherId, ClassroomSessionStatus status);

    boolean existsByCurriculumSessionPlanId(Long curriculumSessionPlanId);

    boolean existsByCurriculumSessionPlanUnitId(Long unitId);

    List<ClassSchedule> findByDeliveryModeAndStatusIn(
            ClassroomDeliveryMode deliveryMode,
            Collection<ClassroomSessionStatus> statuses
    );

    List<ClassSchedule> findByDeliveryModeAndSessionDateBetweenOrderBySessionDateAscStartTimeAsc(
            ClassroomDeliveryMode deliveryMode,
            LocalDate fromDate,
            LocalDate toDate
    );

    List<ClassSchedule> findByStatusInAndSessionDateBetweenOrderBySessionDateAscStartTimeAsc(
            Collection<ClassroomSessionStatus> statuses,
            LocalDate fromDate,
            LocalDate toDate
    );

    @Query("""
            SELECT s FROM ClassSchedule s
            WHERE s.status IN :statuses
              AND (s.sessionDate < :cutoffDate
                   OR (s.sessionDate = :cutoffDate AND s.endTime <= :cutoffTime))
            ORDER BY s.sessionDate ASC, s.endTime ASC
            """)
    List<ClassSchedule> findSessionsEndedBefore(
            @Param("statuses") Collection<ClassroomSessionStatus> statuses,
            @Param("cutoffDate") LocalDate cutoffDate,
            @Param("cutoffTime") LocalTime cutoffTime
    );

    @Query("""
            SELECT s FROM ClassSchedule s
            WHERE s.deliveryMode = :deliveryMode
              AND s.status IN :sessionStatuses
              AND s.sessionDate >= :fromDate
              AND (s.larkSyncStatus IS NULL OR s.larkSyncStatus IN :syncStatuses)
            ORDER BY s.updatedAt ASC, s.id ASC
            """)
    List<ClassSchedule> findVirtualMeetingsPendingSync(
            @Param("deliveryMode") ClassroomDeliveryMode deliveryMode,
            @Param("sessionStatuses") Collection<ClassroomSessionStatus> sessionStatuses,
            @Param("syncStatuses") Collection<String> syncStatuses,
            @Param("fromDate") LocalDate fromDate,
            Pageable pageable
    );

    Optional<ClassSchedule> findByLarkMeetingId(String larkMeetingId);

    Optional<ClassSchedule> findByLarkMeetingNo(String larkMeetingNo);

    List<ClassSchedule> findByLarkEmptySinceIsNotNullAndLarkEmptySinceBefore(LocalDateTime cutoff);

    List<ClassSchedule> findByRecordingVisibleTrueAndRecordingExpiresAtBefore(LocalDateTime now);

    @Query("""
            SELECT s FROM ClassSchedule s
            WHERE s.recordingSyncStatus IN :statuses
              AND s.larkMeetingId IS NOT NULL
              AND s.recordingSyncAttempts < :maxAttempts
              AND (s.recordingLastAttemptAt IS NULL OR s.recordingLastAttemptAt <= :retryBefore)
            ORDER BY s.recordingLastAttemptAt ASC, s.id ASC
            """)
    List<ClassSchedule> findRecordingsPendingSync(
            @Param("statuses") Collection<RecordingSyncStatus> statuses,
            @Param("maxAttempts") int maxAttempts,
            @Param("retryBefore") LocalDateTime retryBefore
    );

    @Query("""
            SELECT s FROM ClassSchedule s
            WHERE s.recordingSyncStatus IN :statuses
              AND s.larkMeetingId LIKE 'spaces/%'
              AND s.recordingSyncAttempts < :maxAttempts
              AND (s.recordingLastAttemptAt IS NULL OR s.recordingLastAttemptAt <= :retryBefore)
            ORDER BY s.recordingLastAttemptAt ASC, s.id ASC
            """)
    List<ClassSchedule> findGoogleMeetRecordingsPendingSync(
            @Param("statuses") Collection<RecordingSyncStatus> statuses,
            @Param("maxAttempts") int maxAttempts,
            @Param("retryBefore") LocalDateTime retryBefore
    );

    @Query("""
            SELECT s FROM ClassSchedule s
            WHERE s.teacher.id = :teacherId
              AND s.status IN :statuses
              AND s.sessionDate = :sessionDate
              AND s.startTime < :endTime AND s.endTime > :startTime
              AND (:excludeSessionId IS NULL OR s.id <> :excludeSessionId)
            """)
    List<ClassSchedule> findTeacherConflicts(
            @Param("teacherId") Long teacherId,
            @Param("sessionDate") LocalDate sessionDate,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("statuses") Collection<ClassroomSessionStatus> statuses,
            @Param("excludeSessionId") Long excludeSessionId
    );

    @Query("""
            SELECT s FROM ClassSchedule s
            WHERE s.room.id = :roomId
              AND s.status IN :statuses
              AND s.sessionDate = :sessionDate
              AND s.startTime < :endTime AND s.endTime > :startTime
              AND (:excludeSessionId IS NULL OR s.id <> :excludeSessionId)
            """)
    List<ClassSchedule> findRoomConflicts(
            @Param("roomId") Long roomId,
            @Param("sessionDate") LocalDate sessionDate,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("statuses") Collection<ClassroomSessionStatus> statuses,
            @Param("excludeSessionId") Long excludeSessionId
    );

    @Query("""
            SELECT s FROM ClassSchedule s
            JOIN ClassEnrollment e ON e.classSection = s.classSection
            WHERE e.student.id = :studentId
              AND e.status IN ('ENROLLED', 'WAITING')
              AND s.status IN :statuses
              AND s.sessionDate = :sessionDate
              AND s.startTime < :endTime AND s.endTime > :startTime
              AND (:excludeSessionId IS NULL OR s.id <> :excludeSessionId)
            """)
    List<ClassSchedule> findLearnerConflicts(
            @Param("studentId") Long studentId,
            @Param("sessionDate") LocalDate sessionDate,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("statuses") Collection<ClassroomSessionStatus> statuses,
            @Param("excludeSessionId") Long excludeSessionId
    );

    @Query("""
            SELECT DISTINCT s FROM ClassSchedule s
            JOIN s.classSection co
            JOIN ClassroomTeacherAssignment ta ON ta.classSection = co
            WHERE ta.teacher.id = :teacherId
              AND s.sessionDate >= :fromDate
              AND (ta.effectiveFrom IS NULL OR ta.effectiveFrom <= :fromDate)
              AND (ta.effectiveTo IS NULL OR ta.effectiveTo >= :fromDate)
              AND (ta.classSchedule IS NULL OR ta.classSchedule = s)
            ORDER BY s.sessionDate ASC, s.startTime ASC
            """)
    List<ClassSchedule> findTeacherSchedule(
            @Param("teacherId") Long teacherId,
            @Param("fromDate") LocalDate fromDate
    );
}
