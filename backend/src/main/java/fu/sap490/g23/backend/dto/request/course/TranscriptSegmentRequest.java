package fu.sap490.g23.backend.dto.request.course;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TranscriptSegmentRequest {

    @Min(0)
    private Double startSeconds;

    @Min(0)
    private Double endSeconds;

    @Size(max = 1200)
    private String text;
}
