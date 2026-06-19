package fu.sap490.g23.backend.dto.response.classroom;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClassroomMaterialResponse {
    private Long id;
    private String title;
    private String fileUrl;
    private String fileType;
    private String visibility;
    private Long sessionId;
}
