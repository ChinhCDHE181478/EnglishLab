package fu.sap490.g23.backend.dto.response.classroom;

import fu.sap490.g23.backend.entity.classroom.ConflictType;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class ConflictItemResponse {
    private ConflictType type;
    private String message;
    private Map<String, Object> details;
}
