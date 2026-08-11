package fu.sep490.g23.backend.dto.response.course;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseCertificateResponse {
    private boolean eligible;
    private boolean verified;
    private Long courseId;
    private Long enrollmentId;
    private String learnerName;
    private String courseTitle;
    private String targetOutcome;
    private LocalDateTime completionDate;
    private String verificationCode;
    private String verificationUrl;
    private String platformName;
    private String message;
}
