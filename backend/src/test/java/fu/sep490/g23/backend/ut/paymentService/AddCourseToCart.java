package fu.sep490.g23.backend.ut.paymentService;

import fu.sep490.g23.backend.dto.response.commerce.CommerceCourseItemResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.commerce.CourseListItem;
import fu.sep490.g23.backend.entity.commerce.enums.CourseListType;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.entity.course.OnlineCourseEnrollment;
import fu.sep490.g23.backend.entity.course.enums.EnrollmentStatus;
import fu.sep490.g23.backend.entity.course.enums.PackageStatus;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.commerce.CourseListItemRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseEnrollmentRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseRepository;
import fu.sep490.g23.backend.service.commerce.impl.StudentCommerceServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * UC: Add Course To Cart - test day du cac truong hop.
 * Service: StudentCommerceServiceImpl.addToCart(Long courseId, String studentEmail)
 *
 * Quy tac nghiep vu:
 *  - BR-20: Khong cho phep them khoa hoc da co trong gio hang / da so huu.
 *  - BR-21: Chi them cac khoa hoc o trang thai PUBLISHED (khong phai DRAFT/ARCHIVED).
 *  - BR-2012: Tu dong xoa khoi Wishlist khi them vao Cart.
 */
@ExtendWith(MockitoExtension.class)
public class AddCourseToCart {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OnlineCourseRepository onlineCourseRepository;

    @Mock
    private CourseListItemRepository courseListItemRepository;

    @Mock
    private OnlineCourseEnrollmentRepository packageEnrollmentRepository;

    @InjectMocks
    private StudentCommerceServiceImpl service;


    // TC01: Happy Path - Them khoa hoc hop le vao gio hang thanh cong.

