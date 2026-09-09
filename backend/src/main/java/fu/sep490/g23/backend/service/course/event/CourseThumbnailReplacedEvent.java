package fu.sep490.g23.backend.service.course.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Published after a course thumbnail is replaced and the database transaction has committed.
 * The listener deletes the previous thumbnail file from the object store.
 */
@Getter
@RequiredArgsConstructor
public class CourseThumbnailReplacedEvent {
    /** The R2 key of the previous thumbnail (e.g. "course-thumbnails/course-thumbnail-xxx.png"). Null if there was none. */
    private final String previousThumbnailKey;
}
