package fu.sep490.g23.backend.service.course.impl;

import fu.sep490.g23.backend.dto.request.course.CreateCourseVersionRequest;
import fu.sep490.g23.backend.dto.response.course.LessonResponse;
import fu.sep490.g23.backend.dto.response.course.ModuleResponse;
import fu.sep490.g23.backend.dto.response.course.OnlineCourseResponse;
import fu.sep490.g23.backend.dto.response.course.OnlineCoursePreviewResponse;
import fu.sep490.g23.backend.dto.response.course.OnlineCourseVersionResponse;
import fu.sep490.g23.backend.dto.response.assessment.CourseAssessmentResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.assessment.CourseAssessment;
import fu.sep490.g23.backend.entity.assessment.enums.SubmissionStatus;
import fu.sep490.g23.backend.entity.course.CourseLessonFlashcardRef;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.entity.course.OnlineCourseModule;
import fu.sep490.g23.backend.entity.course.OnlineCourseVersion;
import fu.sep490.g23.backend.entity.course.OnlineCourseEnrollment;
import fu.sep490.g23.backend.entity.course.OnlineLesson;
import fu.sep490.g23.backend.entity.course.enums.CourseVersionStatus;
import fu.sep490.g23.backend.entity.course.enums.LessonProgressStatus;
import fu.sep490.g23.backend.entity.course.enums.PackageStatus;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.assessment.AssessmentSubmissionRepository;
import fu.sep490.g23.backend.repository.assessment.CourseAssessmentRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseVersionRepository;
import fu.sep490.g23.backend.repository.course.OnlineLessonRepository;
import fu.sep490.g23.backend.repository.course.LessonProgressRepository;
import fu.sep490.g23.backend.security.ContentManagementRolePolicy;
import fu.sep490.g23.backend.service.course.OnlineCourseMapper;
import fu.sep490.g23.backend.service.course.OnlineCoursePreviewValidator;
import fu.sep490.g23.backend.service.course.OnlineCourseVersionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

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
    private final AssessmentSubmissionRepository assessmentSubmissionRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final OnlineLessonRepository lessonRepository;
    private final UserRepository userRepository;
    private final OnlineCourseMapper mapper;
    private final OnlineCoursePreviewValidator previewValidator;

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
        initializeVersionModules(version);
        OnlineCourseResponse courseResponse = mapper.toResponse(course, version.getModules());
        List<CourseAssessmentResponse> assessments = courseAssessmentRepository
                .findByOnlineCourseVersionAndActiveTrueOrderByDisplayOrderAscIdAsc(version).stream()
                .map(this::toPreviewAssessmentResponse)
                .toList();
        List<fu.sep490.g23.backend.dto.response.course.ModuleResponse> modules = courseResponse.getModules() == null
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
            throw new IllegalStateException("Khóa học đã có một phiên bản chưa xuất bản.");
        }

        List<OnlineCourseVersion> versions = versionRepository.findByOnlineCourseOrderByVersionNumberDesc(course);
        OnlineCourseVersion published = versions.stream()
                .filter(version -> version.getStatus() == CourseVersionStatus.PUBLISHED)
                .findFirst()
                .orElse(null);
        List<CourseAssessment> publishedAssessments = published == null
                ? List.of()
                : courseAssessmentRepository
                        .findByOnlineCourseVersionAndActiveTrueOrderByDisplayOrderAscIdAsc(published);

        OnlineCourseVersion draft = OnlineCourseVersion.builder()
                .onlineCourse(course)
                .versionNumber(versions.stream()
                        .map(OnlineCourseVersion::getVersionNumber)
                        .max(Integer::compareTo)
                        .orElse(0) + 1)
                .status(CourseVersionStatus.DRAFT)
                .totalRequiredLessons(0)
                .totalRequiredAssessments(0)
                .changeNote(normalize(request == null ? null : request.getChangeNote()))
                .createdBy(actor)
                .build();
        OnlineCourseVersion savedDraft = versionRepository.save(draft);
        if (published != null) {
            cloneModulesOntoDraft(published, savedDraft);
            versionRepository.saveAndFlush(savedDraft);
            List<CourseAssessment> draftAssessments = cloneAssessmentsForDraft(publishedAssessments, savedDraft);
            savedDraft.setTotalRequiredAssessments(draftAssessments.size());
            if (!savedDraft.getModules().isEmpty()) {
                savedDraft.setTotalRequiredLessons(countLessons(savedDraft.getModules()));
            }
        }
        return toResponse(savedDraft, true);
    }

    @Override
    public OnlineCourseVersionResponse publish(Long courseId, Long versionId, String actorEmail) {
        User publisher = requireEditor(actorEmail);
        OnlineCourse course = findCourse(courseId);
        OnlineCourseVersion version = findVersion(course, versionId);
        if (version.getStatus() != CourseVersionStatus.DRAFT
                && version.getStatus() != CourseVersionStatus.PENDING_REVIEW) {
            throw new IllegalStateException("Chỉ phiên bản nháp mới có thể xuất bản.");
        }
        validateReadyToPublish(course);

        versionRepository.findFirstByOnlineCourseAndStatusOrderByVersionNumberDesc(
                course,
                CourseVersionStatus.PUBLISHED
        ).ifPresent(current -> {
            if (!current.getId().equals(version.getId())) {
                current.setStatus(CourseVersionStatus.RETIRED);
                versionRepository.saveAndFlush(current);
            }
        });

        synchronizeVersionMetadata(version);
        version.setStatus(CourseVersionStatus.PUBLISHED);
        version.setPublishedBy(publisher);
        version.setPublishedAt(LocalDateTime.now());
        course.setStatus(PackageStatus.PUBLISHED);
        return toResponse(version, true);
    }

    @Override
    public void assertEditableDraft(OnlineCourse course, String actorEmail) {
        if (actorEmail != null && !actorEmail.isBlank()) {
            requireEditor(actorEmail);
        }
        if (findEditableVersion(course).isPresent()) {
            return;
        }
        OnlineCourseVersion published = versionRepository
                .findFirstByOnlineCourseAndStatusOrderByVersionNumberDesc(course, CourseVersionStatus.PUBLISHED)
                .orElse(null);
        if (published == null) {
            PackageStatus status = course.getStatus();
            if (status == PackageStatus.DRAFT || status == PackageStatus.REJECTED) {
                return;
            }
        }
        throw new IllegalStateException("Khóa học đã xuất bản. Hãy tạo phiên bản nháp mới trước khi chỉnh sửa.");
    }

    @Override
    @Transactional(readOnly = true)
    public OnlineCourseVersion requireEditableVersion(OnlineCourse course) {
        return findEditableVersion(course)
                .orElseThrow(() -> new IllegalStateException(
                        "Khóa học đã xuất bản. Hãy tạo phiên bản nháp mới trước khi chỉnh sửa."
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public OnlineCourseVersion resolveWorkingVersion(OnlineCourse course) {
        if (course == null) {
            return null;
        }
        List<OnlineCourseVersion> versions = versionRepository.findByOnlineCourseOrderByVersionNumberDesc(course);
        for (CourseVersionStatus status : List.of(
                CourseVersionStatus.DRAFT,
                CourseVersionStatus.PENDING_REVIEW,
                CourseVersionStatus.PUBLISHED
        )) {
            OnlineCourseVersion match = versions.stream()
                    .filter(version -> version.getStatus() == status)
                    .findFirst()
                    .orElse(null);
            if (match != null) {
                initializeVersionModules(match);
                return match;
            }
        }
        OnlineCourseVersion any = versions.stream().findFirst().orElse(null);
        if (any != null) {
            initializeVersionModules(any);
        }
        return any;
    }

    @Override
    public void synchronizeDraftSnapshot(OnlineCourse course) {
        OnlineCourseVersion editableVersion = findEditableVersion(course).orElse(null);
        if (editableVersion != null) {
            synchronizeVersionMetadata(editableVersion);
        }
    }

    @Override
    public void assertEnrollmentCourseVersionBelongsToCourse(OnlineCourseEnrollment enrollment, OnlineCourse course) {
        if (enrollment == null || course == null || enrollment.getCourseVersion() == null) {
            return;
        }
        OnlineCourseVersion version = enrollment.getCourseVersion();
        OnlineCourse versionCourse = version.getOnlineCourse();
        if (versionCourse == null || !course.getId().equals(versionCourse.getId())) {
            throw new IllegalArgumentException("Phiên bản khóa học không thuộc khóa học của enrollment.");
        }
    }

    @Override
    public OnlineCourseVersion requirePublishedVersion(OnlineCourse course) {
        OnlineCourseVersion existing = versionRepository
                .findFirstByOnlineCourseAndStatusOrderByVersionNumberDesc(course, CourseVersionStatus.PUBLISHED)
                .orElse(null);
        if (existing != null) {
            initializeVersionModules(existing);
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
                        .createdBy(course.getCreatedBy())
                        .build());
        synchronizeVersionMetadata(version);
        version.setStatus(CourseVersionStatus.PUBLISHED);
        version.setPublishedAt(LocalDateTime.now());
        return versionRepository.save(version);
    }

    @Override
    public void refreshPublishedSnapshot(OnlineCourse course) {
        OnlineCourseVersion published = requirePublishedVersion(course);
        synchronizeVersionMetadata(published);
        versionRepository.save(published);
    }

    /**
     * Constructs the full course details tailored for a specific student's enrollment.
     * This ensures the student sees the correct version of the course (pinned or latest)
     * and attaches their personal learning progress.
     */
    @Override
    @Transactional(readOnly = true)
    public OnlineCourseResponse readLatestPublishedForEnrollment(OnlineCourseEnrollment enrollment, OnlineCourse liveCourse) {
        // 1. Determine which version of the course this student should see
        OnlineCourseVersion pinned = resolvePinnedOrLatestPublished(enrollment, liveCourse);
        
        // 2. Read the course structure (modules/lessons) from that specific version
        OnlineCourseResponse response = readVersionContent(pinned, liveCourse);
        
        // 3. Attach student's personal enrollment data to the response
        response.setRegistered(true);
        response.setProgressPercent(enrollment.getProgressPercent());
        response.setEnrollmentId(enrollment.getId());
        
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public OnlineCourseResponse readPublishedSnapshot(OnlineCourse course, boolean includeLessonContent) {
        OnlineCourseVersion published = findLatestPublishedVersion(course);
        OnlineCourseResponse response = readVersionContent(published, course);
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
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<Long> getLatestPublishedAssessmentIds(OnlineCourseEnrollment enrollment) {
        OnlineCourse course = resolveEnrollmentCourse(enrollment);
        normalizeAssessmentProgressKeys(course);
        OnlineCourseVersion version = findLatestPublishedVersion(course);
        if (version == null) {
            return courseAssessmentRepository
                    .findByOnlineCourseAndActiveTrueOrderByDisplayOrderAscIdAsc(course)
                    .stream()
                    .map(CourseAssessment::getId)
                    .toList();
        }
        return courseAssessmentRepository
                .findByOnlineCourseVersionAndActiveTrueOrderByDisplayOrderAscIdAsc(version).stream()
                .map(CourseAssessment::getId)
                .toList();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<Long> getProgressBaselineAssessmentIds(OnlineCourseEnrollment enrollment) {
        OnlineCourse course = resolveEnrollmentCourse(enrollment);
        normalizeAssessmentProgressKeys(course);
        OnlineCourseVersion baselineVersion = enrollment.getCourseVersion();
        if (baselineVersion == null) {
            baselineVersion = findLatestPublishedVersion(course);
        }
        if (baselineVersion == null) {
            return courseAssessmentRepository
                    .findByOnlineCourseAndActiveTrueOrderByDisplayOrderAscIdAsc(course)
                    .stream()
                    .map(CourseAssessment::getId)
                    .toList();
        }
        return courseAssessmentRepository
                .findByOnlineCourseVersionAndActiveTrueOrderByDisplayOrderAscIdAsc(baselineVersion).stream()
                .map(CourseAssessment::getId)
                .toList();
    }

    private void validateReadyToPublish(OnlineCourse course) {
        List<CourseAssessmentResponse> assessments = courseAssessmentRepository
                .findByOnlineCourseAndActiveTrueOrderByDisplayOrderAscIdAsc(course)
                .stream()
                .map(this::toPreviewAssessmentResponse)
                .toList();
        List<String> blockingErrors = previewValidator.validate(mapper.toResponse(course), assessments).stream()
                .filter(warning -> "ERROR".equalsIgnoreCase(warning.getSeverity()))
                .map(warning -> warning.getMessage())
                .distinct()
                .toList();
        if (!blockingErrors.isEmpty()) {
            throw new IllegalArgumentException(
                    "Không thể xuất bản vì nội dung chưa hợp lệ: " + String.join(" ", blockingErrors)
            );
        }
    }

    private CourseAssessmentResponse toPreviewAssessmentResponse(CourseAssessment assessment) {
        return CourseAssessmentResponse.builder()
                .id(assessment.getId())
                .courseId(assessment.getOnlineCourseVersion().getOnlineCourse().getId())
                .moduleId(assessment.getModule() == null ? null : assessment.getModule().getId())
                .lessonId(assessment.getOnlineLesson() == null ? null : assessment.getOnlineLesson().getId())
                .assessmentBankItemId(assessment.getAssessmentBankItem() == null
                        ? null
                        : assessment.getAssessmentBankItem().getId())
                .moduleTitle(assessment.getModule() == null ? null : assessment.getModule().getTitle())
                .lessonTitle(assessment.getOnlineLesson() == null ? null : assessment.getOnlineLesson().getTitle())
                .title(assessment.getTitle())
                .description(assessment.getDescription())
                .type(assessment.getType())
                .skill(assessment.getSkill())
                .aiEvaluationMode(assessment.getAiEvaluationMode())
                .instructions(assessment.getInstructions())
                .objectiveAnswerKey(assessment.getObjectiveAnswerKey())
                .uiConfigJson(assessment.getAssessmentConfig())
                .passingScore(assessment.getPassingScore())
                .maxScore(assessment.getMaxScore())
                .timeLimitMinutes(assessment.getTimeLimitMinutes())
                .displayOrder(assessment.getDisplayOrder())
                .active(assessment.isActive())
                .build();
    }

    @Override
    @Transactional
    public void assertAssessmentBelongsToEnrollment(OnlineCourseEnrollment enrollment, Long assessmentId) {
        if (!getLatestPublishedAssessmentIds(enrollment).contains(assessmentId)) {
            throw new IllegalArgumentException("Bài đánh giá không thuộc phiên bản mới nhất của khóa học này.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void assertLessonBelongsToEnrollment(OnlineCourseEnrollment enrollment, Long lessonId) {
        OnlineCourse course = resolveEnrollmentCourse(enrollment);
        OnlineCourseVersion pinned = resolvePinnedOrLatestPublished(enrollment, course);
        if (!lessonBelongsToVersion(lessonId, pinned, course)) {
            throw new IllegalArgumentException("Bài học không thuộc phiên bản đã đăng ký của khóa học này.");
        }
    }

    /**
     * Asserts whether a student is allowed to transition a lesson's progress (e.g., mark as completed)
     * based on the sequential learning rules of the course version.
     */
    @Override
    @Transactional(readOnly = true)
    public void assertLessonProgressTransitionAllowed(
            OnlineCourseEnrollment enrollment,
            Long lessonId,
            boolean completed
    ) {
        // 1. Resolve the correct course and the specific version the student is enrolled in
        OnlineCourse course = resolveEnrollmentCourse(enrollment);
        OnlineCourseVersion pinned = resolvePinnedOrLatestPublished(enrollment, course);
        OnlineCourseResponse snapshot = readVersionContent(pinned, course);
        
        // 2. Build a flattened, ordered list of all lesson IDs in this course version
        List<Long> orderedLessonIds = snapshot.getModules() == null
                ? List.of()
                : snapshot.getModules().stream()
                .flatMap(module -> module.getLessons().stream())
                .map(LessonResponse::getId)
                .toList();
                
        // 3. Find the position (index) of the requested lesson within the course
        int lessonIndex = orderedLessonIds.indexOf(lessonId);
        if (lessonIndex < 0) {
            throw new IllegalArgumentException("Bài học không thuộc phiên bản đã đăng ký của khóa học này.");
        }
        
        // 4. If the user is un-completing a lesson (completed = false), always allow it
        if (!completed) {
            return;
        }
        
        // 5. Fetch all lessons the user has previously completed in this course version
        var completedLessonIds = lessonProgressRepository
                .findByEnrollmentAndStatusOrderByCompletedAtDesc(enrollment, LessonProgressStatus.COMPLETED)
                .stream()
                .map(progress -> progress.getLesson().getId())
                .collect(java.util.stream.Collectors.toSet());
                
        // 6. Find the furthest lesson index the user has reached so far
        int furthestCompletedIndex = completedLessonIds.stream()
                .mapToInt(orderedLessonIds::indexOf)
                .max()
                .orElse(-1);
                
        // 7. If the requested lesson is at or before their furthest reached index, allow it (e.g., re-completing an old lesson)
        if (lessonIndex <= furthestCompletedIndex) {
            return;
        }
        
        // 8. Enforce sequential progression: if it's a new lesson (beyond furthest reached), 
        // the immediate preceding lesson must have been completed.
        if (lessonIndex > 0 && !completedLessonIds.contains(orderedLessonIds.get(lessonIndex - 1))) {
            throw new IllegalStateException("Bạn cần hoàn thành bài học trước đó trong phiên bản này trước khi tiếp tục.");
        }
        
        // 9. Ensure any required module assessments preceding this lesson have been passed
        assertPreviousModuleAssessmentsPassed(enrollment, course, snapshot, lessonId);
    }

    private void assertPreviousModuleAssessmentsPassed(
            OnlineCourseEnrollment enrollment,
            OnlineCourse course,
            OnlineCourseResponse snapshot,
            Long lessonId
    ) {
        User student = enrollment.getStudent();
        if (student == null || snapshot.getModules() == null) {
            return;
        }
        int moduleIndex = -1;
        for (int index = 0; index < snapshot.getModules().size(); index++) {
            boolean belongsToModule = snapshot.getModules().get(index).getLessons().stream()
                    .anyMatch(lesson -> lessonId.equals(lesson.getId()));
            if (belongsToModule) {
                moduleIndex = index;
                break;
            }
        }
        if (moduleIndex <= 0) {
            return;
        }
        Set<Long> previousModuleIds = snapshot.getModules().subList(0, moduleIndex).stream()
                .map(ModuleResponse::getId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        if (previousModuleIds.isEmpty()) {
            return;
        }
        List<CourseAssessment> previousModuleTests = courseAssessmentRepository
                .findByOnlineCourseAndActiveTrueOrderByDisplayOrderAscIdAsc(course)
                .stream()
                .filter(assessment -> assessment.getModule() != null && previousModuleIds.contains(assessment.getModule().getId()))
                .toList();
        boolean previousTestsPassed = previousModuleTests.stream().allMatch(assessment ->
                assessmentSubmissionRepository.existsByAssessmentAndStudentAndStatusIn(
                        assessment,
                        student,
                        Set.of(SubmissionStatus.PASSED, SubmissionStatus.AI_EVALUATED)
                ));
        if (!previousTestsPassed) {
            throw new IllegalStateException("Bạn cần hoàn thành bài đánh giá của mô-đun trước trước khi học mô-đun tiếp theo.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAssessmentReferencedByPublishedHistory(OnlineCourse course, Long assessmentId) {
        if (course == null || assessmentId == null) {
            return false;
        }
        return versionRepository.findByOnlineCourseOrderByVersionNumberDesc(course).stream()
                .filter(version -> version.getStatus() == CourseVersionStatus.PUBLISHED
                        || version.getStatus() == CourseVersionStatus.RETIRED)
                .flatMap(version -> courseAssessmentRepository
                        .findByOnlineCourseVersionAndActiveTrueOrderByDisplayOrderAscIdAsc(version).stream())
                .anyMatch(assessment -> assessmentId.equals(assessment.getId()));
    }

    private void normalizeAssessmentProgressKeys(OnlineCourse course) {
        List<OnlineCourseVersion> history = versionRepository.findByOnlineCourseOrderByVersionNumberDesc(course)
                .stream()
                .filter(version -> version.getStatus() == CourseVersionStatus.PUBLISHED
                        || version.getStatus() == CourseVersionStatus.RETIRED)
                .sorted(Comparator.comparing(OnlineCourseVersion::getVersionNumber))
                .toList();
        if (history.isEmpty()) {
            return;
        }

        List<CourseAssessment> changed = new ArrayList<>();
        List<CourseAssessment> previousAssessments = List.of();
        for (OnlineCourseVersion version : history) {
            List<CourseAssessment> assessments = courseAssessmentRepository
                    .findByOnlineCourseVersionAndActiveTrueOrderByDisplayOrderAscIdAsc(version);
            Set<String> usedProgressKeys = new HashSet<>();

            for (int index = 0; index < assessments.size(); index++) {
                CourseAssessment assessment = assessments.get(index);
                if (assessment.getProgressKey() == null || assessment.getProgressKey().isBlank()) {
                    CourseAssessment predecessor = findAssessmentPredecessor(
                            assessment,
                            previousAssessments,
                            usedProgressKeys,
                            index
                    );
                    String progressKey = predecessor == null ? null : predecessor.getProgressKey();
                    if (progressKey == null || progressKey.isBlank()) {
                        progressKey = UUID.randomUUID().toString();
                    }
                    assessment.setProgressKey(progressKey);
                    changed.add(assessment);
                }
                usedProgressKeys.add(assessment.getProgressKey());
            }
            previousAssessments = assessments;
        }

        if (!changed.isEmpty()) {
            courseAssessmentRepository.saveAll(changed);
        }
    }

    private CourseAssessment findAssessmentPredecessor(
            CourseAssessment assessment,
            List<CourseAssessment> previousAssessments,
            Set<String> usedProgressKeys,
            int snapshotIndex
    ) {
        if (previousAssessments.isEmpty()) {
            return null;
        }

        CourseAssessment sameId = previousAssessments.stream()
                .filter(candidate -> Objects.equals(candidate.getId(), assessment.getId()))
                .findFirst()
                .orElse(null);
        if (sameId != null) {
            return sameId;
        }

        List<CourseAssessment> available = previousAssessments.stream()
                .filter(candidate -> candidate.getProgressKey() != null)
                .filter(candidate -> !usedProgressKeys.contains(candidate.getProgressKey()))
                .toList();
        if (assessment.getAssessmentBankItem() != null) {
            CourseAssessment sameBankItem = available.stream()
                    .filter(candidate -> candidate.getAssessmentBankItem() != null)
                    .filter(candidate -> Objects.equals(
                            candidate.getAssessmentBankItem().getId(),
                            assessment.getAssessmentBankItem().getId()
                    ))
                    .findFirst()
                    .orElse(null);
            if (sameBankItem != null) {
                return sameBankItem;
            }
        }

        List<CourseAssessment> sameSlot = available.stream()
                .filter(candidate -> sameAssessmentModule(candidate, assessment))
                .filter(candidate -> Objects.equals(candidate.getDisplayOrder(), assessment.getDisplayOrder()))
                .filter(candidate -> candidate.getType() == assessment.getType())
                .filter(candidate -> candidate.getSkill() == assessment.getSkill())
                .toList();
        if (sameSlot.size() == 1) {
            return sameSlot.get(0);
        }

        List<CourseAssessment> sameModuleAndKind = available.stream()
                .filter(candidate -> sameAssessmentModule(candidate, assessment))
                .filter(candidate -> candidate.getType() == assessment.getType())
                .filter(candidate -> candidate.getSkill() == assessment.getSkill())
                .toList();
        if (sameModuleAndKind.size() == 1) {
            return sameModuleAndKind.get(0);
        }

        if (snapshotIndex < previousAssessments.size()) {
            CourseAssessment samePosition = previousAssessments.get(snapshotIndex);
            if (samePosition.getProgressKey() != null
                    && !usedProgressKeys.contains(samePosition.getProgressKey())) {
                return samePosition;
            }
        }
        return null;
    }

    private boolean sameAssessmentModule(CourseAssessment left, CourseAssessment right) {
        Long leftModuleId = left.getModule() == null ? null : left.getModule().getId();
        Long rightModuleId = right.getModule() == null ? null : right.getModule().getId();
        return Objects.equals(leftModuleId, rightModuleId);
    }

    private OnlineCourseVersion findLatestPublishedVersion(OnlineCourse course) {
        if (course == null) {
            return null;
        }
        return versionRepository
                .findFirstByOnlineCourseAndStatusOrderByVersionNumberDesc(course, CourseVersionStatus.PUBLISHED)
                .orElse(null);
    }

    private OnlineCourse resolveEnrollmentCourse(OnlineCourseEnrollment enrollment) {
        if (enrollment == null) {
            throw new IllegalArgumentException("Không tìm thấy enrollment khóa học.");
        }
        if (enrollment.getOnlineCourse() != null) {
            return enrollment.getOnlineCourse();
        }
        if (enrollment.getCourseVersion() != null && enrollment.getCourseVersion().getOnlineCourse() != null) {
            return enrollment.getCourseVersion().getOnlineCourse();
        }
        return java.util.Optional.ofNullable(enrollment.getOnlineCourse())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khóa học của enrollment này."));
    }

    private OnlineCourseVersion resolvePinnedOrLatestPublished(OnlineCourseEnrollment enrollment, OnlineCourse course) {
        OnlineCourseVersion pinned = enrollment == null ? null : enrollment.getCourseVersion();
        if (pinned != null) {
            OnlineCourse versionCourse = pinned.getOnlineCourse();
            if (versionCourse != null && course != null && !course.getId().equals(versionCourse.getId())) {
                throw new IllegalArgumentException("Phiên bản khóa học không thuộc khóa học của enrollment.");
            }
            initializeVersionModules(pinned);
            return pinned;
        }
        return findLatestPublishedVersion(course);
    }

    private boolean lessonBelongsToVersion(Long lessonId, OnlineCourseVersion version, OnlineCourse course) {
        if (lessonId == null) {
            return false;
        }
        if (version != null) {
            initializeVersionModules(version);
            if (version.getModules() != null && !version.getModules().isEmpty()) {
                return version.getModules().stream()
                        .flatMap(module -> module.getLessons().stream())
                        .anyMatch(lesson -> lessonId.equals(lesson.getId()));
            }
            OnlineLesson lesson = lessonRepository.findById(lessonId).orElse(null);
            if (lesson != null
                    && lesson.getModule() != null
                    && lesson.getModule().getOnlineCourseVersion() != null
                    && version.getId() != null
                    && version.getId().equals(lesson.getModule().getOnlineCourseVersion().getId())) {
                return true;
            }
        }
        OnlineCourseResponse snapshot = readVersionContent(version, course);
        return snapshot.getModules() != null && snapshot.getModules().stream()
                .flatMap(module -> module.getLessons().stream())
                .anyMatch(lesson -> lessonId.equals(lesson.getId()));
    }

    private OnlineCourseResponse readVersionContent(OnlineCourseVersion version, OnlineCourse fallbackCourse) {
        if (version != null) {
            initializeVersionModules(version);
            if (version.getModules() != null && !version.getModules().isEmpty()) {
                return mapper.toResponse(fallbackCourse, version.getModules());
            }
        }
        return mapper.toResponse(fallbackCourse);
    }

    @Override
    @Transactional(readOnly = true)
    public void assertLessonCanBeRemoved(OnlineCourse course, Long lessonId) {
        boolean referenced = versionRepository.findByOnlineCourseOrderByVersionNumberDesc(course).stream()
                .filter(version -> version.getStatus() == CourseVersionStatus.PUBLISHED
                        || version.getStatus() == CourseVersionStatus.RETIRED)
                .peek(this::initializeVersionModules)
                .flatMap(version -> version.getModules().stream())
                .flatMap(module -> module.getLessons().stream())
                .anyMatch(lesson -> lessonId.equals(lesson.getId()));
        if (referenced) {
            throw new IllegalStateException(
                    "Không thể xóa bài học đã thuộc phiên bản đang được học. Hãy giữ lessonKey và thay nội dung ở bài mới."
            );
        }
    }

    private void hideProtectedLessonContent(LessonResponse lesson) {
        if (lesson.isPreview()) {
            return;
        }
        lesson.setContentText(null);
        lesson.setVideoUrl(null);
        lesson.setMaterialUrl(null);
        lesson.setTranscriptSegments(List.of());
        lesson.setFlashcardSets(List.of());
    }

    private void synchronizeVersionMetadata(OnlineCourseVersion version) {
        initializeVersionModules(version);
        version.setTotalRequiredLessons(countLessons(version.getModules()));
        version.setTotalRequiredAssessments(Math.toIntExact(
                courseAssessmentRepository.countByOnlineCourseVersionAndActiveTrue(version)
        ));
    }

    private void cloneModulesOntoDraft(OnlineCourseVersion published, OnlineCourseVersion draft) {
        if (published.getModules() == null) {
            published.setModules(new ArrayList<>());
        }
        initializeVersionModules(published);
        if (published.getModules().isEmpty()) {
            return;
        }
        for (OnlineCourseModule sourceModule : published.getModules()) {
            OnlineCourseModule clonedModule = OnlineCourseModule.builder()
                    .title(sourceModule.getTitle())
                    .description(sourceModule.getDescription())
                    .sequenceNumber(sourceModule.getSequenceNumber())
                    .build();
            for (OnlineLesson sourceLesson : sourceModule.getLessons()) {
                OnlineLesson clonedLesson = OnlineLesson.builder()
                        .stableLessonKey(sourceLesson.getStableLessonKey())
                        .title(sourceLesson.getTitle())
                        .description(sourceLesson.getDescription())
                        .contentType(sourceLesson.getContentType())
                        .contentText(sourceLesson.getContentText())
                        .videoUrl(sourceLesson.getVideoUrl())
                        .materialUrl(sourceLesson.getMaterialUrl())
                        .transcriptJson(sourceLesson.getTranscriptJson())
                        .durationMinutes(sourceLesson.getDurationMinutes())
                        .sequenceNumber(sourceLesson.getSequenceNumber())
                        .preview(sourceLesson.isPreview())
                        .build();
                if (sourceLesson.getFlashcardRefs() != null) {
                    for (CourseLessonFlashcardRef sourceRef : sourceLesson.getFlashcardRefs()) {
                        clonedLesson.addFlashcardRef(CourseLessonFlashcardRef.builder()
                                .contentBankItem(sourceRef.getContentBankItem())
                                .displayOrder(sourceRef.getDisplayOrder())
                                .build());
                    }
                }
                clonedModule.addLesson(clonedLesson);
            }
            draft.addModule(clonedModule);
        }
    }

    private Optional<OnlineCourseVersion> findEditableVersion(OnlineCourse course) {
        return versionRepository
                .findFirstByOnlineCourseAndStatusOrderByVersionNumberDesc(course, CourseVersionStatus.DRAFT)
                .or(() -> versionRepository.findFirstByOnlineCourseAndStatusOrderByVersionNumberDesc(
                        course,
                        CourseVersionStatus.PENDING_REVIEW
                ))
                .map(version -> {
                    initializeVersionModules(version);
                    return version;
                });
    }

    private void initializeVersionModules(OnlineCourseVersion version) {
        if (version == null || version.getModules() == null) {
            return;
        }
        version.getModules().forEach(module -> {
            module.getLessons().size();
            module.getLessons().forEach(lesson -> {
                if (lesson.getFlashcardRefs() != null) {
                    lesson.getFlashcardRefs().size();
                }
            });
        });
    }

    private int countLessons(OnlineCourse course) {
        return countLessons(course.getLatestModules());
    }

    private int countLessons(List<OnlineCourseModule> modules) {
        if (modules == null) {
            return 0;
        }
        return modules.stream().mapToInt(module -> module.getLessons().size()).sum();
    }

    private List<CourseAssessment> cloneAssessmentsForDraft(
            List<CourseAssessment> source,
            OnlineCourseVersion draft
    ) {
        if (source.isEmpty()) {
            return List.of();
        }
        List<CourseAssessment> clones = source.stream()
                .sorted(Comparator.comparing(CourseAssessment::getDisplayOrder).thenComparing(CourseAssessment::getId))
                .map(assessment -> {
                    String progressKey = assessment.getProgressKey();
                    if (progressKey == null || progressKey.isBlank()) {
                        progressKey = UUID.randomUUID().toString();
                        assessment.setProgressKey(progressKey);
                    }
                    assessment.setActive(false);
                    OnlineCourseModule draftModule = findDraftModule(draft, assessment.getModule());
                    OnlineLesson draftLesson = findDraftLesson(draftModule, assessment.getOnlineLesson());
                    return CourseAssessment.builder()
                            .onlineCourseVersion(draft)
                            .module(draftModule)
                            .onlineLesson(draftLesson)
                            .rubric(assessment.getRubric())
                            .assessmentBankItem(assessment.getAssessmentBankItem())
                            .progressKey(progressKey)
                            .title(assessment.getTitle())
                            .description(assessment.getDescription())
                            .type(assessment.getType())
                            .skill(assessment.getSkill())
                            .aiEvaluationMode(assessment.getAiEvaluationMode())
                            .instructions(assessment.getInstructions())
                            .objectiveAnswerKey(assessment.getObjectiveAnswerKey())
                            .assessmentConfig(assessment.getAssessmentConfig())
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

    private OnlineCourseModule findDraftModule(
            OnlineCourseVersion draft,
            OnlineCourseModule sourceModule
    ) {
        if (sourceModule == null) {
            return null;
        }
        return draft.getModules().stream()
                .filter(module -> Objects.equals(module.getSequenceNumber(), sourceModule.getSequenceNumber()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy mô-đun tương ứng trong phiên bản nháp."));
    }

    private OnlineLesson findDraftLesson(
            OnlineCourseModule draftModule,
            OnlineLesson sourceLesson
    ) {
        if (sourceLesson == null) {
            return null;
        }
        if (draftModule == null) {
            throw new IllegalStateException("Bài đánh giá theo bài học không có mô-đun tương ứng.");
        }
        return draftModule.getLessons().stream()
                .filter(lesson -> Objects.equals(lesson.getStableLessonKey(), sourceLesson.getStableLessonKey()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy bài học tương ứng trong phiên bản nháp."));
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
                .createdByName(version.getCreatedBy() == null ? null : version.getCreatedBy().getFullName())
                .publishedByName(version.getPublishedBy() == null ? null : version.getPublishedBy().getFullName())
                .publishedAt(version.getPublishedAt())
                .createdAt(version.getCreatedAt())
                .updatedAt(version.getUpdatedAt())
                .content(includeContent ? readVersionContent(version, version.getOnlineCourse()) : null)
                .build();
    }

    private OnlineCourse findCourse(Long courseId) {
        OnlineCourse course = onlineCourseRepository.findWithModulesById(courseId)
                .filter(item -> item.getStatus() != PackageStatus.ARCHIVED)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học."));
        course.getLatestModules().forEach(module -> module.getLessons().size());
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

    private User requireUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng."));
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
