package fu.sap490.g23.backend.dto.response.classroom;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ClassroomRoomDetailResponse {
    private Long id;
    private String name;
    private Integer capacity;
    private boolean active;
    private Long campusId;
    private String campusName;
}
