package fu.sap490.g23.backend.dto.response.teacher;

import fu.sap490.g23.backend.entity.teacher.enums.CredentialVerificationStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherCredentialResponse {
    private Long id;
    private String type;
    private String title;
    private String issuer;
    private String credentialNumber;
    private LocalDate issuedDate;
    private LocalDate expiryDate;
    private String documentUrl;
    private CredentialVerificationStatus verificationStatus;
    private String verifiedByName;
    private LocalDateTime verifiedAt;
    private String verificationNote;
}
