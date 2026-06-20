package fu.sap490.g23.backend.repository.course;

import fu.sap490.g23.backend.entity.course.CourseDiscussionReply;
import fu.sap490.g23.backend.entity.course.CourseDiscussionReplyVote;
import fu.sap490.g23.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CourseDiscussionReplyVoteRepository extends JpaRepository<CourseDiscussionReplyVote, Long> {
    Optional<CourseDiscussionReplyVote> findByReplyAndUser(CourseDiscussionReply reply, User user);
}
