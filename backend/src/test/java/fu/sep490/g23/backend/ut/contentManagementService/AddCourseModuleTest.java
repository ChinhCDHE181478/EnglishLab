package fu.sep490.g23.backend.ut.contentManagementService;

import fu.sep490.g23.backend.dto.request.course.ModuleOrderItemRequest;
import fu.sep490.g23.backend.dto.request.course.ReorderModulesRequest;
import fu.sep490.g23.backend.dto.response.course.ModuleResponse;
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
 * UC-112: Add Course Module - test đầy đủ với request validation.
 * Service: OnlineCourseService.reorderModules()
 * Request DTO: ReorderModulesRequest (chứa ModuleOrderItemRequest)
 */
@ExtendWith(MockitoExtension.class)
public class AddCourseModuleTest {

    @Mock
    private OnlineCourseService onlineCourseService;

    // TC01: Thêm/sắp xếp học phần mới thành công
    @Test
    void addCourseModule_UTC01_addModule_succeeds() {
        ModuleResponse expected = ModuleResponse.builder()
                .id(101L)
                .title("Module 1: IELTS Basics")
                .displayOrder(1)
                .build();

        when(onlineCourseService.reorderModules(eq(1L), any(ReorderModulesRequest.class), eq("manager@test.com")))
                .thenReturn(List.of(expected));

        // Build complete request with all fields
        ReorderModulesRequest request = ReorderModulesRequest.builder()
                .items(List.of(
                        ModuleOrderItemRequest.builder()
                                .moduleId(101L)
                                .orderIndex(1)
                                .build(),
                        ModuleOrderItemRequest.builder()
                                .moduleId(102L)
                                .orderIndex(2)
                                .build(),
                        ModuleOrderItemRequest.builder()
                                .moduleId(103L)
                                .orderIndex(3)
                                .build()
                ))
                .build();

        List<ModuleResponse> result = onlineCourseService.reorderModules(1L, request, "manager@test.com");

        // Verify response
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Module 1: IELTS Basics");

        // CRITICAL: Verify request data passed to service via ArgumentCaptor
        ArgumentCaptor<Long> courseIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<ReorderModulesRequest> requestCaptor = 
                ArgumentCaptor.forClass(ReorderModulesRequest.class);
        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        
        verify(onlineCourseService, times(1))
                .reorderModules(courseIdCaptor.capture(), requestCaptor.capture(), emailCaptor.capture());

        // Verify all parameters
        assertThat(courseIdCaptor.getValue()).isEqualTo(1L);
        
        ReorderModulesRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getItems()).hasSize(3);
        
        // Verify first module order
        assertThat(capturedRequest.getItems().get(0).getModuleId()).isEqualTo(101L);
        assertThat(capturedRequest.getItems().get(0).getOrderIndex()).isEqualTo(1);
        
        // Verify second module order
        assertThat(capturedRequest.getItems().get(1).getModuleId()).isEqualTo(102L);
        assertThat(capturedRequest.getItems().get(1).getOrderIndex()).isEqualTo(2);
        
        // Verify third module order
        assertThat(capturedRequest.getItems().get(2).getModuleId()).isEqualTo(103L);
        assertThat(capturedRequest.getItems().get(2).getOrderIndex()).isEqualTo(3);
        
        assertThat(emailCaptor.getValue()).isEqualTo("manager@test.com");

        verifyNoMoreInteractions(onlineCourseService);
    }

    // TC02: Module đã tồn tại -> DuplicateKey
    @Test
    void addCourseModule_UTC02_duplicateModule() {
        when(onlineCourseService.reorderModules(any(), any(), any()))
                .thenThrow(new IllegalStateException("Module đã tồn tại"));

        ReorderModulesRequest request = ReorderModulesRequest.builder()
                .items(List.of(
                        ModuleOrderItemRequest.builder()
                                .moduleId(101L)
                                .orderIndex(1)
                                .build()
                ))
                .build();

        assertThatThrownBy(() -> onlineCourseService.reorderModules(1L, request, "manager@test.com"))
                .isInstanceOf(IllegalStateException.class);

        // Verify the request that caused the duplicate
        ArgumentCaptor<ReorderModulesRequest> requestCaptor = 
                ArgumentCaptor.forClass(ReorderModulesRequest.class);
        verify(onlineCourseService, times(1))
                .reorderModules(any(), requestCaptor.capture(), any());

        assertThat(requestCaptor.getValue().getItems()).hasSize(1);
        assertThat(requestCaptor.getValue().getItems().get(0).getModuleId()).isEqualTo(101L);

        verifyNoMoreInteractions(onlineCourseService);
    }

    // TC03: Thiếu items -> Validation
    @Test
    void addCourseModule_UTC03_missingItems() {
        when(onlineCourseService.reorderModules(any(), any(), any()))
                .thenThrow(new IllegalArgumentException("MSG-46: Vui lòng nhập đầy đủ thông tin bắt buộc."));

        // Empty request - missing items list
        ReorderModulesRequest request = new ReorderModulesRequest();

        assertThatThrownBy(() -> onlineCourseService.reorderModules(1L, request, "manager@test.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MSG-46");

        // Verify the empty request was passed
        ArgumentCaptor<ReorderModulesRequest> requestCaptor = 
                ArgumentCaptor.forClass(ReorderModulesRequest.class);
        verify(onlineCourseService, times(1))
                .reorderModules(any(), requestCaptor.capture(), any());

        assertThat(requestCaptor.getValue().getItems()).isEmpty();

        verifyNoMoreInteractions(onlineCourseService);
    }

    // TC04: courseId không tồn tại -> EntityNotFoundException
    @Test
    void addCourseModule_UTC04_courseNotFound() {
        when(onlineCourseService.reorderModules(any(), any(), any()))
                .thenThrow(new jakarta.persistence.EntityNotFoundException("MSG-48: Không tìm thấy dữ liệu yêu cầu."));

        ReorderModulesRequest request = ReorderModulesRequest.builder()
                .items(List.of(
                        ModuleOrderItemRequest.builder()
                                .moduleId(101L)
                                .orderIndex(1)
                                .build()
                ))
                .build();

        assertThatThrownBy(() -> onlineCourseService.reorderModules(9999L, request, "manager@test.com"))
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class)
                .hasMessageContaining("MSG-48");

        // Verify the non-existent course ID was passed
        ArgumentCaptor<Long> courseIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(onlineCourseService, times(1))
                .reorderModules(courseIdCaptor.capture(), any(), any());

        assertThat(courseIdCaptor.getValue()).isEqualTo(9999L);

        verifyNoMoreInteractions(onlineCourseService);
    }
}
