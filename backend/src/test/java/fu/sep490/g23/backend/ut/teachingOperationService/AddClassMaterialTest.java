package fu.sep490.g23.backend.ut.teachingOperationService;

import fu.sep490.g23.backend.dto.request.classroom.CreateMaterialRequest;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomMaterialResponse;
import fu.sep490.g23.backend.service.classroom.ClassroomContentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * UC-76: Add Class Material - test đầy đủ với request validation.
 * Service: ClassroomContentService.createMaterial()
 * Request DTO: CreateMaterialRequest
 */
@ExtendWith(MockitoExtension.class)
public class AddClassMaterialTest {

    @Mock
    private ClassroomContentService contentService;

    // TC01: Thêm tài liệu PDF thành công
    @Test
    void tc01_addValidMaterial_succeeds() {
        ClassroomMaterialResponse expected = ClassroomMaterialResponse.builder()
                .id(101L)
                .title("Slide bai giang Buoi 1")
                .fileType("PDF")
                .fileUrl("https://storage.englishlab.com/materials/slide1.pdf")
                .build();
        when(contentService.createMaterial(eq(1L), any(CreateMaterialRequest.class), eq("teacher@test.com")))
                .thenReturn(expected);

        // Build complete request with all fields
        CreateMaterialRequest request = CreateMaterialRequest.builder()
                .title("Slide bai giang Buoi 1")
                .fileType("PDF")
                .fileUrl("https://storage.englishlab.com/materials/slide1.pdf")
                .description("Slide bài giảng buổi 1 - Giới thiệu về IELTS")
                .materialType("LECTURE_SLIDES")
                .provider("TEACHER")
                .visibility("CLASS_STUDENTS")
                .sourceType("UPLOAD")
                .sessionId(10L)
                .build();

        ClassroomMaterialResponse result = contentService.createMaterial(1L, request, "teacher@test.com");

        // Verify response
        assertThat(result.getId()).isEqualTo(101L);
        assertThat(result.getTitle()).isEqualTo("Slide bai giang Buoi 1");
        assertThat(result.getFileType()).isEqualTo("PDF");

        // CRITICAL: Verify request data passed to service via ArgumentCaptor
        ArgumentCaptor<Long> classIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<CreateMaterialRequest> requestCaptor = 
                ArgumentCaptor.forClass(CreateMaterialRequest.class);
        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        
        verify(contentService, times(1))
                .createMaterial(classIdCaptor.capture(), requestCaptor.capture(), emailCaptor.capture());

        // Verify all fields
        CreateMaterialRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getTitle()).isEqualTo("Slide bai giang Buoi 1");
        assertThat(capturedRequest.getFileType()).isEqualTo("PDF");
        assertThat(capturedRequest.getFileUrl()).isEqualTo("https://storage.englishlab.com/materials/slide1.pdf");
        assertThat(capturedRequest.getDescription()).isEqualTo("Slide bài giảng buổi 1 - Giới thiệu về IELTS");
        assertThat(capturedRequest.getMaterialType()).isEqualTo("LECTURE_SLIDES");
        assertThat(capturedRequest.getProvider()).isEqualTo("TEACHER");
        assertThat(capturedRequest.getVisibility()).isEqualTo("CLASS_STUDENTS");
        assertThat(capturedRequest.getSourceType()).isEqualTo("UPLOAD");
        assertThat(capturedRequest.getSessionId()).isEqualTo(10L);
        
        // Verify primitive parameters
        assertThat(classIdCaptor.getValue()).isEqualTo(1L);
        assertThat(emailCaptor.getValue()).isEqualTo("teacher@test.com");

