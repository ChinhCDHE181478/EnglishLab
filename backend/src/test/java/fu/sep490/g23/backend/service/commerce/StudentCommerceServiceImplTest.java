package fu.sep490.g23.backend.service.commerce;

import fu.sep490.g23.backend.dto.response.commerce.CommerceCourseItemResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.commerce.CourseListItem;
import fu.sep490.g23.backend.entity.commerce.enums.CourseListType;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentCommerceServiceImplTest {

    @Mock private CourseListItemRepository courseListItemRepository;
    @Mock private OnlineCourseRepository onlineCourseRepository;
    @Mock private OnlineCourseEnrollmentRepository enrollmentRepository;
    @Mock private UserRepository userRepository;

    private StudentCommerceServiceImpl commerceService;
    private User student;

    @BeforeEach
    void setUp() {
        commerceService = new StudentCommerceServiceImpl(
                courseListItemRepository,
                onlineCourseRepository,
                enrollmentRepository,
                userRepository
        );
        student = User.builder().id(7L).email("learner@example.com").build();
        when(userRepository.findByEmail("learner@example.com")).thenReturn(Optional.of(student));
    }

    @Test
    void addToCart_rejectsCourseWithZeroOriginalPrice() {
        OnlineCourse freeCourse = publishedCourse(11L, BigDecimal.ZERO, null);
        when(onlineCourseRepository.findById(11L)).thenReturn(Optional.of(freeCourse));

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> commerceService.addToCart(11L, "learner@example.com"));

        assertEquals(
                "Khóa học miễn phí phải được đăng ký trực tiếp, không thể thêm vào giỏ hàng.",
                error.getMessage()
        );
        verify(courseListItemRepository, never()).save(any(CourseListItem.class));
    }

    @Test
    void addToCart_rejectsPaidCourseWithZeroSalePrice() {
        OnlineCourse freePromotionCourse = publishedCourse(
                12L,
                new BigDecimal("1000000"),
                BigDecimal.ZERO
        );
        when(onlineCourseRepository.findById(12L)).thenReturn(Optional.of(freePromotionCourse));

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> commerceService.addToCart(12L, "learner@example.com"));

        assertEquals(
                "Khóa học miễn phí phải được đăng ký trực tiếp, không thể thêm vào giỏ hàng.",
                error.getMessage()
        );
        verify(courseListItemRepository, never()).save(any(CourseListItem.class));
    }

    @Test
    void getCart_removesLegacyFreeItemsAndReturnsPaidItemsOnly() {
        OnlineCourse paidCourse = publishedCourse(21L, new BigDecimal("800000"), null);
        OnlineCourse freeCourse = publishedCourse(22L, BigDecimal.ZERO, null);
        CourseListItem paidItem = cartItem(31L, paidCourse);
        CourseListItem freeItem = cartItem(32L, freeCourse);
        when(courseListItemRepository.findByStudentAndListTypeOrderByAddedAtDesc(student, CourseListType.CART))
                .thenReturn(List.of(paidItem, freeItem));
        when(enrollmentRepository.findByStudentAndOnlineCourse(student, paidCourse)).thenReturn(Optional.empty());

        List<CommerceCourseItemResponse> result = commerceService.getCart("learner@example.com");

        assertEquals(1, result.size());
        assertEquals(21L, result.getFirst().getId());
        verify(courseListItemRepository).deleteAll(List.of(freeItem));
    }

    private OnlineCourse publishedCourse(Long id, BigDecimal price, BigDecimal salePrice) {
        return OnlineCourse.builder()
                .id(id)
                .title("Course " + id)
                .slug("course-" + id)
                .price(price)
                .salePrice(salePrice)
                .status(PackageStatus.PUBLISHED)
                .build();
    }

    private CourseListItem cartItem(Long id, OnlineCourse course) {
        return CourseListItem.builder()
                .id(id)
                .student(student)
                .onlineCourse(course)
                .listType(CourseListType.CART)
                .build();
    }
}
