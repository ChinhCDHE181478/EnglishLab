package fu.sap490.g23.backend.dto.response.course;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonResponse {
    private Long id;
    private String title;
    private String description;
    private String contentType;
    private String contentText;
    private String videoUrl;
    private String bunnyVideoId;
    private String bunnyLibraryId;
    private String bunnyCdnUrl;
    private String materialUrl;
    private Integer durationMinutes;
    private Integer displayOrder;
    private boolean preview;
}
