package fu.sap490.g23.backend.dto.response.curriculum;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CurriculumReferenceResponse {
    private Long id;
    private String type;
    private Long resourceId;
    private String title;
    private String subtitle;
    private String skill;
    private String status;
    private Integer displayOrder;
    private String note;
}
