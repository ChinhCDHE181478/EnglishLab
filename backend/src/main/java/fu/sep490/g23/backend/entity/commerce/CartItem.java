package fu.sep490.g23.backend.entity.commerce;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "cart_items",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_cart_student_course",
                columnNames = {"student_id", "online_course_id"}
        )
)
@EntityListeners(AuditingEntityListener.class)
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "online_course_id", nullable = false)
    private OnlineCourse onlineCourse;

    @CreatedDate
    @Column(name = "added_at", updatable = false)
    private LocalDateTime addedAt;
}
