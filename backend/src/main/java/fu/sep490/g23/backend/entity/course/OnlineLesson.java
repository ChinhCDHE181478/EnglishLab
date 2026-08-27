package fu.sep490.g23.backend.entity.course;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "online_lessons",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_online_lesson_module_seq",
                columnNames = {"module_id", "sequence_number"}
        )
)
public class OnlineLesson {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id", nullable = false)
    private OnlineCourseModule module;

    @Column(name = "stable_lesson_key", nullable = false, length = 120)
    private String stableLessonKey;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(length = 500)
    private String description;

    @Column(name = "content_type", length = 40)
    private String contentType;

    @Column(name = "content_text", columnDefinition = "text")
    private String contentText;

    @Column(name = "video_url", length = 700)
    private String videoUrl;

    @Column(name = "bunny_video_id", length = 80)
    private String bunnyVideoId;

    @Column(name = "bunny_library_id", length = 80)
    private String bunnyLibraryId;

    @Column(name = "bunny_cdn_url", length = 700)
    private String bunnyCdnUrl;

    @Column(name = "material_url", length = 700)
    private String materialUrl;

    @Column(name = "transcript_json", columnDefinition = "text")
    private String transcriptJson;

    @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC, id ASC")
    @Builder.Default
    private List<CourseLessonFlashcardRef> flashcardRefs = new ArrayList<>();

    @Column(name = "duration_minutes", nullable = false)
    @Builder.Default
    private Integer durationMinutes = 0;

    @Column(name = "sequence_number", nullable = false)
    @Builder.Default
    private Integer sequenceNumber = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean preview = false;

    public void addFlashcardRef(CourseLessonFlashcardRef ref) {
        flashcardRefs.add(ref);
        ref.setLesson(this);
    }

    public String getLessonKey() {
        return stableLessonKey;
    }

    public void setLessonKey(String lessonKey) {
        this.stableLessonKey = lessonKey;
    }

    public String getTranscriptSegmentsJson() {
        return transcriptJson;
    }

    public void setTranscriptSegmentsJson(String transcriptSegmentsJson) {
        this.transcriptJson = transcriptSegmentsJson;
    }

    public Integer getDisplayOrder() {
        return sequenceNumber;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.sequenceNumber = displayOrder == null ? 0 : displayOrder;
    }
}
