package fu.sep490.g23.backend.entity.course;

import fu.sep490.g23.backend.entity.course.enums.CourseDiscussionPostType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "course_discussion_post_id_map")
@IdClass(CourseDiscussionPostIdMap.Pk.class)
public class CourseDiscussionPostIdMap {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "legacy_kind", nullable = false, length = 20)
    private CourseDiscussionPostType legacyKind;

    @Id
    @Column(name = "legacy_id", nullable = false)
    private Long legacyId;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Pk implements Serializable {
        private CourseDiscussionPostType legacyKind;
        private Long legacyId;
    }
}
