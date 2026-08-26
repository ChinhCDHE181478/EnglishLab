package fu.sep490.g23.backend.entity.classroom;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.classroom.enums.EnrollmentRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentRequestStatusHistory {
    private Long id;

    private CourseRegistrationRequest courseRegistrationRequest;

    private EnrollmentRequestStatus fromStatus;

    private EnrollmentRequestStatus toStatus;

    private User actor;

    private String reason;

    private LocalDateTime createdAt;
}
