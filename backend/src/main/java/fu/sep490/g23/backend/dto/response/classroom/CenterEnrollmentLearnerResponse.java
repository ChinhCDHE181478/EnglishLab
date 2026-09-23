package fu.sep490.g23.backend.dto.response.classroom;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CenterEnrollmentLearnerResponse {
    private boolean existingAccount;
    private String fullName;
    private String phoneNumber;
}
