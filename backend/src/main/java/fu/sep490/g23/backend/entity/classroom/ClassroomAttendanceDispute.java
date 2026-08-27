package fu.sep490.g23.backend.entity.classroom;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.classroom.enums.AttendanceDisputeStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassroomAttendanceDispute {
    private Long id;

    private ClassroomAttendance attendance;

    private User student;

    private String reason;

    @Builder.Default
    private AttendanceDisputeStatus status = AttendanceDisputeStatus.PENDING;

    private String reviewNote;

    private User reviewedBy;

    private LocalDateTime reviewedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
