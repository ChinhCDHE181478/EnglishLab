package fu.sap490.g23.backend.service.course.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fu.sap490.g23.backend.dto.request.course.CreateCourseVersionRequest;
import fu.sap490.g23.backend.dto.response.course.LessonResponse;
import fu.sap490.g23.backend.dto.response.course.OnlineCourseResponse;
import fu.sap490.g23.backend.dto.response.course.OnlineCoursePreviewResponse;
import fu.sap490.g23.backend.dto.response.course.OnlineCourseVersionResponse;
import fu.sap490.g23.backend.dto.response.assessment.CourseAssessmentResponse;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.assessment.CourseAssessment;
import fu.sap490.g23.backend.entity.course.OnlineCourse;
import fu.sap490.g23.backend.entity.course.OnlineCourseVersion;
import fu.sap490.g23.backend.entity.course.PackageEnrollment;
import fu.sap490.g23.backend.entity.course.enums.CourseVersionStatus;
import fu.sap490.g23.backend.entity.course.enums.LessonProgressStatus;
import fu.sap490.g23.backend.entity.course.enums.PackageStatus;
import fu.sap490.g23.backend.repository.UserRepository;
import fu.sap490.g23.backend.repository.assessment.CourseAssessmentRepository;
import fu.sap490.g23.backend.repository.course.OnlineCourseRepository;
import fu.sap490.g23.backend.repository.course.OnlineCourseVersionRepository;
import fu.sap490.g23.backend.repository.course.LessonProgressRepository;
import fu.sap490.g23.backend.security.ContentManagementRolePolicy;
import fu.sap490.g23.backend.service.course.OnlineCourseMapper;
import fu.sap490.g23.backend.service.course.OnlineCoursePreviewValidator;
import fu.sap490.g23.backend.service.course.OnlineCourseVersionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
@Transactional
public class OnlineCourseVersionServiceImpl implements OnlineCourseVersionService {
    private static final List<CourseVersionStatus> OPEN_STATUSES = List.of(
            CourseVersionStatus.DRAFT,
            CourseVersionStatus.PENDING_REVIEW
    );

