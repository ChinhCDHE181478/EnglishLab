package fu.sep490.g23.backend.dto.response.classroom;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ClassroomAnnouncementResponse {
    private Long id;
    private String title;
    private String content;
    private String createdByName;
    private LocalDateTime createdAt;
}
