package fu.sep490.g23.backend.dto.request.course;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
    @NotNull
    private Double startSeconds;

    @Min(0)
    @NotNull
    private Double endSeconds;

    @NotBlank
    @Size(max = 1200)
    private String text;

    @AssertTrue(message = "Mốc kết thúc của bản chép lời phải lớn hơn mốc bắt đầu")
    public boolean isTimelineValid() {
        return startSeconds == null || endSeconds == null || endSeconds > startSeconds;
    }
}
