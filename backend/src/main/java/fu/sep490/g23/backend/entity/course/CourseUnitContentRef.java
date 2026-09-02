package fu.sep490.g23.backend.entity.course;

import fu.sep490.g23.backend.entity.classroom.CenterMaterialLibraryItem;
import fu.sep490.g23.backend.entity.course.enums.CourseUnitContentType;
import fu.sep490.g23.backend.entity.curriculum.ContentBankItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Canonical unit content ref ({@code course_unit_content_refs}).
 * IDs preserved from {@code curriculum_resource_refs}.
 * MATERIAL → {@code learning_resource_id}; bank types → {@code content_bank_item_id}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "course_unit_content_refs")
@EntityListeners(AuditingEntityListener.class)
public class CourseUnitContentRef {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_unit_id", nullable = false)
    private CourseUnit courseUnit;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 20)
    private CourseUnitContentType contentType;

    @Column(name = "sequence_number", nullable = false)
    @Builder.Default
    private Integer sequenceNumber = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learning_resource_id")
    private CenterMaterialLibraryItem learningResource;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_bank_item_id")
    private ContentBankItem contentBankItem;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
