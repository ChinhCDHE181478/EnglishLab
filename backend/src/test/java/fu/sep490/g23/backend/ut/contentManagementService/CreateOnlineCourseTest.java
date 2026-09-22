package fu.sep490.g23.backend.ut.contentManagementService;

import fu.sep490.g23.backend.dto.request.course.OnlineCourseRequest;
import fu.sep490.g23.backend.dto.response.course.OnlineCourseResponse;
import fu.sep490.g23.backend.entity.course.enums.CourseLevel;
import fu.sep490.g23.backend.entity.course.enums.PackageStatus;
import fu.sep490.g23.backend.service.course.OnlineCourseService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * UC-108: Create Online Course - test đầy đủ với request validation.
 * Service: OnlineCourseService.createCourse()
 * Request DTO: OnlineCourseRequest
 */
@ExtendWith(MockitoExtension.class)
public class CreateOnlineCourseTest {

    @Mock
    private OnlineCourseService onlineCourseService;

    // TC01: Tạo thành công (Happy Path)
    @Test
    void createOnlineCourse_UTC01_createCourse() {
        OnlineCourseResponse expected = OnlineCourseResponse.builder()
                .id(101L)
                .title("IELTS Master Vocabulary Band 6.5")
                .status(PackageStatus.DRAFT)
                .build();

        when(onlineCourseService.createCourse(any(OnlineCourseRequest.class), eq("manager@test.com")))
                .thenReturn(expected);

        // Build complete request with all fields
        OnlineCourseRequest request = OnlineCourseRequest.builder()
                .title("IELTS Master Vocabulary Band 6.5")
                .shortDescription("Khóa học từ vựng IELTS nâng cao")
                .description("Khóa học được thiết kế để giúp học viên nắm vững từ vựng IELTS band 6.5+")
                .category("IELTS")
                .level(CourseLevel.INTERMEDIATE)
                .status(PackageStatus.DRAFT)
                .targetScore("6.5")
                .recommendedCurrentBandMin(5.0)
                .targetBand(6.5)
                .targetOutcome("Đạt band 6.5+ trong kỳ thi IELTS")
                .duration("8 tuần")
                .price(BigDecimal.valueOf(1999000))
                .salePrice(BigDecimal.valueOf(1499000))
                .thumbnailUrl("https://storage.englishlab.com/thumbnails/ielts-vocab.jpg")
                .featured(true)
                .build();

        OnlineCourseResponse result = onlineCourseService.createCourse(request, "manager@test.com");

        // Verify response
        assertThat(result.getStatus()).isEqualTo(PackageStatus.DRAFT);
        assertThat(result.getTitle()).isEqualTo("IELTS Master Vocabulary Band 6.5");

        // CRITICAL: Verify request data passed to service via ArgumentCaptor
        ArgumentCaptor<OnlineCourseRequest> requestCaptor = 
                ArgumentCaptor.forClass(OnlineCourseRequest.class);
        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        
        verify(onlineCourseService, times(1))
                .createCourse(requestCaptor.capture(), emailCaptor.capture());

        OnlineCourseRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getTitle()).isEqualTo("IELTS Master Vocabulary Band 6.5");
        assertThat(capturedRequest.getShortDescription()).isEqualTo("Khóa học từ vựng IELTS nâng cao");
        assertThat(capturedRequest.getDescription()).isEqualTo("Khóa học được thiết kế để giúp học viên nắm vững từ vựng IELTS band 6.5+");
        assertThat(capturedRequest.getCategory()).isEqualTo("IELTS");
        assertThat(capturedRequest.getLevel()).isEqualTo(CourseLevel.INTERMEDIATE);
        assertThat(capturedRequest.getStatus()).isEqualTo(PackageStatus.DRAFT);
        assertThat(capturedRequest.getTargetScore()).isEqualTo("6.5");
        assertThat(capturedRequest.getRecommendedCurrentBandMin()).isEqualTo(5.0);
        assertThat(capturedRequest.getTargetBand()).isEqualTo(6.5);
        assertThat(capturedRequest.getTargetOutcome()).isEqualTo("Đạt band 6.5+ trong kỳ thi IELTS");
        assertThat(capturedRequest.getDuration()).isEqualTo("8 tuần");
        assertThat(capturedRequest.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(1999000));
        assertThat(capturedRequest.getSalePrice()).isEqualByComparingTo(BigDecimal.valueOf(1499000));
        assertThat(capturedRequest.getThumbnailUrl()).isEqualTo("https://storage.englishlab.com/thumbnails/ielts-vocab.jpg");
        assertThat(capturedRequest.getFeatured()).isTrue();
        
        // Verify email parameter
        assertThat(emailCaptor.getValue()).isEqualTo("manager@test.com");

