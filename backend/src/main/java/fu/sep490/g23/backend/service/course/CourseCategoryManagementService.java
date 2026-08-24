package fu.sep490.g23.backend.service.course;

import fu.sep490.g23.backend.dto.request.course.CourseCategoryRequest;
import fu.sep490.g23.backend.dto.response.course.CourseCategoryResponse;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CourseCategoryManagementService {

    List<CourseCategoryResponse> getCategories();
    Page<CourseCategoryResponse> getCategories(Pageable pageable);
    List<CourseCategoryResponse> getActiveCategories();
    CourseCategoryResponse createCategory(CourseCategoryRequest request);
    CourseCategoryResponse updateCategory(Long id, CourseCategoryRequest request);
    void deleteCategory(Long id);
}
