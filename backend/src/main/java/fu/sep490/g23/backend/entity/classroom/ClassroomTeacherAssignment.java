package fu.sep490.g23.backend.entity.classroom;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomTeacherRole;

import fu.sep490.g23.backend.entity.classroom.enums.*;

import fu.sep490.g23.backend.entity.User;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassroomTeacherAssignment {
    private Long id;

    private ClassSection classSection;

    private User teacher;

    /**
     * Chỉ được gắn khi đây là quyền dạy thay cho một buổi cụ thể.
     * Phân công giáo viên chính của cả lớp để trống trường này.
     */
    private ClassSchedule classSchedule;

    @Builder.Default
    private ClassroomTeacherRole role = ClassroomTeacherRole.PRIMARY;

    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    private String reason;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