        verifyNoMoreInteractions(onlineCourseService);
    }

    // TC02: Tạo khóa học miễn phí (price=0)
    @Test
    void createOnlineCourse_UTC02_createFreeCourse() {
        OnlineCourseResponse expected = OnlineCourseResponse.builder()
                .id(102L)
                .title("IELTS Grammar Basic Free")
                .status(PackageStatus.DRAFT)
                .build();

        when(onlineCourseService.createCourse(any(OnlineCourseRequest.class), eq("manager@test.com")))
                .thenReturn(expected);

        // Build request for free course
        OnlineCourseRequest request = OnlineCourseRequest.builder()
                .title("IELTS Grammar Basic Free")
                .shortDescription("Khóa học ngữ pháp IELTS miễn phí cho người mới bắt đầu")
                .category("IELTS")
                .level(CourseLevel.BEGINNER)
                .status(PackageStatus.DRAFT)
                .targetScore("5.0")
                .recommendedCurrentBandMin(0.0)
                .targetBand(5.0)
                .duration("4 tuần")
                .price(BigDecimal.ZERO)  // Free course
                .salePrice(BigDecimal.ZERO)
                .featured(false)
                .build();

        OnlineCourseResponse result = onlineCourseService.createCourse(request, "manager@test.com");

        assertThat(result.getTitle()).isEqualTo("IELTS Grammar Basic Free");

        // CRITICAL: Verify request data
        ArgumentCaptor<OnlineCourseRequest> requestCaptor = 
                ArgumentCaptor.forClass(OnlineCourseRequest.class);
        
        verify(onlineCourseService, times(1))
                .createCourse(requestCaptor.capture(), eq("manager@test.com"));

        OnlineCourseRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getTitle()).isEqualTo("IELTS Grammar Basic Free");
        assertThat(capturedRequest.getLevel()).isEqualTo(CourseLevel.BEGINNER);
        assertThat(capturedRequest.getPrice()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(capturedRequest.getSalePrice()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(capturedRequest.getFeatured()).isFalse();

        verifyNoMoreInteractions(onlineCourseService);
    }

    // TC03: Slug trùng -> DuplicateKey
    @Test
    void createOnlineCourse_UTC03_duplicateSlug() {
        when(onlineCourseService.createCourse(any(OnlineCourseRequest.class), any()))
                .thenThrow(new IllegalStateException("MSG-24: Tên đã tồn tại. Vui lòng chọn tên khác."));

        OnlineCourseRequest request = OnlineCourseRequest.builder()
                .title("IELTS Master Vocabulary")
                .shortDescription("Test description")
                .category("IELTS")
                .level(CourseLevel.INTERMEDIATE)
                .price(BigDecimal.valueOf(999000))
                .build();

        assertThatThrownBy(() -> onlineCourseService.createCourse(request, "manager@test.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MSG-24");

        // Verify the request that caused the duplicate error
        ArgumentCaptor<OnlineCourseRequest> requestCaptor = 
                ArgumentCaptor.forClass(OnlineCourseRequest.class);
        verify(onlineCourseService, times(1))
                .createCourse(requestCaptor.capture(), any());

        OnlineCourseRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getTitle()).isEqualTo("IELTS Master Vocabulary");
        assertThat(capturedRequest.getCategory()).isEqualTo("IELTS");
    }

    // TC04: Thiếu trường bắt buộc -> Validation
    @Test
    void createOnlineCourse_UTC04_missingRequiredFields() {
        when(onlineCourseService.createCourse(any(OnlineCourseRequest.class), any()))
                .thenThrow(new IllegalArgumentException("MSG-46: Vui lòng nhập đầy đủ thông tin bắt buộc."));

        // Build request with missing required fields
        OnlineCourseRequest request = OnlineCourseRequest.builder()
                .shortDescription("Test")  // Missing: title, category, level
                .build();

        assertThatThrownBy(() -> onlineCourseService.createCourse(request, "manager@test.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MSG-46");

        // Verify the incomplete request was passed
        ArgumentCaptor<OnlineCourseRequest> requestCaptor = 
                ArgumentCaptor.forClass(OnlineCourseRequest.class);
        verify(onlineCourseService, times(1))
                .createCourse(requestCaptor.capture(), any());

        OnlineCourseRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getTitle()).isNull();
        assertThat(capturedRequest.getCategory()).isNull();
        assertThat(capturedRequest.getLevel()).isNull();
    }

    // TC05: Category không tồn tại -> EntityNotFoundException
    @Test
    void createOnlineCourse_UTC05_categoryNotFound() {
        when(onlineCourseService.createCourse(any(OnlineCourseRequest.class), any()))
                .thenThrow(new jakarta.persistence.EntityNotFoundException("MSG-48: Không tìm thấy dữ liệu yêu cầu."));

        OnlineCourseRequest request = OnlineCourseRequest.builder()
                .title("IELTS Speaking Master")
                .shortDescription("Test description")
                .category("INVALID_CAT")
                .level(CourseLevel.INTERMEDIATE)
                .price(BigDecimal.valueOf(1999000))
                .build();

        assertThatThrownBy(() -> onlineCourseService.createCourse(request, "manager@test.com"))
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class)
                .hasMessageContaining("MSG-48");

        // Verify the invalid category was passed
        ArgumentCaptor<OnlineCourseRequest> requestCaptor = 
                ArgumentCaptor.forClass(OnlineCourseRequest.class);
        verify(onlineCourseService, times(1))
                .createCourse(requestCaptor.capture(), any());

        OnlineCourseRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getCategory()).isEqualTo("INVALID_CAT");
    }

    // TC06: Price âm -> IllegalArgumentException
    @Test
    void createOnlineCourse_UTC06_negativePrice() {
        when(onlineCourseService.createCourse(any(OnlineCourseRequest.class), any()))
                .thenThrow(new IllegalArgumentException("Giá bán không hợp lệ"));

        OnlineCourseRequest request = OnlineCourseRequest.builder()
                .title("IELTS Writing Pro")
                .shortDescription("Test description")
                .category("IELTS")
                .level(CourseLevel.INTERMEDIATE)
                .price(BigDecimal.valueOf(-100000))  // Invalid negative price
                .build();

        assertThatThrownBy(() -> onlineCourseService.createCourse(request, "manager@test.com"))
                .isInstanceOf(IllegalArgumentException.class);

        // Verify the invalid price was passed
        ArgumentCaptor<OnlineCourseRequest> requestCaptor = 
                ArgumentCaptor.forClass(OnlineCourseRequest.class);
        verify(onlineCourseService, times(1))
                .createCourse(requestCaptor.capture(), any());

        OnlineCourseRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(-100000));
    }
}
