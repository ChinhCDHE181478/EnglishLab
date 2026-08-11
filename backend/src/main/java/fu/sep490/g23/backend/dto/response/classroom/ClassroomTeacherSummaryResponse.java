package fu.sep490.g23.backend.dto.response.classroom;

import fu.sep490.g23.backend.entity.classroom.enums.ClassroomTeacherRole;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClassroomTeacherSummaryResponse {
    private Long teacherId;
    private String teacherName;
    private ClassroomTeacherRole role;
}
