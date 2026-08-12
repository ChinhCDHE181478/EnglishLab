package fu.sep490.g23.backend.service.course.impl;

import fu.sep490.g23.backend.dto.request.course.CourseCategoryRequest;
import fu.sep490.g23.backend.dto.response.course.CourseCategoryResponse;
import fu.sep490.g23.backend.entity.course.CourseCategory;
import fu.sep490.g23.backend.repository.course.CourseCategoryRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseRepository;
import fu.sep490.g23.backend.service.course.CourseCategoryManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional
public class CourseCategoryManagementServiceImpl implements CourseCategoryManagementService {

    private static final Pattern CATEGORY_CODE_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_]*$");

    private final CourseCategoryRepository courseCategoryRepository;
    private final OnlineCourseRepository onlineCourseRepository;

    @Transactional(readOnly = true)
    public List<CourseCategoryResponse> getCategories() {
        return courseCategoryRepository.findAllByOrderByDisplayOrderAscNameAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CourseCategoryResponse> getActiveCategories() {
        return courseCategoryRepository.findAllByOrderByDisplayOrderAscNameAsc().stream()
                .filter(CourseCategory::isActive)
                .map(this::toResponse)
                .toList();
    }

    public CourseCategoryResponse createCategory(CourseCategoryRequest request) {
        if (request.getCode() == null || request.getCode().isBlank()) {
            throw new IllegalArgumentException("Mã danh mục không được để trống.");
        }
        String code = normalizeCode(request.getCode());
        validateCodeFormat(code);
        if (courseCategoryRepository.existsByCode(code)) {
            throw new IllegalArgumentException("Mã danh mục này đã tồn tại.");
        }

        CourseCategory category = CourseCategory.builder()
                .code(code)
                .name(request.getName().trim())
                .description(clean(request.getDescription()))
                .displayOrder(defaultOrder(request.getDisplayOrder()))
                .active(request.getActive() == null || request.getActive())
                .build();
        return toResponse(courseCategoryRepository.save(category));
    }

    public CourseCategoryResponse updateCategory(Long id, CourseCategoryRequest request) {
        CourseCategory category = findCategory(id);
        category.setName(request.getName().trim());
        category.setDescription(clean(request.getDescription()));
        category.setDisplayOrder(defaultOrder(request.getDisplayOrder()));
        if (request.getActive() != null) {
            category.setActive(request.getActive());
        }
        return toResponse(courseCategoryRepository.save(category));
    }

    public void deleteCategory(Long id) {
        CourseCategory category = findCategory(id);
        long courseCount = onlineCourseRepository.countByCategoryAndLearningPackageDeletedFalse(category);
        if (courseCount > 0) {
            throw new IllegalStateException(
                    "Không thể xóa danh mục đang được " + courseCount + " khóa học sử dụng. Hãy chuyển các khóa học sang danh mục khác trước."
            );
        }
        courseCategoryRepository.delete(category);
    }

    private CourseCategory findCategory(Long id) {
        return courseCategoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục khóa học."));
    }

    private CourseCategoryResponse toResponse(CourseCategory category) {
        return CourseCategoryResponse.builder()
                .id(category.getId())
                .code(category.getCode())
                .name(category.getName())
                .description(category.getDescription())
                .displayOrder(category.getDisplayOrder())
                .active(category.isActive())
                .courseCount(onlineCourseRepository.countByCategoryAndLearningPackageDeletedFalse(category))
                .build();
    }

    private String clean(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private int defaultOrder(Integer value) {
        return value == null ? 0 : value;
    }

    private String normalizeCode(String value) {
        return value.trim()
                .toUpperCase()
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    private void validateCodeFormat(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Mã danh mục phải chứa ít nhất một chữ cái hoặc chữ số.");
        }
        if (!CATEGORY_CODE_PATTERN.matcher(code).matches()) {
            throw new IllegalArgumentException(
                    "Mã danh mục chỉ được dùng chữ in hoa, số và dấu gạch dưới (ví dụ: BUSINESS_ENGLISH)."
            );
        }
    }
}
