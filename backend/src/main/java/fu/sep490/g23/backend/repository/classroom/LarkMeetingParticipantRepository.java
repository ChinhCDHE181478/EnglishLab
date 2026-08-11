package fu.sep490.g23.backend.repository.classroom;

import fu.sep490.g23.backend.entity.classroom.LarkMeetingParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LarkMeetingParticipantRepository extends JpaRepository<LarkMeetingParticipant, Long> {

    Optional<LarkMeetingParticipant> findByClassroomSessionIdAndParticipantKey(
            Long classroomSessionId,
            String participantKey
    );

    long countByClassroomSessionIdAndActiveTrue(Long classroomSessionId);

    java.util.List<LarkMeetingParticipant> findByClassroomSessionId(Long classroomSessionId);

    void deleteByClassroomSessionId(Long classroomSessionId);
}
