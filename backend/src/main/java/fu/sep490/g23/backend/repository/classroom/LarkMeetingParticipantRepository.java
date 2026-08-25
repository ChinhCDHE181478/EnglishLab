package fu.sep490.g23.backend.repository.classroom;

import fu.sep490.g23.backend.entity.classroom.LarkMeetingParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LarkMeetingParticipantRepository extends JpaRepository<LarkMeetingParticipant, Long> {

    Optional<LarkMeetingParticipant> findByClassScheduleIdAndParticipantKey(
            Long classScheduleId,
            String participantKey
    );

    long countByClassScheduleIdAndActiveTrue(Long classScheduleId);

    java.util.List<LarkMeetingParticipant> findByClassScheduleId(Long classScheduleId);

    void deleteByClassScheduleId(Long classScheduleId);
}
