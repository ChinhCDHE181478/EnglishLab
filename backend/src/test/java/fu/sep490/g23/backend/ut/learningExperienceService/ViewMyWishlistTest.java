package fu.sep490.g23.backend.ut.learningExperienceService;

import fu.sep490.g23.backend.dto.response.commerce.CommerceCourseItemResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.commerce.WishlistItem;
import fu.sep490.g23.backend.entity.course.LearningPackage;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.entity.course.enums.CourseLevel;
import fu.sep490.g23.backend.entity.course.enums.PackageStatus;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.commerce.CartItemRepository;
import fu.sep490.g23.backend.repository.commerce.WishlistItemRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseRepository;
import fu.sep490.g23.backend.repository.course.PackageEnrollmentRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ViewMyWishlistTest {

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private WishlistItemRepository wishlistItemRepository;

    @Mock
    private OnlineCourseRepository onlineCourseRepository;

    @Mock
    private PackageEnrollmentRepository packageEnrollmentRepository;

    @Mock
    private UserRepository userRepository;

    private StudentCommerceServiceImpl service;

    private User student;
    private OnlineCourse course;
    private WishlistItem wishlistItem;


    private OnlineCourse deletedCourse;
    private WishlistItem deletedWishlistItem;

    @BeforeEach
    void setUp() {
        service = new StudentCommerceServiceImpl(
                cartItemRepository,
                wishlistItemRepository,
                onlineCourseRepository,
                packageEnrollmentRepository,
                userRepository
        );

        student = User.builder()
                .id(1L)
                .email("learner@englishlab.com")
                .fullName("Nguyen Van A")
                .build();

        LearningPackage learningPackage = LearningPackage.builder()
                .id(10L)
                .title("IELTS Foundation Course")
                .slug("ielts-foundation")
                .price(BigDecimal.valueOf(990000))
                .salePrice(BigDecimal.valueOf(790000))
                .duration("3 thang")
                .status(PackageStatus.PUBLISHED)
                .deleted(false)
                .build();

        course = OnlineCourse.builder()
                .id(100L)
                .learningPackage(learningPackage)
                .level(CourseLevel.BEGINNER)
                .totalLessons(40)
                .totalHours(60)
                .modules(new ArrayList<>())
                .build();

        wishlistItem = WishlistItem.builder()
                .id(500L)
                .student(student)
                .onlineCourse(course)
                .addedAt(LocalDateTime.of(2026, 8, 9, 14, 30))
                .build();

        // ---- Setup cho test case 3: course ─æ├ú bß╗ï x├│a ----
        // learningPackage.deleted = true -> service kh├┤ng filter khi getWishlist,
        // item vß║½n ─æ╞░ß╗úc trß║ú vß╗ü vß╗¢i status gß╗æc, learner thß║Ñy ─æß╗â tß╗▒ xß╗¡ l├╜ UI
        LearningPackage deletedPackage = LearningPackage.builder()
                .id(20L)
                .title("Removed IELTS Course")
                .slug("removed-ielts-course")
                .price(BigDecimal.valueOf(500000))
                .salePrice(BigDecimal.valueOf(300000))
                .duration("2 thang")
                .status(PackageStatus.PUBLISHED)
                .deleted(true) // <-- ─æ├ú bß╗ï x├│a mß╗üm
                .build();
        deletedCourse = OnlineCourse.builder()
                .id(200L)
                .learningPackage(deletedPackage)
                .level(CourseLevel.INTERMEDIATE)
                .totalLessons(30)
                .totalHours(45)
                .modules(new ArrayList<>())
                .build();
        deletedWishlistItem = WishlistItem.builder()
                .id(600L)
                .student(student)
                .onlineCourse(deletedCourse)
                .addedAt(LocalDateTime.of(2026, 8, 7, 10, 0))
                .build();
    }

    /**
     * Mß╗Ñc ─æ├¡ch: Learner ─æ├ú l╞░u 1 kh├│a hß╗ìc hß╗úp lß╗ç v├áo wishlist.
     * Input: email cß╗ºa learner hß╗úp lß╗ç.
     * Kß╗│ vß╗ìng:
     *   - Trß║ú vß╗ü list 1 phß║ºn tß╗¡ chß╗⌐a ─æß║ºy ─æß╗º th├┤ng tin kh├│a hß╗ìc.
     *   - registered = false (learner ch╞░a mua).
     */
    @Test
    void viewMyWishlist_WithSavedCourse() {
        // Arrange
        when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));
        when(wishlistItemRepository.findByStudentOrderByAddedAtDesc(student))
                .thenReturn(List.of(wishlistItem));
        // Learner ch╞░a sß╗ƒ hß╗»u kh├│a hß╗ìc n├áy
        when(packageEnrollmentRepository.findByStudentAndLearningPackage(student, course.getLearningPackage()))
                .thenReturn(Optional.empty());

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
        assertFalse(item.isRegistered(), "Learner ch╞░a mua kh├│a hß╗ìc n├¬n registered = false");
        assertEquals(wishlistItem.getAddedAt(), item.getAddedAt());

        // Verify
        verify(userRepository).findByEmail(student.getEmail());
        verify(wishlistItemRepository).findByStudentOrderByAddedAtDesc(student);
    }

    /**
     * Mß╗Ñc ─æ├¡ch: Learner ch╞░a l╞░u kh├│a hß╗ìc n├áo v├áo wishlist.
     * Input: email cß╗ºa learner hß╗úp lß╗ç.
     * Kß╗│ vß╗ìng:
     *   - Service trß║ú vß╗ü list rß╗ùng (size = 0).
     *   - Kh├┤ng gß╗ìi packageEnrollmentRepository v├¼ kh├┤ng c├│ item n├áo.
     *   - Kh├┤ng n├⌐m exception.
     */
    @Test
    void viewMyWishlist_EmptyWishlist() {
        // Arrange
        when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));
        when(wishlistItemRepository.findByStudentOrderByAddedAtDesc(student))
                .thenReturn(new ArrayList<>());

        // Act
        List<CommerceCourseItemResponse> response = service.getWishlist(student.getEmail());

        // Assert
        assertNotNull(response, "Response kh├┤ng ─æ╞░ß╗úc null kß╗â cß║ú khi wishlist rß╗ùng");
        assertTrue(response.isEmpty(), "Wishlist rß╗ùng -> list trß║ú vß╗ü phß║úi rß╗ùng");
        assertEquals(0, response.size());

        // Verify - KH├öNG gß╗ìi packageEnrollmentRepository v├¼ kh├┤ng c├│ item ─æß╗â check isRegistered
        verify(userRepository).findByEmail(student.getEmail());
        verify(wishlistItemRepository).findByStudentOrderByAddedAtDesc(student);
        verify(packageEnrollmentRepository, never()).findByStudentAndLearningPackage(student, course.getLearningPackage());
    }

    /**
     * Mß╗Ñc ─æ├¡ch: Kh├│a hß╗ìc trong wishlist ─æ├ú bß╗ï x├│a (learningPackage.deleted = true).
     * Input: email cß╗ºa learner hß╗úp lß╗ç, wishlist chß╗⌐a 1 course ─æ├ú bß╗ï x├│a.
     * Kß╗│ vß╗ìng (theo code hiß╗çn tß║íi - getWishlist KH├öNG filter):
     *   - Service vß║½n trß║ú vß╗ü list 1 phß║ºn tß╗¡ (kh├┤ng filter).
     *   - Item phß║ún ├ính ─æ├║ng th├┤ng tin: status vß║½n PUBLISHED, title/slug/price nh╞░ c┼⌐.
     *   - registered = false (ch╞░a ─æ─âng k├╜, chß╗ë l├á course ─æ├ú bß╗ï x├│a).
     *   - Frontend sß║╜ dß╗▒a v├áo flag deleted hoß║╖c status ─æß╗â hiß╗ân thß╗ï cß║únh b├ío cho learner.
     */
    @Test
    void viewMyWishlist_WithDeletedCourse() {
        // Arrange
        when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));
        when(wishlistItemRepository.findByStudentOrderByAddedAtDesc(student))
                .thenReturn(List.of(deletedWishlistItem));
        // Learner ch╞░a ─æ─âng k├╜ course n├áy
        when(packageEnrollmentRepository.findByStudentAndLearningPackage(student, deletedCourse.getLearningPackage()))
                .thenReturn(Optional.empty());

        // Act
        List<CommerceCourseItemResponse> response = service.getWishlist(student.getEmail());

        // Assert
        assertNotNull(response);
        assertEquals(1, response.size(), "Service KH├öNG filter course ─æ├ú x├│a - vß║½n trß║ú vß╗ü trong wishlist");

        CommerceCourseItemResponse item = response.get(0);
        assertEquals(deletedCourse.getId(), item.getId());
        assertEquals("Removed IELTS Course", item.getTitle());
        assertEquals("removed-ielts-course", item.getSlug());
        assertEquals(PackageStatus.PUBLISHED.name(), item.getStatus(),
                "Status trong response l├á cß╗ºa package (PUBLISHED), KH├öNG phß║ún ├ính trß║íng th├íi deleted");
        assertEquals(0, BigDecimal.valueOf(500000).compareTo(item.getOriginalPrice()));
        assertEquals(0, BigDecimal.valueOf(300000).compareTo(item.getPrice()));
        assertFalse(item.isRegistered(), "Learner ch╞░a mua course ─æ├ú x├│a -> registered = false");
        assertEquals(deletedWishlistItem.getAddedAt(), item.getAddedAt());

        // Verify
        verify(userRepository).findByEmail(student.getEmail());
        verify(wishlistItemRepository).findByStudentOrderByAddedAtDesc(student);
        verify(packageEnrollmentRepository).findByStudentAndLearningPackage(student, deletedCourse.getLearningPackage());
    }

    /**
     * Mß╗Ñc ─æ├¡ch: X├│a 1 kh├│a hß╗ìc ─æang tß╗ôn tß║íi trong wishlist cß╗ºa learner.
     * Pre: Wishlist c├│ chß╗⌐a kh├│a hß╗ìc (id = course.getId() = 100L).
     * Input: email learner hß╗úp lß╗ç + courseId hß╗úp lß╗ç.
     * Kß╗│ vß╗ìng (theo code hiß╗çn tß║íi - removeFromWishlist KH├öNG check exists):
     *   - Service gß╗ìi deleteByStudentAndOnlineCourseId(student, courseId).
     *   - Kh├┤ng n├⌐m exception, kh├┤ng return gi├í trß╗ï.
     *   - KH├öNG gß╗ìi th├¬m repository n├áo kh├íc (cart, enrollment...).
     */
    @Test
    void removeFromWishlist_ExistingItem() {
        // Arrange
        Long courseId = course.getId(); // 100L - course hß╗úp lß╗ç ─æang c├│ trong wishlist
        when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));

        // Act
        service.removeFromWishlist(courseId, student.getEmail());

        // Verify - chß╗ë cß║ºn ─æ├║ng 1 lß║ºn gß╗ìi delete ─æ├║ng tham sß╗æ
        verify(userRepository).findByEmail(student.getEmail());
        verify(wishlistItemRepository).deleteByStudentAndOnlineCourseId(student, courseId);
        verifyNoMoreInteractions(wishlistItemRepository);
    }
}
