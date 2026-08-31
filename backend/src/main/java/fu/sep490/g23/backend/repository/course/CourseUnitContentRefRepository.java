package fu.sep490.g23.backend.repository.course;

import fu.sep490.g23.backend.entity.course.CourseUnitContentRef;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import fu.sep490.g23.backend.entity.course.enums.CourseUnitContentType;

public interface CourseUnitContentRefRepository extends JpaRepository<CourseUnitContentRef, Long> {
    List<CourseUnitContentRef> findByCourseUnitIdOrderBySequenceNumberAscIdAsc(Long courseUnitId);

    List<CourseUnitContentRef> findByCourseUnitInstructorLedCourseIdAndContentTypeOrderByCourseUnitSequenceNumberAscSequenceNumberAscIdAsc(
            Long instructorLedCourseId,
            CourseUnitContentType contentType
    );

    boolean existsByCourseUnitIdAndContentTypeAndLearningResourceId(
            Long courseUnitId,
            CourseUnitContentType contentType,
            Long learningResourceId
    );

    boolean existsByContentTypeAndLearningResourceId(
            CourseUnitContentType contentType,
            Long learningResourceId
    );

    boolean existsByCourseUnitIdAndContentTypeAndContentBankItemId(
            Long courseUnitId,
            CourseUnitContentType contentType,
            Long contentBankItemId
    );
}
