package fu.sep490.g23.backend.repository.course;

import fu.sep490.g23.backend.entity.course.CourseDiscussionReply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseDiscussionReplyRepository extends JpaRepository<CourseDiscussionReply, Long> {
}
