package fu.sap490.g23.backend.service.course.impl;

import fu.sap490.g23.backend.service.course.*;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fu.sap490.g23.backend.dto.request.assessment.ContentManagerCourseAssessmentRequest;
import fu.sap490.g23.backend.dto.request.course.LessonRequest;
import fu.sap490.g23.backend.dto.request.course.ModuleRequest;
import fu.sap490.g23.backend.dto.request.course.OnlineCourseRequest;
import fu.sap490.g23.backend.dto.request.course.LearningPathOrderRequest;
import fu.sap490.g23.backend.dto.request.course.TranscriptSegmentRequest;
import fu.sap490.g23.backend.dto.response.assessment.AiAssessmentSubmissionResponse;
import fu.sap490.g23.backend.dto.response.assessment.AssessmentRubricResponse;
import fu.sap490.g23.backend.dto.response.assessment.CourseAssessmentResponse;
import fu.sap490.g23.backend.dto.response.assessment.RubricCriterionResponse;
import fu.sap490.g23.backend.dto.response.course.BunnyVideoUploadResponse;
import fu.sap490.g23.backend.dto.response.course.CourseCertificateResponse;
import fu.sap490.g23.backend.dto.response.course.CourseCompletionResponse;
import fu.sap490.g23.backend.dto.response.course.CourseStatsResponse;
import fu.sap490.g23.backend.dto.response.course.LessonResponse;
import fu.sap490.g23.backend.dto.response.course.OnlineCourseResponse;
import fu.sap490.g23.backend.dto.response.course.PackageEnrollmentResponse;
import fu.sap490.g23.backend.dto.response.course.TranscriptSegmentResponse;
import fu.sap490.g23.backend.dto.response.course.VocabularyTermResponse;
import fu.sap490.g23.backend.dto.response.course.LearnerLearningPathCourseResponse;
import fu.sap490.g23.backend.dto.response.course.LearnerLearningPathResponse;
import fu.sap490.g23.backend.dto.response.curriculum.FlashcardSetResponse;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.assessment.AssessmentRubric;
import fu.sap490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sap490.g23.backend.entity.assessment.CourseAssessment;
import fu.sap490.g23.backend.entity.assessment.PlacementTestAttempt;
import fu.sap490.g23.backend.service.assessment.IeltsBandScale;
import fu.sap490.g23.backend.entity.assessment.RubricCriterion;
import fu.sap490.g23.backend.entity.course.*;
import fu.sap490.g23.backend.entity.course.enums.*;
import fu.sap490.g23.backend.entity.curriculum.AssessmentBankItem;
import fu.sap490.g23.backend.entity.curriculum.FlashcardSet;
import fu.sap490.g23.backend.exception.CourseUnavailableException;
import fu.sap490.g23.backend.repository.UserRepository;
import fu.sap490.g23.backend.repository.assessment.AssessmentRubricRepository;
import fu.sap490.g23.backend.repository.assessment.AssessmentSubmissionRepository;
import fu.sap490.g23.backend.repository.assessment.CourseAssessmentRepository;
import fu.sap490.g23.backend.repository.assessment.PlacementTestAttemptRepository;
import fu.sap490.g23.backend.repository.course.*;
import fu.sap490.g23.backend.repository.curriculum.AssessmentBankItemRepository;
import fu.sap490.g23.backend.repository.curriculum.FlashcardSetRepository;
import fu.sap490.g23.backend.service.mail.CourseEnrollmentMailService;
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

    private final OnlineCourseRepository onlineCourseRepository;
    private final LearningPackageRepository learningPackageRepository;
    private final PackageTypeRepository packageTypeRepository;
    private final CourseCategoryRepository courseCategoryRepository;
    private final PackageEnrollmentRepository enrollmentRepository;
    private final LessonRepository lessonRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final VocabularyProgressRepository vocabularyProgressRepository;
    private final CourseAssessmentRepository courseAssessmentRepository;
    private final AssessmentBankItemRepository assessmentBankItemRepository;
    private final FlashcardSetRepository flashcardSetRepository;
    private final CourseLessonFlashcardRefRepository courseLessonFlashcardRefRepository;
    private final AssessmentRubricRepository assessmentRubricRepository;
    private final AssessmentSubmissionRepository assessmentSubmissionRepository;
    private final PlacementTestAttemptRepository placementTestAttemptRepository;
    private final UserRepository userRepository;
    private final OnlineCourseMapper mapper;
    private final BunnyStreamService bunnyStreamService;
    private final CourseProgressService courseProgressService;
    private final CourseProgressionGuard courseProgressionGuard;
    private final CourseEnrollmentAccessPolicy courseEnrollmentAccessPolicy;
    private final CourseEnrollmentMailService courseEnrollmentMailService;
    private final YouTubeTranscriptService youTubeTranscriptService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(readOnly = true)
    public Page<OnlineCourseResponse> getPublicCourses(String keyword, String category, Double currentBand, Double targetBand, AssessmentSkill skill, Pageable pageable) {
        return onlineCourseRepository.findAll(courseSpec(clean(keyword), category, currentBand, targetBand, skill, PackageStatus.PUBLISHED), pageable)
                .map(mapper::toPublicResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public OnlineCourseResponse getPublicCourse(String slugOrId) {
        return mapper.toPublicResponse(findPublicCourse(slugOrId));
    }

    @Override
    @Transactional(readOnly = true)
    public CourseCertificateResponse verifyCourseCertificate(String verificationCode) {
        PackageEnrollment enrollment = findEnrollmentByCertificateCode(verificationCode);
        OnlineCourse course = onlineCourseRepository.findByLearningPackage(enrollment.getLearningPackage())
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
    public Page<OnlineCourseResponse> getManagerCourses(String keyword, String category, PackageStatus status, Pageable pageable) {
        return onlineCourseRepository.findAll(courseSpec(clean(keyword), category, null, null, null, status), pageable)
                .map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public OnlineCourseResponse getManagerCourse(String slugOrId) {
        return mapper.toResponse(findManagerCourse(slugOrId));
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
        OnlineCourse course = findCourse(courseId);
        synchronizeAssessments(course, requests == null ? List.of() : requests);
        OnlineCourse savedCourse = onlineCourseRepository.save(course);
        courseProgressService.refreshCourseEnrollments(savedCourse);
        return courseAssessmentRepository.findByOnlineCourseAndActiveTrueOrderByDisplayOrderAscIdAsc(course).stream()
                .map(this::toManagerAssessmentResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CourseStatsResponse getStats() {
        return CourseStatsResponse.builder()
                .totalCourses(onlineCourseRepository.countByLearningPackageDeletedFalse())
                .publishedCourses(onlineCourseRepository.countByLearningPackageDeletedFalseAndLearningPackageStatus(PackageStatus.PUBLISHED))
                .draftCourses(onlineCourseRepository.countByLearningPackageDeletedFalseAndLearningPackageStatus(PackageStatus.DRAFT))
                .archivedCourses(onlineCourseRepository.countByLearningPackageDeletedFalseAndLearningPackageStatus(PackageStatus.ARCHIVED))
                .totalLessons(lessonRepository.countActiveLessons())
                .totalEnrollments(enrollmentRepository.count())
                .build();
    }

    @Override
    public OnlineCourseResponse createCourse(OnlineCourseRequest request, String creatorEmail) {
        validateCourseRequest(request);
        User creator = userRepository.findByEmail(creatorEmail).orElse(null);
        PackageType packageType = packageTypeRepository.findByCode(PackageTypeCode.ONLINE_COURSE)
                .orElseThrow(() -> new RuntimeException("ONLINE_COURSE package type is missing"));

        CourseCategory category = courseCategoryRepository.findByCode(normalizeCategoryCode(request.getCategory()))
                .orElseThrow(() -> new RuntimeException("Course category not found"));
        if (!category.isActive()) {
            throw new IllegalArgumentException("Danh mục khóa học đã ngừng hoạt động.");
        }

        LearningPackage learningPackage = LearningPackage.builder()
                .packageType(packageType)
                .title(request.getTitle().trim())
                .slug(generateUniqueSlug(request.getTitle()))
                .shortDescription(request.getShortDescription())
                .description(request.getDescription())
                .targetScore(request.getTargetScore())
                .duration(request.getDuration())
                .studyMode(request.getStudyMode())
                .price(defaultBigDecimal(request.getPrice()))
                .salePrice(resolveSalePrice(request.getPrice(), request.getSalePrice()))
                .thumbnailUrl(request.getThumbnailUrl())
                .status(request.getStatus() == null ? PackageStatus.DRAFT : request.getStatus())
                .displayOrder(defaultInt(request.getDisplayOrder()))
                .featured(Boolean.TRUE.equals(request.getFeatured()))
                .createdBy(creator)
                .build();


        OnlineCourse course = OnlineCourse.builder()
                .learningPackage(learningPackage)
                .category(category)
                .level(request.getLevel())
                .recommendedCurrentBandMin(request.getRecommendedCurrentBandMin())
                .recommendedCurrentBandMax(request.getRecommendedCurrentBandMax())
                .targetBand(request.getTargetBand())
                .learningPathCode(request.getLearningPathCode())
                .learningPathName(request.getLearningPathName())
                .learningPathOrder(request.getLearningPathOrder())
                .targetOutcome(request.getTargetOutcome())
                .recommendedNextCourseSlug(request.getRecommendedNextCourseSlug())
                .totalLessons(defaultInt(request.getTotalLessons()))
                .totalHours(defaultInt(request.getTotalHours()))
                .build();
        rebuildModules(course, request.getModules());
        return mapper.toResponse(onlineCourseRepository.save(course));
    }

    @Override
    public OnlineCourseResponse updateCourse(Long id, OnlineCourseRequest request) {
        validateCourseRequest(request);
        OnlineCourse course = findCourse(id);
        LearningPackage learningPackage = course.getLearningPackage();

        CourseCategory category = courseCategoryRepository.findByCode(normalizeCategoryCode(request.getCategory()))
                .orElseThrow(() -> new RuntimeException("Course category not found"));
        if (!category.isActive()
                && (course.getCategory() == null || !category.getId().equals(course.getCategory().getId()))) {
            throw new IllegalArgumentException("Danh mục khóa học đã ngừng hoạt động.");
        }

        learningPackage.setTitle(request.getTitle().trim());
        learningPackage.setShortDescription(request.getShortDescription());
        learningPackage.setDescription(request.getDescription());
        learningPackage.setTargetScore(request.getTargetScore());
        learningPackage.setDuration(request.getDuration());
        learningPackage.setStudyMode(request.getStudyMode());
        learningPackage.setPrice(defaultBigDecimal(request.getPrice()));
        learningPackage.setSalePrice(resolveSalePrice(request.getPrice(), request.getSalePrice()));
        learningPackage.setThumbnailUrl(request.getThumbnailUrl());
        learningPackage.setStatus(request.getStatus() == null ? learningPackage.getStatus() : request.getStatus());
        learningPackage.setDisplayOrder(defaultInt(request.getDisplayOrder()));
        learningPackage.setFeatured(Boolean.TRUE.equals(request.getFeatured()));

        course.setCategory(category);
        course.setLevel(request.getLevel());
        course.setRecommendedCurrentBandMin(request.getRecommendedCurrentBandMin());
        course.setRecommendedCurrentBandMax(request.getRecommendedCurrentBandMax());
        course.setTargetBand(request.getTargetBand());
        course.setLearningPathCode(request.getLearningPathCode());
        course.setLearningPathName(request.getLearningPathName());
        course.setLearningPathOrder(request.getLearningPathOrder());
        course.setTargetOutcome(request.getTargetOutcome());
        course.setRecommendedNextCourseSlug(request.getRecommendedNextCourseSlug());
        course.setTotalLessons(defaultInt(request.getTotalLessons()));
        course.setTotalHours(defaultInt(request.getTotalHours()));
        synchronizeModules(course, request.getModules());
        OnlineCourse savedCourse = onlineCourseRepository.save(course);
        courseProgressService.refreshCourseEnrollments(savedCourse);
        return mapper.toResponse(savedCourse);
    }

    @Override
    public OnlineCourseResponse publishCourse(Long id) {
        OnlineCourse course = findCourse(id);
        validatePublishableCourse(course);
        LearningPackage learningPackage = course.getLearningPackage();
        if (learningPackage.getStatus() != PackageStatus.DRAFT
                && learningPackage.getStatus() != PackageStatus.PENDING_REVIEW
                && learningPackage.getStatus() != PackageStatus.REJECTED) {
            throw new IllegalArgumentException("Khóa học không ở trạng thái có thể xuất bản.");
        }
        learningPackage.setStatus(PackageStatus.PUBLISHED);
        learningPackage.setReviewNote(null);
        learningPackage.setReviewedAt(LocalDateTime.now());
        return mapper.toResponse(course);
    }

    @Override
    public OnlineCourseResponse submitForReview(Long id) {
        OnlineCourse course = findCourse(id);
        validatePublishableCourse(course);
        LearningPackage learningPackage = course.getLearningPackage();
        if (learningPackage.getStatus() != PackageStatus.DRAFT
                && learningPackage.getStatus() != PackageStatus.REJECTED) {
            throw new IllegalArgumentException("Chỉ khóa học nháp hoặc bị từ chối mới có thể gửi duyệt.");
        }
        learningPackage.setStatus(PackageStatus.PENDING_REVIEW);
        learningPackage.setSubmittedForReviewAt(LocalDateTime.now());
        learningPackage.setReviewNote(null);
        return mapper.toResponse(course);
    }

    @Override
    public OnlineCourseResponse approveCourse(Long id, String reviewerEmail, String reviewNote) {
        OnlineCourse course = findCourse(id);
        validatePublishableCourse(course);
        LearningPackage learningPackage = course.getLearningPackage();
        if (learningPackage.getStatus() != PackageStatus.PENDING_REVIEW) {
            throw new IllegalArgumentException("Khóa học chưa ở trạng thái chờ duyệt.");
        }
        User reviewer = userRepository.findByEmail(reviewerEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người duyệt."));
        learningPackage.setStatus(PackageStatus.PUBLISHED);
        learningPackage.setReviewNote(reviewNote);
        learningPackage.setReviewedAt(LocalDateTime.now());
        learningPackage.setReviewedBy(reviewer);
        return mapper.toResponse(course);
    }

    @Override
    public OnlineCourseResponse rejectCourse(Long id, String reviewerEmail, String reviewNote) {
        if (reviewNote == null || reviewNote.isBlank()) {
            throw new IllegalArgumentException("Vui lòng ghi chú lý do từ chối.");
        }
        OnlineCourse course = findCourse(id);
        LearningPackage learningPackage = course.getLearningPackage();
        if (learningPackage.getStatus() != PackageStatus.PENDING_REVIEW) {
            throw new IllegalArgumentException("Khóa học chưa ở trạng thái chờ duyệt.");
        }
        User reviewer = userRepository.findByEmail(reviewerEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người duyệt."));
        learningPackage.setStatus(PackageStatus.REJECTED);
        learningPackage.setReviewNote(reviewNote.trim());
        learningPackage.setReviewedAt(LocalDateTime.now());
        learningPackage.setReviewedBy(reviewer);
        return mapper.toResponse(course);
    }

    @Override
    public OnlineCourseResponse archiveCourse(Long id) {
        OnlineCourse course = findCourse(id);
        course.getLearningPackage().setStatus(PackageStatus.ARCHIVED);
        return mapper.toResponse(course);
    }

    @Override
    public void deleteCourse(Long id) {
        OnlineCourse course = findCourse(id);
        course.getLearningPackage().setDeleted(true);
        course.getLearningPackage().setStatus(PackageStatus.ARCHIVED);
    }

    @Override
    public BunnyVideoUploadResponse uploadLessonVideo(Long courseId, Long lessonId, String title, MultipartFile file) {
        OnlineCourse course = findCourse(courseId);
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Lesson not found"));

        if (lesson.getModule() == null
                || lesson.getModule().getOnlineCourse() == null
                || !course.getId().equals(lesson.getModule().getOnlineCourse().getId())) {
            throw new RuntimeException("Lesson does not belong to this course");
        }

        BunnyVideoUploadResponse upload = bunnyStreamService.uploadVideo(file, title == null || title.isBlank() ? lesson.getTitle() : title);
        lesson.setVideoUrl(upload.getEmbedUrl());
        lesson.setBunnyVideoId(upload.getVideoId());
        lesson.setBunnyLibraryId(upload.getLibraryId());
        lesson.setBunnyCdnUrl(upload.getCdnUrl());
        lesson.setContentType("video");
        lesson.setTranscriptSegmentsJson(null);

        Lesson savedLesson = lessonRepository.save(lesson);
        upload.setLesson(toLessonResponse(savedLesson));
        return upload;
    }

    @Override
    public OnlineCourseResponse refreshLessonTranscript(Long courseId, Long lessonId) {
        OnlineCourse course = findCourse(courseId);
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài học."));

        if (lesson.getModule() == null
                || lesson.getModule().getOnlineCourse() == null
                || !course.getId().equals(lesson.getModule().getOnlineCourse().getId())) {
            throw new RuntimeException("Bài học không thuộc khóa học này.");
        }

        List<TranscriptSegmentResponse> segments = youTubeTranscriptService.fetchTranscriptSegments(lesson.getVideoUrl());
        if (segments.isEmpty()) {
            throw new IllegalArgumentException("Video này không có caption YouTube công khai. Bản chép lời hiện tại được giữ nguyên.");
        }
        lesson.setTranscriptSegmentsJson(writeTranscriptSegments(segments));
        lessonRepository.save(lesson);
        return mapper.toResponse(findCourse(courseId));
    }

    @Override
    public OnlineCourseResponse registerCourse(Long courseId, String studentEmail) {
        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        OnlineCourse course = findPublishedCourseForEnrollment(courseId);
        if (!isFreeCourse(course.getLearningPackage())) {
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
        PackageEnrollment enrollment = courseEnrollmentAccessPolicy.requireLearningAccess(student, course);
        return mapper.toResponse(course, true, enrollment.getProgressPercent(), enrollment.getId());
    }

    @Override
    public OnlineCourseResponse activatePaidCourse(Long courseId, String studentEmail) {
        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        return activateEnrollment(findPublishedCourseForEnrollment(courseId), student);
    }

    private OnlineCourseResponse activateEnrollment(OnlineCourse course, User student) {
        LearningPackage learningPackage = learningPackageRepository
                .findByIdAndDeletedFalseAndStatusForUpdate(course.getLearningPackage().getId(), PackageStatus.PUBLISHED)
                .orElseThrow(() -> new CourseUnavailableException("Course not found or not available for enrollment"));

        var existingEnrollment = enrollmentRepository.findByStudentAndLearningPackage(student, learningPackage);
        if (existingEnrollment.isPresent()) {
            PackageEnrollment enrollment = existingEnrollment.get();
            if (!courseEnrollmentAccessPolicy.hasLearningAccess(enrollment)) {
                enrollment = courseEnrollmentAccessPolicy.reactivateCancelledEnrollment(enrollment);
                courseEnrollmentMailService.sendEnrollmentSuccessEmail(student, course, enrollment);
            }
            return mapper.toResponse(course, true, enrollment.getProgressPercent(), enrollment.getId());
        }

        PackageEnrollment enrollment = enrollmentRepository.save(PackageEnrollment.builder()
                .student(student)
                .learningPackage(learningPackage)
                .status(EnrollmentStatus.ACTIVE)
                .progressPercent(0)
                .build());
        courseEnrollmentMailService.sendEnrollmentSuccessEmail(student, course, enrollment);
        return mapper.toResponse(course, true, enrollment.getProgressPercent(), enrollment.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PackageEnrollmentResponse> getMyEnrollments(String studentEmail) {
        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        return enrollmentRepository.findByStudentOrderByRegisteredAtDesc(student).stream()
                .filter(enrollment -> enrollment.getStatus() != EnrollmentStatus.CANCELLED)
                .filter(enrollment -> !enrollment.getLearningPackage().isDeleted())
                .filter(enrollment -> onlineCourseRepository.findByLearningPackage(enrollment.getLearningPackage()).isPresent())
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

    @Override
    @Transactional(readOnly = true)
    public List<OnlineCourseResponse> getRecommendedCourses(String studentEmail) {
        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy học viên."));
        List<OnlineCourse> publishedCourses = onlineCourseRepository
                .findAll(courseSpec(null, null, null, null, null, PackageStatus.PUBLISHED), Pageable.unpaged())
                .getContent();
        Map<Long, PackageEnrollment> enrollmentsByPackage = enrollmentRepository
                .findByStudentOrderByRegisteredAtDesc(student)
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        enrollment -> enrollment.getLearningPackage().getId(),
                        enrollment -> enrollment,
                        (first, ignored) -> first
                ));
        PlacementTestAttempt latestAttempt = placementTestAttemptRepository
                .findTopByStudentOrderBySubmittedAtDesc(student)
                .orElse(null);
        Set<AssessmentSkill> weakSkills = resolveWeakSkills(latestAttempt);
        Double currentBand = student.getCurrentBand() != null
                ? student.getCurrentBand()
                : decimalToDouble(latestAttempt == null ? null : latestAttempt.getOverallScore());
        Double targetBand = parseBand(student.getTargetScore());

        return publishedCourses.stream()
                .filter(course -> !isCompletedEnrollment(enrollmentsByPackage.get(course.getLearningPackage().getId())))
                .map(course -> scoreRecommendation(
                        course,
                        enrollmentsByPackage.get(course.getLearningPackage().getId()),
                        student.getTargetExam(),
                        currentBand,
                        targetBand,
                        weakSkills
                ))
                .sorted(Comparator.comparingDouble(ScoredRecommendation::score).reversed()
                        .thenComparing(item -> defaultInt(item.course().getLearningPathOrder()))
                        .thenComparing(item -> item.course().getId()))
                .limit(6)
                .map(ScoredRecommendation::response)
                .toList();
    }

    private ScoredRecommendation scoreRecommendation(
            OnlineCourse course,
            PackageEnrollment enrollment,
            String targetExam,
            Double currentBand,
            Double targetBand,
            Set<AssessmentSkill> weakSkills
    ) {
        OnlineCourseResponse response = mapper.toPublicResponse(course);
        if (enrollment != null && enrollment.getStatus() != EnrollmentStatus.CANCELLED) {
            response.setRegistered(true);
            response.setEnrollmentId(enrollment.getId());
            response.setProgressPercent(defaultInt(enrollment.getProgressPercent()));
        }

        double score = 0;
        String searchableCourse = String.join(" ",
                safe(course.getLearningPackage().getTitle()),
                safe(course.getLearningPackage().getShortDescription()),
                safe(response.getCategory()),
                safe(response.getCategoryName()),
                safe(course.getLearningPathCode()),
                safe(course.getLearningPathName())
        ).toUpperCase(Locale.ROOT);
        String normalizedExam = safe(targetExam).toUpperCase(Locale.ROOT);
        boolean examMatches = !normalizedExam.isBlank() && searchableCourse.contains(normalizedExam);
        if (examMatches) score += 6;

        Double minBand = course.getRecommendedCurrentBandMin();
        Double maxBand = course.getRecommendedCurrentBandMax();
        if (currentBand != null && minBand != null && maxBand != null) {
            if (currentBand >= minBand && currentBand <= maxBand) {
                score += 6;
            } else {
                double distance = currentBand < minBand ? minBand - currentBand : currentBand - maxBand;
                score += Math.max(-2, 3 - distance * 2);
            }
        }
        if (targetBand != null && course.getTargetBand() != null) {
            score += Math.max(-1, 5 - Math.abs(targetBand - course.getTargetBand()) * 2);
        }

        Set<AssessmentSkill> matchedWeakSkills = response.getFocusSkills().stream()
                .map(this::parseAssessmentSkill)
                .filter(java.util.Objects::nonNull)
                .filter(weakSkills::contains)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        score += matchedWeakSkills.size() * 8D;
        if (course.getLearningPackage().isFeatured()) score += 1;

        response.setRecommendationReason(buildRecommendationReason(
                matchedWeakSkills,
                examMatches,
                targetExam,
                currentBand,
                targetBand
        ));
        return new ScoredRecommendation(course, response, score);
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

    private boolean isCompletedEnrollment(PackageEnrollment enrollment) {
        return enrollment != null && (enrollment.getStatus() == EnrollmentStatus.COMPLETED
                || defaultInt(enrollment.getProgressPercent()) >= 100);
    }

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

    private record ScoredRecommendation(OnlineCourse course, OnlineCourseResponse response, double score) {}

    @Override
    @Transactional(readOnly = true)
    public LearnerLearningPathResponse getMyLearningPath(String studentEmail) {
        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy học viên."));
        List<OnlineCourse> courses = onlineCourseRepository.findPublishedLearningPathCourses(PackageStatus.PUBLISHED);
        Map<Long, PackageEnrollment> enrollmentsByPackageId = enrollmentRepository
                .findByStudentOrderByRegisteredAtDesc(student)
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        enrollment -> enrollment.getLearningPackage().getId(),
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
            Map<Long, PackageEnrollment> enrollmentsByPackageId
    ) {
        List<OnlineCourse> sortedCourses = pathCourses.stream()
                .sorted(Comparator.comparing((OnlineCourse course) -> defaultInt(course.getLearningPathOrder()))
                        .thenComparing(OnlineCourse::getId))
                .toList();

        OnlineCourse enrolledCurrentCourse = sortedCourses.stream()
                .filter(course -> {
                    PackageEnrollment enrollment = activeLearningPathEnrollment(
                            enrollmentsByPackageId.get(course.getLearningPackage().getId())
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
                PackageEnrollment enrollment = activeLearningPathEnrollment(
                        enrollmentsByPackageId.get(candidate.getLearningPackage().getId())
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
                PackageEnrollment enrollment = activeLearningPathEnrollment(
                        enrollmentsByPackageId.get(course.getLearningPackage().getId())
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
            PackageEnrollment enrollment = activeLearningPathEnrollment(
                    enrollmentsByPackageId.get(course.getLearningPackage().getId())
            );
            boolean completed = isLearningPathCourseCompleted(enrollment);
            boolean accessible = enrollment != null || prerequisiteCompleted;
            courseResponses.add(LearnerLearningPathCourseResponse.builder()
                    .courseId(course.getId())
                    .slug(course.getLearningPackage().getSlug())
                    .title(course.getLearningPackage().getTitle())
                    .thumbnailUrl(course.getLearningPackage().getThumbnailUrl())
                    .learningPathOrder(course.getLearningPathOrder())
                    .enrollmentStatus(enrollment == null ? "NOT_ENROLLED" : enrollment.getStatus().name())
                    .progressPercent(enrollment == null ? 0 : defaultInt(enrollment.getProgressPercent()))
                    .completed(completed)
                    .lockedReason(accessible ? null : "Hoàn thành khóa học trước để mở bước này.")
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

    private boolean isLearningPathCourseCompleted(PackageEnrollment enrollment) {
        return enrollment != null
                && (enrollment.getStatus() == EnrollmentStatus.COMPLETED || defaultInt(enrollment.getProgressPercent()) >= 100);
    }

    private PackageEnrollment activeLearningPathEnrollment(PackageEnrollment enrollment) {
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
        PackageEnrollment enrollment = courseEnrollmentAccessPolicy.requireLearningAccess(student, course);
        return courseProgressService.buildCompletionResponse(enrollment, course, student);
    }

    @Override
    public CourseCertificateResponse getCourseCertificate(Long courseId, String studentEmail) {
        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy học viên."));
        OnlineCourse course = findCourse(courseId);
        PackageEnrollment enrollment = courseEnrollmentAccessPolicy.requireLearningAccess(student, course);
        CourseCompletionResponse completion = courseProgressService.buildCompletionResponse(enrollment, course, student);
        return buildCertificateResponse(course, enrollment, student, completion, false);
    }

    @Override
    public PackageEnrollmentResponse updateLessonProgress(Long courseId, Long lessonId, boolean completed, String studentEmail) {
        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        OnlineCourse course = findCourse(courseId);
        PackageEnrollment enrollment = courseEnrollmentAccessPolicy.requireLearningAccess(student, course);
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Lesson not found"));

        if (lesson.getModule() == null
                || lesson.getModule().getOnlineCourse() == null
                || !course.getId().equals(lesson.getModule().getOnlineCourse().getId())) {
            throw new RuntimeException("Lesson does not belong to this course");
        }

        LessonProgress progress = lessonProgressRepository.findByStudentAndLesson(student, lesson)
                .orElseGet(() -> LessonProgress.builder()
                        .student(student)
                        .lesson(lesson)
                        .enrollment(enrollment)
                        .build());

        progress.setLastAccessedAt(LocalDateTime.now());
        if (completed) {
            courseProgressionGuard.ensureLessonCanBeCompleted(student, course, lesson);
            progress.setStatus(LessonProgressStatus.COMPLETED);
            progress.setProgressPercent(100);
            if (progress.getCompletedAt() == null) {
                progress.setCompletedAt(LocalDateTime.now());
            }
        } else {
            courseProgressionGuard.ensureLessonCanBeMarkedIncomplete(student, course, lesson);
            progress.setStatus(LessonProgressStatus.IN_PROGRESS);
            progress.setProgressPercent(0);
            progress.setCompletedAt(null);
        }
        lessonProgressRepository.save(progress);

        PackageEnrollment savedEnrollment = courseProgressService.refreshEnrollmentProgress(enrollment, course, student);
        return mapper.toEnrollmentResponse(savedEnrollment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VocabularyTermResponse> getVocabularyTerms(Long courseId, String studentEmail) {
        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        OnlineCourse course = findCourse(courseId);
        ensureEnrolled(student, course);

        List<VocabularyProgress> progressItems = vocabularyProgressRepository.findByStudentAndCourse(student, course);
        return extractVocabularyTerms(course).stream()
                .map(term -> applyVocabularyProgress(term, progressItems))
                .toList();
    }

    @Override
    public VocabularyTermResponse updateVocabularyProgress(Long courseId, String termKey, VocabularyProgressStatus status, Boolean starred, Boolean reviewed, Boolean correct, String studentEmail) {
        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        OnlineCourse course = findCourse(courseId);
        ensureEnrolled(student, course);

        VocabularyTermResponse term = extractVocabularyTerms(course).stream()
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
                .filter(foundCourse -> !foundCourse.getLearningPackage().isDeleted())
                .orElseThrow(() -> new RuntimeException("Course not found"));
        initializeModules(course);
        return course;
    }

    private OnlineCourse findPublicCourse(String slugOrId) {
        try {
            return findPublishedCourseByIdOrPackageId(Long.parseLong(slugOrId));
        } catch (NumberFormatException ex) {
            LearningPackage learningPackage = learningPackageRepository.findBySlugAndDeletedFalseAndStatus(slugOrId, PackageStatus.PUBLISHED)
                    .orElseThrow(() -> new CourseUnavailableException("Course not found"));

            OnlineCourse course = onlineCourseRepository.findByLearningPackage(learningPackage)
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
                    .orElseGet(() -> {
                        LearningPackage learningPackage = learningPackageRepository.findByIdAndDeletedFalse(numericId)
                                .orElseThrow(() -> new RuntimeException("Course not found"));

                        return onlineCourseRepository.findByLearningPackage(learningPackage)
                                .orElseThrow(() -> new RuntimeException("Course not found"));
                    });
        } catch (NumberFormatException ex) {
            LearningPackage learningPackage = learningPackageRepository.findBySlugAndDeletedFalse(slugOrId)
                    .orElseThrow(() -> new RuntimeException("Course not found"));

            course = onlineCourseRepository.findByLearningPackage(learningPackage)
                    .orElseThrow(() -> new RuntimeException("Course not found"));
        }

        if (course.getLearningPackage().isDeleted()) {
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
                .findWithModulesByIdAndLearningPackageDeletedFalseAndLearningPackageStatus(courseId, PackageStatus.PUBLISHED)
                .orElseThrow(() -> new CourseUnavailableException("Course not found or not available for enrollment"));
        initializeModules(course);
        return course;
    }

    private OnlineCourse findPublishedCourseByIdOrPackageId(Long slugOrId) {
        OnlineCourse course = onlineCourseRepository
                .findWithModulesByIdAndLearningPackageDeletedFalseAndLearningPackageStatus(slugOrId, PackageStatus.PUBLISHED)
                .orElseGet(() -> {
                    LearningPackage learningPackage = learningPackageRepository
                            .findByIdAndDeletedFalseAndStatus(slugOrId, PackageStatus.PUBLISHED)
                            .orElseThrow(() -> new CourseUnavailableException("Course not found"));

                    return onlineCourseRepository.findByLearningPackage(learningPackage)
                            .orElseThrow(() -> new CourseUnavailableException("Course not found"));
                });
        initializeModules(course);
        return course;
    }

    private void ensureEnrolled(User student, OnlineCourse course) {
        courseEnrollmentAccessPolicy.requireLearningAccess(student, course);
    }

    private List<VocabularyTermResponse> extractVocabularyTerms(OnlineCourse course) {
        List<VocabularyTermResponse> terms = new ArrayList<>();
        for (CourseModule module : course.getModules()) {
            for (Lesson lesson : module.getLessons()) {
                terms.addAll(extractVocabularyTerms(module, lesson));
            }
        }
        return terms;
    }

    private List<VocabularyTermResponse> extractVocabularyTerms(CourseModule module, Lesson lesson) {
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

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private String toTermKey(CourseModule module, Lesson lesson, String term) {
        return "%s-%s-%s".formatted(module.getId(), lesson.getId(), toSlug(term));
    }

    private void rebuildModules(OnlineCourse course, List<ModuleRequest> modules) {
        if (modules == null) return;
        for (ModuleRequest moduleRequest : modules) {
            CourseModule module = CourseModule.builder()
                    .title(moduleRequest.getTitle())
                    .description(moduleRequest.getDescription())
                    .displayOrder(defaultInt(moduleRequest.getDisplayOrder()))
                    .build();
            if (moduleRequest.getLessons() != null) {
                for (LessonRequest lessonRequest : moduleRequest.getLessons()) {
                    Lesson lesson = Lesson.builder()
                            .title(lessonRequest.getTitle())
                            .description(lessonRequest.getDescription())
                            .contentType(lessonRequest.getContentType())
                            .contentText(lessonRequest.getContentText())
                            .videoUrl(lessonRequest.getVideoUrl())
                            .materialUrl(lessonRequest.getMaterialUrl())
                            .durationMinutes(defaultInt(lessonRequest.getDurationMinutes()))
                            .displayOrder(defaultInt(lessonRequest.getDisplayOrder()))
                            .preview(Boolean.TRUE.equals(lessonRequest.getPreview()))
                            .build();
                    applyLessonTranscript(lesson, lessonRequest, null);
                    synchronizeLessonFlashcardRefs(lesson, lessonRequest.getFlashcardSetIds());
                    module.addLesson(lesson);
                }
            }
            course.addModule(module);
        }
    }

    private void synchronizeModules(OnlineCourse course, List<ModuleRequest> modules) {
        if (modules == null) {
            return;
        }

        List<CourseModule> existingModules = course.getModules();
        Set<Long> incomingModuleIds = new HashSet<>();
        List<CourseModule> nextModules = new ArrayList<>();

        for (int moduleIndex = 0; moduleIndex < modules.size(); moduleIndex++) {
            ModuleRequest moduleRequest = modules.get(moduleIndex);
            CourseModule module = findExistingModule(existingModules, moduleRequest.getId());

            if (module == null) {
                module = CourseModule.builder().build();
                module.setOnlineCourse(course);
            } else if (module.getId() != null) {
                incomingModuleIds.add(module.getId());
            }

            module.setTitle(moduleRequest.getTitle());
            module.setDescription(moduleRequest.getDescription());
            module.setDisplayOrder(defaultInt(moduleRequest.getDisplayOrder()));
            synchronizeLessons(module, moduleRequest.getLessons());
            nextModules.add(module);
        }

        for (CourseModule existingModule : new ArrayList<>(existingModules)) {
            if (existingModule.getId() != null && !incomingModuleIds.contains(existingModule.getId())) {
                ensureModuleCanBeRemoved(existingModule);
            }
        }

        existingModules.clear();
        nextModules.forEach(course::addModule);
    }

    private void synchronizeLessons(CourseModule module, List<LessonRequest> lessons) {
        List<Lesson> existingLessons = module.getLessons();
        Set<Long> incomingLessonIds = new HashSet<>();
        List<Lesson> nextLessons = new ArrayList<>();

        if (lessons != null) {
            for (LessonRequest lessonRequest : lessons) {
                Lesson lesson = findExistingLesson(existingLessons, lessonRequest.getId());

                if (lesson == null) {
                    lesson = Lesson.builder().build();
                    lesson.setModule(module);
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
                lesson.setDisplayOrder(defaultInt(lessonRequest.getDisplayOrder()));
                lesson.setPreview(Boolean.TRUE.equals(lessonRequest.getPreview()));
                applyLessonTranscript(lesson, lessonRequest, previousVideoUrl);
                synchronizeLessonFlashcardRefs(lesson, lessonRequest.getFlashcardSetIds());
                nextLessons.add(lesson);
            }
        }

        for (Lesson existingLesson : new ArrayList<>(existingLessons)) {
            if (existingLesson.getId() != null && !incomingLessonIds.contains(existingLesson.getId())) {
                ensureLessonCanBeRemoved(existingLesson);
            }
        }

        existingLessons.clear();
        nextLessons.forEach(module::addLesson);
    }

    private void synchronizeAssessments(OnlineCourse course, List<ContentManagerCourseAssessmentRequest> requests) {
        List<CourseAssessment> existingAssessments = courseAssessmentRepository.findByOnlineCourseAndActiveTrueOrderByDisplayOrderAscIdAsc(course);
        Set<Long> incomingAssessmentIds = new HashSet<>();
        List<CourseModule> modules = new ArrayList<>(course.getModules());

        for (int index = 0; index < requests.size(); index++) {
            ContentManagerCourseAssessmentRequest request = requests.get(index);
            CourseAssessment assessment = findExistingAssessment(existingAssessments, request.getId());

            if (assessment == null) {
                assessment = CourseAssessment.builder().onlineCourse(course).build();
            } else if (assessment.getId() != null) {
                incomingAssessmentIds.add(assessment.getId());
            }

            CourseModule targetModule = resolveAssessmentModule(modules, request.getModuleId());
            AssessmentBankItem bankItem = resolveAssessmentBankItem(request.getAssessmentBankItemId());
            AssessmentRubric rubric = resolveAssessmentRubric(request.getRubricId());
            if (rubric == null && bankItem != null) {
                rubric = bankItem.getRubric();
            }
            validateAssessmentConfiguration(request, bankItem);

            assessment.setOnlineCourse(course);
            assessment.setModule(targetModule);
            assessment.setRubric(rubric);
            assessment.setAssessmentBankItem(bankItem);
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
            if (assessmentSubmissionRepository.existsByAssessmentId(existingAssessment.getId())) {
                existingAssessment.setActive(false);
                continue;
            }
            courseAssessmentRepository.delete(existingAssessment);
        }
    }

    private CourseModule findExistingModule(List<CourseModule> modules, Long moduleId) {
        if (moduleId == null) {
            return null;
        }
        return modules.stream()
                .filter(module -> moduleId.equals(module.getId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Module not found in this course"));
    }

    private Lesson findExistingLesson(List<Lesson> lessons, Long lessonId) {
        if (lessonId == null) {
            return null;
        }
        return lessons.stream()
                .filter(lesson -> lessonId.equals(lesson.getId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Lesson not found in this module"));
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

    private CourseModule resolveAssessmentModule(List<CourseModule> modules, Long moduleId) {
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

    private void ensureModuleCanBeRemoved(CourseModule module) {
        for (Lesson lesson : module.getLessons()) {
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

    private void ensureLessonCanBeRemoved(Lesson lesson) {
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

    private LessonResponse toLessonResponse(Lesson lesson) {
        return LessonResponse.builder()
                .id(lesson.getId())
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

    private void synchronizeLessonFlashcardRefs(Lesson lesson, List<Long> flashcardSetIds) {
        courseLessonFlashcardRefRepository.deleteByLessonId(lesson.getId());
        courseLessonFlashcardRefRepository.flush();
        lesson.getFlashcardRefs().clear();
        if (flashcardSetIds == null || flashcardSetIds.isEmpty()) {
            return;
        }
        Set<Long> uniqueIds = new LinkedHashSet<>(flashcardSetIds);
        int displayOrder = 1;
        for (Long flashcardSetId : uniqueIds) {
            FlashcardSet set = flashcardSetRepository.findById(flashcardSetId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy bộ flashcard."));
            if ("ARCHIVED".equalsIgnoreCase(set.getStatus())) {
                throw new RuntimeException("Bộ flashcard \"" + set.getTitle() + "\" đã được lưu trữ.");
            }
            lesson.addFlashcardRef(CourseLessonFlashcardRef.builder()
                    .flashcardSet(set)
                    .displayOrder(displayOrder++)
                    .build());
        }
    }

    private List<FlashcardSetResponse> toFlashcardSetResponses(Lesson lesson) {
        if (lesson.getFlashcardRefs() == null) {
            return List.of();
        }
        return lesson.getFlashcardRefs().stream()
                .map(ref -> toFlashcardSetResponse(ref.getFlashcardSet()))
                .filter(response -> response != null)
                .toList();
    }

    private FlashcardSetResponse toFlashcardSetResponse(FlashcardSet set) {
        if (set == null) {
            return null;
        }
        return FlashcardSetResponse.builder()
                .id(set.getId())
                .title(set.getTitle())
                .description(set.getDescription())
                .examCategory(set.getExamCategory())
                .skill(set.getSkill())
                .tags(set.getTags())
                .cardsJson(set.getCardsJson())
                .status(set.getStatus())
                .displayOrder(set.getDisplayOrder())
                .createdAt(set.getCreatedAt())
                .updatedAt(set.getUpdatedAt())
                .build();
    }

    private void applyLessonTranscript(Lesson lesson, LessonRequest lessonRequest, String previousVideoUrl) {
        if (lessonRequest.getTranscriptSegments() != null) {
            lesson.setTranscriptSegmentsJson(writeTranscriptSegments(toTranscriptResponses(lessonRequest.getTranscriptSegments())));
            return;
        }

        String nextVideoUrl = lessonRequest.getVideoUrl();
        boolean videoChanged = previousVideoUrl == null
                ? nextVideoUrl != null && !nextVideoUrl.isBlank()
                : !previousVideoUrl.equals(nextVideoUrl);
        boolean missingTranscript = lesson.getTranscriptSegmentsJson() == null || lesson.getTranscriptSegmentsJson().isBlank();

        if ((videoChanged || missingTranscript) && youTubeTranscriptService.extractVideoId(nextVideoUrl).isPresent()) {
            lesson.setTranscriptSegmentsJson(writeTranscriptSegments(youTubeTranscriptService.fetchTranscriptSegments(nextVideoUrl)));
        } else if (nextVideoUrl == null || nextVideoUrl.isBlank()) {
            lesson.setTranscriptSegmentsJson(null);
        }
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
        List<TranscriptSegmentResponse> safeSegments = segments == null ? List.of() : segments;
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

    private Specification<OnlineCourse> courseSpec(String keyword, String category, Double currentBand, Double targetBand, AssessmentSkill skill, PackageStatus status) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<OnlineCourse, LearningPackage> learningPackage = root.join("learningPackage");

            Join<OnlineCourse, CourseCategory> categoryJoin = root.join("category");
            query.distinct(true);
            predicates.add(criteriaBuilder.isFalse(learningPackage.get("deleted")));

            if (status != null) {
                predicates.add(criteriaBuilder.equal(learningPackage.get("status"), status));
            }
            if (category != null && !category.isBlank()) {
                predicates.add(criteriaBuilder.equal(categoryJoin.get("code"), normalizeCategoryCode(category)));
            }
            if (keyword != null) {
                String pattern = "%" + keyword.toLowerCase(Locale.ROOT) + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(learningPackage.get("title")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(learningPackage.get("shortDescription")), pattern)
                ));
            }
            if (currentBand != null) {
                Predicate hasBandInfo = criteriaBuilder.or(
                        criteriaBuilder.isNotNull(root.get("recommendedCurrentBandMin")),
                        criteriaBuilder.isNotNull(root.get("recommendedCurrentBandMax"))
                );
                Predicate minMatches = criteriaBuilder.or(
                        criteriaBuilder.isNull(root.get("recommendedCurrentBandMin")),
                        criteriaBuilder.lessThanOrEqualTo(root.get("recommendedCurrentBandMin"), currentBand)
                );
                Predicate maxMatches = criteriaBuilder.or(
                        criteriaBuilder.isNull(root.get("recommendedCurrentBandMax")),
                        criteriaBuilder.greaterThanOrEqualTo(root.get("recommendedCurrentBandMax"), currentBand)
                );
                predicates.add(criteriaBuilder.and(hasBandInfo, minMatches, maxMatches));
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
                var assessmentRoot = assessmentSubquery.from(fu.sap490.g23.backend.entity.assessment.CourseAssessment.class);
                assessmentSubquery.select(assessmentRoot.get("id"));
                assessmentSubquery.where(
                        criteriaBuilder.equal(assessmentRoot.get("onlineCourse"), root),
                        criteriaBuilder.isTrue(assessmentRoot.get("active")),
                        criteriaBuilder.equal(assessmentRoot.get("skill"), skill)
                );

                Join<OnlineCourse, CourseModule> moduleJoin = root.join("modules", jakarta.persistence.criteria.JoinType.LEFT);
                Join<CourseModule, Lesson> lessonJoin = moduleJoin.join("lessons", jakarta.persistence.criteria.JoinType.LEFT);
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

    private void validateCourseRequest(OnlineCourseRequest request) {
        Double minBand = request.getRecommendedCurrentBandMin();
        Double maxBand = request.getRecommendedCurrentBandMax();
        if (minBand != null && maxBand != null && minBand > maxBand) {
            throw new IllegalArgumentException("Band đầu vào tối thiểu không thể lớn hơn band đầu vào tối đa.");
        }

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
    }

    private void validatePublishableCourse(OnlineCourse course) {
        if (course.getModules() == null
                || course.getModules().isEmpty()
                || course.getModules().stream().allMatch(module -> module.getLessons() == null || module.getLessons().isEmpty())) {
            throw new IllegalArgumentException("Khóa học cần có ít nhất một mô-đun và bài học trước khi xuất bản.");
        }
    }

    private String normalizeCategoryCode(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private CourseCertificateResponse buildCertificateResponse(
            OnlineCourse course,
            PackageEnrollment enrollment,
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
                    .courseTitle(course.getLearningPackage().getTitle())
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
                .courseTitle(course.getLearningPackage().getTitle())
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

    private PackageEnrollment findEnrollmentByCertificateCode(String verificationCode) {
        var matcher = CERTIFICATE_CODE_PATTERN.matcher(verificationCode == null ? "" : verificationCode.trim().toUpperCase(Locale.ROOT));
        if (!matcher.matches()) {
            throw new RuntimeException("Mã xác thực chứng nhận không hợp lệ.");
        }

        Long enrollmentId = Long.parseLong(matcher.group(1));
        Long courseId = Long.parseLong(matcher.group(2));
        PackageEnrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chứng nhận cần xác thực."));
        OnlineCourse course = onlineCourseRepository.findByLearningPackage(enrollment.getLearningPackage())
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

    private String buildVerificationCode(PackageEnrollment enrollment, OnlineCourse course, User student) {
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
        while (learningPackageRepository.existsBySlug(slug)) {
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

    private boolean isFreeCourse(LearningPackage learningPackage) {
        BigDecimal price = defaultBigDecimal(learningPackage.getPrice());

        BigDecimal salePrice = resolveSalePrice(price, learningPackage.getSalePrice());
        return (salePrice == null ? price : salePrice).compareTo(BigDecimal.ZERO) <= 0;
    }
}
