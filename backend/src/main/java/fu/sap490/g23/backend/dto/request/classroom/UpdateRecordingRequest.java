package fu.sap490.g23.backend.dto.request.classroom;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateRecordingRequest {
    @Size(max = 700)
    private String recordingUrl;
    private Boolean recordingVisible;
}
