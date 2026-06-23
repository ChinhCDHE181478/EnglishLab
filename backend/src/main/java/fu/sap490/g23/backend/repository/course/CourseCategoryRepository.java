package fu.sap490.g23.backend.repository.course;

import fu.sap490.g23.backend.entity.course.CourseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface CourseCategoryRepository extends JpaRepository<CourseCategory, Long> {
    Optional<CourseCategory> findByCode(String code);
    boolean existsByCode(String code);
    List<CourseCategory> findAllByOrderByDisplayOrderAscNameAsc();
}