        verifyNoMoreInteractions(contentService);
    }

    // TC02: File vuot 20MB -> IllegalArgumentException
    @Test
    void tc02_fileTooLarge_throwsException() {
        when(contentService.createMaterial(any(), any(), any()))
                .thenThrow(new IllegalArgumentException("MSG-15: File dinh kem khong duoc vuot qua 20 MB."));

        // Build request with large file
        CreateMaterialRequest request = CreateMaterialRequest.builder()
                .title("Slide bài giảng")
                .fileUrl("https://storage.englishlab.com/materials/book.pdf")
                .fileType("PDF")
                .description("Slide bài giảng")
                .build();

        assertThatThrownBy(() -> contentService.createMaterial(1L, request, "teacher@test.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MSG-15");

        // Verify request data that caused the error
        ArgumentCaptor<Long> classIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<CreateMaterialRequest> requestCaptor = 
                ArgumentCaptor.forClass(CreateMaterialRequest.class);
        
        verify(contentService, times(1))
                .createMaterial(classIdCaptor.capture(), requestCaptor.capture(), any());
        
        CreateMaterialRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getTitle()).isEqualTo("Slide bài giảng");
        assertThat(capturedRequest.getFileUrl()).contains("book.pdf");
        assertThat(capturedRequest.getFileType()).isEqualTo("PDF");
        assertThat(classIdCaptor.getValue()).isEqualTo(1L);
    }

    // TC03: Giao vien khong phan cong -> AccessDenied
    @Test
    void tc03_teacherNotAssigned_throwsAccessDenied() {
        when(contentService.createMaterial(any(), any(), any()))
                .thenThrow(new org.springframework.security.access.AccessDeniedException(
                        "MSG-19: Ban khong duoc phan cong phu trach lop hoc nay."));
        
        CreateMaterialRequest request = CreateMaterialRequest.builder()
                .title("Slide bài giảng")
                .fileUrl("https://storage.englishlab.com/slide.pdf")
                .fileType("PDF")
                .build();

        assertThatThrownBy(() -> contentService.createMaterial(1L, request, "unknown@test.com"))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
                .hasMessageContaining("MSG-19");

        // Verify the unauthorized email was passed
        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        verify(contentService, times(1))
                .createMaterial(eq(1L), any(), emailCaptor.capture());
        
        assertThat(emailCaptor.getValue()).isEqualTo("unknown@test.com");
    }

    // TC04: Thieu title/fileUrl -> Validation
    @Test
    void tc04_missingRequiredFields_throwsException() {
        when(contentService.createMaterial(any(), any(), any()))
                .thenThrow(new IllegalArgumentException("MSG-46: Vui long nhap day du thong tin bat buoc."));

        // Build request with missing title
        CreateMaterialRequest request = CreateMaterialRequest.builder()
                .title("")  // Empty - required
                .fileUrl("https://storage.englishlab.com/slide.pdf")
                .fileType("PDF")
                .build();

        assertThatThrownBy(() -> contentService.createMaterial(1L, request, "teacher@test.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MSG-46");

        // Verify the invalid request was passed
        ArgumentCaptor<CreateMaterialRequest> requestCaptor = 
                ArgumentCaptor.forClass(CreateMaterialRequest.class);
        verify(contentService, times(1))
                .createMaterial(eq(1L), requestCaptor.capture(), eq("teacher@test.com"));
        
        CreateMaterialRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getTitle()).isEmpty();
        assertThat(capturedRequest.getFileUrl()).isEqualTo("https://storage.englishlab.com/slide.pdf");
    }

    // TC05: Lop CLOSED -> BusinessRule
    @Test
    void tc05_classClosed_throwsException() {
        when(contentService.createMaterial(any(), any(), any()))
                .thenThrow(new IllegalStateException("MSG-49: Lop hoc da dong, khong the cap nhat."));

        // Build request for closed class
        CreateMaterialRequest request = CreateMaterialRequest.builder()
                .title("Slide bài giảng")
                .fileUrl("https://storage.englishlab.com/slide.pdf")
                .fileType("PDF")
                .description("Nội dung cho lớp đã đóng")
                .sessionId(10L)
                .build();

        assertThatThrownBy(() -> contentService.createMaterial(999L, request, "teacher@test.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MSG-49");

        // Verify the classId and request that caused the error
        ArgumentCaptor<Long> classIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<CreateMaterialRequest> requestCaptor = 
                ArgumentCaptor.forClass(CreateMaterialRequest.class);
        
        verify(contentService, times(1))
                .createMaterial(classIdCaptor.capture(), requestCaptor.capture(), any());
        
        assertThat(classIdCaptor.getValue()).isEqualTo(999L);
        CreateMaterialRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getTitle()).isEqualTo("Slide bài giảng");
        assertThat(capturedRequest.getSessionId()).isEqualTo(10L);
    }

    // TC06: classId khong ton tai -> EntityNotFoundException
    @Test
    void tc06_classNotFound_throwsException() {
        when(contentService.createMaterial(any(), any(), any()))
                .thenThrow(new jakarta.persistence.EntityNotFoundException("MSG-48: Khong tim thay du lieu yeu cau."));

        CreateMaterialRequest request = CreateMaterialRequest.builder()
                .title("Slide bài giảng")
                .fileUrl("https://storage.englishlab.com/slide.pdf")
                .fileType("PDF")
                .sessionId(10L)
                .build();

        assertThatThrownBy(() -> contentService.createMaterial(9999L, request, "teacher@test.com"))
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class)
                .hasMessageContaining("MSG-48");

        // Verify the non-existent classId was passed
        ArgumentCaptor<Long> classIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(contentService, times(1))
                .createMaterial(classIdCaptor.capture(), any(), any());
        
        assertThat(classIdCaptor.getValue()).isEqualTo(9999L);
    }
}
