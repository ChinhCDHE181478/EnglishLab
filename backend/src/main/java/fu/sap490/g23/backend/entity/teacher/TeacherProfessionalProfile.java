package fu.sap490.g23.backend.entity.teacher;

import fu.sap490.g23.backend.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "teacher_professional_profiles")
public class TeacherProfessionalProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_id", nullable = false, unique = true)
    private User teacher;

    @Column(length = 180)
    private String headline;

    @Column(columnDefinition = "text")
    private String biography;

    @Column(name = "specializations", length = 700)
    private String specializations;

    @Column(name = "teaching_languages", length = 300)
    private String teachingLanguages;

    @Column(name = "years_of_experience")
    private Integer yearsOfExperience;

    @Column(name = "highest_qualification", length = 250)
    private String highestQualification;

    @Column(name = "public_profile", nullable = false)
    @Builder.Default
    private boolean publicProfile = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