    private final OnlineCourseRepository onlineCourseRepository;
    private final OnlineCourseVersionRepository versionRepository;
    private final CourseAssessmentRepository courseAssessmentRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final UserRepository userRepository;
    private final OnlineCourseMapper mapper;
    private final OnlineCoursePreviewValidator previewValidator;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Override
    @Transactional(readOnly = true)
    public List<OnlineCourseVersionResponse> getVersions(Long courseId, String actorEmail) {
        requireEditor(actorEmail);
        OnlineCourse course = findCourse(courseId);
        return versionRepository.findByOnlineCourseOrderByVersionNumberDesc(course).stream()
                .map(version -> toResponse(version, false))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OnlineCourseVersionResponse> getPendingReviews(String actorEmail) {
        requireApprover(actorEmail);
        return versionRepository.findByStatusOrderBySubmittedAtAsc(CourseVersionStatus.PENDING_REVIEW).stream()
                .map(version -> toResponse(version, true))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OnlineCourseVersionResponse getVersion(Long courseId, Long versionId, String actorEmail) {
        requireEditor(actorEmail);
        OnlineCourse course = findCourse(courseId);
        return toResponse(findVersion(course, versionId), true);
    }

    @Override
    @Transactional(readOnly = true)
    public OnlineCoursePreviewResponse getVersionPreview(Long courseId, Long versionId, String actorEmail) {
        requireEditor(actorEmail);
        OnlineCourse course = findCourse(courseId);
        OnlineCourseVersion version = findVersion(course, versionId);
        OnlineCourseResponse courseResponse = readSnapshot(version, course);
        List<CourseAssessmentResponse> assessments = courseAssessmentRepository.findAllById(readAssessmentIds(version))
                .stream()
                .filter(assessment -> assessment.getOnlineCourse().getId().equals(courseId))
                .sorted(Comparator.comparing(CourseAssessment::getDisplayOrder).thenComparing(CourseAssessment::getId))
                .map(this::toPreviewAssessmentResponse)
                .toList();
        List<fu.sap490.g23.backend.dto.response.course.ModuleResponse> modules = courseResponse.getModules() == null
                ? List.of()
                : List.copyOf(courseResponse.getModules());
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
    public OnlineCourseVersionResponse createDraft(
            Long courseId,
            CreateCourseVersionRequest request,
            String actorEmail
    ) {
        User actor = requireEditor(actorEmail);
        OnlineCourse course = findCourse(courseId);
        if (versionRepository.existsByOnlineCourseAndStatusIn(course, OPEN_STATUSES)) {
            throw new IllegalStateException("Khóa học đã có một phiên bản nháp hoặc đang chờ duyệt.");
        }

        List<OnlineCourseVersion> versions = versionRepository.findByOnlineCourseOrderByVersionNumberDesc(course);
        OnlineCourseVersion published = versions.stream()
                .filter(version -> version.getStatus() == CourseVersionStatus.PUBLISHED)
                .findFirst()
                .orElse(null);
        List<CourseAssessment> publishedAssessments = courseAssessmentRepository
                .findByOnlineCourseAndActiveTrueOrderByDisplayOrderAscIdAsc(course);
        if (published != null) {
            published.setAssessmentIdsJson(writeAssessmentIds(publishedAssessments));
        }
        List<CourseAssessment> draftAssessments = published == null
                ? publishedAssessments
                : cloneAssessmentsForDraft(publishedAssessments);
        String snapshot;
        if (published == null) {
            snapshot = writeSnapshot(course);
        } else if (isEmptySnapshot(published.getContentSnapshotJson())) {
            snapshot = writeSnapshot(course);
            published.setContentSnapshotJson(snapshot);
            published.setTotalRequiredLessons(countLessons(course));
            published.setTotalRequiredAssessments(countAssessments(course));
        } else {
            snapshot = published.getContentSnapshotJson();
        }

        OnlineCourseVersion draft = OnlineCourseVersion.builder()
                .onlineCourse(course)
                .versionNumber(versions.stream()
                        .map(OnlineCourseVersion::getVersionNumber)
                        .max(Integer::compareTo)
                        .orElse(0) + 1)
                .status(CourseVersionStatus.DRAFT)
                .contentSnapshotJson(snapshot)
                .assessmentIdsJson(writeAssessmentIds(draftAssessments))
                .totalRequiredLessons(published == null ? countLessons(course) : published.getTotalRequiredLessons())
                .totalRequiredAssessments(published == null
                        ? countAssessments(course)
                        : published.getTotalRequiredAssessments())
                .changeNote(normalize(request == null ? null : request.getChangeNote()))
                .createdBy(actor)
                .build();
        return toResponse(versionRepository.save(draft), true);
    }

    @Override
    public OnlineCourseVersionResponse submitForReview(Long courseId, Long versionId, String actorEmail) {
        requireEditor(actorEmail);
        OnlineCourse course = findCourse(courseId);
        OnlineCourseVersion version = findVersion(course, versionId);
        if (version.getStatus() != CourseVersionStatus.DRAFT) {
            throw new IllegalStateException("Chỉ phiên bản nháp mới có thể gửi duyệt.");
        }
        synchronizeSnapshot(version, course);
        version.setStatus(CourseVersionStatus.PENDING_REVIEW);
        version.setSubmittedAt(LocalDateTime.now());
        version.setReviewNote(null);
        boolean hasPublishedVersion = versionRepository
                .findFirstByOnlineCourseAndStatusOrderByVersionNumberDesc(course, CourseVersionStatus.PUBLISHED)
                .isPresent();
        if (!hasPublishedVersion) {
            course.getLearningPackage().setStatus(PackageStatus.PENDING_REVIEW);
            course.getLearningPackage().setSubmittedForReviewAt(version.getSubmittedAt());
            course.getLearningPackage().setReviewNote(null);
        }
        return toResponse(version, true);
    }

    @Override
    public OnlineCourseVersionResponse publish(Long courseId, Long versionId, String actorEmail) {
        User reviewer = requireApprover(actorEmail);
        OnlineCourse course = findCourse(courseId);
        OnlineCourseVersion version = findVersion(course, versionId);
        if (version.getStatus() != CourseVersionStatus.PENDING_REVIEW) {
            throw new IllegalStateException("Phiên bản khóa học chưa ở trạng thái chờ duyệt.");
        }

        versionRepository.findFirstByOnlineCourseAndStatusOrderByVersionNumberDesc(
                course,
                CourseVersionStatus.PUBLISHED
        ).ifPresent(current -> {
            if (!current.getId().equals(version.getId())) {
                current.setStatus(CourseVersionStatus.RETIRED);
                versionRepository.saveAndFlush(current);
            }
        });

        synchronizeSnapshot(version, course);
        version.setStatus(CourseVersionStatus.PUBLISHED);
        version.setReviewedBy(reviewer);
        version.setReviewNote(null);
        version.setPublishedAt(LocalDateTime.now());
        course.getLearningPackage().setStatus(PackageStatus.PUBLISHED);
        course.getLearningPackage().setReviewedBy(reviewer);
        course.getLearningPackage().setReviewedAt(LocalDateTime.now());
        course.getLearningPackage().setReviewNote(null);
        return toResponse(version, true);
    }

    @Override
    public OnlineCourseVersionResponse reject(
            Long courseId,
            Long versionId,
            String reviewNote,
            String actorEmail
    ) {
        if (reviewNote == null || reviewNote.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập lý do từ chối phiên bản.");
        }
        User reviewer = requireApprover(actorEmail);
        OnlineCourse course = findCourse(courseId);
        OnlineCourseVersion version = findVersion(course, versionId);
        if (version.getStatus() != CourseVersionStatus.PENDING_REVIEW) {
            throw new IllegalStateException("Phiên bản khóa học chưa ở trạng thái chờ duyệt.");
        }
        version.setStatus(CourseVersionStatus.DRAFT);
        version.setReviewedBy(reviewer);
        version.setReviewNote(reviewNote.trim());
        boolean hasPublishedVersion = versionRepository
                .findFirstByOnlineCourseAndStatusOrderByVersionNumberDesc(course, CourseVersionStatus.PUBLISHED)
                .isPresent();
        if (!hasPublishedVersion) {
            course.getLearningPackage().setStatus(PackageStatus.REJECTED);
            course.getLearningPackage().setReviewedBy(reviewer);
            course.getLearningPackage().setReviewedAt(LocalDateTime.now());
            course.getLearningPackage().setReviewNote(reviewNote.trim());
        }
        return toResponse(version, true);
    }

    @Override
    public void assertEditableDraft(OnlineCourse course, String actorEmail) {
        if (actorEmail != null && !actorEmail.isBlank()) {
            requireEditor(actorEmail);
        }
        OnlineCourseVersion draft = versionRepository
                .findFirstByOnlineCourseAndStatusOrderByVersionNumberDesc(course, CourseVersionStatus.DRAFT)
                .orElse(null);
        if (draft != null) {
            return;
        }
        OnlineCourseVersion pending = versionRepository
                .findFirstByOnlineCourseAndStatusOrderByVersionNumberDesc(course, CourseVersionStatus.PENDING_REVIEW)
                .orElse(null);
        if (pending != null) {
            throw new IllegalStateException("Phiên bản đang chờ duyệt nên không thể tiếp tục chỉnh sửa.");
        }
        OnlineCourseVersion published = versionRepository
                .findFirstByOnlineCourseAndStatusOrderByVersionNumberDesc(course, CourseVersionStatus.PUBLISHED)
                .orElse(null);
        if (published == null) {
            PackageStatus status = course.getLearningPackage().getStatus();
            if (status == PackageStatus.DRAFT || status == PackageStatus.REJECTED) {
                return;
            }
        }
        throw new IllegalStateException("Khóa học đã xuất bản. Hãy tạo phiên bản nháp mới trước khi chỉnh sửa.");
    }

    @Override
    public void synchronizeDraftSnapshot(OnlineCourse course) {
        versionRepository.findFirstByOnlineCourseAndStatusOrderByVersionNumberDesc(
                course,
                CourseVersionStatus.DRAFT
        ).ifPresent(version -> synchronizeSnapshot(version, course));
    }

    @Override
    public OnlineCourseVersion requirePublishedVersion(OnlineCourse course) {
        OnlineCourseVersion existing = versionRepository
                .findFirstByOnlineCourseAndStatusOrderByVersionNumberDesc(course, CourseVersionStatus.PUBLISHED)
                .orElse(null);
        if (existing != null) {
            if (isEmptySnapshot(existing.getContentSnapshotJson())) {
                synchronizeSnapshot(existing, course);
            }
            return existing;
        }

        List<OnlineCourseVersion> versions = versionRepository.findByOnlineCourseOrderByVersionNumberDesc(course);
        OnlineCourseVersion version = versions.stream()
                .filter(item -> item.getStatus() == CourseVersionStatus.DRAFT)
                .findFirst()
                .orElseGet(() -> OnlineCourseVersion.builder()
                        .onlineCourse(course)
                        .versionNumber(versions.stream()
                                .map(OnlineCourseVersion::getVersionNumber)
                                .max(Integer::compareTo)
                                .orElse(0) + 1)
                        .createdBy(course.getLearningPackage().getCreatedBy())
                        .build());
        synchronizeSnapshot(version, course);
        version.setStatus(CourseVersionStatus.PUBLISHED);
        version.setPublishedAt(LocalDateTime.now());
        return versionRepository.save(version);
    }

    @Override
    @Transactional(readOnly = true)
    public OnlineCourseResponse readEnrollmentSnapshot(PackageEnrollment enrollment, OnlineCourse liveCourse) {
        OnlineCourseResponse response = readSnapshot(enrollment.getCourseVersion(), liveCourse);
        response.setRegistered(true);
        response.setProgressPercent(enrollment.getProgressPercent());
        response.setEnrollmentId(enrollment.getId());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public OnlineCourseResponse readPublishedSnapshot(OnlineCourse course, boolean includeLessonContent) {
        OnlineCourseVersion published = versionRepository
                .findFirstByOnlineCourseAndStatusOrderByVersionNumberDesc(course, CourseVersionStatus.PUBLISHED)
                .orElse(null);
        OnlineCourseResponse response = readSnapshot(published, course);
        response.setStatus(PackageStatus.PUBLISHED);
        response.setRegistered(false);
        response.setProgressPercent(null);
        response.setEnrollmentId(null);
        if (!includeLessonContent) {
            response.getModules().forEach(module -> module.getLessons().forEach(this::hideProtectedLessonContent));
        }
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getEnrollmentAssessmentIds(PackageEnrollment enrollment) {
        OnlineCourseVersion version = enrollment == null ? null : enrollment.getCourseVersion();
        if (version == null) {
            return List.of();
        }
        try {
            List<Long> ids = objectMapper.readValue(
                    version.getAssessmentIdsJson() == null ? "[]" : version.getAssessmentIdsJson(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Long.class)
            );
            if (ids.isEmpty() && isEmptySnapshot(version.getContentSnapshotJson())) {
                return courseAssessmentRepository
                        .findByOnlineCourseAndActiveTrueOrderByDisplayOrderAscIdAsc(version.getOnlineCourse())
                        .stream()
                        .map(CourseAssessment::getId)
                        .toList();
            }
            return ids;
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Snapshot bài đánh giá của phiên bản không hợp lệ.", ex);
        }
    }

    private List<Long> readAssessmentIds(OnlineCourseVersion version) {
        try {
            return objectMapper.readValue(
                    version.getAssessmentIdsJson() == null ? "[]" : version.getAssessmentIdsJson(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Long.class)
            );
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Snapshot bài đánh giá của phiên bản không hợp lệ.", ex);
        }
    }

    private CourseAssessmentResponse toPreviewAssessmentResponse(CourseAssessment assessment) {
        return CourseAssessmentResponse.builder()
                .id(assessment.getId())
                .courseId(assessment.getOnlineCourse().getId())
                .moduleId(assessment.getModule() == null ? null : assessment.getModule().getId())
                .assessmentBankItemId(assessment.getAssessmentBankItem() == null
                        ? null
                        : assessment.getAssessmentBankItem().getId())
                .moduleTitle(assessment.getModule() == null ? null : assessment.getModule().getTitle())
                .title(assessment.getTitle())
                .description(assessment.getDescription())
                .type(assessment.getType())
                .skill(assessment.getSkill())
                .aiEvaluationMode(assessment.getAiEvaluationMode())
                .instructions(assessment.getInstructions())
                .objectiveAnswerKey(assessment.getObjectiveAnswerKey())
                .uiConfigJson(assessment.getUiConfigJson())
                .passingScore(assessment.getPassingScore())
                .maxScore(assessment.getMaxScore())
                .timeLimitMinutes(assessment.getTimeLimitMinutes())
                .displayOrder(assessment.getDisplayOrder())
                .active(assessment.isActive())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public void assertAssessmentBelongsToEnrollment(PackageEnrollment enrollment, Long assessmentId) {
        if (enrollment.getCourseVersion() != null
                && !getEnrollmentAssessmentIds(enrollment).contains(assessmentId)) {
            throw new IllegalArgumentException("Bài đánh giá không thuộc phiên bản khóa học của enrollment này.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void assertLessonBelongsToEnrollment(PackageEnrollment enrollment, Long lessonId) {
        if (enrollment.getCourseVersion() == null) {
            return;
        }
        OnlineCourseResponse snapshot = readSnapshot(
                enrollment.getCourseVersion(),
                enrollment.getCourseVersion().getOnlineCourse()
        );
        boolean found = snapshot.getModules() != null && snapshot.getModules().stream()
                .flatMap(module -> module.getLessons().stream())
                .anyMatch(lesson -> lessonId.equals(lesson.getId()));
        if (!found) {
            throw new IllegalArgumentException("Bài học không thuộc phiên bản khóa học của enrollment này.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void assertLessonProgressTransitionAllowed(
            PackageEnrollment enrollment,
            Long lessonId,
            boolean completed
    ) {
        if (enrollment.getCourseVersion() == null) {
            return;
        }
        OnlineCourseResponse snapshot = readSnapshot(
                enrollment.getCourseVersion(),
                enrollment.getCourseVersion().getOnlineCourse()
        );
        List<Long> orderedLessonIds = snapshot.getModules().stream()
                .flatMap(module -> module.getLessons().stream())
                .map(LessonResponse::getId)
                .toList();
        int lessonIndex = orderedLessonIds.indexOf(lessonId);
        if (lessonIndex < 0) {
            throw new IllegalArgumentException("Bài học không thuộc phiên bản khóa học của enrollment này.");
        }
        var completedLessonIds = lessonProgressRepository
                .findByEnrollmentAndStatusOrderByCompletedAtDesc(enrollment, LessonProgressStatus.COMPLETED)
                .stream()
                .map(progress -> progress.getLesson().getId())
                .collect(java.util.stream.Collectors.toSet());
        if (completed && lessonIndex > 0 && !completedLessonIds.contains(orderedLessonIds.get(lessonIndex - 1))) {
            throw new IllegalStateException("Bạn cần hoàn thành bài học trước đó trong phiên bản này trước khi tiếp tục.");
        }
        if (!completed && orderedLessonIds.subList(lessonIndex + 1, orderedLessonIds.size()).stream()
                .anyMatch(completedLessonIds::contains)) {
            throw new IllegalStateException("Không thể bỏ hoàn thành vì bạn đã học xong bài phía sau trong phiên bản này.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void assertLessonCanBeRemoved(OnlineCourse course, Long lessonId) {
        boolean referenced = versionRepository.findByOnlineCourseOrderByVersionNumberDesc(course).stream()
                .filter(version -> version.getStatus() == CourseVersionStatus.PUBLISHED
                        || version.getStatus() == CourseVersionStatus.RETIRED)
                .filter(version -> !isEmptySnapshot(version.getContentSnapshotJson()))
                .map(version -> readSnapshot(version, course))
                .filter(snapshot -> snapshot.getModules() != null)
                .flatMap(snapshot -> snapshot.getModules().stream())
                .flatMap(module -> module.getLessons().stream())
                .anyMatch(lesson -> lessonId.equals(lesson.getId()));
        if (referenced) {
            throw new IllegalStateException(
                    "Không thể xóa bài học đã thuộc phiên bản đang được học. Hãy giữ lessonKey và thay nội dung ở bài mới."
            );
        }
    }

    private OnlineCourseResponse readSnapshot(OnlineCourseVersion version, OnlineCourse fallbackCourse) {
        if (version == null || isEmptySnapshot(version.getContentSnapshotJson())) {
            return mapper.toResponse(fallbackCourse);
        }
        try {
            return objectMapper.readValue(version.getContentSnapshotJson(), OnlineCourseResponse.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Snapshot phiên bản khóa học không hợp lệ.", ex);
        }
    }

    private void hideProtectedLessonContent(LessonResponse lesson) {
        if (lesson.isPreview()) {
            return;
        }
        lesson.setContentText(null);
        lesson.setVideoUrl(null);
        lesson.setBunnyVideoId(null);
        lesson.setBunnyLibraryId(null);
        lesson.setBunnyCdnUrl(null);
        lesson.setMaterialUrl(null);
        lesson.setTranscriptSegments(List.of());
        lesson.setFlashcardSets(List.of());
    }

    private void synchronizeSnapshot(OnlineCourseVersion version, OnlineCourse course) {
        version.setContentSnapshotJson(writeSnapshot(course));
        version.setAssessmentIdsJson(writeAssessmentIds(
                courseAssessmentRepository.findByOnlineCourseAndActiveTrueOrderByDisplayOrderAscIdAsc(course)
        ));
        version.setTotalRequiredLessons(countLessons(course));
        version.setTotalRequiredAssessments(countAssessments(course));
    }

    private String writeSnapshot(OnlineCourse course) {
        try {
            return objectMapper.writeValueAsString(mapper.toResponse(course));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Không thể tạo snapshot cho phiên bản khóa học.", ex);
        }
    }

    private int countLessons(OnlineCourse course) {
        return course.getModules().stream().mapToInt(module -> module.getLessons().size()).sum();
    }

    private int countAssessments(OnlineCourse course) {
        return Math.toIntExact(courseAssessmentRepository.countByOnlineCourseAndActiveTrue(course));
    }

    private String writeAssessmentIds(List<CourseAssessment> assessments) {
        try {
            return objectMapper.writeValueAsString(assessments.stream()
                    .map(CourseAssessment::getId)
                    .filter(id -> id != null)
                    .toList());
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Không thể tạo snapshot bài đánh giá.", ex);
        }
    }

    private List<CourseAssessment> cloneAssessmentsForDraft(List<CourseAssessment> source) {
        if (source.isEmpty()) {
            return List.of();
        }
        List<CourseAssessment> clones = source.stream()
                .sorted(Comparator.comparing(CourseAssessment::getDisplayOrder).thenComparing(CourseAssessment::getId))
                .map(assessment -> {
                    assessment.setActive(false);
                    return CourseAssessment.builder()
                            .onlineCourse(assessment.getOnlineCourse())
                            .module(assessment.getModule())
                            .rubric(assessment.getRubric())
                            .assessmentBankItem(assessment.getAssessmentBankItem())
                            .title(assessment.getTitle())
                            .description(assessment.getDescription())
                            .type(assessment.getType())
                            .skill(assessment.getSkill())
                            .aiEvaluationMode(assessment.getAiEvaluationMode())
                            .instructions(assessment.getInstructions())
                            .objectiveAnswerKey(assessment.getObjectiveAnswerKey())
                            .uiConfigJson(assessment.getUiConfigJson())
                            .passingScore(assessment.getPassingScore())
                            .maxScore(assessment.getMaxScore())
                            .timeLimitMinutes(assessment.getTimeLimitMinutes())
                            .displayOrder(assessment.getDisplayOrder())
                            .active(true)
                            .build();
                })
                .toList();
        courseAssessmentRepository.saveAll(source);
        return courseAssessmentRepository.saveAll(clones);
    }

    private OnlineCourseVersionResponse toResponse(OnlineCourseVersion version, boolean includeContent) {
        return OnlineCourseVersionResponse.builder()
                .id(version.getId())
                .courseId(version.getOnlineCourse().getId())
                .versionNumber(version.getVersionNumber())
                .status(version.getStatus())
                .totalRequiredLessons(version.getTotalRequiredLessons())
                .totalRequiredAssessments(version.getTotalRequiredAssessments())
                .changeNote(version.getChangeNote())
                .reviewNote(version.getReviewNote())
                .createdByName(version.getCreatedBy() == null ? null : version.getCreatedBy().getFullName())
                .reviewedByName(version.getReviewedBy() == null ? null : version.getReviewedBy().getFullName())
                .submittedAt(version.getSubmittedAt())
                .publishedAt(version.getPublishedAt())
                .createdAt(version.getCreatedAt())
                .updatedAt(version.getUpdatedAt())
                .content(includeContent ? readSnapshot(version, version.getOnlineCourse()) : null)
                .build();
    }

    private OnlineCourse findCourse(Long courseId) {
        OnlineCourse course = onlineCourseRepository.findWithModulesById(courseId)
                .filter(item -> !item.getLearningPackage().isDeleted())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học."));
        course.getModules().forEach(module -> module.getLessons().size());
        return course;
    }

    private OnlineCourseVersion findVersion(OnlineCourse course, Long versionId) {
        return versionRepository.findByIdAndOnlineCourseId(versionId, course.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiên bản khóa học."));
    }

    private User requireEditor(String email) {
        User actor = requireUser(email);
        if (!ContentManagementRolePolicy.canEdit(actor)) {
            throw new AccessDeniedException("Bạn không có quyền chỉnh sửa phiên bản khóa học.");
        }
        return actor;
    }

    private User requireApprover(String email) {
        User actor = requireUser(email);
        if (!ContentManagementRolePolicy.canApprove(actor)) {
            throw new AccessDeniedException("Bạn không có quyền duyệt phiên bản khóa học.");
        }
        return actor;
    }

    private User requireUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng."));
    }

    private boolean isEmptySnapshot(String snapshot) {
        return snapshot == null || snapshot.isBlank() || "{}".equals(snapshot.trim());
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
