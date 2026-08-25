package fu.sep490.g23.backend.service.payment;

import fu.sep490.g23.backend.dto.response.course.OnlineCourseEnrollmentResponse;
import fu.sep490.g23.backend.dto.response.payment.PaymentQuoteResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.course.LearningPackage;
import fu.sep490.g23.backend.entity.course.LearningPath;
import fu.sep490.g23.backend.entity.course.LearningPathCourse;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.entity.course.enums.PackageStatus;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomEnrollmentRepository;
import fu.sep490.g23.backend.repository.course.LearningPathCourseRepository;
import fu.sep490.g23.backend.repository.course.LearningPathRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseRepository;
import fu.sep490.g23.backend.repository.payment.DiscountCodeRepository;
import fu.sep490.g23.backend.repository.payment.PaymentOrderRepository;
import fu.sep490.g23.backend.service.classroom.ClassroomOfferingService;
import fu.sep490.g23.backend.service.course.OnlineCourseService;
import fu.sep490.g23.backend.service.payment.impl.PaymentServiceImpl;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplLearningPathTest {
    @Mock private PayosProperties payosProperties;
    @Mock private PaymentOrderRepository paymentOrderRepository;
    @Mock private DiscountCodeRepository discountCodeRepository;
    @Mock private OnlineCourseRepository onlineCourseRepository;
    @Mock private LearningPathRepository learningPathRepository;
    @Mock private LearningPathCourseRepository learningPathCourseRepository;
    @Mock private ClassroomEnrollmentRepository classroomEnrollmentRepository;
    @Mock private UserRepository userRepository;
    @Mock private OnlineCourseService onlineCourseService;
    @Mock private ClassroomOfferingService classroomOfferingService;
    @Mock private PaymentReceiptPdfService paymentReceiptPdfService;

    private PaymentServiceImpl paymentService;
    private LearningPath path;
    private List<LearningPathCourse> courseRefs;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentServiceImpl(
                payosProperties,
                paymentOrderRepository,
                discountCodeRepository,
                onlineCourseRepository,
                learningPathRepository,
                learningPathCourseRepository,
                classroomEnrollmentRepository,
                userRepository,
                onlineCourseService,
                classroomOfferingService,
                paymentReceiptPdfService
        );
        path = LearningPath.builder()
                .id(9L)
                .code("TOEIC_PATH")
                .name("TOEIC Path")
                .discountPercent(10)
                .minimumCoursesForDiscount(2)
                .build();
        courseRefs = List.of(courseRef(1L, 101L, 1), courseRef(2L, 102L, 2), courseRef(3L, 103L, 3));
        User student = User.builder().id(7L).email("learner@example.com").build();
        when(userRepository.findByEmail("learner@example.com")).thenReturn(Optional.of(student));
        when(learningPathRepository.findById(9L)).thenReturn(Optional.of(path));
        when(learningPathCourseRepository.findByLearningPathIdOrderByDisplayOrderAscIdAsc(9L)).thenReturn(courseRefs);
    }

    @Test
    void quoteLearningPath_appliesConfiguredDiscountWhenTwoCoursesRemain() {
        when(onlineCourseService.getMyEnrollments("learner@example.com"))
                .thenReturn(List.of(OnlineCourseEnrollmentResponse.builder().courseId(1L).build()));

        PaymentQuoteResponse quote = paymentService.quotePayment(
                List.of(), List.of(), 9L, null, "learner@example.com");

        assertEquals(2_000_000L, quote.getOriginalAmount());
        assertEquals(200_000L, quote.getLearningPathDiscountAmount());
        assertEquals(1_800_000L, quote.getTotalAmount());
        assertEquals(9L, quote.getLearningPathId());
    }

    @Test
    void quoteLearningPath_doesNotDiscountOnlyRemainingCourse() {
        when(onlineCourseService.getMyEnrollments("learner@example.com"))
                .thenReturn(List.of(
                        OnlineCourseEnrollmentResponse.builder().courseId(1L).build(),
                        OnlineCourseEnrollmentResponse.builder().courseId(2L).build()
                ));

        PaymentQuoteResponse quote = paymentService.quotePayment(
                List.of(), List.of(), 9L, null, "learner@example.com");

        assertEquals(1_000_000L, quote.getOriginalAmount());
        assertEquals(0L, quote.getLearningPathDiscountAmount());
        assertEquals(1_000_000L, quote.getTotalAmount());
    }

    @Test
    void quoteLearningPath_rejectsWhenAllCoursesAreOwned() {
        when(onlineCourseService.getMyEnrollments("learner@example.com"))
                .thenReturn(List.of(
                        OnlineCourseEnrollmentResponse.builder().courseId(1L).build(),
                        OnlineCourseEnrollmentResponse.builder().courseId(2L).build(),
                        OnlineCourseEnrollmentResponse.builder().courseId(3L).build()
                ));

        RuntimeException error = assertThrows(RuntimeException.class, () -> paymentService.quotePayment(
                List.of(), List.of(), 9L, null, "learner@example.com"));

        assertEquals("Bạn đã sở hữu toàn bộ khóa học trong lộ trình này.", error.getMessage());
    }

    private LearningPathCourse courseRef(Long courseId, Long packageId, int order) {
        LearningPackage learningPackage = LearningPackage.builder()
                .id(packageId)
                .title("Course " + courseId)
                .slug("course-" + courseId)
                .price(new BigDecimal("1000000"))
                .status(PackageStatus.PUBLISHED)
                .build();
        OnlineCourse course = OnlineCourse.builder().id(courseId).learningPackage(learningPackage).build();
        return LearningPathCourse.builder()
                .learningPath(path)
                .onlineCourse(course)
                .displayOrder(order)
                .build();
    }
}
