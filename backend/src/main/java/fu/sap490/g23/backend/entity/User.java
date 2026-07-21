package fu.sap490.g23.backend.entity;

import fu.sap490.g23.backend.entity.enums.RoleEnum;
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
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
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
import java.util.List;
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

    @Column(name = "lark_open_id", length = 255)
    private String larkOpenId;

    @Column(length = 255)
    private String password;

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

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"),
            uniqueConstraints = @UniqueConstraint(name = "uk_user_roles_user_role", columnNames = {"user_id", "role_id"})
    )
    @Builder.Default
    private Set<Role> roles = new LinkedHashSet<>();

    @Transient
    private RoleEnum role;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<RoleEnum> assignedRoles = getRoleCodes();
        if (assignedRoles.isEmpty()) {
            assignedRoles = Set.of(getRole());
        }
        return assignedRoles.stream()
                .map(item -> new SimpleGrantedAuthority("ROLE_" + item.name()))
                .toList();
    }

    public RoleEnum getRole() {
        if (roles != null && !roles.isEmpty()) {
            return roles.stream()
                    .filter(Role::isActive)
                    .map(Role::getCode)
                    .min(Comparator.comparingInt(User::rolePriority))
                    .orElse(role == null ? RoleEnum.LEARNER : role);
        }
        return role == null ? RoleEnum.LEARNER : role;
    }

    public void setRole(RoleEnum role) {
        this.role = role;
    }

    public Set<RoleEnum> getRoleCodes() {
        if (roles == null) {
            return Set.of();
        }
        return roles.stream()
                .filter(Role::isActive)
                .map(Role::getCode)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    public boolean hasRole(RoleEnum expectedRole) {
        return getRoleCodes().contains(expectedRole) || (getRoleCodes().isEmpty() && getRole() == expectedRole);
    }

    public boolean hasAnyRole(Collection<RoleEnum> expectedRoles) {
        return expectedRoles.stream().anyMatch(this::hasRole);
    }

    private static int rolePriority(RoleEnum role) {
        return switch (role) {
            case ADMIN -> 0;
            case MANAGER -> 1;
            case STAFF -> 2;
            case TRAINING_MANAGER -> 3;
            case CONTENT_MANAGER -> 4;
            case TEACHER -> 5;
            case LEARNER -> 6;
        };
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
