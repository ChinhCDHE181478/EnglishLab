package fu.sep490.g23.backend.service.course;

import fu.sep490.g23.backend.entity.course.CourseDiscussionPost;
import fu.sep490.g23.backend.entity.course.CourseDiscussionPostIdMap;
import fu.sep490.g23.backend.entity.course.enums.CourseDiscussionPostType;
import fu.sep490.g23.backend.repository.course.CourseDiscussionPostIdMapRepository;
import fu.sep490.g23.backend.repository.course.CourseDiscussionPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Resolves legacy thread/reply IDs to {@code course_discussion_posts} IDs via
 * {@code course_discussion_post_id_map}. Prefer post IDs for new traffic; fall back to the map
 * when API clients still send old IDs.
 */
@Component
@RequiredArgsConstructor
public class DiscussionPostIdResolver {

    private final CourseDiscussionPostIdMapRepository idMapRepository;
    private final CourseDiscussionPostRepository postRepository;

    public Optional<Long> resolve(CourseDiscussionPostType legacyKind, Long legacyId) {
        if (legacyKind == null || legacyId == null) {
            return Optional.empty();
        }
        return idMapRepository.findByLegacyKindAndLegacyId(legacyKind, legacyId)
                .map(CourseDiscussionPostIdMap::getPostId);
    }

    public Optional<Long> reverseResolve(CourseDiscussionPostType legacyKind, Long postId) {
        if (legacyKind == null || postId == null) {
            return Optional.empty();
        }
        return idMapRepository.findByLegacyKindAndPostId(legacyKind, postId)
                .map(CourseDiscussionPostIdMap::getLegacyId);
    }

    /**
     * Resolve an ID that may be either a post id or a legacy thread/reply id for the given type.
     */
    public Optional<CourseDiscussionPost> resolvePost(CourseDiscussionPostType type, Long id) {
        if (type == null || id == null) {
            return Optional.empty();
        }
        Optional<CourseDiscussionPost> byPostId = postRepository.findByIdAndPostType(id, type);
        if (byPostId.isPresent()) {
            return byPostId;
        }
        return resolve(type, id).flatMap(postId -> postRepository.findByIdAndPostType(postId, type));
    }

    public CourseDiscussionPost requirePost(CourseDiscussionPostType type, Long id) {
        return resolvePost(type, id)
                .orElseThrow(() -> new RuntimeException(
                        type == CourseDiscussionPostType.THREAD
                                ? "Không tìm thấy thảo luận."
                                : "Không tìm thấy câu trả lời."));
    }
}
