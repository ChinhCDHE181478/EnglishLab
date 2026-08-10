package fu.sap490.g23.backend.dto.response.assessment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssessmentAudioUploadResponse {
    private String fileName;
    private String contentType;
    private long size;
    private String url;
}
