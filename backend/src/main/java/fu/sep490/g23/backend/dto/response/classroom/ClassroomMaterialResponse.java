package fu.sep490.g23.backend.dto.response.classroom;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ClassroomMaterialResponse {
    private Long id;
    private String title;
    private String fileUrl;
    private String fileType;
    private String description;
    private String materialType;
    private String provider;
    private String visibility;
    private String sourceType;
    private Long centerMaterialId;
    private Long sessionId;
    private String sessionTitle;
    @JsonProperty("curriculumUnitId")
    private Long courseUnitId;
    @JsonProperty("curriculumUnitTitle")
    private String courseUnitTitle;
    private Boolean mandatory;
    private String uploadedByName;
    private String reviewStatus;
    private String reviewNote;
    private LocalDateTime submittedForReviewAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
