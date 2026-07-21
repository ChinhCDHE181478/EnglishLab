package fu.sap490.g23.backend.dto.request.classroom;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CompleteEnrollmentConsultationRequest {
    @Size(max = 700)
    private String note;
}
