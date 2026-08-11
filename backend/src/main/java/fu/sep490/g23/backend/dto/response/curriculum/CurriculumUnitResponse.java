package fu.sep490.g23.backend.dto.response.curriculum;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CurriculumUnitResponse {
    private Long id;
    private Long programId;
    private Integer displayOrder;
    private String title;
    private String description;
    private String sessionPlan;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<CurriculumReferenceResponse> materials;
    private List<CurriculumReferenceResponse> exercises;
    private List<CurriculumReferenceResponse> assessments;
    private List<CurriculumReferenceResponse> flashcards;
}
