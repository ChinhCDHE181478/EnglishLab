package fu.sep490.g23.backend.repository.course;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.course.CourseDiscussionPost;
import fu.sep490.g23.backend.entity.course.CourseDiscussionReaction;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseDiscussionReactionRepository extends JpaRepository<CourseDiscussionReaction, Long> {
    List<CourseDiscussionReaction> findByPost(CourseDiscussionPost post);

    @EntityGraph(attributePaths = "user")
    List<CourseDiscussionReaction> findByPostOrderByUpdatedAtDesc(CourseDiscussionPost post);

    Optional<CourseDiscussionReaction> findByPostAndUser(CourseDiscussionPost post, User user);
}
