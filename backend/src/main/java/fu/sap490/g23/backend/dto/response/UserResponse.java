package fu.sap490.g23.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UserResponse {

    private Long id;
    private String fullName;
    private String email;
    private String role;
    private List<String> roles;
    private String phoneNumber;
    private String targetExam;
    private String targetScore;
    private Double currentBand;
    private String studyGoal;
    private boolean profileCompleted;
}
