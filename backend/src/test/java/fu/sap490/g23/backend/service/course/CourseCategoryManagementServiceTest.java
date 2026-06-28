package fu.sap490.g23.backend.service.course;

import fu.sap490.g23.backend.dto.request.course.CourseCategoryRequest;
import fu.sap490.g23.backend.dto.response.course.CourseCategoryResponse;
import fu.sap490.g23.backend.entity.course.CourseCategory;
import fu.sap490.g23.backend.repository.course.CourseCategoryRepository;
import fu.sap490.g23.backend.repository.course.OnlineCourseRepository;
import fu.sap490.g23.backend.service.course.impl.CourseCategoryManagementServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseCategoryManagementServiceTest {

    @Mock
    private CourseCategoryRepository categoryRepository;

    @Mock
    private OnlineCourseRepository courseRepository;

    private CourseCategoryManagementServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CourseCategoryManagementServiceImpl(categoryRepository, courseRepository);
    }

    @Test
    void createCategoryNormalizesCodeAndReturnsUsageCount() {
        CourseCategoryRequest request = CourseCategoryRequest.builder()
                .code("business english")
                .name("Tiếng Anh thương mại")
                .displayOrder(6)
                .active(true)
                .build();

        when(categoryRepository.existsByCode("BUSINESS_ENGLISH")).thenReturn(false);
        when(categoryRepository.save(any(CourseCategory.class))).thenAnswer(invocation -> {
            CourseCategory category = invocation.getArgument(0);
            category.setId(10L);
            return category;
        });
        when(courseRepository.countByCategoryAndLearningPackageDeletedFalse(any(CourseCategory.class))).thenReturn(0L);

        CourseCategoryResponse response = service.createCategory(request);

        assertThat(response.getCode()).isEqualTo("BUSINESS_ENGLISH");
        assertThat(response.getName()).isEqualTo("Tiếng Anh thương mại");
        assertThat(response.getCourseCount()).isZero();
    }

    @Test
    void createCategoryRejectsInvalidCodeFormat() {
        CourseCategoryRequest request = CourseCategoryRequest.builder()
                .code("123")
                .name("Invalid")
                .build();

        assertThatThrownBy(() -> service.createCategory(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("chữ in hoa");
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void createCategoryRejectsDuplicateCode() {
        CourseCategoryRequest request = CourseCategoryRequest.builder()
                .code("IELTS")
                .name("IELTS")
                .build();
        when(categoryRepository.existsByCode("IELTS")).thenReturn(true);

        assertThatThrownBy(() -> service.createCategory(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("đã tồn tại");
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void deleteCategoryRejectsCategoryUsedByCourses() {
        CourseCategory category = CourseCategory.builder()
                .id(3L)
                .code("IELTS")
                .name("IELTS")
                .build();
        when(categoryRepository.findById(3L)).thenReturn(Optional.of(category));
        when(courseRepository.countByCategoryAndLearningPackageDeletedFalse(category)).thenReturn(2L);

        assertThatThrownBy(() -> service.deleteCategory(3L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("2 khóa học");
        verify(categoryRepository, never()).delete(any());
    }
}
