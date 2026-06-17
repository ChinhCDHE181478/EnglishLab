package fu.sap490.g23.backend.repository.course;

import fu.sap490.g23.backend.entity.course.CourseDiscussionStatus;
import fu.sap490.g23.backend.entity.course.CourseDiscussionThread;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseDiscussionThreadRepository extends JpaRepository<CourseDiscussionThread, Long> {
    @EntityGraph(attributePaths = {"author", "replies", "replies.author"})
    List<CourseDiscussionThread> findByCourseIdAndStatusNotOrderByUpdatedAtDesc(Long courseId, CourseDiscussionStatus status);
}
