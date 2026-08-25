package fu.sep490.g23.backend.repository.course;

import fu.sep490.g23.backend.entity.course.CourseDiscussionPostIdMap;
import fu.sep490.g23.backend.entity.course.enums.CourseDiscussionPostType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CourseDiscussionPostIdMapRepository
        extends JpaRepository<CourseDiscussionPostIdMap, CourseDiscussionPostIdMap.Pk> {

    Optional<CourseDiscussionPostIdMap> findByLegacyKindAndLegacyId(
            CourseDiscussionPostType legacyKind,
            Long legacyId
    );

    Optional<CourseDiscussionPostIdMap> findByPostId(Long postId);

    Optional<CourseDiscussionPostIdMap> findByLegacyKindAndPostId(
            CourseDiscussionPostType legacyKind,
            Long postId
    );
}
