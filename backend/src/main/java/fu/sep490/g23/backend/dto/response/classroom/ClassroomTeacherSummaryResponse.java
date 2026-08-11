package fu.sap490.g23.backend.dto.response.classroom;

import fu.sap490.g23.backend.entity.classroom.enums.ClassroomTeacherRole;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class ClassroomTeacherSummaryResponse {
    private Long teacherId;
    private String teacherName;
    private ClassroomTeacherRole role;
    private Long sessionId;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private String reason;
}
