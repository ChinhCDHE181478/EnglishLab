package fu.sap490.g23.backend.entity.classroom;

import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.classroom.enums.*;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "classroom_syllabus_items")
@EntityListeners(AuditingEntityListener.class)
public class ClassroomSyllabusItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_offering_id", nullable = false)
    private ClassroomOffering classroomOffering;

    @Column(nullable = false, length = 220)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "session_plan", columnDefinition = "text")
    private String sessionPlan;

    @Column(name = "homework_notes", columnDefinition = "text")
    private String homeworkNotes;

    @Column(name = "quiz_notes", columnDefinition = "text")
    private String quizNotes;

    @Column(name = "teacher_notes", columnDefinition = "text")
    private String teacherNotes;

    @Column(name = "session_number")
    private Integer sessionNumber;

    @Column(name = "linked_session_id")
    private Long linkedSessionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", length = 20)
    @Builder.Default
    private ContentReviewStatus reviewStatus = ContentReviewStatus.APPROVED;

    @Column(name = "review_note", length = 1000)
    private String reviewNote;

    @Column(name = "submitted_for_review_at")
    private LocalDateTime submittedForReviewAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_id")
    private User reviewedBy;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "DRAFT";

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
