package fu.sep490.g23.backend.dto.response.course;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BunnyVideoUploadResponse {
    private String videoId;
    private String libraryId;
    private String title;
    private String embedUrl;
    private String cdnUrl;
    private LessonResponse lesson;
}
