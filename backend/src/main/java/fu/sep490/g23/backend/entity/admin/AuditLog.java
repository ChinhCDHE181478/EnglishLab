package fu.sep490.g23.backend.entity.admin;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;

@Entity @Table(name="system_audit_logs") @EntityListeners(AuditingEntityListener.class)
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AuditLog {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="actor_email",nullable=false,length=150) private String actorEmail;
    @Column(nullable=false,length=100) private String action;
    @Column(name="target_type",length=100) private String targetType;
    @Column(name="target_id",length=100) private String targetId;
    @Column(length=1000) private String detail;
    @CreatedDate @Column(name="created_at",updatable=false,nullable=false) private LocalDateTime createdAt;
}
