package fu.sep490.g23.backend.service.course.impl;
import fu.sep490.g23.backend.service.course.TranscriptSegmentNormalizer;
import fu.sep490.g23.backend.entity.course.enums.FlashcardPracticeSource;
import fu.sep490.g23.backend.entity.course.enums.CourseLevel;
import fu.sep490.g23.backend.entity.course.enums.EnrollmentStatus;
import fu.sep490.g23.backend.entity.course.CourseCategory;
import fu.sep490.g23.backend.entity.course.CourseLessonFlashcardRef;
import fu.sep490.g23.backend.entity.course.enums.CourseVersionStatus;
import fu.sep490.g23.backend.entity.course.LessonProgress;
import fu.sep490.g23.backend.entity.course.OnlineCourseVersion;
import fu.sep490.g23.backend.entity.course.enums.LessonProgressStatus;
import fu.sep490.g23.backend.entity.course.OnlineCourseModule;
import fu.sep490.g23.backend.service.course.CourseProgressService;
import fu.sep490.g23.backend.entity.course.VocabularyProgress;
import fu.sep490.g23.backend.service.course.OnlineCoursePreviewValidator;
import fu.sep490.g23.backend.service.course.BunnyStreamService;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.entity.course.OnlineCourseEnrollment;
import fu.sep490.g23.backend.entity.course.enums.PackageStatus;
import fu.sep490.g23.backend.entity.course.OnlineLesson;
import fu.sep490.g23.backend.service.course.YouTubeTranscriptService;
import fu.sep490.g23.backend.service.course.CourseEnrollmentAccessPolicy;
import fu.sep490.g23.backend.service.course.FlashcardPracticeService;
import fu.sep490.g23.backend.entity.course.enums.VocabularyProgressStatus;
import fu.sep490.g23.backend.service.course.OnlineCourseService;
import fu.sep490.g23.backend.service.course.BalancedCourseRecommendationSelector;
import fu.sep490.g23.backend.repository.course.VocabularyProgressRepository;
import fu.sep490.g23.backend.repository.course.CourseCategoryRepository;
import fu.sep490.g23.backend.repository.course.CourseLessonFlashcardRefRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseEnrollmentRepository;
import fu.sep490.g23.backend.service.course.OnlineCourseVersionService;
import fu.sep490.g23.backend.repository.course.OnlineCourseVersionRepository;
import fu.sep490.g23.backend.service.course.OnlineCourseMapper;
import fu.sep490.g23.backend.repository.course.OnlineLessonRepository;
import fu.sep490.g23.backend.repository.course.LessonProgressRepository;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fu.sep490.g23.backend.dto.request.assessment.ContentManagerCourseAssessmentRequest;
import fu.sep490.g23.backend.dto.request.course.LessonRequest;
import fu.sep490.g23.backend.dto.request.course.ModuleRequest;
import fu.sep490.g23.backend.dto.request.course.OnlineCourseRequest;
import fu.sep490.g23.backend.dto.request.course.LessonOrderItemRequest;
import fu.sep490.g23.backend.dto.request.course.ModuleOrderItemRequest;
import fu.sep490.g23.backend.dto.request.course.ReorderLessonsRequest;
import fu.sep490.g23.backend.dto.request.course.ReorderModulesRequest;
import fu.sep490.g23.backend.dto.request.course.LearningPathOrderRequest;
import fu.sep490.g23.backend.dto.request.course.TranscriptSegmentRequest;
import fu.sep490.g23.backend.dto.response.assessment.AiAssessmentSubmissionResponse;
import fu.sep490.g23.backend.dto.response.assessment.AssessmentRubricResponse;
import fu.sep490.g23.backend.dto.response.assessment.CourseAssessmentResponse;
import fu.sep490.g23.backend.dto.response.assessment.RubricCriterionResponse;
import fu.sep490.g23.backend.dto.response.course.BunnyVideoUploadResponse;
import fu.sep490.g23.backend.dto.response.course.CourseCertificateResponse;
import fu.sep490.g23.backend.dto.response.course.CourseCompletionResponse;
import fu.sep490.g23.backend.dto.response.course.CourseStatsResponse;
import fu.sep490.g23.backend.dto.response.course.LessonResponse;
import fu.sep490.g23.backend.dto.response.course.OnlineCourseResponse;
import fu.sep490.g23.backend.dto.response.course.OnlineCoursePreviewResponse;
import fu.sep490.g23.backend.dto.response.course.OnlineCourseEnrollmentResponse;
import fu.sep490.g23.backend.dto.response.course.TranscriptSegmentResponse;
import fu.sep490.g23.backend.dto.response.course.VocabularyTermResponse;
import fu.sep490.g23.backend.dto.response.course.LearnerLearningPathCourseResponse;
import fu.sep490.g23.backend.dto.response.course.LearnerLearningPathResponse;
import fu.sep490.g23.backend.dto.response.curriculum.FlashcardSetResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.assessment.AssessmentRubric;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sep490.g23.backend.entity.assessment.CourseAssessment;
import fu.sep490.g23.backend.entity.assessment.PlacementTestAttempt;
import fu.sep490.g23.backend.service.assessment.IeltsBandScale;
import fu.sep490.g23.backend.service.assessment.PlacementRecommendationContext;
import fu.sep490.g23.backend.service.assessment.PlacementRecommendationContextFactory;
import fu.sep490.g23.backend.entity.assessment.RubricCriterion;
import fu.sep490.g23.backend.entity.course.*;
import fu.sep490.g23.backend.entity.curriculum.AssessmentBankItem;
import fu.sep490.g23.backend.entity.curriculum.ContentBankItem;
import fu.sep490.g23.backend.entity.curriculum.enums.ContentBankType;
import fu.sep490.g23.backend.service.curriculum.ContentBankIdResolver;
import fu.sep490.g23.backend.service.curriculum.ContentBankLinkSync;
import fu.sep490.g23.backend.service.curriculum.ContentBankPayloadSupport;
import fu.sep490.g23.backend.service.curriculum.ContentBankTypeGuard;
import fu.sep490.g23.backend.exception.CourseUnavailableException;
import fu.sep490.g23.backend.security.ContentManagementRolePolicy;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.assessment.AssessmentRubricRepository;
import fu.sep490.g23.backend.repository.assessment.AssessmentSubmissionRepository;
import fu.sep490.g23.backend.repository.assessment.CourseAssessmentRepository;
import fu.sep490.g23.backend.repository.assessment.PlacementTestAttemptRepository;
import fu.sep490.g23.backend.repository.curriculum.AssessmentBankItemRepository;
import fu.sep490.g23.backend.repository.curriculum.FlashcardSetRepository;
import fu.sep490.g23.backend.service.course.*;
import fu.sep490.g23.backend.service.mail.CourseEnrollmentMailService;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.EnumMap;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional
public class OnlineCourseServiceImpl implements OnlineCourseService {

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
    private static final Pattern VOCABULARY_HEADING = Pattern.compile("(?m)^###\\s+\\d+\\.\\s+(.+)$");
    private static final Pattern CERTIFICATE_CODE_PATTERN = Pattern.compile("^ELC-(\\d+)-(\\d+)-([A-F0-9]+)$");
    private static final Pattern BAND_NUMBER_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)");
    private static final Set<String> ENGLISH_COURSE_CATEGORIES = Set.of(
            "IELTS",
            "TOEIC",
            "COMMUNICATION",
            "FOUNDATION"
    );

    private final OnlineCourseRepository onlineCourseRepository;
    private final OnlineCourseVersionRepository onlineCourseVersionRepository;
    private final CourseCategoryRepository courseCategoryRepository;
    private final OnlineCourseEnrollmentRepository enrollmentRepository;
    private final OnlineLessonRepository lessonRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final VocabularyProgressRepository vocabularyProgressRepository;
    private final CourseAssessmentRepository courseAssessmentRepository;
    private final AssessmentBankItemRepository assessmentBankItemRepository;
    private final FlashcardSetRepository flashcardSetRepository;
    private final CourseLessonFlashcardRefRepository courseLessonFlashcardRefRepository;
    private final AssessmentRubricRepository assessmentRubricRepository;
    private final AssessmentSubmissionRepository assessmentSubmissionRepository;
    private final PlacementTestAttemptRepository placementTestAttemptRepository;
    private final PlacementRecommendationContextFactory placementRecommendationContextFactory;
    private final UserRepository userRepository;
    private final OnlineCourseMapper mapper;
    private final OnlineCourseVersionService onlineCourseVersionService;
    private final OnlineCoursePreviewValidator previewValidator;
    private final BunnyStreamService bunnyStreamService;
    private final CourseProgressService courseProgressService;
    private final CourseEnrollmentAccessPolicy courseEnrollmentAccessPolicy;
    private final FlashcardPracticeService flashcardPracticeService;
    private final CourseEnrollmentMailService courseEnrollmentMailService;
    private final YouTubeTranscriptService youTubeTranscriptService;
    private final ContentBankIdResolver contentBankIdResolver;
    private final ContentBankTypeGuard contentBankTypeGuard;
    private final ContentBankLinkSync contentBankLinkSync;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(readOnly = true)
    public Page<OnlineCourseResponse> getPublicCourses(String keyword, String category, Double currentBand, Double targetBand, Integer targetScore, AssessmentSkill skill, String promotion, Pageable pageable) {
        Specification<OnlineCourse> specification = courseSpec(clean(keyword), category, currentBand, targetBand, skill, null, PackageStatus.PUBLISHED);
        if (targetScore != null) {
            specification = specification.and((root, query, criteriaBuilder) -> criteriaBuilder.greaterThanOrEqualTo(
                    criteriaBuilder.toInteger(root.get("targetScore")),
                    targetScore
            ));
        }
        if ("promotion".equalsIgnoreCase(promotion)) {
            specification = specification.and((root, query, criteriaBuilder) -> criteriaBuilder.and(
                    criteriaBuilder.isNotNull(root.get("salePrice")),
                    criteriaBuilder.lessThan(root.get("salePrice"), root.get("price")),
                    criteriaBuilder.greaterThan(root.get("salePrice"), BigDecimal.ZERO)
            ));
        } else if ("standard".equalsIgnoreCase(promotion)) {
            specification = specification.and((root, query, criteriaBuilder) -> criteriaBuilder.or(
                    criteriaBuilder.isNull(root.get("salePrice")),
                    criteriaBuilder.greaterThanOrEqualTo(root.get("salePrice"), root.get("price")),
                    criteriaBuilder.lessThanOrEqualTo(root.get("salePrice"), BigDecimal.ZERO)
            ));
        }
        return onlineCourseRepository.findAll(specification, pageable)
                .map(course -> onlineCourseVersionService.readPublishedSnapshot(course, false));
    }

    @Override
    @Transactional(readOnly = true)
    public OnlineCourseResponse getPublicCourse(String slugOrId) {
        return onlineCourseVersionService.readPublishedSnapshot(findPublicCourse(slugOrId), false);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseCertificateResponse verifyCourseCertificate(String verificationCode) {
        OnlineCourseEnrollment enrollment = findEnrollmentByCertificateCode(verificationCode);
        OnlineCourse course = java.util.Optional.of(enrollment.getOnlineCourse())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học cho chứng nhận này."));
        User student = enrollment.getStudent();
        CourseCompletionResponse completion = courseProgressService.buildCompletionResponse(enrollment, course, student);

        if (!completion.isEligibleForCertificate()) {
            throw new RuntimeException("Chứng nhận này chưa đủ điều kiện xác thực.");
        }

        return buildCertificateResponse(course, enrollment, student, completion, true);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OnlineCourseResponse> getManagerCourses(String keyword, String category, CourseLevel level, PackageStatus status, Set<Long> excludedIds, Pageable pageable) {
        Specification<OnlineCourse> specification = courseSpec(clean(keyword), category, null, null, null, level, status);
        if (excludedIds != null && !excludedIds.isEmpty()) {
            specification = specification.and((root, query, criteriaBuilder) -> root.get("id").in(excludedIds).not());
        }
        return onlineCourseRepository.findAll(specification, pageable)
                .map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public OnlineCourseResponse getManagerCourse(String slugOrId) {
        return mapper.toResponse(findManagerCourse(slugOrId));
    }

    @Override
    @Transactional(readOnly = true)
    public OnlineCoursePreviewResponse getManagerCoursePreview(String slugOrId) {
        OnlineCourse course = findManagerCourse(slugOrId);
        OnlineCourseResponse courseResponse = mapper.toResponse(course);
        List<CourseAssessmentResponse> assessments = courseAssessmentRepository
                .findByOnlineCourseAndActiveTrueOrderByDisplayOrderAscIdAsc(course).stream()
                .map(this::toManagerAssessmentResponse)
                .toList();
        List<fu.sep490.g23.backend.dto.response.course.ModuleResponse> modules = List.copyOf(courseResponse.getModules());
        var warnings = previewValidator.validate(courseResponse, assessments);
        courseResponse.setModules(List.of());

        return OnlineCoursePreviewResponse.builder()
                .course(courseResponse)
                .modules(modules)
                .assessments(assessments)
                .validationWarnings(warnings)
                .previewMode(true)
                .build();
    }

    @Override
    public List<fu.sep490.g23.backend.dto.response.course.ModuleResponse> reorderModules(
            Long courseId,
            ReorderModulesRequest request,
            String actorEmail
    ) {
        assertCanEditCourseContent(actorEmail);
        OnlineCourse course = findCourse(courseId);
        onlineCourseVersionService.assertEditableDraft(course, actorEmail);
        OnlineCourseVersion editableVersion = onlineCourseVersionService.requireEditableVersion(course);
        List<OnlineCourseModule> modules = new ArrayList<>(editableVersion.getModules());
        validateModuleOrder(modules, request.getItems());

        moveModuleOrdersToTemporaryRange(modules);
        onlineCourseRepository.flush();
        Map<Long, OnlineCourseModule> modulesById = modules.stream()
                .collect(java.util.stream.Collectors.toMap(OnlineCourseModule::getId, module -> module));
        request.getItems().forEach(item -> modulesById.get(item.getModuleId()).setDisplayOrder(item.getOrderIndex()));
        onlineCourseRepository.flush();
        onlineCourseVersionService.synchronizeDraftSnapshot(course);

        return mapper.toResponse(course).getModules();
    }

    @Override
    public List<LessonResponse> reorderLessons(
            Long courseId,
            Long moduleId,
            ReorderLessonsRequest request,
            String actorEmail
    ) {
        assertCanEditCourseContent(actorEmail);
        OnlineCourse course = findCourse(courseId);
        onlineCourseVersionService.assertEditableDraft(course, actorEmail);
        OnlineCourseVersion editableVersion = onlineCourseVersionService.requireEditableVersion(course);
        OnlineCourseModule module = editableVersion.getModules().stream()
                .filter(item -> moduleId.equals(item.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Mô-đun không thuộc khóa học này."));
        List<OnlineLesson> lessons = new ArrayList<>(module.getLessons());
        validateLessonOrder(lessons, request.getItems());

        for (int index = 0; index < lessons.size(); index++) {
            lessons.get(index).setDisplayOrder(-(index + 1));
        }
        onlineCourseRepository.flush();
        Map<Long, OnlineLesson> lessonsById = lessons.stream()
                .collect(java.util.stream.Collectors.toMap(OnlineLesson::getId, lesson -> lesson));
        request.getItems().forEach(item -> lessonsById.get(item.getLessonId()).setDisplayOrder(item.getOrderIndex()));
        onlineCourseRepository.flush();
        onlineCourseVersionService.synchronizeDraftSnapshot(course);

        return mapper.toResponse(course).getModules().stream()
                .filter(item -> moduleId.equals(item.getId()))
                .findFirst()
                .map(fu.sep490.g23.backend.dto.response.course.ModuleResponse::getLessons)
                .orElseThrow(() -> new IllegalStateException("Không thể đọc lại thứ tự bài học."));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseAssessmentResponse> getManagerCourseAssessments(Long courseId) {
        OnlineCourse course = findCourse(courseId);
        return courseAssessmentRepository.findByOnlineCourseAndActiveTrueOrderByDisplayOrderAscIdAsc(course).stream()
                .map(this::toManagerAssessmentResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssessmentRubricResponse> getManagerAssessmentRubrics() {
        return assessmentRubricRepository.findAll().stream()
                .filter(AssessmentRubric::isActive)
                .sorted(Comparator
                        .comparing((AssessmentRubric rubric) -> rubric.getSkill() == null ? "" : rubric.getSkill().name())
                        .thenComparing(AssessmentRubric::getId))
                .map(this::toRubricResponse)
                .toList();
    }

    @Override
    public List<CourseAssessmentResponse> saveManagerCourseAssessments(Long courseId, List<ContentManagerCourseAssessmentRequest> requests) {
        return saveManagerCourseAssessments(courseId, requests, null);
    }

    @Override
    public List<CourseAssessmentResponse> saveManagerCourseAssessments(
            Long courseId,
            List<ContentManagerCourseAssessmentRequest> requests,
            String actorEmail
    ) {
        OnlineCourse course = findCourse(courseId);
        onlineCourseVersionService.assertEditableDraft(course, actorEmail);
        synchronizeAssessments(course, requests == null ? List.of() : requests);
        OnlineCourse savedCourse = onlineCourseRepository.save(course);
        onlineCourseVersionService.synchronizeDraftSnapshot(savedCourse);
        return courseAssessmentRepository.findByOnlineCourseAndActiveTrueOrderByDisplayOrderAscIdAsc(course).stream()
                .map(this::toManagerAssessmentResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CourseStatsResponse getStats() {
        Map<String, Long> categoryDistribution = new java.util.LinkedHashMap<>();
        onlineCourseRepository.summarizeCategoryDistribution().forEach(row ->
                categoryDistribution.put(String.valueOf(row[0]), ((Number) row[1]).longValue()));
        return CourseStatsResponse.builder()
                .totalCourses(onlineCourseRepository.countByDeletedFalse())
                .publishedCourses(onlineCourseRepository.countByDeletedFalseAndStatus(PackageStatus.PUBLISHED))
                .draftCourses(onlineCourseRepository.countByDeletedFalseAndStatus(PackageStatus.DRAFT))
                .archivedCourses(onlineCourseRepository.countByDeletedFalseAndStatus(PackageStatus.ARCHIVED))
                .totalLessons(lessonRepository.countActiveLessons())
                .totalEnrollments(enrollmentRepository.count())
                .categoryDistribution(categoryDistribution)
                .build();
    }

    @Override
    public OnlineCourseResponse createCourse(OnlineCourseRequest request, String creatorEmail) {
        validateCourseRequest(request);
        User creator = userRepository.findByEmail(creatorEmail).orElse(null);

        CourseCategory category = courseCategoryRepository.findByCode(normalizeCategoryCode(request.getCategory()))
                .orElseThrow(() -> new RuntimeException("Course category not found"));
        if (!category.isActive()) {
            throw new IllegalArgumentException("Danh mục khóa học đã ngừng hoạt động.");
        }

        String slug = generateUniqueSlug(request.getTitle());
        BigDecimal price = defaultBigDecimal(request.getPrice());
        BigDecimal salePrice = resolveSalePrice(request.getPrice(), request.getSalePrice());

        OnlineCourse course = OnlineCourse.builder()
                .category(category)
                .level(request.getLevel())
                .recommendedCurrentBandMin(request.getRecommendedCurrentBandMin())
                .targetBand(request.getTargetBand())
                .learningPathCode(request.getLearningPathCode())
                .learningPathName(request.getLearningPathName())
                .learningPathOrder(request.getLearningPathOrder())
                .targetOutcome(request.getTargetOutcome())
                .recommendedNextCourseSlug(request.getRecommendedNextCourseSlug())
                .title(request.getTitle().trim())
                .slug(slug)
                .shortDescription(request.getShortDescription())
                .description(request.getDescription())
                .targetScore(request.getTargetScore())
                .duration(request.getDuration())
                .studyMode("Online")
                .price(price)
                .salePrice(salePrice)
                .thumbnailUrl(request.getThumbnailUrl())
                .status(PackageStatus.DRAFT)
                .displayOrder(defaultInt(request.getDisplayOrder()))
                .featured(Boolean.TRUE.equals(request.getFeatured()))
                .createdBy(creator)
                .build();
        OnlineCourse savedCourse = onlineCourseRepository.save(course);
        onlineCourseVersionService.createDraft(savedCourse.getId(), null, creatorEmail);
        OnlineCourseVersion editableVersion = onlineCourseVersionService.requireEditableVersion(savedCourse);
        rebuildModules(savedCourse, editableVersion, request.getModules());
        refreshCourseTotals(savedCourse, editableVersion.getModules());
        onlineCourseVersionRepository.save(editableVersion);
        onlineCourseVersionService.synchronizeDraftSnapshot(savedCourse);
        return mapper.toResponse(savedCourse);
    }

    @Override
    public OnlineCourseResponse updateCourse(Long id, OnlineCourseRequest request) {
        return updateCourse(id, request, null);
    }

    @Override
    public OnlineCourseResponse updateCourse(Long id, OnlineCourseRequest request, String actorEmail) {
        validateCourseRequest(request);
        OnlineCourse course = findCourse(id);
        onlineCourseVersionService.assertEditableDraft(course, actorEmail);

        CourseCategory category = courseCategoryRepository.findByCode(normalizeCategoryCode(request.getCategory()))
                .orElseThrow(() -> new RuntimeException("Course category not found"));
        if (!category.isActive()
                && (course.getCategory() == null || !category.getId().equals(course.getCategory().getId()))) {
            throw new IllegalArgumentException("Danh mục khóa học đã ngừng hoạt động.");
        }

        course.setTitle(request.getTitle().trim());
        course.setShortDescription(request.getShortDescription());
        course.setDescription(request.getDescription());
        course.setTargetScore(request.getTargetScore());
        course.setDuration(request.getDuration());
        course.setStudyMode("Online");
        course.setPrice(defaultBigDecimal(request.getPrice()));
        course.setSalePrice(resolveSalePrice(request.getPrice(), request.getSalePrice()));
        course.setThumbnailUrl(request.getThumbnailUrl());
        course.setDisplayOrder(defaultInt(request.getDisplayOrder()));
        course.setFeatured(Boolean.TRUE.equals(request.getFeatured()));
        course.setCategory(category);
        course.setLevel(request.getLevel());
        course.setRecommendedCurrentBandMin(request.getRecommendedCurrentBandMin());
        course.setTargetBand(request.getTargetBand());
        course.setLearningPathCode(request.getLearningPathCode());
        course.setLearningPathName(request.getLearningPathName());
        course.setLearningPathOrder(request.getLearningPathOrder());
        course.setTargetOutcome(request.getTargetOutcome());
        course.setRecommendedNextCourseSlug(request.getRecommendedNextCourseSlug());
        
        OnlineCourseVersion editableVersion = onlineCourseVersionService.requireEditableVersion(course);
        moveContentOrdersToTemporaryRange(editableVersion.getModules());
        onlineCourseRepository.flush();
        synchronizeModules(course, editableVersion, request.getModules());
        refreshCourseTotals(course, editableVersion.getModules());
        OnlineCourse savedCourse = onlineCourseRepository.save(course);
        onlineCourseVersionRepository.save(editableVersion);
        onlineCourseVersionService.synchronizeDraftSnapshot(savedCourse);
        return mapper.toResponse(savedCourse);
    }

    @Override
    public OnlineCourseResponse publishCourse(Long id, String actorEmail) {
        OnlineCourse course = findCourse(id);
        OnlineCourseVersion draftVersion = onlineCourseVersionRepository
                .findFirstByOnlineCourseAndStatusOrderByVersionNumberDesc(course, CourseVersionStatus.DRAFT)
                .orElseGet(() -> onlineCourseVersionRepository
                        .findFirstByOnlineCourseAndStatusOrderByVersionNumberDesc(
                                course,
                                CourseVersionStatus.PENDING_REVIEW
                        )
                        .orElseThrow(() -> new IllegalStateException(
                                "Khóa học chưa có phiên bản nháp để xuất bản."
                        )));
        validatePublishableCourse(course);
        onlineCourseVersionService.publish(course.getId(), draftVersion.getId(), actorEmail);
        return mapper.toResponse(course);
    }

    @Override
    public OnlineCourseResponse archiveCourse(Long id) {
        OnlineCourse course = findCourse(id);
        course.setStatus(PackageStatus.ARCHIVED);
        course.setStatus(PackageStatus.ARCHIVED);
        return mapper.toResponse(course);
    }

    @Override
    public void deleteCourse(Long id) {
        OnlineCourse course = findCourse(id);
        course.setDeleted(true);
        course.setStatus(PackageStatus.ARCHIVED);
        course.setDeleted(true);
        course.setStatus(PackageStatus.ARCHIVED);
    }

    @Override
    public BunnyVideoUploadResponse uploadLessonVideo(Long courseId, Long lessonId, String title, MultipartFile file) {
        return uploadLessonVideo(courseId, lessonId, title, file, null);
    }

    @Override
    public BunnyVideoUploadResponse uploadLessonVideo(
            Long courseId,
            Long lessonId,
            String title,
            MultipartFile file,
            String actorEmail
    ) {
        OnlineCourse course = findCourse(courseId);
        onlineCourseVersionService.assertEditableDraft(course, actorEmail);
        OnlineLesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("OnlineLesson not found"));

        if (lesson.getModule() == null
                || lesson.getModule().getOnlineCourse() == null
                || !course.getId().equals(lesson.getModule().getOnlineCourse().getId())) {
            throw new RuntimeException("OnlineLesson does not belong to this course");
        }

        BunnyVideoUploadResponse upload = bunnyStreamService.uploadVideo(file, title == null || title.isBlank() ? lesson.getTitle() : title);
        lesson.setVideoUrl(upload.getEmbedUrl());
        lesson.setBunnyVideoId(upload.getVideoId());
        lesson.setBunnyLibraryId(upload.getLibraryId());
        lesson.setBunnyCdnUrl(upload.getCdnUrl());
        lesson.setContentType("video");
        lesson.setTranscriptSegmentsJson(null);

        OnlineLesson savedLesson = lessonRepository.save(lesson);
        onlineCourseVersionService.synchronizeDraftSnapshot(course);
        upload.setLesson(toLessonResponse(savedLesson));
        return upload;
    }

    @Override
    public OnlineCourseResponse refreshLessonTranscript(Long courseId, Long lessonId) {
        return refreshLessonTranscript(courseId, lessonId, null);
    }

    @Override
    public OnlineCourseResponse refreshLessonTranscript(Long courseId, Long lessonId, String actorEmail) {
        OnlineCourse course = findCourse(courseId);
        onlineCourseVersionService.assertEditableDraft(course, actorEmail);
        OnlineLesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài học."));

        if (lesson.getModule() == null
                || lesson.getModule().getOnlineCourse() == null
                || !course.getId().equals(lesson.getModule().getOnlineCourse().getId())) {
            throw new RuntimeException("Bài học không thuộc khóa học này.");
        }

        List<TranscriptSegmentResponse> segments = resolveAutoTranscriptSegments(lesson);
        if (segments.isEmpty()) {
            if (canAutoFetchTranscript(lesson)) {
                throw new IllegalArgumentException("Chưa lấy được bản chép lời từ video này. Với Bunny, caption có thể vẫn đang xử lý — thử lại sau vài phút.");
            }
            throw new IllegalArgumentException("Chỉ lấy tự động từ YouTube hoặc video Bunny. Hãy dán link phù hợp hoặc tải video lên hệ thống trước.");
        }
        lesson.setTranscriptSegmentsJson(writeTranscriptSegments(segments));
        lessonRepository.save(lesson);
        onlineCourseVersionService.synchronizeDraftSnapshot(course);
        return mapper.toResponse(findCourse(courseId));
    }

    @Override
    public OnlineCourseResponse registerCourse(Long courseId, String studentEmail) {
        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        OnlineCourse course = findPublishedCourseForEnrollment(courseId);
        if (!isFreeCourse(course)) {
            throw new IllegalStateException("Khóa học trả phí chỉ được kích hoạt sau khi thanh toán thành công.");
        }
        return activateEnrollment(course, student);
    }

    @Override
    @Transactional(readOnly = true)
    public OnlineCourseResponse getEnrolledCourse(Long courseId, String studentEmail) {
        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        OnlineCourse course = findPublishedCourseForEnrollment(courseId);
        OnlineCourseEnrollment enrollment = courseEnrollmentAccessPolicy.requireLearningAccess(student, course);
        return onlineCourseVersionService.readLatestPublishedForEnrollment(enrollment, course);
    }

    @Override
    public OnlineCourseResponse activatePaidCourse(Long courseId, String studentEmail) {
        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        return activateEnrollment(findPublishedCourseForEnrollment(courseId), student);
    }

    @Override
    public void revokePaidCourseAccess(Long courseId, String studentEmail) {
        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy học viên."));
        OnlineCourse course = findCourse(courseId);
        if (course == null) {
            return;
        }
        enrollmentRepository.findByStudentAndOnlineCourse(student, course)
                .ifPresent(enrollment -> {
                    enrollment.setStatus(EnrollmentStatus.CANCELLED);
                    enrollmentRepository.save(enrollment);
                });
    }

    private OnlineCourseResponse activateEnrollment(OnlineCourse course, User student) {
        if (course.isDeleted() || course.getStatus() != PackageStatus.PUBLISHED) {
            throw new CourseUnavailableException("Course not found or not available for enrollment");
        }

        var existingEnrollment = enrollmentRepository.findByStudentAndOnlineCourse(student, course);
        if (existingEnrollment.isPresent()) {
            OnlineCourseEnrollment enrollment = existingEnrollment.get();
            if (enrollment.getOnlineCourse() == null) {
                enrollment.setOnlineCourse(course);
            }
            if (enrollment.getCourseVersion() == null) {
                enrollment.setCourseVersion(onlineCourseVersionService.requirePublishedVersion(course));
                onlineCourseVersionService.assertEnrollmentCourseVersionBelongsToCourse(enrollment, course);
                enrollment = enrollmentRepository.save(enrollment);
            } else {
                onlineCourseVersionService.assertEnrollmentCourseVersionBelongsToCourse(enrollment, course);
            }
            if (!courseEnrollmentAccessPolicy.hasLearningAccess(enrollment)) {
                enrollment = courseEnrollmentAccessPolicy.reactivateCancelledEnrollment(enrollment);
                courseEnrollmentMailService.sendEnrollmentSuccessEmail(student, course, enrollment);
            }
            return mapper.toResponse(course, true, enrollment.getProgressPercent(), enrollment.getId());
        }

        OnlineCourseEnrollment enrollment = enrollmentRepository.save(OnlineCourseEnrollment.builder()
                .student(student)
                .onlineCourse(course)
                
                .courseVersion(onlineCourseVersionService.requirePublishedVersion(course))
                .status(EnrollmentStatus.ACTIVE)
                .progressPercent(0)
                .build());
        onlineCourseVersionService.assertEnrollmentCourseVersionBelongsToCourse(enrollment, course);
        courseEnrollmentMailService.sendEnrollmentSuccessEmail(student, course, enrollment);
        return mapper.toResponse(course, true, enrollment.getProgressPercent(), enrollment.getId());
    }

    @Override
    @Transactional
    public List<OnlineCourseEnrollmentResponse> getMyEnrollments(String studentEmail) {
        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        return enrollmentRepository.findByStudentOrderByRegisteredAtDesc(student).stream()
                .filter(enrollment -> enrollment.getStatus() != EnrollmentStatus.CANCELLED)
                .filter(enrollment -> {
                    OnlineCourse course = enrollment.getOnlineCourse();
                    if (course != null) {
                        return !course.isDeleted();
                    }
                    return enrollment.getOnlineCourse() != null && !false;
                })
                .map(enrollment -> {
                    OnlineCourse course = enrollment.getOnlineCourse() != null
                            ? enrollment.getOnlineCourse()
                            : java.util.Optional.of(enrollment.getOnlineCourse()).orElse(null);
                    return course == null ? null : courseProgressService.refreshEnrollmentProgress(enrollment, course, student);
                })
                .filter(java.util.Objects::nonNull)
                .map(mapper::toEnrollmentResponse)
                .toList();
    }

    @Override
    public List<OnlineCourseResponse> updateLearningPathOrder(LearningPathOrderRequest request) {
        List<Long> courseIds = request.getCourseIds();
        if (courseIds.stream().distinct().count() != courseIds.size()) {
            throw new IllegalArgumentException("Danh sách khóa học trong lộ trình bị trùng.");
        }
        List<OnlineCourse> courses = onlineCourseRepository.findAllById(courseIds);
        if (courses.size() != courseIds.size()) {
            throw new IllegalArgumentException("Không tìm thấy đầy đủ khóa học cần sắp xếp.");
        }
        String pathCode = courses.getFirst().getLearningPathCode();
        if (pathCode == null || pathCode.isBlank() || courses.stream().anyMatch(course -> !pathCode.equals(course.getLearningPathCode()))) {
            throw new IllegalArgumentException("Chỉ có thể sắp xếp các khóa học trong cùng một lộ trình.");
        }
        java.util.Map<Long, OnlineCourse> byId = courses.stream().collect(java.util.stream.Collectors.toMap(OnlineCourse::getId, course -> course));
        List<OnlineCourseResponse> responses = new java.util.ArrayList<>();
        for (int index = 0; index < courseIds.size(); index++) {
            OnlineCourse course = byId.get(courseIds.get(index));
            course.setLearningPathOrder(index + 1);
            responses.add(mapper.toResponse(course));
        }
        onlineCourseRepository.saveAll(courses);
        return responses;
    }

    /**
     * Catalog "recommended for you" when the learner is not coming from a specific attempt.
     * Builds context from the latest attempt (or profile target if none), then reuses recommendCourses().
     */
    @Override
    @Transactional(readOnly = true)
    public List<OnlineCourseResponse> getRecommendedCourses(String studentEmail) {
        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy học viên."));
        PlacementTestAttempt latestAttempt = placementTestAttemptRepository
                .findTopByStudentOrderBySubmittedAtDesc(student)
                .orElse(null);
        PlacementRecommendationContext context = latestAttempt == null
                ? PlacementRecommendationContext.builder()
                    .learnerId(student.getId())
                    .examType(safe(student.getTargetExam()).toUpperCase(Locale.ROOT))
                    .overallScore(student.getCurrentBand() == null ? null : BigDecimal.valueOf(student.getCurrentBand()))
                    .targetExam(student.getTargetExam())
                    .targetScore(parseBand(student.getTargetScore()) == null ? null : BigDecimal.valueOf(parseBand(student.getTargetScore())))
                    .weakSkills(Set.of())
                    .build()
                : placementRecommendationContextFactory.fromAttempt(student, latestAttempt, latestAttempt.getRecommendedLevel());
        return recommendCourses(student, context);
    }

    /**
     * Core ranking for placement → online-course suggestions.
     *
     * Pipeline:
     * 1. Load every PUBLISHED course.
     * 2. Index this learner's enrollments by package id (latest row wins) to know progress.
     * 3. Drop courses already finished (COMPLETED or 100%).
     * 4. Drop hard exam mismatch (IELTS course vs TOEIC placement, and vice versa).
     * 5. scoreRecommendation() — numeric match + flags (weak skill, band window).
     * 6. Sort: higher score first; tie-break by learning-path order, then id.
     * 7. BalancedCourseRecommendationSelector — mixed shortlist, not raw top-N,
     *    so the UI shows both "fix a weak skill" and "match your level".
     */
    @Override
    @Transactional(readOnly = true)
    public List<OnlineCourseResponse> recommendCourses(User student, PlacementRecommendationContext context) {
        List<OnlineCourse> publishedCourses = onlineCourseRepository
                .findAll(courseSpec(null, null, null, null, null, null, PackageStatus.PUBLISHED), Pageable.unpaged())
                .getContent();
        // One enrollment per online course; duplicate rows keep the newest because the query is descending.
        Map<Long, OnlineCourseEnrollment> enrollmentsByPackage = enrollmentRepository
                .findByStudentOrderByRegisteredAtDesc(student)
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        enrollment -> enrollment.getOnlineCourse().getId(),
                        enrollment -> enrollment,
                        (first, ignored) -> first
                ));
        List<ScoredRecommendation> rankedRecommendations = publishedCourses.stream()
                .filter(course -> !isCompletedEnrollment(enrollmentsByPackage.get(course.getId())))
                .filter(course -> isExamCompatible(course, context.getExamType()))
                .map(course -> scoreRecommendation(
                        course,
                        enrollmentsByPackage.get(course.getId()),
                        context
                ))
                .sorted(Comparator.comparingDouble(ScoredRecommendation::score).reversed()
                        .thenComparing(item -> defaultInt(item.course().getLearningPathOrder()))
                        .thenComparing(item -> item.course().getId()))
                .toList();

        // rankedRecommendations is the full ordered list; selector only chooses which 6 to show.
        return BalancedCourseRecommendationSelector.select(
                        rankedRecommendations,
                        ScoredRecommendation::bandCompatible,
                        ScoredRecommendation::matchesWeakSkill,
                        context.getWeakSkills() != null && !context.getWeakSkills().isEmpty()
                ).stream()
                .map(ScoredRecommendation::response)
                .toList();
    }

    /**
     * Score one course against the placement context. Higher = better match.
     *
     * Weights (rough order of impact):
     *   +15 / -3  same vs different BEGINNER|INTERMEDIATE|ADVANCED
     *   +8 each   course focus skill overlaps a placement weak skill
     *   +2..10    IELTS: current band sits in the course window, closer to entry min is better (target 5.5 so plus for 5.0 -> 6.5) [ 10 - (my band - min band) x 4 ]
     *   -0..8     IELTS: current band outside that window (distance penalty)
     *   -1..5     IELTS: course targetBand close to the learner's goal
     *   +6        exam name appears in title / category / path name
     *   +1        featured flag (tie-break)
     *
     * Also stamps recommendationReason and two flags used later by the selector:
     * matchesWeakSkill, bandCompatible.
     */
    private ScoredRecommendation scoreRecommendation(
            OnlineCourse course,
            OnlineCourseEnrollment enrollment,
            PlacementRecommendationContext context
    ) {
        OnlineCourseResponse response = mapper.toPublicResponse(course);
        if (enrollment != null && enrollment.getStatus() != EnrollmentStatus.CANCELLED) {
            // Still enrolled (not cancelled): surface progress so the UI can say "continue".
            response.setRegistered(true);
            response.setEnrollmentId(enrollment.getId());
            response.setProgressPercent(defaultInt(enrollment.getProgressPercent()));
        }

        double score = 0;
        String searchableCourse = String.join(" ",
                safe(course.getTitle()),
                safe(course.getShortDescription()),
                safe(response.getCategory()),
                safe(response.getCategoryName()),
                safe(course.getLearningPathCode()),
                safe(course.getLearningPathName())
        ).toUpperCase(Locale.ROOT);
        String normalizedExam = safe(context.getExamType()).toUpperCase(Locale.ROOT);

        // 1) Soft exam signal: "IELTS Writing" in the title matches an IELTS placement.
        //    Hard mismatch is already filtered by isExamCompatible; this only boosts keyword hits.
        boolean examMatches = !normalizedExam.isBlank() && searchableCourse.contains(normalizedExam);
        if (examMatches) score += 6;

        // 2) Placement level vs course level. Same level is the strongest single boost.
        //    Wrong level is penalized so Advanced is not pushed to a Beginner.
        if (context.getRecommendedLevel() != null && course.getLevel() != null) {
            score += context.getRecommendedLevel().name().equals(course.getLevel().name()) ? 15 : -3;
        }

        Double minBand = course.getRecommendedCurrentBandMin();
        Double currentBand = decimalToDouble(context.getOverallScore());
        Double targetBand = decimalToDouble(context.getTargetScore());
        boolean bandCompatible = isBandCompatible(course, normalizedExam, currentBand);

        // 3) IELTS band window. Inside [courseMin, courseTarget]: closer to entry min = "right now" difficulty.
        //    Outside: subtract distance so far-away bands fall down the list. Floor 2 / cap 8 keep it bounded.
        if ("IELTS".equals(normalizedExam) && currentBand != null && minBand != null) {
            if (bandCompatible) {
                score += Math.max(2, 10 - Math.abs(currentBand - minBand) * 4);
            } else {
                score -= Math.min(8, Math.abs(minBand - currentBand) * 4);
            }
        }
        // 4) If the learner set a goal (e.g. 6.5), prefer courses whose targetBand is near that goal.
        if ("IELTS".equals(normalizedExam) && targetBand != null && course.getTargetBand() != null) {
            score += Math.max(-1, 5 - Math.abs(targetBand - course.getTargetBand()) * 2);
        }

        // 5) Weak-skill overlap. Each shared skill is +8 — this is how a weak Writing learner
        //    sees Writing courses rise above generic level-matched courses.
        Set<AssessmentSkill> matchedWeakSkills = response.getFocusSkills().stream()
                .map(this::parseAssessmentSkill)
                .filter(java.util.Objects::nonNull)
                .filter(context.getWeakSkills()::contains)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        score += matchedWeakSkills.size() * 8D;
        if (course.isFeatured()) score += 1;

        response.setRecommendationReason(buildRecommendationReason(
                matchedWeakSkills,
                examMatches,
                context.getExamType(),
                currentBand,
                targetBand
        ));
        return new ScoredRecommendation(course, response, score, !matchedWeakSkills.isEmpty(), bandCompatible);
    }

    /**
     * IELTS band window: current overall must sit in [recommendedCurrentBandMin, targetBand].
     * TOEIC / missing current band → true (do not filter by IELTS window).
     * Missing min or target on the course → false, so the selector can fall back to the full list.
     */
    private boolean isBandCompatible(OnlineCourse course, String normalizedExam, Double currentBand) {
        if (!"IELTS".equals(normalizedExam) || currentBand == null) {
            return true;
        }
        Double minBand = course.getRecommendedCurrentBandMin();
        Double courseTargetBand = course.getTargetBand();
        return minBand != null
                && courseTargetBand != null
                && currentBand >= minBand
                && currentBand <= courseTargetBand;
    }

    /**
     * Hard exam filter before scoring.
     * Category IELTS/TOEIC must equal the placement exam. Other categories (skills, general) stay eligible.
     */
    private boolean isExamCompatible(OnlineCourse course, String examType) {
        String category = course.getCategory() == null ? "" : safe(course.getCategory().getCode()).toUpperCase(Locale.ROOT);
        String normalizedExam = safe(examType).toUpperCase(Locale.ROOT);
        if (Set.of("IELTS", "TOEIC").contains(category)) return category.equals(normalizedExam);
        return true;
    }

    private Set<AssessmentSkill> resolveWeakSkills(PlacementTestAttempt attempt) {
        if (attempt == null) return Set.of();
        Map<AssessmentSkill, BigDecimal> scores = new EnumMap<>(AssessmentSkill.class);
        putScore(scores, AssessmentSkill.LISTENING, attempt.getListeningScore());
        putScore(scores, AssessmentSkill.READING, attempt.getReadingScore());
        putScore(scores, AssessmentSkill.WRITING, attempt.getWritingScore());
        putScore(scores, AssessmentSkill.SPEAKING, attempt.getSpeakingScore());

        Set<AssessmentSkill> weakSkills = new LinkedHashSet<>();
        scores.values().stream().min(BigDecimal::compareTo).ifPresent(minimum -> scores.forEach((skill, score) -> {
            if (score.compareTo(minimum.add(BigDecimal.valueOf(0.5))) <= 0) weakSkills.add(skill);
        }));

        if (weakSkills.isEmpty()) {
            String feedback = safe(attempt.getAiFeedbackJson()).toUpperCase(Locale.ROOT);
            for (AssessmentSkill skill : List.of(
                    AssessmentSkill.LISTENING,
                    AssessmentSkill.READING,
                    AssessmentSkill.WRITING,
                    AssessmentSkill.SPEAKING
            )) {
                if (feedback.contains(skill.name())) weakSkills.add(skill);
            }
        }
        return weakSkills;
    }

    private void putScore(Map<AssessmentSkill, BigDecimal> scores, AssessmentSkill skill, BigDecimal score) {
        if (score != null) scores.put(skill, score);
    }

    /**
     * UI reason, first match wins:
     * 1) covers a weak skill from placement
     * 2) course can raise band toward the learner's goal
     * 3) same exam keyword
     * 4) same current band
     * 5) generic fallback
     */
    private String buildRecommendationReason(
            Set<AssessmentSkill> matchedWeakSkills,
            boolean examMatches,
            String targetExam,
            Double currentBand,
            Double targetBand
    ) {
        if (!matchedWeakSkills.isEmpty()) {
            return "Ưu tiên vì bạn cần cải thiện kỹ năng " + skillLabel(matchedWeakSkills.iterator().next()) + ".";
        }
        if (currentBand != null && targetBand != null && targetBand > currentBand) {
            return "Phù hợp để nâng band từ " + formatBand(currentBand) + " lên " + formatBand(targetBand) + ".";
        }
        if (examMatches) {
            return "Phù hợp với mục tiêu " + safe(targetExam).toUpperCase(Locale.ROOT) + " của bạn.";
        }
        if (currentBand != null) {
            return "Phù hợp với trình độ hiện tại band " + formatBand(currentBand) + ".";
        }
        return "Được đề xuất dựa trên hồ sơ học tập của bạn.";
    }

    /** True when this online-course enrollment is done, so recommendCourses must not suggest it again. */
    private boolean isCompletedEnrollment(OnlineCourseEnrollment enrollment) {
        return enrollment != null && (enrollment.getStatus() == EnrollmentStatus.COMPLETED
                || defaultInt(enrollment.getProgressPercent()) >= 100);
    }

    /** Map course focusSkills strings to enums; unknown tokens are ignored (not a scoring error). */
    private AssessmentSkill parseAssessmentSkill(String value) {
        try {
            return AssessmentSkill.valueOf(safe(value).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private String skillLabel(AssessmentSkill skill) {
        String value = skill.name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private Double parseBand(String value) {
        var matcher = BAND_NUMBER_PATTERN.matcher(safe(value));
        if (!matcher.find()) return null;
        try {
            return Double.parseDouble(matcher.group(1));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Double decimalToDouble(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }

    private String formatBand(Double value) {
        return value == null ? "" : BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    /**
     * One scored course plus two flags the selector reads:
     * matchesWeakSkill — course trains a placement weak skill
     * bandCompatible — IELTS current band is inside the course [min, target] window
     */
    private record ScoredRecommendation(
            OnlineCourse course,
            OnlineCourseResponse response,
            double score,
            boolean matchesWeakSkill,
            boolean bandCompatible
    ) {}

    @Override
    @Transactional(readOnly = true)
    public LearnerLearningPathResponse getMyLearningPath(String studentEmail) {
        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy học viên."));
        List<OnlineCourse> courses = onlineCourseRepository.findPublishedLearningPathCourses(PackageStatus.PUBLISHED);
        Map<Long, OnlineCourseEnrollment> enrollmentsByPackageId = enrollmentRepository
                .findByStudentOrderByRegisteredAtDesc(student)
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        enrollment -> enrollment.getOnlineCourse().getId(),
                        enrollment -> enrollment,
                        (first, ignored) -> first
                ));

        Map<String, List<OnlineCourse>> groupedCourses = new LinkedHashMap<>();
        courses.forEach(course -> groupedCourses
                .computeIfAbsent(course.getLearningPathCode().trim(), ignored -> new ArrayList<>())
                .add(course));

        List<LearnerLearningPathResponse.PathOverview> allPaths = groupedCourses.entrySet().stream()
                .map(entry -> buildLearningPathOverview(entry.getKey(), entry.getValue(), enrollmentsByPackageId))
                .sorted(Comparator.comparing(LearnerLearningPathResponse.PathOverview::getCode)
                        .thenComparing(LearnerLearningPathResponse.PathOverview::getName))
                .toList();
        List<LearnerLearningPathResponse.PathOverview> paths = allPaths.stream()
                .filter(path -> path.getCourses().stream().anyMatch(course -> !"NOT_ENROLLED".equals(course.getEnrollmentStatus())))
                .findFirst()
                .map(List::of)
                .orElseGet(() -> allPaths.isEmpty() ? List.of() : List.of(allPaths.getFirst()));

        return LearnerLearningPathResponse.builder()
                .currentBand(student.getCurrentBand())
                .targetExam(student.getTargetExam())
                .targetScore(student.getTargetScore())
                .paths(paths)
                .build();
    }

    private LearnerLearningPathResponse.PathOverview buildLearningPathOverview(
            String code,
            List<OnlineCourse> pathCourses,
            Map<Long, OnlineCourseEnrollment> enrollmentsByPackageId
    ) {
        List<OnlineCourse> sortedCourses = pathCourses.stream()
                .sorted(Comparator.comparing((OnlineCourse course) -> defaultInt(course.getLearningPathOrder()))
                        .thenComparing(OnlineCourse::getId))
                .toList();

        OnlineCourse enrolledCurrentCourse = sortedCourses.stream()
                .filter(course -> {
                    OnlineCourseEnrollment enrollment = activeLearningPathEnrollment(
                            enrollmentsByPackageId.get(course.getId())
                    );
                    return enrollment != null
                            && enrollment.getStatus() == EnrollmentStatus.ACTIVE
                            && defaultInt(enrollment.getProgressPercent()) < 100;
                })
                .findFirst()
                .orElse(null);

        Long nextCourseId = null;
        Long currentStepCourseId;
        if (enrolledCurrentCourse != null) {
            currentStepCourseId = enrolledCurrentCourse.getId();
            int currentIndex = sortedCourses.indexOf(enrolledCurrentCourse);
            for (int index = currentIndex + 1; index < sortedCourses.size(); index++) {
                OnlineCourse candidate = sortedCourses.get(index);
                OnlineCourseEnrollment enrollment = activeLearningPathEnrollment(
                        enrollmentsByPackageId.get(candidate.getId())
                );
                if (enrollment == null) {
                    nextCourseId = candidate.getId();
                    break;
                }
            }
        } else {
            currentStepCourseId = null;
            boolean previousCoursesCompleted = true;
            for (OnlineCourse course : sortedCourses) {
                OnlineCourseEnrollment enrollment = activeLearningPathEnrollment(
                        enrollmentsByPackageId.get(course.getId())
                );
                boolean completed = isLearningPathCourseCompleted(enrollment);
                if (previousCoursesCompleted && enrollment == null) {
                    currentStepCourseId = course.getId();
                    nextCourseId = course.getId();
                    break;
                }
                previousCoursesCompleted = previousCoursesCompleted && completed;
            }
        }

        List<LearnerLearningPathCourseResponse> courseResponses = new ArrayList<>();
        boolean prerequisiteCompleted = true;
        for (OnlineCourse course : sortedCourses) {
            OnlineCourseEnrollment enrollment = activeLearningPathEnrollment(
                    enrollmentsByPackageId.get(course.getId())
            );
            boolean completed = isLearningPathCourseCompleted(enrollment);
            boolean accessible = enrollment != null || prerequisiteCompleted;
            courseResponses.add(LearnerLearningPathCourseResponse.builder()
                    .courseId(course.getId())
                    .slug(course.getSlug())
                    .title(course.getTitle())
                    .thumbnailUrl(course.getThumbnailUrl())
                    .learningPathOrder(course.getLearningPathOrder())
                    .enrollmentStatus(enrollment == null ? "NOT_ENROLLED" : enrollment.getStatus().name())
                    .progressPercent(enrollment == null ? 0 : defaultInt(enrollment.getProgressPercent()))
                    .completed(completed)
                    .lockedReason(accessible ? null : "Hoàn thành khóa học trước để mở giai đoạn này.")
                    .build());
            prerequisiteCompleted = prerequisiteCompleted && completed;
        }

        return LearnerLearningPathResponse.PathOverview.builder()
                .code(code)
                .name(sortedCourses.getFirst().getLearningPathName())
                .totalCourses(sortedCourses.size())
                .completedCourses((int) courseResponses.stream().filter(LearnerLearningPathCourseResponse::isCompleted).count())
                .currentStepCourseId(currentStepCourseId)
                .nextCourseId(nextCourseId)
                .courses(courseResponses)
                .build();
    }

    private boolean isLearningPathCourseCompleted(OnlineCourseEnrollment enrollment) {
        return enrollment != null
                && (enrollment.getStatus() == EnrollmentStatus.COMPLETED || defaultInt(enrollment.getProgressPercent()) >= 100);
    }

    private OnlineCourseEnrollment activeLearningPathEnrollment(OnlineCourseEnrollment enrollment) {
        if (enrollment == null || enrollment.getStatus() == EnrollmentStatus.CANCELLED) {
            return null;
        }
        return enrollment;
    }

    @Override
    @Transactional(readOnly = true)
    public CourseCompletionResponse getCourseCompletion(Long courseId, String studentEmail) {
        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy học viên."));
        OnlineCourse course = findCourse(courseId);
        OnlineCourseEnrollment enrollment = courseEnrollmentAccessPolicy.requireLearningAccess(student, course);
        return courseProgressService.buildCompletionResponse(enrollment, course, student);
    }

    @Override
    public CourseCertificateResponse getCourseCertificate(Long courseId, String studentEmail) {
        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy học viên."));
        OnlineCourse course = findCourse(courseId);
        OnlineCourseEnrollment enrollment = courseEnrollmentAccessPolicy.requireLearningAccess(student, course);
        CourseCompletionResponse completion = courseProgressService.buildCompletionResponse(enrollment, course, student);
        return buildCertificateResponse(course, enrollment, student, completion, false);
    }

    @Override
    public OnlineCourseEnrollmentResponse updateLessonProgress(Long courseId, Long lessonId, boolean completed, String studentEmail) {
        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        OnlineCourse course = findCourse(courseId);
        OnlineCourseEnrollment enrollment = courseEnrollmentAccessPolicy.requireLearningAccess(student, course);
        onlineCourseVersionService.assertLessonBelongsToEnrollment(enrollment, lessonId);
        OnlineCourseVersion pinnedVersion = enrollment.getCourseVersion() != null
                ? enrollment.getCourseVersion()
                : onlineCourseVersionService.requirePublishedVersion(course);
        OnlineLesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("OnlineLesson not found"));

        if (lesson.getModule() == null
                || lesson.getModule().getOnlineCourseVersion() == null
                || !pinnedVersion.getId().equals(lesson.getModule().getOnlineCourseVersion().getId())) {
            throw new IllegalArgumentException("Bài học không thuộc phiên bản đã đăng ký của khóa học này.");
        }

        LessonProgress progress = lessonProgressRepository.findByEnrollmentAndLesson(enrollment, lesson)
                .or(() -> lessonProgressRepository.findByStudentAndLesson(student, lesson))
                .orElseGet(() -> LessonProgress.builder()
                        .student(student)
                        .lesson(lesson)
                        .enrollment(enrollment)
                        .courseVersion(pinnedVersion)
                        .lessonKey(lesson.getLessonKey())
                        .build());
        if (progress.getCourseVersion() == null) {
            progress.setCourseVersion(pinnedVersion);
        }
        if (progress.getEnrollment() == null) {
            progress.setEnrollment(enrollment);
        }
        if (progress.getLessonKey() == null || progress.getLessonKey().isBlank()) {
            progress.setLessonKey(lesson.getLessonKey());
        }

        progress.setLastAccessedAt(LocalDateTime.now());
        if (progress.getFirstAccessedAt() == null) {
            progress.setFirstAccessedAt(progress.getLastAccessedAt());
        }
        if (completed) {
            onlineCourseVersionService.assertLessonProgressTransitionAllowed(enrollment, lessonId, true);
            progress.setStatus(LessonProgressStatus.COMPLETED);
            progress.setProgressPercent(100);
            if (progress.getCompletedAt() == null) {
                progress.setCompletedAt(LocalDateTime.now());
            }
        } else {
            onlineCourseVersionService.assertLessonProgressTransitionAllowed(enrollment, lessonId, false);
            progress.setStatus(LessonProgressStatus.IN_PROGRESS);
            progress.setProgressPercent(0);
            progress.setCompletedAt(null);
        }
        lessonProgressRepository.save(progress);

        OnlineCourseEnrollment savedEnrollment = courseProgressService.refreshEnrollmentProgress(enrollment, course, student);
        return mapper.toEnrollmentResponse(savedEnrollment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VocabularyTermResponse> getVocabularyTerms(Long courseId, String studentEmail) {
        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        OnlineCourse course = findCourse(courseId);
        ensureEnrolled(student, course);

        return flashcardPracticeService.getPracticeTerms(
                FlashcardPracticeSource.ENROLLED,
                courseId,
                false,
                studentEmail
        );
    }

    @Override
    public VocabularyTermResponse updateVocabularyProgress(Long courseId, String termKey, VocabularyProgressStatus status, Boolean starred, Boolean reviewed, Boolean correct, String studentEmail) {
        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        OnlineCourse course = findCourse(courseId);
        ensureEnrolled(student, course);

        VocabularyTermResponse term = flashcardPracticeService.getPracticeTerms(
                        FlashcardPracticeSource.ENROLLED,
                        courseId,
                        false,
                        studentEmail
                ).stream()
                .filter(item -> item.getTermKey().equals(termKey))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Vocabulary term not found"));

        VocabularyProgress progress = vocabularyProgressRepository.findByStudentAndCourseAndTermKey(student, course, termKey)
                .orElseGet(() -> VocabularyProgress.builder()
                        .student(student)
                        .course(course)
                        .termKey(termKey)
                        .build());

        if (status != null) {
            progress.setStatus(status);
        }
        if (starred != null) {
            progress.setStarred(starred);
        }
        if (Boolean.TRUE.equals(reviewed)) {
            progress.setReviewCount((progress.getReviewCount() == null ? 0 : progress.getReviewCount()) + 1);
            progress.setLastReviewedAt(LocalDateTime.now());
        }
        if (correct != null) {
            progress.setLastResultCorrect(correct);
            if (correct) {
                progress.setCorrectCount((progress.getCorrectCount() == null ? 0 : progress.getCorrectCount()) + 1);
            } else {
                progress.setIncorrectCount((progress.getIncorrectCount() == null ? 0 : progress.getIncorrectCount()) + 1);
            }
        }
        if (progress.getLastReviewedAt() == null) {
            progress.setLastReviewedAt(LocalDateTime.now());
        }
        VocabularyProgress savedProgress = vocabularyProgressRepository.save(progress);

        return applyVocabularyProgress(term, List.of(savedProgress));
    }

    private OnlineCourse findCourse(Long id) {
        OnlineCourse course = onlineCourseRepository.findWithModulesById(id)
                .filter(foundCourse -> !foundCourse.isDeleted())
                .orElseThrow(() -> new RuntimeException("Course not found"));
        initializeModules(course);
        return course;
    }

    private OnlineCourse findPublicCourse(String slugOrId) {
        try {
            return findPublishedCourseByIdOrPackageId(Long.parseLong(slugOrId));
        } catch (NumberFormatException ex) {
            OnlineCourse course = onlineCourseRepository.findBySlugAndDeletedFalseAndStatus(slugOrId, PackageStatus.PUBLISHED)
                    .orElseThrow(() -> new CourseUnavailableException("Course not found"));
            initializeModules(course);
            return course;
        }
    }

    private OnlineCourse findManagerCourse(String slugOrId) {
        OnlineCourse course;
        try {
            Long numericId = Long.parseLong(slugOrId);
            course = onlineCourseRepository.findWithModulesById(numericId)
                    .orElseThrow(() -> new RuntimeException("Course not found"));
        } catch (NumberFormatException ex) {
            course = onlineCourseRepository.findBySlugAndDeletedFalseAndStatus(slugOrId, PackageStatus.PUBLISHED)
                    .or(() -> onlineCourseRepository.findAll().stream()
                            .filter(c -> !c.isDeleted() && slugOrId.equalsIgnoreCase(c.getSlug()))
                            .findFirst())
                    .orElseThrow(() -> new RuntimeException("Course not found"));
        }

        if (course.isDeleted()) {
            throw new RuntimeException("Course not found");
        }
        initializeModules(course);
        return course;
    }

    private void initializeModules(OnlineCourse course) {
        course.getModules().forEach(module -> module.getLessons().size());
    }

    private OnlineCourse findPublishedCourseForEnrollment(Long courseId) {
        OnlineCourse course = onlineCourseRepository
                .findWithModulesByIdAndDeletedFalseAndStatus(courseId, PackageStatus.PUBLISHED)
                .orElseThrow(() -> new CourseUnavailableException("Course not found or not available for enrollment"));
        initializeModules(course);
        return course;
    }

    private OnlineCourse findPublishedCourseByIdOrPackageId(Long slugOrId) {
        OnlineCourse course = onlineCourseRepository
                .findWithModulesByIdAndDeletedFalseAndStatus(slugOrId, PackageStatus.PUBLISHED)
                .orElseThrow(() -> new CourseUnavailableException("Course not found"));
        initializeModules(course);
        return course;
    }

    private void ensureEnrolled(User student, OnlineCourse course) {
        courseEnrollmentAccessPolicy.requireLearningAccess(student, course);
    }

    private List<VocabularyTermResponse> extractVocabularyTerms(OnlineCourse course) {
        List<VocabularyTermResponse> terms = new ArrayList<>();
        for (OnlineCourseModule module : course.getModules()) {
            for (OnlineLesson lesson : module.getLessons()) {
                terms.addAll(extractVocabularyTerms(module, lesson));
            }
        }
        return terms;
    }

    private List<VocabularyTermResponse> extractVocabularyTerms(OnlineCourseModule module, OnlineLesson lesson) {
        String content = lesson.getContentText();
        if (content == null || !content.contains("### ")) {
            return List.of();
        }

        java.util.regex.Matcher matcher = VOCABULARY_HEADING.matcher(content);
        List<java.util.regex.MatchResult> headings = matcher.results().toList();
        List<VocabularyTermResponse> terms = new ArrayList<>();

        for (int index = 0; index < headings.size(); index++) {
            java.util.regex.MatchResult heading = headings.get(index);
            int start = heading.end();
            int end = index + 1 < headings.size() ? headings.get(index + 1).start() : content.length();
            String block = content.substring(start, end);
            String meaning = findVocabularyField(block, "Meaning");
            if (meaning == null || meaning.isBlank()) {
                continue;
            }

            String term = cleanMarkdown(heading.group(1));
            terms.add(VocabularyTermResponse.builder()
                    .termKey(toTermKey(module, lesson, term))
                    .term(term)
                    .meaning(meaning)
                    .example(firstNonBlank(findVocabularyField(block, "IELTS example"), findVocabularyField(block, "Example")))
                    .commonError(firstNonBlank(findVocabularyField(block, "Common error to avoid"), findVocabularyField(block, "Common error")))
                    .lessonId(lesson.getId())
                    .lessonTitle(lesson.getTitle())
                    .moduleId(module.getId())
                    .moduleTitle(module.getTitle())
                    .status(VocabularyProgressStatus.NEW)
                    .starred(false)
                    .reviewCount(0)
                    .correctCount(0)
                    .incorrectCount(0)
                    .lastResultCorrect(null)
                    .build());
        }

        return terms;
    }

    private VocabularyTermResponse applyVocabularyProgress(VocabularyTermResponse term, List<VocabularyProgress> progressItems) {
        progressItems.stream()
                .filter(progress -> progress.getTermKey().equals(term.getTermKey()))
                .findFirst()
                .ifPresent(progress -> {
                    term.setStatus(progress.getStatus());
                    term.setStarred(progress.isStarred());
                    term.setReviewCount(progress.getReviewCount() == null ? 0 : progress.getReviewCount());
                    term.setCorrectCount(progress.getCorrectCount() == null ? 0 : progress.getCorrectCount());
                    term.setIncorrectCount(progress.getIncorrectCount() == null ? 0 : progress.getIncorrectCount());
                    term.setLastResultCorrect(progress.getLastResultCorrect());
                });
        return term;
    }

    private String findVocabularyField(String block, String label) {
        Pattern fieldPattern = Pattern.compile("(?m)^\\*\\*" + Pattern.quote(label) + ":\\*\\*\\s*(.+)$", Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = fieldPattern.matcher(block);
        return matcher.find() ? cleanMarkdown(matcher.group(1)) : null;
    }

    private String cleanMarkdown(String value) {
        return value == null ? "" : value.replace("**", "").replaceAll("^['\"]|['\"]$", "").trim();
    }

    private String normalizeModuleTitle(String value) {
        if (value == null) return null;
        return value.replaceFirst(
                "(?iu)^\\s*(?:module|m[oô]\\s*[-–—]?\\s*đun)\\s*\\d+\\s*(?::|[-–—])?\\s*",
                ""
        ).trim();
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private String toTermKey(OnlineCourseModule module, OnlineLesson lesson, String term) {
        return "%s-%s-%s".formatted(module.getId(), lesson.getId(), toSlug(term));
    }

    private void rebuildModules(OnlineCourse course, OnlineCourseVersion version, List<ModuleRequest> modules) {
        if (modules == null) return;
        for (int moduleIndex = 0; moduleIndex < modules.size(); moduleIndex++) {
            ModuleRequest moduleRequest = modules.get(moduleIndex);
            OnlineCourseModule module = OnlineCourseModule.builder()
                    .title(normalizeModuleTitle(moduleRequest.getTitle()))
                    .description(moduleRequest.getDescription())
                    .sequenceNumber(moduleIndex + 1)
                    .build();
            if (moduleRequest.getLessons() != null) {
                for (int lessonIndex = 0; lessonIndex < moduleRequest.getLessons().size(); lessonIndex++) {
                    LessonRequest lessonRequest = moduleRequest.getLessons().get(lessonIndex);
                    OnlineLesson lesson = OnlineLesson.builder()
                            .title(lessonRequest.getTitle())
                            .description(lessonRequest.getDescription())
                            .contentType(lessonRequest.getContentType())
                            .contentText(lessonRequest.getContentText())
                            .videoUrl(lessonRequest.getVideoUrl())
                            .materialUrl(lessonRequest.getMaterialUrl())
                            .durationMinutes(defaultInt(lessonRequest.getDurationMinutes()))
                            .sequenceNumber(lessonIndex + 1)
                            .preview(Boolean.TRUE.equals(lessonRequest.getPreview()))
                            .stableLessonKey("LESSON-" + java.util.UUID.randomUUID())
                            .build();
                    applyLessonTranscript(lesson, lessonRequest, null);
                    synchronizeLessonFlashcardRefs(lesson, lessonRequest.getFlashcardSetIds());
                    module.addLesson(lesson);
                }
            }
            version.addModule(module);
        }
    }

    private void synchronizeModules(OnlineCourse course, OnlineCourseVersion version, List<ModuleRequest> modules) {
        if (modules == null) {
            return;
        }

        List<OnlineCourseModule> existingModules = version.getModules();
        Set<Long> incomingModuleIds = new HashSet<>();
        List<OnlineCourseModule> nextModules = new ArrayList<>();

        for (int moduleIndex = 0; moduleIndex < modules.size(); moduleIndex++) {
            ModuleRequest moduleRequest = modules.get(moduleIndex);
            OnlineCourseModule module = findExistingModule(existingModules, moduleRequest.getId());

            if (module == null) {
                module = OnlineCourseModule.builder().build();
                module.setOnlineCourse(course);
            } else if (module.getId() != null) {
                incomingModuleIds.add(module.getId());
            }

            module.setTitle(normalizeModuleTitle(moduleRequest.getTitle()));
            module.setDescription(moduleRequest.getDescription());
            module.setDisplayOrder(moduleIndex + 1);
            synchronizeLessons(module, moduleRequest.getLessons());
            nextModules.add(module);
        }

        for (OnlineCourseModule existingModule : new ArrayList<>(existingModules)) {
            if (existingModule.getId() != null && !incomingModuleIds.contains(existingModule.getId())) {
                ensureModuleCanBeRemoved(existingModule);
            }
        }

        existingModules.clear();
        nextModules.forEach(version::addModule);
    }

    private void synchronizeLessons(OnlineCourseModule module, List<LessonRequest> lessons) {
        List<OnlineLesson> existingLessons = module.getLessons();
        Set<Long> incomingLessonIds = new HashSet<>();
        List<OnlineLesson> nextLessons = new ArrayList<>();

        if (lessons != null) {
            for (int lessonIndex = 0; lessonIndex < lessons.size(); lessonIndex++) {
                LessonRequest lessonRequest = lessons.get(lessonIndex);
                OnlineLesson lesson = findExistingLesson(existingLessons, lessonRequest.getId());

                if (lesson == null) {
                    lesson = OnlineLesson.builder().build();
                    lesson.setModule(module);
                    lesson.setLessonKey("LESSON-" + java.util.UUID.randomUUID());
                } else if (lesson.getId() != null) {
                    incomingLessonIds.add(lesson.getId());
                }

                lesson.setTitle(lessonRequest.getTitle());
                lesson.setDescription(lessonRequest.getDescription());
                lesson.setContentType(lessonRequest.getContentType());
                lesson.setContentText(lessonRequest.getContentText());
                String previousVideoUrl = lesson.getVideoUrl();
                lesson.setVideoUrl(lessonRequest.getVideoUrl());
                lesson.setMaterialUrl(lessonRequest.getMaterialUrl());
                lesson.setDurationMinutes(defaultInt(lessonRequest.getDurationMinutes()));
                lesson.setDisplayOrder(lessonIndex + 1);
                lesson.setPreview(Boolean.TRUE.equals(lessonRequest.getPreview()));
                applyLessonTranscript(lesson, lessonRequest, previousVideoUrl);
                synchronizeLessonFlashcardRefs(lesson, lessonRequest.getFlashcardSetIds());
                nextLessons.add(lesson);
            }
        }

        for (OnlineLesson existingLesson : new ArrayList<>(existingLessons)) {
            if (existingLesson.getId() != null && !incomingLessonIds.contains(existingLesson.getId())) {
                ensureLessonCanBeRemoved(existingLesson);
            }
        }

        existingLessons.clear();
        nextLessons.forEach(module::addLesson);
    }

    private void refreshCourseTotals(OnlineCourse course, List<OnlineCourseModule> modules) {
        int lessonCount = 0;
        int totalMinutes = 0;
        List<OnlineCourseModule> effectiveModules = modules == null ? List.of() : modules;

        for (OnlineCourseModule module : effectiveModules) {
            for (OnlineLesson lesson : module.getLessons()) {
                lessonCount++;
                totalMinutes += defaultInt(lesson.getDurationMinutes());
            }
        }

        course.setTotalLessons(lessonCount);
        course.setTotalHours(totalMinutes == 0 ? 0 : (int) Math.ceil(totalMinutes / 60.0));
    }

    private void synchronizeAssessments(OnlineCourse course, List<ContentManagerCourseAssessmentRequest> requests) {
        List<CourseAssessment> existingAssessments = courseAssessmentRepository.findByOnlineCourseAndActiveTrueOrderByDisplayOrderAscIdAsc(course);
        Set<Long> incomingAssessmentIds = new HashSet<>();
        OnlineCourseVersion editableVersion = onlineCourseVersionService.requireEditableVersion(course);
        List<OnlineCourseModule> modules = new ArrayList<>(editableVersion.getModules());

        for (int index = 0; index < requests.size(); index++) {
            ContentManagerCourseAssessmentRequest request = requests.get(index);
            CourseAssessment assessment = findExistingAssessment(existingAssessments, request.getId());

            if (assessment == null) {
                assessment = CourseAssessment.builder().onlineCourse(course).build();
            } else if (onlineCourseVersionService.isAssessmentReferencedByPublishedHistory(
                    course,
                    assessment.getId()
            )) {
                String progressKey = assessment.getProgressKey();
                if (progressKey == null || progressKey.isBlank()) {
                    progressKey = UUID.randomUUID().toString();
                    assessment.setProgressKey(progressKey);
                }
                assessment.setActive(false);
                assessment = CourseAssessment.builder()
                        .onlineCourse(course)
                        .progressKey(progressKey)
                        .build();
            } else if (assessment.getId() != null) {
                incomingAssessmentIds.add(assessment.getId());
            }

            OnlineCourseModule targetModule = resolveAssessmentModule(modules, request.getModuleId());
            AssessmentBankItem bankItem = resolveAssessmentBankItem(request.getAssessmentBankItemId());
            AssessmentRubric rubric = bankItem == null
                    ? resolveAssessmentRubric(request.getRubricId())
                    : bankItem.getRubric();
            try {
                validateAssessmentConfiguration(request, bankItem);
                validateAssessmentRubric(
                        bankItem == null ? request.getSkill() : bankItem.getSkill(),
                        rubric
                );
            } catch (RuntimeException exception) {
                throw new RuntimeException(buildAssessmentValidationContext(request, bankItem, targetModule, index)
                        + exception.getMessage(), exception);
            }

            assessment.setOnlineCourse(course);
            assessment.setModule(targetModule);
            assessment.setRubric(rubric);
            assessment.setAssessmentBankItem(bankItem);
            assessment.setLegacyAssessmentBankItemId(contentBankLinkSync.legacyIdForAssessment(bankItem));
            assessment.setLegacyRubricId(contentBankLinkSync.legacyIdForRubric(rubric));
            if (bankItem == null) {
                assessment.setTitle(request.getTitle().trim());
                assessment.setDescription(request.getDescription());
                assessment.setType(request.getType());
                assessment.setSkill(request.getSkill());
                assessment.setAiEvaluationMode(request.getAiEvaluationMode());
                assessment.setInstructions(request.getInstructions());
                assessment.setObjectiveAnswerKey(request.getObjectiveAnswerKey());
                assessment.setUiConfigJson(request.getUiConfigJson());
            } else {
                applyAssessmentBankSnapshot(assessment, bankItem);
            }
            assessment.setPassingScore(IeltsBandScale.normalizeConfiguredPassingScore(
                    bankItem == null ? request.getPassingScore() : bankItem.getPassingScore(),
                    assessment.getType(),
                    assessment.getSkill(),
                    assessment.getAiEvaluationMode()
            ));
            assessment.setMaxScore(IeltsBandScale.normalizeConfiguredMaxScore(
                    bankItem == null ? request.getMaxScore() : bankItem.getMaxScore(),
                    assessment.getType(),
                    assessment.getSkill(),
                    assessment.getAiEvaluationMode()
            ));
            assessment.setTimeLimitMinutes(defaultInt(bankItem == null ? request.getTimeLimitMinutes() : bankItem.getTimeLimitMinutes()));
            assessment.setDisplayOrder(request.getDisplayOrder() == null ? index + 1 : request.getDisplayOrder());
            assessment.setActive(request.getActive() == null || request.getActive());
            courseAssessmentRepository.save(assessment);
        }

        for (CourseAssessment existingAssessment : existingAssessments) {
            if (existingAssessment.getId() == null || incomingAssessmentIds.contains(existingAssessment.getId())) {
                continue;
            }
            if (onlineCourseVersionService.isAssessmentReferencedByPublishedHistory(
                    course,
                    existingAssessment.getId()
            ) || assessmentSubmissionRepository.existsByAssessmentId(existingAssessment.getId())) {
                existingAssessment.setActive(false);
                courseAssessmentRepository.save(existingAssessment);
                continue;
            }
            courseAssessmentRepository.delete(existingAssessment);
        }
    }

    private OnlineCourseModule findExistingModule(List<OnlineCourseModule> modules, Long moduleId) {
        if (moduleId == null) {
            return null;
        }
        return modules.stream()
                .filter(module -> moduleId.equals(module.getId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Module not found in this course"));
    }

    private OnlineLesson findExistingLesson(List<OnlineLesson> lessons, Long lessonId) {
        if (lessonId == null) {
            return null;
        }
        return lessons.stream()
                .filter(lesson -> lessonId.equals(lesson.getId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("OnlineLesson not found in this module"));
    }

    private CourseAssessment findExistingAssessment(List<CourseAssessment> assessments, Long assessmentId) {
        if (assessmentId == null) {
            return null;
        }
        return assessments.stream()
                .filter(assessment -> assessmentId.equals(assessment.getId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Assessment not found in this course"));
    }

    private void validateAssessmentConfiguration(ContentManagerCourseAssessmentRequest request, AssessmentBankItem bankItem) {
        AssessmentSkill skill = bankItem == null ? request.getSkill() : bankItem.getSkill();
        String uiConfigJson = bankItem == null ? request.getUiConfigJson() : bankItem.getUiConfigJson();
        String objectiveAnswerKey = bankItem == null ? request.getObjectiveAnswerKey() : bankItem.getObjectiveAnswerKey();
        if (uiConfigJson == null || uiConfigJson.isBlank()) {
            return;
        }
        try {
            var root = objectMapper.readTree(uiConfigJson);
            if (!root.isObject()) {
                throw new RuntimeException("Cấu hình đề thi phải là JSON object.");
            }
            if (skill == AssessmentSkill.LISTENING || skill == AssessmentSkill.READING) {
                if (!root.path("parts").isArray() || root.path("parts").isEmpty()) {
                    throw new RuntimeException("Cấu hình đề thi phải có ít nhất một phần.");
                }
                if (objectiveAnswerKey == null || objectiveAnswerKey.isBlank()
                        || !objectMapper.readTree(objectiveAnswerKey).isObject()) {
                    throw new RuntimeException("Đáp án tham chiếu của đề thi không hợp lệ.");
                }
                return;
            }
            if (skill == AssessmentSkill.WRITING
                    && (!root.path("tasks").isArray() || root.path("tasks").isEmpty())) {
                throw new RuntimeException("Cấu hình đề Writing phải có ít nhất một task.");
            }
            if (skill == AssessmentSkill.SPEAKING
                    && (!root.path("variants").isArray() || root.path("variants").isEmpty())) {
                throw new RuntimeException("Cấu hình đề Speaking phải có ít nhất một đề.");
            }
        } catch (JsonProcessingException exception) {
            throw new RuntimeException("Cấu hình đề thi hoặc đáp án tham chiếu không phải JSON hợp lệ.");
        }
    }

    private void validateAssessmentRubric(AssessmentSkill skill, AssessmentRubric rubric) {
        if ((skill == AssessmentSkill.LISTENING || skill == AssessmentSkill.READING) && rubric != null) {
            throw new RuntimeException("Bài Listening hoặc Reading không được dùng rubric chấm Writing/Speaking.");
        }
        if (skill == AssessmentSkill.WRITING
                && (rubric == null || rubric.getSkill() != AssessmentSkill.WRITING)) {
            throw new RuntimeException("Bài Writing cần một rubric Writing phù hợp.");
        }
        if (skill == AssessmentSkill.SPEAKING
                && (rubric == null || rubric.getSkill() != AssessmentSkill.SPEAKING)) {
            throw new RuntimeException("Bài Speaking cần một rubric Speaking phù hợp.");
        }
    }

    private String buildAssessmentValidationContext(
            ContentManagerCourseAssessmentRequest request,
            AssessmentBankItem bankItem,
            OnlineCourseModule targetModule,
            int assessmentIndex
    ) {
        String configuredTitle = bankItem == null ? request.getTitle() : bankItem.getTitle();
        String title = configuredTitle == null || configuredTitle.isBlank()
                ? "Bài kiểm tra " + (assessmentIndex + 1)
                : configuredTitle.trim();
        String location = targetModule == null
                ? "phần bài kiểm tra cuối khóa"
                : "mô-đun \"" + targetModule.getTitle() + "\"";
        return "Bài kiểm tra \"" + title + "\" tại " + location + ": ";
    }

    private OnlineCourseModule resolveAssessmentModule(List<OnlineCourseModule> modules, Long moduleId) {
        if (moduleId == null) {
            return null;
        }
        return modules.stream()
                .filter(module -> moduleId.equals(module.getId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Assessment module does not belong to this course"));
    }

    private AssessmentRubric resolveAssessmentRubric(Long rubricId) {
        if (rubricId == null) {
            return null;
        }
        AssessmentRubric rubric = assessmentRubricRepository.findById(rubricId)
                .orElseThrow(() -> new RuntimeException("Rubric not found"));
        if (!rubric.isActive()) {
            throw new RuntimeException("Rubric is not active");
        }
        return rubric;
    }

    private AssessmentBankItem resolveAssessmentBankItem(Long bankItemId) {
        if (bankItemId == null) {
            return null;
        }
        AssessmentBankItem bankItem = assessmentBankItemRepository.findById(bankItemId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đề trong ngân hàng đề."));
        if ("ARCHIVED".equalsIgnoreCase(bankItem.getStatus())) {
            throw new RuntimeException("Đề trong ngân hàng đã được lưu trữ.");
        }
        return bankItem;
    }

    private void applyAssessmentBankSnapshot(CourseAssessment assessment, AssessmentBankItem bankItem) {
        if (bankItem == null) {
            return;
        }
        assessment.setTitle(bankItem.getTitle());
        assessment.setDescription(bankItem.getDescription());
        assessment.setType(bankItem.getType());
        assessment.setSkill(bankItem.getSkill());
        assessment.setRubric(bankItem.getRubric());
        assessment.setAiEvaluationMode(bankItem.getAiEvaluationMode());
        assessment.setInstructions(bankItem.getInstructions());
        assessment.setObjectiveAnswerKey(bankItem.getObjectiveAnswerKey());
        assessment.setUiConfigJson(bankItem.getUiConfigJson());
        assessment.setPassingScore(bankItem.getPassingScore());
        assessment.setMaxScore(IeltsBandScale.normalizeConfiguredMaxScore(
                bankItem.getMaxScore(),
                bankItem.getType(),
                bankItem.getSkill(),
                bankItem.getAiEvaluationMode()
        ));
        assessment.setTimeLimitMinutes(defaultInt(bankItem.getTimeLimitMinutes()));
    }

    private void applyAssessmentBankSnapshot(CourseAssessment assessment) {
        applyAssessmentBankSnapshot(assessment, assessment.getAssessmentBankItem());
    }

    private void ensureModuleCanBeRemoved(OnlineCourseModule module) {
        for (OnlineLesson lesson : module.getLessons()) {
            ensureLessonCanBeRemoved(lesson);
        }
        List<CourseAssessment> moduleAssessments = courseAssessmentRepository.findByModule(module);
        for (CourseAssessment assessment : moduleAssessments) {
            if (assessment.getId() != null && assessmentSubmissionRepository.existsByAssessmentId(assessment.getId())) {
                throw new RuntimeException(
                        "Không thể xóa mô-đun \"" + module.getTitle()
                                + "\" vì đã có bài làm học viên trong bài kiểm tra \"" + assessment.getTitle() + "\"."
                );
            }
        }
        courseAssessmentRepository.deleteAll(moduleAssessments);
        courseAssessmentRepository.flush();
    }

    private void ensureLessonCanBeRemoved(OnlineLesson lesson) {
        if (lesson.getId() != null && lesson.getModule() != null && lesson.getModule().getOnlineCourse() != null) {
            onlineCourseVersionService.assertLessonCanBeRemoved(
                    lesson.getModule().getOnlineCourse(),
                    lesson.getId()
            );
        }
        if (lesson.getId() != null && lessonProgressRepository.existsByLessonId(lesson.getId())) {
            throw new RuntimeException(
                    "Không thể xóa bài học \"" + lesson.getTitle() + "\" vì đã có tiến độ học viên."
            );
        }
    }

    private CourseAssessmentResponse toManagerAssessmentResponse(CourseAssessment assessment) {
        applyAssessmentBankSnapshot(assessment);
        return CourseAssessmentResponse.builder()
                .id(assessment.getId())
                .courseId(assessment.getOnlineCourse().getId())
                .moduleId(assessment.getModule() == null ? null : assessment.getModule().getId())
                .assessmentBankItemId(assessment.getAssessmentBankItem() == null ? null : assessment.getAssessmentBankItem().getId())
                .moduleTitle(assessment.getModule() == null ? null : assessment.getModule().getTitle())
                .title(assessment.getTitle())
                .description(assessment.getDescription())
                .type(assessment.getType())
                .skill(assessment.getSkill())
                .aiEvaluationMode(assessment.getAiEvaluationMode())
                .instructions(assessment.getInstructions())
                .objectiveAnswerKey(assessment.getObjectiveAnswerKey())
                .uiConfigJson(resolveAssessmentUiConfig(assessment))
                .passingScore(assessment.getPassingScore())
                .maxScore(IeltsBandScale.resolveScoreCap(assessment))
                .timeLimitMinutes(assessment.getTimeLimitMinutes())
                .displayOrder(assessment.getDisplayOrder())
                .active(assessment.isActive())
                .rubric(toRubricResponse(assessment.getRubric()))
                .latestSubmission((AiAssessmentSubmissionResponse) null)
                .previousSubmission((AiAssessmentSubmissionResponse) null)
                .build();
    }

    private String resolveAssessmentUiConfig(CourseAssessment assessment) {
        if (assessment.getUiConfigJson() != null && !assessment.getUiConfigJson().isBlank()) {
            return assessment.getUiConfigJson();
        }
        String instructions = assessment.getInstructions();
        String marker = "[ENGLISHLAB_UI_CONFIG]";
        if (instructions == null) {
            return null;
        }
        int markerIndex = instructions.indexOf(marker);
        if (markerIndex < 0) {
            return null;
        }
        String embeddedConfig = instructions.substring(markerIndex + marker.length()).trim();
        return embeddedConfig.isBlank() ? null : embeddedConfig;
    }

    private AssessmentRubricResponse toRubricResponse(AssessmentRubric rubric) {
        if (rubric == null) {
            return null;
        }
        return AssessmentRubricResponse.builder()
                .id(rubric.getId())
                .name(rubric.getName())
                .examType(rubric.getExamType())
                .skill(rubric.getSkill())
                .taskType(rubric.getTaskType())
                .scoringScale(rubric.getScoringScale())
                .description(rubric.getDescription())
                .active(rubric.isActive())
                .criteria(rubric.getCriteria().stream()
                        .sorted(Comparator.comparing(RubricCriterion::getDisplayOrder).thenComparing(RubricCriterion::getId))
                        .map(criterion -> RubricCriterionResponse.builder()
                                .id(criterion.getId())
                                .name(criterion.getName())
                                .weight(criterion.getWeight())
                                .description(criterion.getDescription())
                                .bandDescriptors(criterion.getBandDescriptors())
                                .displayOrder(criterion.getDisplayOrder())
                                .build())
                        .toList())
                .build();
    }

    private LessonResponse toLessonResponse(OnlineLesson lesson) {
        return LessonResponse.builder()
                .id(lesson.getId())
                .lessonKey(lesson.getLessonKey())
                .title(lesson.getTitle())
                .description(lesson.getDescription())
                .contentType(lesson.getContentType())
                .contentText(lesson.getContentText())
                .videoUrl(lesson.getVideoUrl())
                .bunnyVideoId(lesson.getBunnyVideoId())
                .bunnyLibraryId(lesson.getBunnyLibraryId())
                .bunnyCdnUrl(lesson.getBunnyCdnUrl())
                .materialUrl(lesson.getMaterialUrl())
                .transcriptSegments(readTranscriptSegments(lesson.getTranscriptSegmentsJson()))
                .durationMinutes(lesson.getDurationMinutes())
                .displayOrder(lesson.getDisplayOrder())
                .preview(lesson.isPreview())
                .flashcardSets(toFlashcardSetResponses(lesson))
                .build();
    }

    private void synchronizeLessonFlashcardRefs(OnlineLesson lesson, List<Long> flashcardSetIds) {
        courseLessonFlashcardRefRepository.deleteByLessonId(lesson.getId());
        courseLessonFlashcardRefRepository.flush();
        lesson.getFlashcardRefs().clear();
        if (flashcardSetIds == null || flashcardSetIds.isEmpty()) {
            return;
        }
        Set<Long> uniqueIds = new LinkedHashSet<>(flashcardSetIds);
        int displayOrder = 1;
        for (Long flashcardSetId : uniqueIds) {
            ContentBankItem item = contentBankIdResolver.requireItem(ContentBankType.FLASHCARD, flashcardSetId);
            contentBankTypeGuard.assertFlashcard(item);
            if ("ARCHIVED".equalsIgnoreCase(item.getStatus())) {
                throw new RuntimeException("Bộ flashcard \"" + item.getTitle() + "\" đã được lưu trữ.");
            }
            lesson.addFlashcardRef(CourseLessonFlashcardRef.builder()
                    .contentBankItem(item)
                    .displayOrder(displayOrder++)
                    .build());
        }
    }

    private List<FlashcardSetResponse> toFlashcardSetResponses(OnlineLesson lesson) {
        if (lesson.getFlashcardRefs() == null) {
            return List.of();
        }
        return lesson.getFlashcardRefs().stream()
                .map(ref -> toFlashcardSetResponse(ref.getContentBankItem()))
                .filter(response -> response != null)
                .toList();
    }

    private FlashcardSetResponse toFlashcardSetResponse(ContentBankItem item) {
        if (item == null) {
            return null;
        }
        return FlashcardSetResponse.builder()
                .id(item.getId())
                .title(item.getTitle())
                .description(item.getDescription())
                .examCategory(item.getExamCategory())
                .skill(item.getSkill())
                .tags(item.getTags())
                .cardsJson(ContentBankPayloadSupport.cardsJsonFromPayload(item.getPayloadJsonb()))
                .status(item.getStatus())
                .displayOrder(item.getDisplayOrder())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }

    private void applyLessonTranscript(OnlineLesson lesson, LessonRequest lessonRequest, String previousVideoUrl) {
        List<TranscriptSegmentRequest> requestedSegments = lessonRequest.getTranscriptSegments();
        boolean hasExplicitTranscript = requestedSegments != null && requestedSegments.stream()
                .anyMatch(segment -> segment != null && segment.getText() != null && !segment.getText().isBlank());

        if (hasExplicitTranscript) {
            lesson.setTranscriptSegmentsJson(writeTranscriptSegments(toTranscriptResponses(requestedSegments)));
            return;
        }

        String nextVideoUrl = lessonRequest.getVideoUrl();
        boolean videoChanged = previousVideoUrl == null
                ? nextVideoUrl != null && !nextVideoUrl.isBlank()
                : !previousVideoUrl.equals(nextVideoUrl);
        boolean missingTranscript = lesson.getTranscriptSegmentsJson() == null
                || lesson.getTranscriptSegmentsJson().isBlank()
                || "[]".equals(lesson.getTranscriptSegmentsJson().trim());

        // Empty transcript payload from the editor should still auto-fetch YouTube/Bunny captions.
        if (videoChanged || missingTranscript) {
            OnlineLesson probe = new OnlineLesson();
            probe.setVideoUrl(nextVideoUrl);
            probe.setBunnyVideoId(lesson.getBunnyVideoId());
            probe.setBunnyLibraryId(lesson.getBunnyLibraryId());
            if (canAutoFetchTranscript(probe)) {
                List<TranscriptSegmentResponse> autoSegments = resolveAutoTranscriptSegments(probe);
                if (!autoSegments.isEmpty()) {
                    lesson.setTranscriptSegmentsJson(writeTranscriptSegments(autoSegments));
                    return;
                }
            }
        }
        if (nextVideoUrl == null || nextVideoUrl.isBlank()) {
            lesson.setTranscriptSegmentsJson(null);
            return;
        }
        if (requestedSegments != null) {
            lesson.setTranscriptSegmentsJson(writeTranscriptSegments(toTranscriptResponses(requestedSegments)));
        }
    }

    private boolean canAutoFetchTranscript(OnlineLesson lesson) {
        if (lesson == null) {
            return false;
        }
        if (youTubeTranscriptService.extractVideoId(lesson.getVideoUrl()).isPresent()) {
            return true;
        }
        return bunnyStreamService.resolveVideoRef(
                lesson.getVideoUrl(),
                lesson.getBunnyVideoId(),
                lesson.getBunnyLibraryId()
        ).isPresent();
    }

    private List<TranscriptSegmentResponse> resolveAutoTranscriptSegments(OnlineLesson lesson) {
        if (lesson == null) {
            return List.of();
        }

        if (youTubeTranscriptService.extractVideoId(lesson.getVideoUrl()).isPresent()) {
            List<TranscriptSegmentResponse> youtubeSegments = youTubeTranscriptService.fetchTranscriptSegments(lesson.getVideoUrl());
            if (!youtubeSegments.isEmpty()) {
                return youtubeSegments;
            }
        }

        return bunnyStreamService.resolveVideoRef(
                        lesson.getVideoUrl(),
                        lesson.getBunnyVideoId(),
                        lesson.getBunnyLibraryId()
                )
                .map(ref -> bunnyStreamService.fetchTranscriptSegments(ref.libraryId(), ref.videoId()))
                .orElseGet(List::of);
    }

    private List<TranscriptSegmentResponse> toTranscriptResponses(List<TranscriptSegmentRequest> segments) {
        if (segments == null) {
            return List.of();
        }
        return segments.stream()
                .filter(segment -> segment != null && segment.getText() != null && !segment.getText().isBlank())
                .map(segment -> TranscriptSegmentResponse.builder()
                        .startSeconds(segment.getStartSeconds())
                        .endSeconds(segment.getEndSeconds())
                        .text(segment.getText().trim())
                        .build())
                .toList();
    }

    private String writeTranscriptSegments(List<TranscriptSegmentResponse> segments) {
        List<TranscriptSegmentResponse> safeSegments = TranscriptSegmentNormalizer.normalize(segments);
        try {
            return objectMapper.writeValueAsString(safeSegments);
        } catch (JsonProcessingException ex) {
            return "[]";
        }
    }

    private List<TranscriptSegmentResponse> readTranscriptSegments(String transcriptSegmentsJson) {
        if (transcriptSegmentsJson == null || transcriptSegmentsJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(
                    transcriptSegmentsJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, TranscriptSegmentResponse.class)
            );
        } catch (Exception ex) {
            return List.of();
        }
    }

    private Specification<OnlineCourse> courseSpec(String keyword, String category, Double currentBand, Double targetBand, AssessmentSkill skill, CourseLevel level, PackageStatus status) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<OnlineCourse, CourseCategory> categoryJoin = root.join("category");
            query.distinct(true);
            predicates.add(criteriaBuilder.isFalse(root.get("deleted")));

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            if (level != null) {
                predicates.add(criteriaBuilder.equal(root.get("level"), level));
            }
            if (category != null && !category.isBlank()) {
                predicates.add(criteriaBuilder.equal(categoryJoin.get("code"), normalizeCategoryCode(category)));
            }
            if (keyword != null) {
                String pattern = "%" + keyword.toLowerCase(Locale.ROOT) + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("shortDescription")), pattern)
                ));
            }
            if (currentBand != null) {
                predicates.add(criteriaBuilder.and(
                        criteriaBuilder.isNotNull(root.get("recommendedCurrentBandMin")),
                        criteriaBuilder.lessThanOrEqualTo(root.get("recommendedCurrentBandMin"), currentBand)
                ));
            }
            if (targetBand != null) {
                predicates.add(criteriaBuilder.and(
                        criteriaBuilder.isNotNull(root.get("targetBand")),
                        criteriaBuilder.greaterThanOrEqualTo(root.get("targetBand"), targetBand)
                ));
            }
            if (skill != null) {
                String skillToken = skill.name().toLowerCase(Locale.ROOT);
                var assessmentSubquery = query.subquery(Long.class);
                var assessmentRoot = assessmentSubquery.from(CourseAssessment.class);
                assessmentSubquery.select(assessmentRoot.get("id"));
                assessmentSubquery.where(
                        criteriaBuilder.equal(assessmentRoot.get("onlineCourse"), root),
                        criteriaBuilder.isTrue(assessmentRoot.get("active")),
                        criteriaBuilder.equal(assessmentRoot.get("skill"), skill)
                );

                Join<OnlineCourse, OnlineCourseModule> moduleJoin = root.join("modules", jakarta.persistence.criteria.JoinType.LEFT);
                Join<OnlineCourseModule, OnlineLesson> lessonJoin = moduleJoin.join("lessons", jakarta.persistence.criteria.JoinType.LEFT);
                Predicate lessonMatches = criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(lessonJoin.get("title")), "%" + skillToken + "%"),
                        criteriaBuilder.like(criteriaBuilder.lower(lessonJoin.get("description")), "%" + skillToken + "%"),
                        criteriaBuilder.like(criteriaBuilder.lower(lessonJoin.get("contentText")), "%" + skillToken + "%")
                );

                predicates.add(criteriaBuilder.or(criteriaBuilder.exists(assessmentSubquery), lessonMatches));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private void assertCanEditCourseContent(String actorEmail) {
        User actor = userRepository.findByEmail(actorEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng."));
        if (!ContentManagementRolePolicy.canEdit(actor)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Bạn không có quyền thay đổi cấu trúc khóa học."
            );
        }
    }

    private void validateModuleOrder(List<OnlineCourseModule> modules, List<ModuleOrderItemRequest> items) {
        if (items == null || items.size() != modules.size()) {
            throw new IllegalArgumentException("Danh sách reorder phải chứa đầy đủ mô-đun của khóa học.");
        }
        Set<Long> expectedIds = modules.stream().map(OnlineCourseModule::getId).collect(java.util.stream.Collectors.toSet());
        Set<Long> actualIds = items.stream().map(ModuleOrderItemRequest::getModuleId).collect(java.util.stream.Collectors.toSet());
        if (actualIds.size() != items.size()) {
            throw new IllegalArgumentException("Danh sách reorder có moduleId trùng nhau.");
        }
        if (!actualIds.equals(expectedIds)) {
            throw new IllegalArgumentException("Có mô-đun không thuộc khóa học hoặc bị thiếu trong payload.");
        }
        validateContinuousOrder(items.stream().map(ModuleOrderItemRequest::getOrderIndex).toList(), "mô-đun");
    }

    private void validateLessonOrder(List<OnlineLesson> lessons, List<LessonOrderItemRequest> items) {
        if (items == null || items.size() != lessons.size()) {
            throw new IllegalArgumentException("Danh sách reorder phải chứa đầy đủ bài học của mô-đun.");
        }
        Set<Long> expectedIds = lessons.stream().map(OnlineLesson::getId).collect(java.util.stream.Collectors.toSet());
        Set<Long> actualIds = items.stream().map(LessonOrderItemRequest::getLessonId).collect(java.util.stream.Collectors.toSet());
        if (actualIds.size() != items.size()) {
            throw new IllegalArgumentException("Danh sách reorder có lessonId trùng nhau.");
        }
        if (!actualIds.equals(expectedIds)) {
            throw new IllegalArgumentException("Có bài học không thuộc mô-đun hoặc bị thiếu trong payload.");
        }
        validateContinuousOrder(items.stream().map(LessonOrderItemRequest::getOrderIndex).toList(), "bài học");
    }

    private void validateContinuousOrder(List<Integer> orderIndexes, String itemLabel) {
        Set<Integer> uniqueIndexes = new HashSet<>(orderIndexes);
        if (uniqueIndexes.size() != orderIndexes.size()) {
            throw new IllegalArgumentException("Thứ tự " + itemLabel + " không được trùng nhau.");
        }
        for (int expected = 1; expected <= orderIndexes.size(); expected++) {
            if (!uniqueIndexes.contains(expected)) {
                throw new IllegalArgumentException("Thứ tự " + itemLabel + " phải liên tục từ 1 đến " + orderIndexes.size() + ".");
            }
        }
    }

    private void moveModuleOrdersToTemporaryRange(List<OnlineCourseModule> modules) {
        for (int moduleIndex = 0; moduleIndex < modules.size(); moduleIndex++) {
            modules.get(moduleIndex).setDisplayOrder(-(moduleIndex + 1));
        }
    }

    private void moveContentOrdersToTemporaryRange(List<OnlineCourseModule> modules) {
        moveModuleOrdersToTemporaryRange(modules);
        for (OnlineCourseModule module : modules) {
            List<OnlineLesson> lessons = module.getLessons();
            for (int lessonIndex = 0; lessonIndex < lessons.size(); lessonIndex++) {
                lessons.get(lessonIndex).setDisplayOrder(-(lessonIndex + 1));
            }
        }
    }

    private void validateCourseRequest(OnlineCourseRequest request) {
        String category = normalizeCategoryCode(request.getCategory());
        if (!ENGLISH_COURSE_CATEGORIES.contains(category)) {
            throw new IllegalArgumentException(
                    "EnglishLab chỉ cho phép khóa IELTS, TOEIC, tiếng Anh giao tiếp hoặc tiếng Anh nền tảng."
            );
        }
        Double minBand = request.getRecommendedCurrentBandMin();
        validateCourseScoreProfile(
                category,
                request.getTargetScore(),
                minBand,
                request.getTargetBand()
        );

        boolean hasPathCode = request.getLearningPathCode() != null && !request.getLearningPathCode().isBlank();
        boolean hasPathName = request.getLearningPathName() != null && !request.getLearningPathName().isBlank();
        if (hasPathCode != hasPathName) {
            throw new IllegalArgumentException("Mã và tên lộ trình học phải được nhập cùng nhau.");
        }
        if (hasPathCode && request.getLearningPathOrder() == null) {
            throw new IllegalArgumentException("Khóa học thuộc lộ trình phải có thứ tự.");
        }
        if (request.getStatus() == PackageStatus.PUBLISHED
                && (request.getModules() == null
                || request.getModules().isEmpty()
                || request.getModules().stream().allMatch(module -> module.getLessons() == null || module.getLessons().isEmpty()))) {
            throw new IllegalArgumentException("Khóa học cần có ít nhất một mô-đun và bài học trước khi xuất bản.");
        }
        if (request.getStatus() == PackageStatus.PUBLISHED
                && (request.getTargetOutcome() == null || request.getTargetOutcome().isBlank())) {
            throw new IllegalArgumentException("Khóa học phải mô tả chuẩn đầu ra tiếng Anh trước khi xuất bản.");
        }
    }

    private void validatePublishableCourse(OnlineCourse course) {
        OnlineCourseVersion editableVersion = onlineCourseVersionService.requireEditableVersion(course);
        List<OnlineCourseModule> modules = editableVersion.getModules();
        if (modules == null
                || modules.isEmpty()
                || modules.stream().allMatch(module -> module.getLessons() == null || module.getLessons().isEmpty())) {
            throw new IllegalArgumentException("Khóa học cần có ít nhất một mô-đun và bài học trước khi xuất bản.");
        }
        String category = course.getCategory() == null ? "" : normalizeCategoryCode(course.getCategory().getCode());
        validateCourseScoreProfile(
                category,
                course.getTargetScore(),
                course.getRecommendedCurrentBandMin(),
                course.getTargetBand()
        );
        if (course.getTargetOutcome() == null || course.getTargetOutcome().isBlank()) {
            throw new IllegalArgumentException("Khóa học phải mô tả chuẩn đầu ra tiếng Anh trước khi xuất bản.");
        }
        if (courseAssessmentRepository
                .findByOnlineCourseAndActiveTrueOrderByDisplayOrderAscIdAsc(course)
                .isEmpty()) {
            throw new IllegalArgumentException("Khóa học tiếng Anh phải có ít nhất một bài đánh giá kỹ năng trước khi xuất bản.");
        }
    }

    private void validateCourseScoreProfile(
            String category,
            String targetScoreLabel,
            Double minBand,
            Double targetBand
    ) {
        if ("IELTS".equals(category)) {
            if (targetBand == null || targetBand < 0 || targetBand > 9 || Math.rint(targetBand * 2) != targetBand * 2) {
                throw new IllegalArgumentException("Khóa IELTS phải có band mục tiêu từ 0 đến 9, tăng theo bước 0.5.");
            }
            return;
        }
        if ("TOEIC".equals(category)) {
            if (minBand != null || targetBand != null) {
                throw new IllegalArgumentException("Khóa TOEIC không sử dụng thang band IELTS.");
            }
            int score;
            try {
                score = Integer.parseInt(targetScoreLabel == null ? "" : targetScoreLabel.trim());
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Khóa TOEIC phải có điểm mục tiêu dạng số.");
            }
            if (score < 10 || score > 990 || score % 5 != 0) {
                throw new IllegalArgumentException("Điểm mục tiêu TOEIC phải từ 10 đến 990 và tăng theo bước 5.");
            }
            return;
        }
        if (minBand != null || targetBand != null) {
            throw new IllegalArgumentException("Khóa tiếng Anh giao tiếp/nền tảng không sử dụng band IELTS.");
        }
    }

    private String normalizeCategoryCode(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private CourseCertificateResponse buildCertificateResponse(
            OnlineCourse course,
            OnlineCourseEnrollment enrollment,
            User student,
            CourseCompletionResponse completion,
            boolean verified
    ) {
        if (!completion.isEligibleForCertificate()) {
            return CourseCertificateResponse.builder()
                    .eligible(false)
                    .verified(false)
                    .courseId(course.getId())
                    .enrollmentId(enrollment.getId())
                    .learnerName(resolveLearnerName(student))
                    .courseTitle(course.getTitle())
                    .targetOutcome(course.getTargetOutcome())
                    .completionDate(completion.getCompletionDate())
                    .platformName("EnglishLab")
                    .message(completion.getStatusReason())
                    .build();
        }

        String verificationCode = buildVerificationCode(enrollment, course, student);
        return CourseCertificateResponse.builder()
                .eligible(true)
                .verified(verified)
                .courseId(course.getId())
                .enrollmentId(enrollment.getId())
                .learnerName(resolveLearnerName(student))
                .courseTitle(course.getTitle())
                .targetOutcome(course.getTargetOutcome())
                .completionDate(completion.getCompletionDate())
                .verificationCode(verificationCode)
                .verificationUrl("/api/online-courses/certificates/" + verificationCode)
                .platformName("EnglishLab")
                .message(verified
                        ? "Chứng nhận hoàn thành hợp lệ."
                        : "Bạn đã đủ điều kiện nhận chứng nhận hoàn thành.")
                .build();
    }

    private OnlineCourseEnrollment findEnrollmentByCertificateCode(String verificationCode) {
        var matcher = CERTIFICATE_CODE_PATTERN.matcher(verificationCode == null ? "" : verificationCode.trim().toUpperCase(Locale.ROOT));
        if (!matcher.matches()) {
            throw new RuntimeException("Mã xác thực chứng nhận không hợp lệ.");
        }

        Long enrollmentId = Long.parseLong(matcher.group(1));
        Long courseId = Long.parseLong(matcher.group(2));
        OnlineCourseEnrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chứng nhận cần xác thực."));
        OnlineCourse course = java.util.Optional.of(enrollment.getOnlineCourse())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học của chứng nhận này."));

        if (!courseId.equals(course.getId())) {
            throw new RuntimeException("Mã xác thực chứng nhận không khớp với khóa học.");
        }
        String expectedCode = buildVerificationCode(enrollment, course, enrollment.getStudent());
        if (!expectedCode.equalsIgnoreCase(verificationCode)) {
            throw new RuntimeException("Mã xác thực chứng nhận không hợp lệ.");
        }
        return enrollment;
    }

    private String buildVerificationCode(OnlineCourseEnrollment enrollment, OnlineCourse course, User student) {
        try {
            String payload = "%s:%s:%s:%s".formatted(
                    enrollment.getId(),
                    course.getId(),
                    student.getId(),
                    enrollment.getRegisteredAt()
            );
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String checksum = HexFormat.of().formatHex(digest.digest(payload.getBytes(StandardCharsets.UTF_8)))
                    .substring(0, 10)
                    .toUpperCase(Locale.ROOT);
            return "ELC-%d-%d-%s".formatted(enrollment.getId(), course.getId(), checksum);
        } catch (Exception ex) {
            throw new RuntimeException("Chưa thể tạo mã xác thực. Vui lòng thử lại.");
        }
    }

    private String resolveLearnerName(User student) {
        if (student.getFullName() != null && !student.getFullName().isBlank() && !student.getFullName().trim().equalsIgnoreCase("Học viên EnglishLab")) {
            return student.getFullName().trim();
        }
        if (student.getEmail() != null) {
            String[] parts = student.getEmail().split("@");
            if (parts.length > 0) {
                return parts[0];
            }
        }
        return "Học viên EnglishLab";
    }

    private String generateUniqueSlug(String title) {
        String baseSlug = toSlug(title);
        String slug = baseSlug;
        int index = 2;
        while (onlineCourseRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + index++;
        }
        return slug;
    }

    private String toSlug(String input) {
        String nowhitespace = WHITESPACE.matcher(input.trim()).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        slug = slug.replaceAll("-+", "-").toLowerCase(Locale.ENGLISH);
        return slug.isBlank() ? "online-course" : slug;
    }

    private String clean(String keyword) {
        return keyword == null || keyword.isBlank() ? null : keyword.trim();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private Integer defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private BigDecimal defaultBigDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal resolveSalePrice(BigDecimal price, BigDecimal salePrice) {
        BigDecimal originalPrice = defaultBigDecimal(price);
        if (salePrice == null || salePrice.compareTo(BigDecimal.ZERO) < 0 || salePrice.compareTo(originalPrice) >= 0) {
            return null;
        }
        return salePrice;
    }

    private boolean isFreeCourse(OnlineCourse course) {
        BigDecimal price = defaultBigDecimal(course.getPrice());
        BigDecimal salePrice = resolveSalePrice(price, course.getSalePrice());
        return (salePrice == null ? price : salePrice).compareTo(BigDecimal.ZERO) <= 0;
    }
}
