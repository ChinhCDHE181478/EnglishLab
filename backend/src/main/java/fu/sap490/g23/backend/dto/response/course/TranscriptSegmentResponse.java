package fu.sap490.g23.backend.dto.response.course;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TranscriptSegmentResponse {
    private Double startSeconds;
    private Double endSeconds;
    private String text;
}
