package fu.sap490.g23.backend.repository.course;

import fu.sap490.g23.backend.entity.course.CourseCategory;
import fu.sap490.g23.backend.entity.course.CourseCategoryCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CourseCategoryRepository extends JpaRepository<CourseCategory, Long> {
    Optional<CourseCategory> findByCode(CourseCategoryCode code);
    boolean existsByCode(CourseCategoryCode code);
}
