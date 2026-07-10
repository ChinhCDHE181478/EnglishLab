package fu.sap490.g23.backend.entity.classroom;

import fu.sap490.g23.backend.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "center_material_library_items")
@EntityListeners(AuditingEntityListener.class)
public class CenterMaterialLibraryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 220)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(name = "file_url", nullable = false, length = 700)
    private String fileUrl;

    @Column(name = "file_type", length = 80)
    private String fileType;

    @Column(name = "material_type", length = 80)
    private String materialType;

    @Column(length = 120)
    private String provider;

    @Column(name = "exam_category", length = 40)
    private String examCategory;

    @Column(name = "ielts_band_min", precision = 4, scale = 1)
    private BigDecimal ieltsBandMin;

    @Column(name = "ielts_band_max", precision = 4, scale = 1)
    private BigDecimal ieltsBandMax;

    @Column(name = "toeic_score_min")
    private Integer toeicScoreMin;

    @Column(name = "toeic_score_max")
    private Integer toeicScoreMax;

    @Column(length = 80)
    private String skill;

    @Column(length = 500)
    private String tags;

    @Column(length = 40)
    @Builder.Default
    private String status = "PUBLISHED";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_id")
    private User updatedBy;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
