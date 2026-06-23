package fu.sap490.g23.backend.entity.classroom;

import fu.sap490.g23.backend.entity.classroom.enums.*;

import fu.sap490.g23.backend.entity.User;
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
@Table(name = "classroom_materials")
@EntityListeners(AuditingEntityListener.class)
public class ClassroomMaterial {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_offering_id", nullable = false)
    private ClassroomOffering classroomOffering;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private ClassroomSession session;

    @Column(nullable = false, length = 220)
    private String title;

    @Column(name = "file_url", length = 700)
    private String fileUrl;

    @Column(name = "file_type", length = 80)
    private String fileType;

    @Column(length = 2000)
    private String description;

    @Column(name = "material_type", length = 80)
    private String materialType;

    @Column(length = 120)
    private String provider;

    @Column(name = "visibility", length = 40)
    @Builder.Default
    private String visibility = "LEARNERS_IN_CLASS";

    @Column(name = "source_type", length = 40)
    @Builder.Default
    private String sourceType = "CLASSROOM_UPLOAD";

    @Column(name = "center_material_id")
    private Long centerMaterialId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_id")
    private User uploadedBy;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
