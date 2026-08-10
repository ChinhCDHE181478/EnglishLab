package fu.sap490.g23.backend.dto.request.course;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonRequest {

    private Long id;

    @NotBlank(message = "Lesson title is required")
    @Size(max = 180)
    private String title;

    @Size(max = 500)
    private String description;

    @Size(max = 40)
    private String contentType;

    private String contentText;

    @Size(max = 700)
    private String videoUrl;

    @Size(max = 700)
    private String materialUrl;

    @Valid
    private List<TranscriptSegmentRequest> transcriptSegments;

    private List<Long> flashcardSetIds;

    @Min(0)
    private Integer durationMinutes;

    @Min(0)
    private Integer displayOrder;

    private Boolean preview;
}
