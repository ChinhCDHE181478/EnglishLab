package fu.sep490.g23.backend.service.course;

import fu.sep490.g23.backend.dto.request.course.CourseCategoryRequest;
import fu.sep490.g23.backend.dto.response.course.CourseCategoryResponse;
import java.util.List;

public interface CourseCategoryManagementService {

    List<CourseCategoryResponse> getCategories();
    List<CourseCategoryResponse> getActiveCategories();
    CourseCategoryResponse createCategory(CourseCategoryRequest request);
    CourseCategoryResponse updateCategory(Long id, CourseCategoryRequest request);
    void deleteCategory(Long id);
}
