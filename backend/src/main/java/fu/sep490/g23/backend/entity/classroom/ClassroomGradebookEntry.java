package fu.sep490.g23.backend.entity.classroom;
import fu.sep490.g23.backend.entity.classroom.enums.GradebookEntryStatus;

import fu.sep490.g23.backend.entity.classroom.enums.*;

import fu.sep490.g23.backend.entity.User;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassroomGradebookEntry {
    private Long id;

    private ClassSection classSection;

    private User student;

    private BigDecimal homeworkScore;

    private BigDecimal attendancePercent;

    private BigDecimal finalResult;

    private String teacherComment;

    @Builder.Default
    private GradebookEntryStatus status = GradebookEntryStatus.PENDING;

    private User updatedBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
