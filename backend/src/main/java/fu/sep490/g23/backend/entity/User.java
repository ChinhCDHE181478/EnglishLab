package fu.sep490.g23.backend.entity;
import fu.sep490.g23.backend.entity.enums.RoleCodes;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "google_id", unique = true, length = 255)
    private String googleId;

    @Column(name = "facebook_id", unique = true, length = 255)
    private String facebookId;

    @Column(length = 255)
    private String password;

    @Column(name = "password_set", nullable = false, columnDefinition = "boolean default true")
    @Builder.Default
    private boolean passwordSet = true;

    @Column(name = "phone_number", length = 30)
    private String phoneNumber;

    @Column(name = "target_exam", length = 30)
    private String targetExam;

    @Column(name = "target_score", length = 30)
    private String targetScore;

    @Column(name = "current_band")
    private Double currentBand;

    @Column(name = "study_goal", length = 500)
    private String studyGoal;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "profile_completed", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean profileCompleted = false;

    @Column(name = "email_verified", nullable = false, columnDefinition = "boolean default true")
    @Builder.Default
    private boolean emailVerified = true;

    @Column(name = "notification_email_enabled")
    @Builder.Default
    private boolean notificationEmailEnabled = true;

    @Column(name = "notification_in_app_enabled")
    @Builder.Default
    private boolean notificationInAppEnabled = true;

    @Column(name = "notification_class_reminder_enabled")
    @Builder.Default
    private boolean notificationClassReminderEnabled = true;

    @Column(name = "notification_study_alert_enabled")
    @Builder.Default
    private boolean notificationStudyAlertEnabled = true;

    @Column(name = "teacher_headline", length = 180)
    private String teacherHeadline;

    @Column(name = "teacher_biography", columnDefinition = "text")
    private String teacherBiography;

    @Column(name = "teacher_specializations", length = 700)
    private String teacherSpecializations;

    @Column(name = "teacher_teaching_languages", length = 300)
    private String teacherTeachingLanguages;

    @Column(name = "teacher_years_of_experience")
    private Integer teacherYearsOfExperience;

    @Column(name = "teacher_highest_qualification", length = 250)
    private String teacherHighestQualification;

    @Column(name = "teacher_public_profile", nullable = false)
    @Builder.Default
    private boolean teacherPublicProfile = false;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_code", referencedColumnName = "code")
    )
    @Builder.Default
    private Set<Role> roles = new LinkedHashSet<>();

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<Role> assignedRoles = roles == null ? Set.of() : roles;
        return assignedRoles.stream()
                .filter(Role::isActive)
                .map(Role::getCode)
                .map(code -> new SimpleGrantedAuthority("ROLE_" + code))
                .toList();
    }

    public Set<String> getRoleCodes() {
        if (roles == null) {
            return Set.of();
        }
        return roles.stream().map(Role::getCode)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    public String getPrimaryRoleCode() {
        return getRoleCodes().stream()
                .min(Comparator.comparingInt(User::rolePriority))
                .orElse(RoleCodes.LEARNER);
    }

    public boolean hasRole(String expectedRoleCode) {
        if (expectedRoleCode == null) {
            return false;
        }
        if (getRoleCodes().contains(expectedRoleCode)) {
            return true;
        }
        return false;
    }

    public boolean hasAnyRoleCodes(Collection<String> expectedRoleCodes) {
        return expectedRoleCodes.stream().anyMatch(this::hasRole);
    }

    private static int rolePriority(String roleCode) {
        int priority = RoleCodes.DISPLAY_PRIORITY.indexOf(roleCode);
        return priority < 0 ? RoleCodes.DISPLAY_PRIORITY.size() : priority;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return emailVerified;
    }
}
