package fu.sep490.g23.backend.service.commerce.impl;
import java.util.Locale;

import fu.sep490.g23.backend.dto.response.commerce.CommerceCourseItemResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.commerce.CourseListItem;
import fu.sep490.g23.backend.entity.commerce.enums.CourseListType;
import fu.sep490.g23.backend.entity.course.LearningPackage;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.entity.course.enums.EnrollmentStatus;
import fu.sep490.g23.backend.entity.course.enums.PackageStatus;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.commerce.CourseListItemRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseRepository;
import fu.sep490.g23.backend.repository.course.PackageEnrollmentRepository;
import fu.sep490.g23.backend.service.commerce.StudentCommerceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class StudentCommerceServiceImpl implements StudentCommerceService {

    private final CourseListItemRepository courseListItemRepository;
    private final OnlineCourseRepository onlineCourseRepository;
    private final PackageEnrollmentRepository packageEnrollmentRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CommerceCourseItemResponse> getCart(String studentEmail) {
        User student = requireStudent(studentEmail);
        return courseListItemRepository.findByStudentAndListTypeOrderByAddedAtDesc(student, CourseListType.CART).stream()
                .map(this::toCommerceItem)
                .toList();
    }

    @Override
    public CommerceCourseItemResponse addToCart(Long courseId, String studentEmail) {
        User student = requireStudent(studentEmail);
        OnlineCourse course = requirePurchasableCourse(courseId, student);
        if (courseListItemRepository.findByStudentAndOnlineCourseIdAndListType(student, courseId, CourseListType.CART).isPresent()) {
            throw new RuntimeException("Khóa học đã có trong giỏ hàng.");
        }
        courseListItemRepository.deleteByStudentAndOnlineCourseIdAndListType(student, courseId, CourseListType.WISHLIST);
        CourseListItem saved = courseListItemRepository.save(CourseListItem.builder()
                .student(student)
                .onlineCourse(course)
                .listType(CourseListType.CART)
                .build());
        return toCommerceItem(saved);
    }

    @Override
    public void removeFromCart(Long courseId, String studentEmail) {
        User student = requireStudent(studentEmail);
        courseListItemRepository.deleteByStudentAndOnlineCourseIdAndListType(student, courseId, CourseListType.CART);
    }

    @Override
    public void clearCart(String studentEmail) {
        User student = requireStudent(studentEmail);
        courseListItemRepository.findByStudentAndListTypeOrderByAddedAtDesc(student, CourseListType.CART)
                .forEach(courseListItemRepository::delete);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommerceCourseItemResponse> getWishlist(String studentEmail) {
        User student = requireStudent(studentEmail);
        return courseListItemRepository.findByStudentAndListTypeOrderByAddedAtDesc(student, CourseListType.WISHLIST).stream()
                .map(this::toCommerceItem)
                .toList();
    }

    @Override
    public CommerceCourseItemResponse addToWishlist(Long courseId, String studentEmail) {
        User student = requireStudent(studentEmail);
        OnlineCourse course = requireVisibleCourse(courseId);
        assertNotEnrolled(student, course);
        if (courseListItemRepository.findByStudentAndOnlineCourseIdAndListType(student, courseId, CourseListType.WISHLIST).isPresent()) {
            throw new RuntimeException("Khóa học đã có trong danh sách yêu thích.");
        }
        CourseListItem saved = courseListItemRepository.save(CourseListItem.builder()
                .student(student)
                .onlineCourse(course)
                .listType(CourseListType.WISHLIST)
                .build());
        return toCommerceItem(saved);
    }

    @Override
    public void removeFromWishlist(Long courseId, String studentEmail) {
        User student = requireStudent(studentEmail);
        courseListItemRepository.deleteByStudentAndOnlineCourseIdAndListType(student, courseId, CourseListType.WISHLIST);
    }

    @Override
    public CommerceCourseItemResponse moveWishlistToCart(Long courseId, String studentEmail) {
        removeFromWishlist(courseId, studentEmail);
        return addToCart(courseId, studentEmail);
    }

    @Override
    public List<CommerceCourseItemResponse> syncCart(List<Long> courseIds, String studentEmail) {
        User student = requireStudent(studentEmail);
        Set<Long> uniqueIds = new LinkedHashSet<>();
        if (courseIds != null) {
            courseIds.stream().filter(id -> id != null && id > 0).forEach(uniqueIds::add);
        }
        List<CommerceCourseItemResponse> synced = new ArrayList<>();
        for (Long courseId : uniqueIds) {
            try {
                synced.add(addToCart(courseId, studentEmail));
            } catch (RuntimeException ex) {
                if (!"Khóa học đã có trong giỏ hàng.".equals(ex.getMessage())) {
                    // skip invalid/unavailable courses during sync
                } else {
                    courseListItemRepository.findByStudentAndOnlineCourseIdAndListType(student, courseId, CourseListType.CART)
                            .map(this::toCommerceItem)
                            .ifPresent(synced::add);
                }
            }
        }
        return getCart(studentEmail);
    }

    private User requireStudent(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người học."));
    }

    private OnlineCourse requireVisibleCourse(Long courseId) {
        OnlineCourse course = onlineCourseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học."));
        LearningPackage learningPackage = course.getLearningPackage();
        if (learningPackage == null || learningPackage.isDeleted()) {
            throw new RuntimeException("Khóa học không còn khả dụng.");
        }
        if (learningPackage.getStatus() != PackageStatus.PUBLISHED) {
            throw new RuntimeException("Khóa học chưa được xuất bản.");
        }
        return course;
    }

    private OnlineCourse requirePurchasableCourse(Long courseId, User student) {
        OnlineCourse course = requireVisibleCourse(courseId);
        assertNotEnrolled(student, course);
        return course;
    }

    private void assertNotEnrolled(User student, OnlineCourse course) {
        packageEnrollmentRepository.findByStudentAndLearningPackage(student, course.getLearningPackage())
                .filter(enrollment -> enrollment.getStatus() == EnrollmentStatus.ACTIVE
                        || enrollment.getStatus() == EnrollmentStatus.COMPLETED)
                .ifPresent(ignored -> {
                    throw new RuntimeException("Bạn đã sở hữu khóa học này.");
                });
    }

    private CommerceCourseItemResponse toCommerceItem(CourseListItem item) {
        return buildCommerceItem(item.getOnlineCourse(), item.getAddedAt(), isRegistered(item.getStudent(), item.getOnlineCourse()));
    }

    private boolean isRegistered(User student, OnlineCourse course) {
        return packageEnrollmentRepository.findByStudentAndLearningPackage(student, course.getLearningPackage())
                .filter(enrollment -> enrollment.getStatus() == EnrollmentStatus.ACTIVE
                        || enrollment.getStatus() == EnrollmentStatus.COMPLETED)
                .isPresent();
    }

    private CommerceCourseItemResponse buildCommerceItem(OnlineCourse course, java.time.LocalDateTime addedAt, boolean registered) {
        LearningPackage learningPackage = course.getLearningPackage();
        BigDecimal originalPrice = safePrice(learningPackage.getPrice());
        BigDecimal salePrice = resolveSalePrice(learningPackage);
        Integer discountPercent = computeDiscountPercent(originalPrice, salePrice);
        return CommerceCourseItemResponse.builder()
                .id(course.getId())
                .slug(learningPackage.getSlug())
                .title(learningPackage.getTitle())
                .thumbnailUrl(learningPackage.getThumbnailUrl())
                .category(course.getCategory() == null ? null : course.getCategory().getCode())
                .categoryName(course.getCategory() == null ? null : course.getCategory().getName())
                .duration(learningPackage.getDuration())
                .totalLessons(course.getTotalLessons())
                .targetBand(course.getTargetBand())
                .targetOutcome(course.getTargetOutcome())
                .shortDescription(learningPackage.getShortDescription())
                .price(salePrice)
                .salePrice(salePrice)
                .originalPrice(originalPrice)
                .discountPercent(discountPercent)
                .registered(registered)
                .status(learningPackage.getStatus().name())
                .addedAt(addedAt)
                .build();
    }

    private BigDecimal safePrice(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal resolveSalePrice(LearningPackage learningPackage) {
        if (learningPackage.getSalePrice() != null && learningPackage.getSalePrice().compareTo(BigDecimal.ZERO) >= 0) {
            return learningPackage.getSalePrice();
        }
        return safePrice(learningPackage.getPrice());
    }

    private Integer computeDiscountPercent(BigDecimal originalPrice, BigDecimal salePrice) {
        if (originalPrice == null || salePrice == null || originalPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        if (salePrice.compareTo(originalPrice) >= 0) {
            return 0;
        }
        return originalPrice.subtract(salePrice)
                .multiply(BigDecimal.valueOf(100))
                .divide(originalPrice, 0, RoundingMode.HALF_UP)
                .intValue();
    }
}
