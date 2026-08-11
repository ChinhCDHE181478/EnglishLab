package fu.sep490.g23.backend.dto.request.classroom;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnrollStudentRequest {

    @NotNull(message = "Học viên không được để trống")
    private Long studentId;

    private String note;
}
