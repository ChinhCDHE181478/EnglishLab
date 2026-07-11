package fu.sap490.g23.backend.entity.classroom;

import fu.sap490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import fu.sap490.g23.backend.entity.course.enums.PackageStatus;
import fu.sap490.g23.backend.entity.curriculum.CurriculumProgram;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "training_programs")
@EntityListeners(AuditingEntityListener.class)
public class TrainingProgram {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(nullable = false, unique = true, length = 120)
    private String code;

    @Column(nullable = false, unique = true, length = 160)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_mode", nullable = false, length = 20)
    private ClassroomDeliveryMode deliveryMode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "curriculum_program_id", nullable = false)
    private CurriculumProgram curriculumProgram;

    @Column(length = 500)
    private String shortDescription;

    @Column(columnDefinition = "text")
    private String description;

    @Column(length = 120)
    private String entryLevel;

    @Column(length = 80)
    private String targetScore;

    @Column(length = 700)
    private String targetOutcome;

    @Column(nullable = false)
    @Builder.Default
    private Integer defaultCapacity = 30;

    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal price = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal salePrice;

    @Column(length = 80)
    private String duration;

    @Column(length = 120)
    private String studyMode;

    @Column(length = 700)
    private String thumbnailUrl;

    @Column(columnDefinition = "text")
    private String syllabusSummary;

    @Column(columnDefinition = "text")
    private String programOutcomes;

    @Column(columnDefinition = "text")
    private String teacherGuide;

    @Column(columnDefinition = "text")
    private String interactionActivities;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private PackageStatus status = PackageStatus.DRAFT;

    @Column(nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean featured = false;

    @OneToMany(mappedBy = "trainingProgram", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC, id ASC")
    @Builder.Default
    private List<TrainingProgramMaterial> materials = new ArrayList<>();

    @OneToMany(mappedBy = "trainingProgram")
    @Builder.Default
    private List<ClassroomOffering> classroomOfferings = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void addMaterial(TrainingProgramMaterial material) {
        materials.add(material);
        material.setTrainingProgram(this);
    }
}
