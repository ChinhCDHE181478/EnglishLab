package fu.sap490.g23.backend.dto.response.classroom;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ClassroomCampusResponse {
    private Long id;
    private String name;
    private String address;
    private String note;
    private boolean active;
    private long roomCount;
}
