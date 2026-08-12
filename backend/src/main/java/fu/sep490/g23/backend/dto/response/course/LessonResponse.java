package fu.sep490.g23.backend.dto.response.course;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import fu.sep490.g23.backend.dto.response.curriculum.FlashcardSetResponse;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonResponse {
    private Long id;
    private String lessonKey;
    private String title;
    private String description;
    private String contentType;
    private String contentText;
    private String videoUrl;
    private String bunnyVideoId;
    private String bunnyLibraryId;
    private String bunnyCdnUrl;
    private String materialUrl;
    private List<TranscriptSegmentResponse> transcriptSegments;
    private List<FlashcardSetResponse> flashcardSets;
    private Integer durationMinutes;
    private Integer displayOrder;
    private boolean preview;
}
