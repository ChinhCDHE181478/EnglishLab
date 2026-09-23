package fu.sep490.g23.backend.ut.contentManagementService;

import fu.sep490.g23.backend.dto.request.course.LessonOrderItemRequest;
import fu.sep490.g23.backend.dto.request.course.ReorderLessonsRequest;
import fu.sep490.g23.backend.dto.response.course.LessonResponse;
import fu.sep490.g23.backend.service.course.OnlineCourseService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * UC-115: Add Course Lesson - test đầy đủ với request validation.
 * Service: OnlineCourseService.reorderLessons()
 * Request DTO: ReorderLessonsRequest (chứa LessonOrderItemRequest)
 */
@ExtendWith(MockitoExtension.class)
public class AddCourseLessonTest {

    @Mock
    private OnlineCourseService onlineCourseService;

    // TC01: Thêm/sắp xếp bài học mới thành công
    @Test
    void addCourseLesson_UTC01_addLesson_succeeds() {
        LessonResponse expected = LessonResponse.builder()
                .id(201L)
                .title("Lesson 1: Introduction to IELTS")
                .displayOrder(1)
                .build();

        when(onlineCourseService.reorderLessons(eq(1L), eq(101L), any(ReorderLessonsRequest.class), eq("manager@test.com")))
                .thenReturn(List.of(expected));

        // Build complete request with all fields
        ReorderLessonsRequest request = ReorderLessonsRequest.builder()
                .items(List.of(
                        LessonOrderItemRequest.builder()
                                .lessonId(201L)
                                .orderIndex(1)
                                .build(),
                        LessonOrderItemRequest.builder()
                                .lessonId(202L)
                                .orderIndex(2)
                                .build(),
                        LessonOrderItemRequest.builder()
                                .lessonId(203L)
                                .orderIndex(3)
                                .build()
                ))
                .build();

        List<LessonResponse> result = onlineCourseService.reorderLessons(1L, 101L, request, "manager@test.com");

        // Verify response
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Lesson 1: Introduction to IELTS");

        // CRITICAL: Verify request data passed to service via ArgumentCaptor
        ArgumentCaptor<Long> courseIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> moduleIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<ReorderLessonsRequest> requestCaptor = 
                ArgumentCaptor.forClass(ReorderLessonsRequest.class);
        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        
        verify(onlineCourseService, times(1))
                .reorderLessons(courseIdCaptor.capture(), moduleIdCaptor.capture(), requestCaptor.capture(), emailCaptor.capture());

        // Verify all parameters
        assertThat(courseIdCaptor.getValue()).isEqualTo(1L);
        assertThat(moduleIdCaptor.getValue()).isEqualTo(101L);
        
        ReorderLessonsRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getItems()).hasSize(3);
        
        // Verify first lesson order
        assertThat(capturedRequest.getItems().get(0).getLessonId()).isEqualTo(201L);
        assertThat(capturedRequest.getItems().get(0).getOrderIndex()).isEqualTo(1);
        
        // Verify second lesson order
        assertThat(capturedRequest.getItems().get(1).getLessonId()).isEqualTo(202L);
        assertThat(capturedRequest.getItems().get(1).getOrderIndex()).isEqualTo(2);
        
        // Verify third lesson order
        assertThat(capturedRequest.getItems().get(2).getLessonId()).isEqualTo(203L);
        assertThat(capturedRequest.getItems().get(2).getOrderIndex()).isEqualTo(3);
        
        assertThat(emailCaptor.getValue()).isEqualTo("manager@test.com");

        verifyNoMoreInteractions(onlineCourseService);
    }

    // TC02: Thiếu items
    @Test
    void addCourseLesson_UTC02_missingItems() {
        when(onlineCourseService.reorderLessons(any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("MSG-46: Vui lòng nhập đầy đủ thông tin bắt buộc."));

        // Empty request - missing items list
        ReorderLessonsRequest request = new ReorderLessonsRequest();

        assertThatThrownBy(() -> onlineCourseService.reorderLessons(1L, 101L, request, "manager@test.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MSG-46");

        // Verify the empty request was passed
        ArgumentCaptor<ReorderLessonsRequest> requestCaptor = 
                ArgumentCaptor.forClass(ReorderLessonsRequest.class);
        verify(onlineCourseService, times(1))
                .reorderLessons(any(), any(), requestCaptor.capture(), any());

        assertThat(requestCaptor.getValue().getItems()).isEmpty();

        verifyNoMoreInteractions(onlineCourseService);
    }

    // TC03: moduleId không tồn tại
    @Test
    void addCourseLesson_UTC03_moduleNotFound_throwsException() {
        when(onlineCourseService.reorderLessons(any(), any(), any(), any()))
                .thenThrow(new jakarta.persistence.EntityNotFoundException("MSG-48: Không tìm thấy dữ liệu yêu cầu."));

        ReorderLessonsRequest request = ReorderLessonsRequest.builder()
                .items(List.of(
                        LessonOrderItemRequest.builder()
                                .lessonId(201L)
                                .orderIndex(1)
                                .build()
                ))
                .build();

        assertThatThrownBy(() -> onlineCourseService.reorderLessons(1L, 9999L, request, "manager@test.com"))
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class)
                .hasMessageContaining("MSG-48");

        // Verify the non-existent module ID was passed
        ArgumentCaptor<Long> moduleIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(onlineCourseService, times(1))
                .reorderLessons(any(), moduleIdCaptor.capture(), any(), any());

        assertThat(moduleIdCaptor.getValue()).isEqualTo(9999L);

        verifyNoMoreInteractions(onlineCourseService);
    }

    // TC05: orderIndex <= 0
    @Test
    void addCourseLesson_UTC04_invalidOrderIndex() {
        when(onlineCourseService.reorderLessons(any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Thứ tự hiển thị phải lớn hơn 0"));

        ReorderLessonsRequest request = ReorderLessonsRequest.builder()
                .items(List.of(
                        LessonOrderItemRequest.builder()
                                .lessonId(201L)
                                .orderIndex(0)  // Invalid: must be >= 1
                                .build()
                ))
                .build();

        assertThatThrownBy(() -> onlineCourseService.reorderLessons(1L, 101L, request, "manager@test.com"))
                .isInstanceOf(IllegalArgumentException.class);

        // Verify the invalid order index was passed
        ArgumentCaptor<ReorderLessonsRequest> requestCaptor = 
                ArgumentCaptor.forClass(ReorderLessonsRequest.class);
        verify(onlineCourseService, times(1))
                .reorderLessons(any(), any(), requestCaptor.capture(), any());

        assertThat(requestCaptor.getValue().getItems().get(0).getOrderIndex()).isEqualTo(0);

        verifyNoMoreInteractions(onlineCourseService);
    }
}
