package fu.sep490.g23.backend.entity.classroom;

import fu.sep490.g23.backend.entity.classroom.enums.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LarkMeetingParticipant {
    private Long id;

    private ClassSchedule classSchedule;

    private String participantKey;

    private Long userId;

    private boolean active;

    private LocalDateTime joinedAt;

    private LocalDateTime leftAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
