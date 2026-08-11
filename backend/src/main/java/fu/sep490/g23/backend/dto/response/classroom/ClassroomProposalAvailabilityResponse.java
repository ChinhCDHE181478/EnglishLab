package fu.sap490.g23.backend.dto.response.classroom;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ClassroomProposalAvailabilityResponse {
    private List<ClassroomPickerOptionResponse> teachers;
    private List<ClassroomPickerOptionResponse> rooms;
}
