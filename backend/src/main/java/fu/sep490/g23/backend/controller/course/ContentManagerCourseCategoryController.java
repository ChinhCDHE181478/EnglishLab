package fu.sep490.g23.backend.controller.course;

import fu.sep490.g23.backend.dto.request.course.CourseCategoryRequest;
import fu.sep490.g23.backend.dto.response.ApiResponse;
import fu.sep490.g23.backend.dto.response.course.CourseCategoryResponse;
import fu.sep490.g23.backend.service.course.CourseCategoryManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/content-manager/course-categories")
@RequiredArgsConstructor
public class ContentManagerCourseCategoryController {

    private final CourseCategoryManagementService categoryService;

    @GetMapping
    public ResponseEntity<List<CourseCategoryResponse>> getCategories() {
        return ResponseEntity.ok(categoryService.getCategories());
    }

    @PostMapping
    public ResponseEntity<CourseCategoryResponse> createCategory(
            @Valid @RequestBody CourseCategoryRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.createCategory(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CourseCategoryResponse> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CourseCategoryRequest request
    ) {
        return ResponseEntity.ok(categoryService.updateCategory(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.builder()
                .message("Đã xóa danh mục khóa học.")
                .description("Danh mục không còn xuất hiện trong khu vực quản lý nội dung.")
                .build());
    }
}
