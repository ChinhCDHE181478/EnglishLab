package fu.sap490.g23.backend.dto.response.classroom;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ClassroomSessionTemplateResponse {
    private Long id;
    private String name;
    private String slotsJson;
    private String description;
    private String teacherGuide;
    private String interactionActivities;
    private String postSessionHomework;
    private Integer defaultDurationMinutes;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
