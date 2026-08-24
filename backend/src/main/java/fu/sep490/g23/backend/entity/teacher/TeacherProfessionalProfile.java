package fu.sep490.g23.backend.entity.teacher;

import fu.sep490.g23.backend.entity.DomainRecord;
import fu.sep490.g23.backend.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;
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
@Table(name = "user_auxiliary_records")
@SQLRestriction("record_type = 'teacher_professional_profiles'")
public class TeacherProfessionalProfile extends DomainRecord {
    @Override
    protected String domainRecordType() {
        return "teacher_professional_profiles";
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
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
