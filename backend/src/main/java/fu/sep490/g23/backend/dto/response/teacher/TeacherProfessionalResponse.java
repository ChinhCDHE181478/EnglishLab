package fu.sep490.g23.backend.dto.response.teacher;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherProfessionalResponse {
    private Long teacherId;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String avatarUrl;
    private String headline;
    private String biography;
    private String specializations;
    private String teachingLanguages;
    private Integer yearsOfExperience;
    private String highestQualification;
    private boolean publicProfile;
    private long assignedClassrooms;
    private long totalSessions;
    private long completedSessions;
    private BigDecimal averagePerformanceScore;
    private long verifiedCredentials;
    private List<TeacherCredentialResponse> credentials;
    private List<TeacherEvaluationResponse> evaluations;
}
