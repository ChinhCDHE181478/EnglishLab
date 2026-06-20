package fu.sap490.g23.backend.entity.course;

import fu.sap490.g23.backend.entity.course.enums.*;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "lessons")
public class Lesson {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id", nullable = false)
    private CourseModule module;

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

    @Column(name = "transcript_segments_json", columnDefinition = "text")
    private String transcriptSegmentsJson;

    @Column(name = "duration_minutes", nullable = false)
    @Builder.Default
    private Integer durationMinutes = 0;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean preview = false;
}
