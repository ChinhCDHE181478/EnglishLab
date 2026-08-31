package fu.sep490.g23.backend.entity.classroom;
import fu.sep490.g23.backend.entity.classroom.enums.ContentReviewStatus;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.classroom.enums.*;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassroomSyllabusItem {
    private Long id;

    private ClassSection classSection;

    private String title;

    private String description;

    @Builder.Default
    private Integer displayOrder = 0;

    private String sessionPlan;

    private String homeworkNotes;

    private String quizNotes;

    private String teacherNotes;

    private Integer sessionNumber;

    private Long linkedSessionId;

    @Builder.Default
    private ContentReviewStatus reviewStatus = ContentReviewStatus.APPROVED;

    private String reviewNote;

    private LocalDateTime submittedForReviewAt;

    private LocalDateTime reviewedAt;

    private User reviewedBy;

    @Builder.Default
    private String status = "DRAFT";

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