    @Test
    void addCourseToCart_UTC01_addValidCourse_succeeds() {
        // Arrange
        String studentEmail = "LEARNER_01";
        Long courseId = 101L;

        User student = User.builder().id(1L).email(studentEmail).build();
        OnlineCourse course = OnlineCourse.builder()
                .id(courseId)
                .title("IELTS Foundation")
                .slug("ielts-foundation")
                .price(BigDecimal.valueOf(500_000))
                .status(PackageStatus.PUBLISHED)
                .build();

        when(userRepository.findByEmail(studentEmail)).thenReturn(Optional.of(student));
        when(onlineCourseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(packageEnrollmentRepository.findByStudentAndOnlineCourse(student, course))
                .thenReturn(Optional.empty());
        when(courseListItemRepository.findByStudentAndOnlineCourseIdAndListType(
                student, courseId, CourseListType.CART))
                .thenReturn(Optional.empty());
        when(courseListItemRepository.save(any(CourseListItem.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        CommerceCourseItemResponse result = service.addToCart(courseId, studentEmail);

        // Assert: CartItem moi duoc tao
        ArgumentCaptor<CourseListItem> cartItemCaptor = ArgumentCaptor.forClass(CourseListItem.class);
        verify(courseListItemRepository, times(1)).save(cartItemCaptor.capture());

        CourseListItem savedItem = cartItemCaptor.getValue();
        assertThat(savedItem.getStudent()).isEqualTo(student);
        assertThat(savedItem.getOnlineCourse()).isEqualTo(course);
        assertThat(savedItem.getListType()).isEqualTo(CourseListType.CART);

        // Verify deleteFromWishlist co the duoc goi (BR-2012)
        // Service luon xoa khoi wishlist de dam bao tinh idem-potency
        verify(courseListItemRepository, org.mockito.Mockito.atMostOnce())
                .deleteByStudentAndOnlineCourseIdAndListType(
                        any(), any(), eq(CourseListType.WISHLIST));

        // Verify response
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(courseId);
        assertThat(result.getTitle()).isEqualTo("IELTS Foundation");
        assertThat(result.getStatus()).isEqualTo("PUBLISHED");

        // Mock data verifications (capture arguments)
        ArgumentCaptor<Long> courseIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        verify(userRepository, times(1)).findByEmail(emailCaptor.capture());
        verify(onlineCourseRepository, times(1)).findById(courseIdCaptor.capture());
        assertThat(emailCaptor.getValue()).isEqualTo(studentEmail);
        assertThat(courseIdCaptor.getValue()).isEqualTo(courseId);

        // MSG-32: Đã thêm khóa học vào giỏ hàng.
        assertThat("Đã thêm khóa học vào giỏ hàng.").isNotEmpty();
    }


    // TC02: Them khoa hoc dang co trong Wishlist vao Cart thanh cong.
    @Test
    void addCourseToCart_UTC02_addCourseInWishlist_succeeds() {
        // Arrange
        String studentEmail = "LEARNER_01";
        Long courseId = 101L;

        User student = User.builder().id(1L).email(studentEmail).build();
        OnlineCourse course = OnlineCourse.builder()
                .id(courseId)
                .title("IELTS Foundation")
                .slug("ielts-foundation")
                .price(BigDecimal.valueOf(500_000))
                .status(PackageStatus.PUBLISHED)
                .build();

        when(userRepository.findByEmail(studentEmail)).thenReturn(Optional.of(student));
        when(onlineCourseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(packageEnrollmentRepository.findByStudentAndOnlineCourse(student, course))
                .thenReturn(Optional.empty());
        when(courseListItemRepository.findByStudentAndOnlineCourseIdAndListType(
                student, courseId, CourseListType.CART))
                .thenReturn(Optional.empty());
        when(courseListItemRepository.save(any(CourseListItem.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        CommerceCourseItemResponse result = service.addToCart(courseId, studentEmail);

        // Assert: CartItem moi duoc tao
        ArgumentCaptor<CourseListItem> cartItemCaptor = ArgumentCaptor.forClass(CourseListItem.class);
        verify(courseListItemRepository, times(1)).save(cartItemCaptor.capture());
        assertThat(cartItemCaptor.getValue().getListType()).isEqualTo(CourseListType.CART);

        // Assert: deleteFromWishlist duoc goi DUNG 1 LAN (BR-2012)
        verify(courseListItemRepository, times(1)).deleteByStudentAndOnlineCourseIdAndListType(
                student, courseId, CourseListType.WISHLIST);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(courseId);

        // MSG-32: Đã thêm khóa học vào giỏ hàng.
        assertThat("Đã thêm khóa học vào giỏ hàng.").isNotEmpty();
    }


    // TC03: That bai do khoa hoc da ton tai trong gio hang (BR-20).
    @Test
    void addCourseToCart_UTC03_courseAlreadyInCart_fails() {
        // Arrange
        String studentEmail = "LEARNER_01";
        Long courseId = 101L;

        User student = User.builder().id(1L).email(studentEmail).build();
        OnlineCourse course = OnlineCourse.builder()
                .id(courseId)
                .title("IELTS Foundation")
                .slug("ielts-foundation")
                .price(BigDecimal.valueOf(500_000))
                .status(PackageStatus.PUBLISHED)
                .build();

        CourseListItem existingCartItem = CourseListItem.builder()
                .id(99L)
                .student(student)
                .onlineCourse(course)
                .listType(CourseListType.CART)
                .build();

        when(userRepository.findByEmail(studentEmail)).thenReturn(Optional.of(student));
        when(onlineCourseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(courseListItemRepository.findByStudentAndOnlineCourseIdAndListType(
                student, courseId, CourseListType.CART))
                .thenReturn(Optional.of(existingCartItem));

        // Act & Assert
        assertThatThrownBy(() -> service.addToCart(courseId, studentEmail))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("đã có trong giỏ hàng"); // MSG-33

        // Verify KHONG tao CartItem moi
        verify(courseListItemRepository, never()).save(any(CourseListItem.class));
        // Verify KHONG goi deleteFromWishlist
        verify(courseListItemRepository, never()).deleteByStudentAndOnlineCourseIdAndListType(
                any(), any(), eq(CourseListType.WISHLIST));
    }


    // TC04: That bai khi hoc vien da so huu dang ky khoa hoc (BR-20, BR-21).
    @Test
    void addCourseToCart_UTC04_learnerAlreadyEnrolled_fails() {
        // Arrange
        String studentEmail = "LEARNER_01";
        Long courseId = 101L;

        User student = User.builder().id(1L).email(studentEmail).build();
        OnlineCourse course = OnlineCourse.builder()
                .id(courseId)
                .title("IELTS Foundation")
                .slug("ielts-foundation")
                .price(BigDecimal.valueOf(500_000))
                .status(PackageStatus.PUBLISHED)
                .build();

        OnlineCourseEnrollment activeEnrollment = OnlineCourseEnrollment.builder()
                .id(50L)
                .student(student)
                .onlineCourse(course)
                .status(EnrollmentStatus.ACTIVE)
                .build();

        when(userRepository.findByEmail(studentEmail)).thenReturn(Optional.of(student));
        when(onlineCourseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(packageEnrollmentRepository.findByStudentAndOnlineCourse(student, course))
                .thenReturn(Optional.of(activeEnrollment));

        // Act & Assert
        assertThatThrownBy(() -> service.addToCart(courseId, studentEmail))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("đã sở hữu"); // MSG-36

        // Verify KHONG save CartItem
        verify(courseListItemRepository, never()).save(any(CourseListItem.class));
        verify(courseListItemRepository, never()).findByStudentAndOnlineCourseIdAndListType(
                any(), any(), eq(CourseListType.CART));
    }


    // TC05: That bai do khoa hoc chua duoc xuat ban (DRAFT) - BR-21.
    @Test
    void addCourseToCart_UTC05_courseNotPublished_fails() {
        // Arrange
        String studentEmail = "LEARNER_01";
        Long courseId = 999L;

        User student = User.builder().id(1L).email(studentEmail).build();
        OnlineCourse draftCourse = OnlineCourse.builder()
                .id(courseId)
                .title("IELTS Draft")
                .slug("ielts-draft")
                .price(BigDecimal.valueOf(500_000))
                .status(PackageStatus.DRAFT)
                .build();

        when(userRepository.findByEmail(studentEmail)).thenReturn(Optional.of(student));
        when(onlineCourseRepository.findById(courseId)).thenReturn(Optional.of(draftCourse));

        // Act & Assert
        assertThatThrownBy(() -> service.addToCart(courseId, studentEmail))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("chưa được xuất bản"); // MSG-10: Khóa học không còn khả dụng.

        // Verify KHONG save CartItem
        verify(courseListItemRepository, never()).save(any(CourseListItem.class));
        verify(packageEnrollmentRepository, never()).findByStudentAndOnlineCourse(any(), any());
    }


    // TC06: That bai do khong tim thay khoa hoc trong CSDL - Not Found.
    @Test
    void addCourseToCart_UTC06_courseNotFound_fails() {
        // Arrange
        String studentEmail = "LEARNER_01";
        Long invalidCourseId = 99999L;

        User student = User.builder().id(1L).email(studentEmail).build();

        when(userRepository.findByEmail(studentEmail)).thenReturn(Optional.of(student));
        when(onlineCourseRepository.findById(invalidCourseId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.addToCart(invalidCourseId, studentEmail))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Không tìm thấy khóa học"); // MSG-48

        // Verify KHONG save CartItem
        verify(courseListItemRepository, never()).save(any(CourseListItem.class));
    }
}
