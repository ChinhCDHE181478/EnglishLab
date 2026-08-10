package fu.sap490.g23.backend.repository.course;

import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.course.CourseDiscussionReaction;
import fu.sap490.g23.backend.entity.course.enums.CourseDiscussionReactionTarget;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseDiscussionReactionRepository extends JpaRepository<CourseDiscussionReaction, Long> {
    List<CourseDiscussionReaction> findByTargetTypeAndTargetId(CourseDiscussionReactionTarget targetType, Long targetId);

    @EntityGraph(attributePaths = "user")
    List<CourseDiscussionReaction> findByTargetTypeAndTargetIdOrderByUpdatedAtDesc(
            CourseDiscussionReactionTarget targetType,
            Long targetId
    );

    Optional<CourseDiscussionReaction> findByTargetTypeAndTargetIdAndUser(
            CourseDiscussionReactionTarget targetType,
            Long targetId,
            User user
    );
}
