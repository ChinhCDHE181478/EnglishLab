package fu.sep490.g23.backend.entity.assessment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "placement_test_definitions")
public class PlacementTestDefinition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "test_code", nullable = false, unique = true, length = 80)
    private String testCode;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "exam_type", nullable = false, length = 20)
    @Builder.Default
    private String examType = "IELTS";

    @Column(name = "max_attempts", nullable = false)
    private Integer maxAttempts;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "listening_config_json", nullable = false, columnDefinition = "text")
    private String listeningConfigJson;

    @Column(name = "reading_config_json", nullable = false, columnDefinition = "text")
    private String readingConfigJson;

    @Column(name = "writing_config_json", nullable = false, columnDefinition = "text")
    private String writingConfigJson;

    @Column(name = "speaking_config_json", nullable = false, columnDefinition = "text")
    private String speakingConfigJson;

    @Column(name = "toeic_config_json", columnDefinition = "text")
    private String toeicConfigJson;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
