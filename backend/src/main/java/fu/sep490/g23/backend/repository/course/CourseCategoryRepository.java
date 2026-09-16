package fu.sep490.g23.backend.repository.course;

import fu.sep490.g23.backend.entity.course.CourseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseCategoryRepository extends JpaRepository<CourseCategory, Long> {
    Optional<CourseCategory> findByCode(String code);
    boolean existsByCode(String code);
    List<CourseCategory> findAllByOrderByNameAsc();
}
