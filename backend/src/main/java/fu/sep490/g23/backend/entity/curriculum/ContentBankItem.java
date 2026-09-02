package fu.sep490.g23.backend.entity.curriculum;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.curriculum.enums.ContentBankType;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Unified content bank row ({@code content_bank_items}).
 *
 * <p>{@code status} is the single lifecycle and availability source of truth.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "content_bank_items")
@EntityListeners(AuditingEntityListener.class)
public class ContentBankItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "bank_type", nullable = false, length = 30)
    private ContentBankType bankType;

    @Column(length = 120)
    private String code;

    @Column(nullable = false, length = 220)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "exam_category", length = 40)
    private String examCategory;

    @Column(length = 60)
    private String skill;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "DRAFT";

    @Column(length = 500)
    private String tags;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content_data", nullable = false, columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> contentData = new HashMap<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
