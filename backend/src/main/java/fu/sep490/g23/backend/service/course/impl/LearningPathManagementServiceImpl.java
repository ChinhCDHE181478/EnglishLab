package fu.sep490.g23.backend.service.course.impl;
import java.util.LinkedHashMap;

import fu.sep490.g23.backend.dto.request.course.LearningPathCoursesRequest;
import fu.sep490.g23.backend.dto.request.course.LearningPathRequest;
import fu.sep490.g23.backend.dto.response.course.LearnerLearningPathCourseResponse;
import fu.sep490.g23.backend.dto.response.course.LearnerLearningPathResponse;
import fu.sep490.g23.backend.dto.response.course.LearningPathCourseResponse;
import fu.sep490.g23.backend.dto.response.course.LearningPathResponse;
import fu.sep490.g23.backend.dto.response.course.LearningPathOfferCourseResponse;
import fu.sep490.g23.backend.dto.response.course.LearningPathOfferResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.course.LearningPath;
import fu.sep490.g23.backend.entity.course.LearningPathCourse;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.entity.course.OnlineCourseEnrollment;
import fu.sep490.g23.backend.entity.course.enums.EnrollmentStatus;
import fu.sep490.g23.backend.entity.course.enums.PackageStatus;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.course.LearningPathCourseRepository;
import fu.sep490.g23.backend.repository.course.LearningPathRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseEnrollmentRepository;
import fu.sep490.g23.backend.service.course.LearningPathManagementService;
import fu.sep490.g23.backend.service.course.LearningPathRecommendationService;
import fu.sep490.g23.backend.service.assessment.PlacementRecommendationContext;
import fu.sep490.g23.backend.service.assessment.PlacementRecommendationContextFactory;
import fu.sep490.g23.backend.repository.assessment.PlacementTestAttemptRepository;
import fu.sep490.g23.backend.entity.assessment.PlacementTestAttempt;
import fu.sep490.g23.backend.entity.assessment.enums.PlacementEvaluationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class LearningPathManagementServiceImpl implements LearningPathManagementService {
    private final LearningPathRepository learningPathRepository;
    private final LearningPathCourseRepository learningPathCourseRepository;
    private final OnlineCourseRepository onlineCourseRepository;
    private final UserRepository userRepository;
    private final OnlineCourseEnrollmentRepository enrollmentRepository;
    private final PlacementTestAttemptRepository placementAttemptRepository;
    private final PlacementRecommendationContextFactory recommendationContextFactory;
    private final LearningPathRecommendationService learningPathRecommendationService;

    @Override
    @Transactional(readOnly = true)
    public Page<LearningPathResponse> getManagedPaths(Pageable pageable) {
        return learningPathRepository.findAll(pageable).map(this::toResponse);
    }

    @Override
    public LearningPathResponse createPath(LearningPathRequest request) {
        String code = required(request.getCode(), "Mã lộ trình");
        if (learningPathRepository.existsByCodeIgnoreCase(code)) {
            throw new RuntimeException("Mã lộ trình đã tồn tại.");
        }
        LearningPath path = learningPathRepository.save(LearningPath.builder()
                .code(code)
                .name(required(request.getName(), "Tên lộ trình"))
                .examCategory(normalizeExamCategory(request.getExamCategory()))
                .targetBand(request.getTargetBand())
                .targetScore(request.getTargetScore())
                .discountPercent(defaultDiscountPercent(request.getDiscountPercent()))
                .minimumCoursesForDiscount(defaultMinimumCourses(request.getMinimumCoursesForDiscount()))
                .build());
        validateTarget(path);
        return toResponse(path);
    }

    @Override
    public LearningPathResponse updatePath(Long pathId, LearningPathRequest request) {
        LearningPath path = findPath(pathId);
        String code = required(request.getCode(), "Mã lộ trình");
        learningPathRepository.findByCodeIgnoreCase(code)
                .filter(existing -> !existing.getId().equals(pathId))
                .ifPresent(existing -> { throw new RuntimeException("Mã lộ trình đã tồn tại."); });
        path.setCode(code);
        path.setName(required(request.getName(), "Tên lộ trình"));
        path.setExamCategory(normalizeExamCategory(request.getExamCategory()));
        path.setTargetBand(request.getTargetBand());
        path.setTargetScore(request.getTargetScore());
        path.setDiscountPercent(defaultDiscountPercent(request.getDiscountPercent()));
        path.setMinimumCoursesForDiscount(defaultMinimumCourses(request.getMinimumCoursesForDiscount()));
        validateTarget(path);
        return toResponse(learningPathRepository.save(path));
    }

    @Override
    public LearningPathResponse addCourses(Long pathId, LearningPathCoursesRequest request) {
        LearningPath path = findPath(pathId);
        Set<Long> ids = new LinkedHashSet<>(request.getCourseIds());
        List<LearningPathCourse> existing = learningPathCourseRepository
                .findByLearningPathIdOrderByDisplayOrderAscIdAsc(pathId);
        int nextOrder = existing.size() + 1;
        for (Long courseId : ids) {
            if (learningPathCourseRepository.existsByLearningPathIdAndOnlineCourseId(pathId, courseId)) {
                continue;
            }
            OnlineCourse course = onlineCourseRepository.findById(courseId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học."));
            learningPathCourseRepository.save(LearningPathCourse.builder()
                    .learningPath(path)
                    .onlineCourse(course)
                    .displayOrder(nextOrder++)
                    .build());
        }
        return toResponse(path);
    }

    @Override
    public LearningPathResponse reorderCourses(Long pathId, LearningPathCoursesRequest request) {
        List<LearningPathCourse> existing = learningPathCourseRepository
                .findByLearningPathIdOrderByDisplayOrderAscIdAsc(pathId);
        Set<Long> requestedIds = new LinkedHashSet<>(request.getCourseIds());
        Set<Long> existingIds = existing.stream()
                .map(ref -> ref.getOnlineCourse().getId())
                .collect(java.util.stream.Collectors.toSet());
        if (requestedIds.size() != existingIds.size() || !requestedIds.equals(existingIds)) {
            throw new RuntimeException("Danh sách sắp xếp không hợp lệ.");
        }
        Map<Long, LearningPathCourse> refsByCourseId = existing.stream()
                .collect(java.util.stream.Collectors.toMap(ref -> ref.getOnlineCourse().getId(), ref -> ref));
        int order = 1;
        for (Long courseId : requestedIds) {
            refsByCourseId.get(courseId).setDisplayOrder(order++);
        }
        learningPathCourseRepository.saveAll(existing);
        return toResponse(findPath(pathId));
    }

    @Override
    public void deletePath(Long pathId) {
        learningPathRepository.delete(findPath(pathId));
    }

    @Override
    @Transactional(readOnly = true)
    public LearnerLearningPathResponse getMyLearningPath(String studentEmail) {
        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy học viên."));
        PlacementTestAttempt attempt = placementAttemptRepository
                .findTopByStudentAndEvaluationStatusOrderBySubmittedAtDesc(student, PlacementEvaluationStatus.ELIGIBLE)
                .orElse(null);
        PlacementRecommendationContext context = attempt == null
                ? profileContext(student)
                : recommendationContextFactory.fromAttempt(student, attempt, attempt.getRecommendedLevel());
        LearnerLearningPathResponse.PathOverview path = learningPathRecommendationService.recommend(student, context, true);
        return LearnerLearningPathResponse.builder()
                .currentBand(student.getCurrentBand())
                .examType(context.getExamType())
                .currentScore(context.getOverallScore())
                .targetExam(student.getTargetExam())
                .targetScore(student.getTargetScore())
                .paths(path == null ? List.of() : List.of(path))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LearningPathOfferResponse> getPublicOffers(String studentEmail) {
        User student = optionalStudent(studentEmail);
        return learningPathRepository.findAll().stream()
                .sorted(Comparator.comparing(LearningPath::getName, String.CASE_INSENSITIVE_ORDER))
                .map(path -> toOfferResponse(path, student))
                .filter(offer -> offer.getTotalCourses() > 0)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LearningPathOfferResponse> getPublicOffers(String studentEmail, Pageable pageable) {
        User student = optionalStudent(studentEmail);
        return learningPathRepository.findPublicPaths(pageable)
                .map(path -> toOfferResponse(path, student));
    }

    @Override
    @Transactional(readOnly = true)
    public LearningPathOfferResponse getPublicOffer(String code, String studentEmail) {
        LearningPath path = learningPathRepository.findByCodeIgnoreCase(required(code, "Mã lộ trình"))
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lộ trình."));
        LearningPathOfferResponse offer = toOfferResponse(path, optionalStudent(studentEmail));
        if (offer.getTotalCourses() == 0) {
            throw new RuntimeException("Lộ trình chưa có khóa học đang mở bán.");
        }
        return offer;
    }

    private LearnerLearningPathResponse.PathOverview toLearnerPath(
            LearningPath path, Map<Long, OnlineCourseEnrollment> enrollmentsByPackageId) {
        List<LearningPathCourse> refs = learningPathCourseRepository.findByLearningPathIdOrderByDisplayOrderAscIdAsc(path.getId())
                .stream()
                .filter(ref -> ref.getOnlineCourse().getStatus() == PackageStatus.PUBLISHED
                        )
                .toList();
        boolean prerequisiteCompleted = true;
        List<LearnerLearningPathCourseResponse> courses = new ArrayList<>();
        Long currentStepCourseId = null;
        Long nextCourseId = null;
        for (LearningPathCourse ref : refs) {
            OnlineCourse course = ref.getOnlineCourse();
            OnlineCourseEnrollment enrollment = activeEnrollment(enrollmentsByPackageId.get(course.getId()));
            boolean completed = enrollment != null && (enrollment.getStatus() == EnrollmentStatus.COMPLETED
                    || defaultInt(enrollment.getProgressPercent()) >= 100);
            boolean accessible = enrollment != null || prerequisiteCompleted;
            if (currentStepCourseId == null && accessible && !completed) {
                currentStepCourseId = course.getId();
                nextCourseId = enrollment == null ? course.getId() : null;
            }
            courses.add(LearnerLearningPathCourseResponse.builder()
                    .courseId(course.getId())
                    .slug(course.getSlug())
                    .title(course.getTitle())
                    .thumbnailUrl(course.getThumbnailUrl())
                    .learningPathOrder(ref.getDisplayOrder())
                    .enrollmentStatus(enrollment == null ? "NOT_ENROLLED" : enrollment.getStatus().name())
                    .progressPercent(enrollment == null ? 0 : defaultInt(enrollment.getProgressPercent()))
                    .completed(completed)
                    .lockedReason(accessible ? null : "Hoàn thành khóa học trước để mở giai đoạn này.")
                    .build());
            prerequisiteCompleted = prerequisiteCompleted && completed;
        }
        return LearnerLearningPathResponse.PathOverview.builder()
                .code(path.getCode())
                .name(path.getName())
                .totalCourses(courses.size())
                .completedCourses((int) courses.stream().filter(LearnerLearningPathCourseResponse::isCompleted).count())
                .currentStepCourseId(currentStepCourseId)
                .nextCourseId(nextCourseId)
                .courses(courses)
                .build();
    }

    private LearningPath findPath(Long pathId) {
        return learningPathRepository.findById(pathId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lộ trình."));
    }

    private LearningPathResponse toResponse(LearningPath path) {
        List<LearningPathCourseResponse> courses = learningPathCourseRepository
                .findByLearningPathIdOrderByDisplayOrderAscIdAsc(path.getId()).stream()
                .map(ref -> LearningPathCourseResponse.builder()
                        .courseId(ref.getOnlineCourse().getId())
                        .slug(ref.getOnlineCourse().getSlug())
                        .title(ref.getOnlineCourse().getTitle())
                        .thumbnailUrl(ref.getOnlineCourse().getThumbnailUrl())
                        .targetOutcome(ref.getOnlineCourse().getTargetOutcome())
                        .displayOrder(ref.getDisplayOrder())
                        .build())
                .toList();
        return LearningPathResponse.builder()
                .id(path.getId())
                .code(path.getCode())
                .name(path.getName())
                .examCategory(path.getExamCategory())
                .targetBand(path.getTargetBand())
                .targetScore(path.getTargetScore())
                .discountPercent(defaultDiscountPercent(path.getDiscountPercent()))
                .minimumCoursesForDiscount(defaultMinimumCourses(path.getMinimumCoursesForDiscount()))
                .courses(courses)
                .build();
    }

    private LearningPathOfferResponse toOfferResponse(LearningPath path, User student) {
        Set<Long> ownedPackageIds = student == null
                ? Set.of()
                : enrollmentRepository.findByStudentOrderByRegisteredAtDesc(student).stream()
                        .filter(enrollment -> enrollment.getStatus() == EnrollmentStatus.ACTIVE
                                || enrollment.getStatus() == EnrollmentStatus.COMPLETED)
                        .map(enrollment -> enrollment.getOnlineCourse().getId())
                        .collect(java.util.stream.Collectors.toSet());
        List<LearningPathOfferCourseResponse> courses = learningPathCourseRepository
                .findByLearningPathIdOrderByDisplayOrderAscIdAsc(path.getId()).stream()
                .filter(ref -> ref.getOnlineCourse() != null
                        && (ref.getOnlineCourse().getStatus() == fu.sep490.g23.backend.entity.course.enums.PackageStatus.PUBLISHED))
                .map(ref -> {
                    OnlineCourse course = ref.getOnlineCourse();
                    BigDecimal originalPrice = safePrice(course.getPrice());
                    BigDecimal currentPrice = resolveCurrentPrice(originalPrice, course.getSalePrice());
                    return LearningPathOfferCourseResponse.builder()
                            .courseId(course.getId())
                            .slug(course.getSlug())
                            .title(course.getTitle())
                            .thumbnailUrl(course.getThumbnailUrl())
                            .shortDescription(course.getShortDescription())
                            .displayOrder(ref.getDisplayOrder())
                            .originalPrice(originalPrice)
                            .currentPrice(currentPrice)
                            .owned(ownedPackageIds.contains(course.getId()))
                            .build();
                })
                .toList();
        List<LearningPathOfferCourseResponse> remaining = courses.stream()
                .filter(course -> !course.isOwned())
                .toList();
        long originalAmount = remaining.stream().mapToLong(course -> toVnd(course.getOriginalPrice())).sum();
        long subtotalAmount = remaining.stream().mapToLong(course -> toVnd(course.getCurrentPrice())).sum();
        int discountPercent = defaultDiscountPercent(path.getDiscountPercent());
        int minimumCourses = defaultMinimumCourses(path.getMinimumCoursesForDiscount());
        boolean discountApplied = discountPercent > 0 && remaining.size() >= minimumCourses;
        long pathDiscount = discountApplied
                ? BigDecimal.valueOf(subtotalAmount)
                        .multiply(BigDecimal.valueOf(discountPercent))
                        .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                        .longValue()
                : 0L;
        return LearningPathOfferResponse.builder()
                .id(path.getId())
                .code(path.getCode())
                .name(path.getName())
                .examCategory(path.getExamCategory())
                .targetBand(path.getTargetBand())
                .targetScore(path.getTargetScore())
                .discountPercent(discountPercent)
                .minimumCoursesForDiscount(minimumCourses)
                .totalCourses(courses.size())
                .ownedCourses(courses.size() - remaining.size())
                .remainingCourses(remaining.size())
                .originalAmount(originalAmount)
                .subtotalAmount(subtotalAmount)
                .learningPathDiscountAmount(pathDiscount)
                .totalAmount(Math.max(0L, subtotalAmount - pathDiscount))
                .discountApplied(discountApplied)
                .purchaseAvailable(!remaining.isEmpty())
                .courses(courses)
                .build();
    }

    private User optionalStudent(String studentEmail) {
        if (studentEmail == null || studentEmail.isBlank()) return null;
        return userRepository.findByEmail(studentEmail).orElse(null);
    }

    private BigDecimal safePrice(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.max(BigDecimal.ZERO);
    }

    private BigDecimal resolveCurrentPrice(BigDecimal originalPrice, BigDecimal salePrice) {
        if (salePrice == null || salePrice.compareTo(BigDecimal.ZERO) < 0 || salePrice.compareTo(originalPrice) >= 0) {
            return originalPrice;
        }
        return salePrice;
    }

    private long toVnd(BigDecimal value) {
        return safePrice(value).setScale(0, RoundingMode.HALF_UP).longValue();
    }

    private OnlineCourseEnrollment activeEnrollment(OnlineCourseEnrollment enrollment) {
        return enrollment == null || enrollment.getStatus() == EnrollmentStatus.CANCELLED ? null : enrollment;
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private int defaultDiscountPercent(Integer value) {
        return value == null ? 0 : value;
    }

    private int defaultMinimumCourses(Integer value) {
        return value == null ? 2 : Math.max(2, value);
    }

    private String required(String value, String field) {
        String result = value == null ? "" : value.trim();
        if (result.isEmpty()) throw new RuntimeException(field + " không được để trống.");
        return result;
    }

    private String normalizeExamCategory(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("IELTS", "TOEIC").contains(normalized)) {
            throw new IllegalArgumentException("Kỳ thi của lộ trình chỉ hỗ trợ IELTS hoặc TOEIC.");
        }
        return normalized;
    }

    private void validateTarget(LearningPath path) {
        if ("IELTS".equals(path.getExamCategory())) {
            if (path.getTargetScore() != null) throw new IllegalArgumentException("Lộ trình IELTS không sử dụng điểm TOEIC.");
            if (path.getTargetBand() != null && path.getTargetBand().remainder(BigDecimal.valueOf(0.5)).compareTo(BigDecimal.ZERO) != 0) {
                throw new IllegalArgumentException("Band mục tiêu IELTS phải tăng theo bước 0.5.");
            }
        } else if ("TOEIC".equals(path.getExamCategory())) {
            if (path.getTargetBand() != null) throw new IllegalArgumentException("Lộ trình TOEIC không sử dụng band IELTS.");
            if (path.getTargetScore() != null && path.getTargetScore() % 5 != 0) {
                throw new IllegalArgumentException("Điểm mục tiêu TOEIC phải tăng theo bước 5.");
            }
        } else if (path.getTargetBand() != null || path.getTargetScore() != null) {
            throw new IllegalArgumentException("Hãy chọn kỳ thi trước khi nhập mục tiêu.");
        }
    }

    private PlacementRecommendationContext profileContext(User student) {
        String rawExam = student.getTargetExam() == null ? "" : student.getTargetExam().trim().toUpperCase(Locale.ROOT);
        String exam = Set.of("IELTS", "TOEIC").contains(rawExam) ? rawExam : "IELTS";
        BigDecimal target = parseDecimal(student.getTargetScore());
        return PlacementRecommendationContext.builder()
                .learnerId(student.getId())
                .examType(exam)
                .overallScore(student.getCurrentBand() == null ? null : BigDecimal.valueOf(student.getCurrentBand()))
                .targetExam(exam)
                .targetScore(target)
                .weakSkills(Set.of())
                .build();
    }

    private BigDecimal parseDecimal(String value) {
        if (value == null) return null;
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(\\d+(?:\\.\\d+)?)").matcher(value);
        return matcher.find() ? new BigDecimal(matcher.group(1)) : null;
    }
}
