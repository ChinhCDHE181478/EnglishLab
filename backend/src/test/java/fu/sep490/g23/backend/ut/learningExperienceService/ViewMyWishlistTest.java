package fu.sep490.g23.backend.ut.learningExperienceService;

import fu.sep490.g23.backend.dto.response.commerce.CommerceCourseItemResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.commerce.CourseListItem;
import fu.sep490.g23.backend.entity.commerce.enums.CourseListType;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.entity.course.enums.CourseLevel;
import fu.sep490.g23.backend.entity.course.enums.PackageStatus;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.commerce.CourseListItemRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseEnrollmentRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseRepository;
import fu.sep490.g23.backend.service.commerce.impl.StudentCommerceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ViewMyWishlistTest {

    @Mock
    private CourseListItemRepository courseListItemRepository;

    @Mock
    private OnlineCourseRepository onlineCourseRepository;

    @Mock
    private OnlineCourseEnrollmentRepository packageEnrollmentRepository;

    @Mock
    private UserRepository userRepository;

    private StudentCommerceServiceImpl service;

    private User student;
    private OnlineCourse course;
    private CourseListItem wishlistItem;

    private OnlineCourse deletedCourse;
    private CourseListItem deletedWishlistItem;

    @BeforeEach
    void setUp() {
        service = new StudentCommerceServiceImpl(
                courseListItemRepository,
                onlineCourseRepository,
                packageEnrollmentRepository,
                userRepository
        );

        student = User.builder()
                .id(1L)
                .email("learner@englishlab.com")
                .fullName("Nguyen Van A")
                .build();

        course = OnlineCourse.builder()
                .id(100L)
                .title("IELTS Foundation Course")
                .slug("ielts-foundation")
                .price(BigDecimal.valueOf(990000))
                .salePrice(BigDecimal.valueOf(790000))
                .duration("3 thang")
                .level(CourseLevel.BEGINNER)
                .status(PackageStatus.PUBLISHED)
                .totalLessons(40)
                .totalHours(60)
                .build();

        wishlistItem = CourseListItem.builder()
                .id(500L)
                .student(student)
                .onlineCourse(course)
                .listType(CourseListType.WISHLIST)
                .addedAt(LocalDateTime.of(2026, 8, 9, 14, 30))
                .build();

        // Setup cho test case 3: course đã bị xoá
        deletedCourse = OnlineCourse.builder()
                .id(200L)
                .title("Removed IELTS Course")
                .slug("removed-ielts-course")
                .price(BigDecimal.valueOf(500000))
                .salePrice(BigDecimal.valueOf(300000))
                .duration("2 thang")
                .level(CourseLevel.INTERMEDIATE)
                .status(PackageStatus.PUBLISHED)
                .totalLessons(30)
                .totalHours(45)
                .build();
        deletedWishlistItem = CourseListItem.builder()
                .id(600L)
                .student(student)
                .onlineCourse(deletedCourse)
                .listType(CourseListType.WISHLIST)
                .addedAt(LocalDateTime.of(2026, 8, 7, 10, 0))
                .build();
    }

    /**
     * Mục đích: Learner đã lưu 1 khóa học hợp lệ vào wishlist.
     * Input: email của learner hợp lệ.
     * Kỳ vọng: Trả về list 1 phần tử chứa đầy đủ thông tin khóa học.
     */
    @Test
    void viewMyWishlist_WithSavedCourse() {
        // Arrange
        when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));
        when(courseListItemRepository.findByStudentAndListTypeOrderByAddedAtDesc(student, CourseListType.WISHLIST))
                .thenReturn(List.of(wishlistItem));

        // Act
        List<CommerceCourseItemResponse> response = service.getWishlist(student.getEmail());

        // Assert
        assertNotNull(response);
        assertEquals(1, response.size());

        CommerceCourseItemResponse item = response.get(0);
        assertEquals(course.getId(), item.getId());
        assertEquals("IELTS Foundation Course", item.getTitle());
        assertEquals("ielts-foundation", item.getSlug());
        assertEquals(0, BigDecimal.valueOf(990000).compareTo(item.getOriginalPrice()));
        assertEquals(0, BigDecimal.valueOf(790000).compareTo(item.getPrice()));
        assertEquals(PackageStatus.PUBLISHED.name(), item.getStatus());
        assertFalse(item.isRegistered(), "Learner chưa mua khóa học nên registered = false");
        assertEquals(wishlistItem.getAddedAt(), item.getAddedAt());

        verify(userRepository).findByEmail(student.getEmail());
        verify(courseListItemRepository).findByStudentAndListTypeOrderByAddedAtDesc(student, CourseListType.WISHLIST);
    }

    /**
     * Mục đích: Learner chưa lưu khóa học nào vào wishlist.
     * Input: email của learner hợp lệ.
     * Kỳ vọng: Service trả về list rỗng.
     */
    @Test
    void viewMyWishlist_EmptyWishlist() {
        // Arrange
        when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));
        when(courseListItemRepository.findByStudentAndListTypeOrderByAddedAtDesc(student, CourseListType.WISHLIST))
                .thenReturn(new ArrayList<>());

        // Act
        List<CommerceCourseItemResponse> response = service.getWishlist(student.getEmail());

        // Assert
        assertNotNull(response, "Response không được null kể cả khi wishlist rỗng");
        assertTrue(response.isEmpty(), "Wishlist rỗng -> list trả về phải rỗng");
        assertEquals(0, response.size());

        verify(userRepository).findByEmail(student.getEmail());
        verify(courseListItemRepository).findByStudentAndListTypeOrderByAddedAtDesc(student, CourseListType.WISHLIST);
    }

    /**
     * Mục đích: Khóa học trong wishlist đã bị xoá.
     * Kỳ vọng: Service vẫn trả về item đó với thông tin nguyên trạng.
     */
    @Test
    void viewMyWishlist_WithDeletedCourse() {
        // Arrange
        when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));
        when(courseListItemRepository.findByStudentAndListTypeOrderByAddedAtDesc(student, CourseListType.WISHLIST))
                .thenReturn(List.of(deletedWishlistItem));

        // Act
        List<CommerceCourseItemResponse> response = service.getWishlist(student.getEmail());

        // Assert
        assertNotNull(response);
        assertEquals(1, response.size());

        CommerceCourseItemResponse item = response.get(0);
        assertEquals(deletedCourse.getId(), item.getId());
        assertEquals("Removed IELTS Course", item.getTitle());
        assertEquals("removed-ielts-course", item.getSlug());
        assertEquals(PackageStatus.PUBLISHED.name(), item.getStatus());
        assertEquals(0, BigDecimal.valueOf(500000).compareTo(item.getOriginalPrice()));
        assertEquals(0, BigDecimal.valueOf(300000).compareTo(item.getPrice()));
        assertFalse(item.isRegistered(), "Learner chưa mua course đã xoá -> registered = false");
        assertEquals(deletedWishlistItem.getAddedAt(), item.getAddedAt());
    }

    /**
     * Mục đích: Xoá 1 khóa học đang tồn tại trong wishlist của learner.
     * Kỳ vọng: Service gọi delete với tham số đúng.
     */
    @Test
    void removeFromWishlist_ExistingItem() {
        // Arrange
        Long courseId = course.getId();
        when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));

        // Act
        service.removeFromWishlist(courseId, student.getEmail());

        // Verify
        verify(userRepository).findByEmail(student.getEmail());
        verify(courseListItemRepository).deleteByStudentAndOnlineCourseIdAndListType(
                student, courseId, CourseListType.WISHLIST);
    }
}
