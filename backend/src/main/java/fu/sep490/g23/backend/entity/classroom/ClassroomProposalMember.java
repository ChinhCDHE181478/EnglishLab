package fu.sep490.g23.backend.entity.classroom;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassroomProposalMember {
    private Long id;

    private ClassroomProposal proposal;

    private CourseRegistrationRequest courseRegistrationRequest;

    private Long classEnrollmentId;

    private LocalDateTime createdAt;
}
