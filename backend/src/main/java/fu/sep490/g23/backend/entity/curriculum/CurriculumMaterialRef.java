package fu.sap490.g23.backend.entity.curriculum;

import fu.sap490.g23.backend.entity.classroom.CenterMaterialLibraryItem;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "curriculum_material_refs",
        uniqueConstraints = @UniqueConstraint(columnNames = {"unit_id", "material_id"})
)
public class CurriculumMaterialRef {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id", nullable = false)
    private CurriculumUnit unit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id", nullable = false)
    private CenterMaterialLibraryItem material;

    @Column(nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(length = 500)
    private String note;
}
