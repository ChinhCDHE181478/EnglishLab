package fu.sap490.g23.backend.entity.curriculum;

import fu.sap490.g23.backend.entity.classroom.ClassroomOffering;
import fu.sap490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
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
@Table(name = "curriculum_programs")
@EntityListeners(AuditingEntityListener.class)
public class CurriculumProgram {
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
    @Column(nullable = false, length = 20)
    private ClassroomDeliveryMode deliveryMode;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String examCategory = "IELTS";

    @Column(precision = 3, scale = 1)
    private BigDecimal targetBand;

    private Integer targetScore;

    @Column(length = 120)
    private String entryLevel;

    @Column(columnDefinition = "text")
    private String outcomes;

    @Column(columnDefinition = "text")
    private String teacherGuide;

    @Column(columnDefinition = "text")
    private String interactionActivities;

    @Column(nullable = false)
    @Builder.Default
    private Integer totalSessions = 0;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "DRAFT";

    @Column(nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @OneToMany(mappedBy = "program", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC, id ASC")
    @Builder.Default
    private List<CurriculumUnit> units = new ArrayList<>();

    @OneToMany(mappedBy = "curriculumProgram")
    @Builder.Default
    private List<ClassroomOffering> classroomOfferings = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void addUnit(CurriculumUnit unit) {
        units.add(unit);
        unit.setProgram(this);
    }
}
