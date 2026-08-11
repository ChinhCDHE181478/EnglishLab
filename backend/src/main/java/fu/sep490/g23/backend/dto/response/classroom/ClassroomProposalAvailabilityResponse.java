package fu.sep490.g23.backend.dto.response.classroom;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomPickerOptionResponse;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ClassroomProposalAvailabilityResponse {
    private List<ClassroomPickerOptionResponse> teachers;
    private List<ClassroomPickerOptionResponse> rooms;
}
