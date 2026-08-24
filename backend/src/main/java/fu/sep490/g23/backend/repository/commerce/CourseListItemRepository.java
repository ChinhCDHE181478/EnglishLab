package fu.sep490.g23.backend.repository.commerce;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.commerce.CourseListItem;
import fu.sep490.g23.backend.entity.commerce.enums.CourseListType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseListItemRepository extends JpaRepository<CourseListItem, Long> {
    List<CourseListItem> findByStudentAndListTypeOrderByAddedAtDesc(User student, CourseListType listType);

    Optional<CourseListItem> findByStudentAndOnlineCourseIdAndListType(
            User student,
            Long onlineCourseId,
            CourseListType listType
    );

    void deleteByStudentAndOnlineCourseIdAndListType(
            User student,
            Long onlineCourseId,
            CourseListType listType
    );
}
