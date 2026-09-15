package fu.sep490.g23.backend.service.curriculum.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fu.sep490.g23.backend.dto.request.curriculum.AssessmentBankItemRequest;
import fu.sep490.g23.backend.dto.request.curriculum.InstructorLedCourseRequest;
import fu.sep490.g23.backend.dto.request.curriculum.CourseUnitContentRefRequest;
import fu.sep490.g23.backend.dto.request.curriculum.CourseLessonRequest;
import fu.sep490.g23.backend.dto.request.curriculum.CourseUnitRequest;
import fu.sep490.g23.backend.dto.request.curriculum.FlashcardSetRequest;
import fu.sep490.g23.backend.dto.response.assessment.AssessmentRubricResponse;
import fu.sep490.g23.backend.dto.response.assessment.RubricCriterionResponse;
import fu.sep490.g23.backend.dto.response.curriculum.AssessmentBankItemResponse;
import fu.sep490.g23.backend.dto.response.curriculum.InstructorLedCourseResponse;
import fu.sep490.g23.backend.dto.response.curriculum.CourseUnitContentRefResponse;
import fu.sep490.g23.backend.dto.response.curriculum.CourseLessonResponse;
import fu.sep490.g23.backend.dto.response.curriculum.CourseUnitResponse;
import fu.sep490.g23.backend.dto.response.curriculum.FlashcardSetResponse;
import fu.sep490.g23.backend.entity.assessment.AssessmentRubric;
import fu.sep490.g23.backend.entity.assessment.ExerciseBankItem;
import fu.sep490.g23.backend.entity.assessment.enums.AiEvaluationMode;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentType;
import fu.sep490.g23.backend.entity.classroom.CenterMaterialLibraryItem;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomOfferingStatus;
import fu.sep490.g23.backend.entity.curriculum.AssessmentBankItem;
import fu.sep490.g23.backend.entity.course.InstructorLedCourse;
import fu.sep490.g23.backend.entity.course.CourseLesson;
import fu.sep490.g23.backend.entity.course.CourseUnit;
import fu.sep490.g23.backend.entity.course.CourseUnitContentRef;
import fu.sep490.g23.backend.entity.course.enums.CourseUnitContentType;
import fu.sep490.g23.backend.entity.course.enums.PackageStatus;
import fu.sep490.g23.backend.entity.curriculum.ContentBankItem;
import fu.sep490.g23.backend.entity.curriculum.FlashcardSet;
import fu.sep490.g23.backend.repository.assessment.AssessmentRubricRepository;
import fu.sep490.g23.backend.repository.assessment.ExerciseBankItemRepository;
import fu.sep490.g23.backend.repository.classroom.CenterMaterialLibraryItemRepository;
import fu.sep490.g23.backend.repository.classroom.ClassScheduleRepository;
import fu.sep490.g23.backend.repository.curriculum.AssessmentBankItemRepository;
import fu.sep490.g23.backend.repository.course.InstructorLedCourseRepository;
import fu.sep490.g23.backend.repository.course.CourseLessonRepository;
import fu.sep490.g23.backend.repository.course.CourseUnitRepository;
import fu.sep490.g23.backend.repository.course.CourseUnitContentRefRepository;
import fu.sep490.g23.backend.repository.curriculum.ContentBankItemRepository;
import fu.sep490.g23.backend.repository.curriculum.FlashcardSetRepository;
import fu.sep490.g23.backend.service.curriculum.InstructorLedCourseManagementService;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.classroom.ClassSection;
import fu.sep490.g23.backend.entity.curriculum.enums.ContentBankType;
import fu.sep490.g23.backend.security.ClassroomAccessHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Handles instructor-led course management, including units, lessons, and the
 * learning resources attached to each unit.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class InstructorLedCourseManagementServiceImpl implements InstructorLedCourseManagementService {

    private static final int COURSE_CODE_MAX_LENGTH = 120;
    private static final Set<String> EXAM_CATEGORIES = Set.of("IELTS", "TOEIC", "GENERAL_ENGLISH");
    private static final Set<String> CEFR_LEVELS = Set.of("A1", "A2", "B1", "B2", "C1", "C2");
    private static final List<String> SKILL_ORDER = List.of(
            "LISTENING",
            "READING",
            "WRITING",
            "SPEAKING",
            "VOCABULARY",
            "GRAMMAR",
            "PRONUNCIATION",
            "COMMUNICATION"
    );

    private final InstructorLedCourseRepository instructorLedCourseRepository;
    private final CourseUnitRepository unitRepository;
    private final CourseLessonRepository courseLessonRepository;
    private final CourseUnitContentRefRepository contentRefRepository;
    private final ContentBankItemRepository contentBankItemRepository;
    private final CenterMaterialLibraryItemRepository materialRepository;
    private final ExerciseBankItemRepository exerciseRepository;
    private final AssessmentRubricRepository assessmentRubricRepository;
    private final AssessmentBankItemRepository assessmentBankRepository;
    private final FlashcardSetRepository flashcardSetRepository;
    private final ClassroomAccessHelper accessHelper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Lists courses by most recent update without loading detailed unit structures. */
    @Override
    @Transactional(readOnly = true)
    public List<InstructorLedCourseResponse> listInstructorLedCourses() {
        List<InstructorLedCourse> courses = instructorLedCourseRepository.findAllByOrderByUpdatedAtDescIdDesc();
        return courses.stream().map(course -> toInstructorLedCourseResponse(course, false)).toList();
    }

    /** Filters and paginates courses by keyword, exam, entry level, and status. */
    @Override
    @Transactional(readOnly = true)
    public Page<InstructorLedCourseResponse> pageInstructorLedCourses(
            String keyword,
            String examCategory,
            String entryLevel,
            String status,
            Pageable pageable
    ) {
        Specification<InstructorLedCourse> specification = (root, query, criteriaBuilder) ->
                criteriaBuilder.conjunction();
        if (StringUtils.hasText(keyword)) {
            String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, query, criteriaBuilder) -> criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("code")), pattern)
            ));
        }
        if (StringUtils.hasText(examCategory)) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("examType"), examCategory.trim().toUpperCase(Locale.ROOT)));
        }
        if (StringUtils.hasText(entryLevel)) {
            String pattern = "%" + entryLevel.trim().toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("entryLevel")), pattern));
        }
        if (StringUtils.hasText(status)) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("publicationStatus"), PackageStatus.valueOf(status.trim().toUpperCase(Locale.ROOT))));
        }
        return instructorLedCourseRepository.findAll(specification, pageable)
                .map(course -> toInstructorLedCourseResponse(course, false));
    }

    /** Returns course details with all units, lessons, and linked resources. */
    @Override
    @Transactional(readOnly = true)
    public InstructorLedCourseResponse getInstructorLedCourse(Long id) {
        return toInstructorLedCourseResponse(findInstructorLedCourse(id), true);
    }

    /**
     * Creates a new instructor-led course in DRAFT status with a unique course code.
     */
    @Override
    public InstructorLedCourseResponse createInstructorLedCourse(InstructorLedCourseRequest request) {
        String code = resolveNewCourseCode(request);
        InstructorLedCourse course = InstructorLedCourse.builder()
                .title(requireText(request.getTitle(), "Tên khóa học không được để trống."))
                .code(code)
                .shortDescription(trimOrNull(request.getShortDescription()))
                .description(trimOrNull(request.getDescription()))
                .durationLabel(trimOrNull(request.getDurationLabel()))
                .baseTuitionFeeVnd(request.getBaseTuitionFeeVnd() != null ? request.getBaseTuitionFeeVnd() : BigDecimal.ZERO)
                .saleTuitionFeeVnd(request.getSaleTuitionFeeVnd())
                .learningOutcomes(trimOrNull(request.getOutcomes()))
                .teacherGuide(trimOrNull(request.getTeacherGuide()))
                .publicationStatus(parsePublicationStatus(request.getStatus()))
                .build();
        applyEnglishProfile(course, request);

        // Disallow publishing new courses without units and lessons
        if (course.getPublicationStatus() == PackageStatus.PUBLISHED) {
            throw new RuntimeException("Khóa học mới tạo chưa có Unit và bài học nên chưa thể xuất bản. Hãy lưu nháp trước.");
        }
        return toInstructorLedCourseResponse(saveAndSyncCourse(course), true);
    }

    /**
     * Updates an existing instructor-led course metadata and publication status.
     */
    @Override
    public InstructorLedCourseResponse updateInstructorLedCourse(Long id, InstructorLedCourseRequest request) {
        InstructorLedCourse course = findInstructorLedCourse(id);
        String code = StringUtils.hasText(request.getCode())
                ? normalizeCourseCode(request.getCode())
                : course.getCode();
        if (!course.getCode().equalsIgnoreCase(code) && instructorLedCourseRepository.existsByCodeIgnoreCase(code)) {
            throw new RuntimeException("Mã khóa học có giảng viên đã tồn tại.");
        }
        course.setTitle(requireText(request.getTitle(), "Tên khóa học không được để trống."));
        course.setCode(code);
        course.setShortDescription(trimOrNull(request.getShortDescription())) ;
        course.setDescription(trimOrNull(request.getDescription()));
        course.setDurationLabel(trimOrNull(request.getDurationLabel()));
        if (request.getBaseTuitionFeeVnd() != null) {
            course.setBaseTuitionFeeVnd(request.getBaseTuitionFeeVnd());
        }
        course.setSaleTuitionFeeVnd(request.getSaleTuitionFeeVnd());
        applyEnglishProfile(course, request);
        course.setLearningOutcomes(trimOrNull(request.getOutcomes()));
        course.setTeacherGuide(trimOrNull(request.getTeacherGuide()));
        PackageStatus previousStatus = course.getPublicationStatus();
        PackageStatus nextStatus = parsePublicationStatus(request.getStatus());

        // Validate course readiness before publishing
        if (nextStatus == PackageStatus.PUBLISHED && previousStatus != PackageStatus.PUBLISHED) {
            validateReadyForPublish(course);
        }
        course.setPublicationStatus(nextStatus);
        return toInstructorLedCourseResponse(saveAndSyncCourse(course), true);
    }

    /** Archives a course when no active classroom is using it. */
    @Override
    public void archiveInstructorLedCourse(Long id) {
        InstructorLedCourse course = findInstructorLedCourse(id);
        long activeClassrooms = countActiveClassrooms(course);
        if (activeClassrooms > 0) {
            throw new RuntimeException(
                    "Không thể lưu trữ: khóa học đang được " + activeClassrooms
                            + " lớp sắp khai giảng hoặc đang diễn ra sử dụng.");
        }
        course.setPublicationStatus(PackageStatus.ARCHIVED);
        saveAndSyncCourse(course);
    }

    /** Clones a course, its units, lessons, and resource links into a new draft. */
    @Override
    public InstructorLedCourseResponse cloneInstructorLedCourse(Long id) {
        InstructorLedCourse source = findInstructorLedCourse(id);
        InstructorLedCourse clone = InstructorLedCourse.builder()
                .title(source.getTitle() + " (Bản sao)")
                .code(uniqueCourseCode(source.getCode()))
                .shortDescription(source.getShortDescription())
                .description(source.getDescription())
                .durationLabel(source.getDurationLabel())
                .baseTuitionFeeVnd(source.getBaseTuitionFeeVnd())
                .saleTuitionFeeVnd(source.getSaleTuitionFeeVnd())
                .examType(source.getExamType())
                .focusSkills(source.getFocusSkills())
                .targetBand(source.getTargetBand())
                .targetScore(source.getTargetScore())
                .entryLevel(source.getEntryLevel())
                .learningOutcomes(source.getLearningOutcomes())
                .teacherGuide(source.getTeacherGuide())
                .publicationStatus(PackageStatus.DRAFT)
                .build();

        for (CourseUnit unit : source.getUnits()) {
            CourseUnit unitClone = CourseUnit.builder()
                    .sequenceNumber(unit.getSequenceNumber())
                    .title(unit.getTitle())
                    .description(unit.getDescription())
                    .learningObjectives(unit.getLearningObjectives())
                    .build();
            clone.addUnit(unitClone);
            unit.getContentRefs().forEach(ref -> unitClone.addContentRef(CourseUnitContentRef.builder()
                    .contentType(ref.getContentType())
                    .learningResource(ref.getLearningResource())
                    .contentBankItem(ref.getContentBankItem())
                    .sequenceNumber(ref.getSequenceNumber())
                    .build()));
            unit.getLessons().forEach(lesson -> unitClone.addLesson(CourseLesson.builder()
                    .sequenceNumber(lesson.getSequenceNumber())
                    .title(lesson.getTitle())
                    .description(lesson.getDescription())
                    .learningObjectives(lesson.getLearningObjectives())
                    .build()));
        }
        InstructorLedCourse saved = instructorLedCourseRepository.save(clone);
        return toInstructorLedCourseResponse(saved, true);
    }

    /** Validates course readiness and publishes it under the acting user. */
    @Override
    public InstructorLedCourseResponse publishInstructorLedCourse(Long id, String actorEmail) {
        InstructorLedCourse course = findInstructorLedCourse(id);
        if (course.getPublicationStatus() == PackageStatus.PUBLISHED) {
            throw new RuntimeException("Khóa học đã được xuất bản.");
        }
        validateReadyForPublish(course);
        User actor = accessHelper.requireUser(actorEmail);
        course.setPublicationStatus(PackageStatus.PUBLISHED);
        course.setReviewNote(null);
        course.setCreatedBy(actor);
        course.setSubmittedAt(LocalDateTime.now());
        course.setReviewedBy(actor);
        course.setReviewedAt(LocalDateTime.now());
        course = saveAndSyncCourse(course);
        return toInstructorLedCourseResponse(course, true);
    }

    /**
     * Creates a new course unit in the instructor-led course.
     */
    @Override
    public CourseUnitResponse createUnit(Long instructorLedCourseId, CourseUnitRequest request) {
        InstructorLedCourse course = findInstructorLedCourse(instructorLedCourseId);
        CourseUnit unit = CourseUnit.builder()
                .instructorLedCourse(course)
                .sequenceNumber(defaultInt(request.getDisplayOrder()))
                .title(requireText(request.getTitle(), "Tên Unit không được để trống."))
                .description(trimOrNull(request.getDescription()))
                .learningObjectives(trimOrNull(request.getSessionPlan()))
                .build();
        course.addUnit(unit);
        CourseUnit saved = unitRepository.save(unit);
        return toUnitResponse(saved);
    }

    /**
     * Updates an existing course unit.
     */
    @Override
    public CourseUnitResponse updateUnit(Long unitId, CourseUnitRequest request) {
        CourseUnit unit = findUnit(unitId);
        unit.setSequenceNumber(defaultInt(request.getDisplayOrder()));
        unit.setTitle(requireText(request.getTitle(), "Tên Unit không được để trống."));
        unit.setDescription(trimOrNull(request.getDescription()));
        unit.setLearningObjectives(trimOrNull(request.getSessionPlan()));
        CourseUnit saved = unitRepository.save(unit);
        return toUnitResponse(saved);
    }

    /**
     * Deletes a course unit and synchronizes total sessions count.
     */
    @Override
    public void deleteUnit(Long unitId) {
        CourseUnit unit = findUnit(unitId);
        InstructorLedCourse course = unit.getInstructorLedCourse();
        unitRepository.delete(unit);
        unitRepository.flush();
        synchronizeTotalSessions(course);
    }

    /**
     * Creates a new lesson under a specified course unit.
     */
    @Override
    public CourseLessonResponse createCourseLesson(
            Long unitId,
            CourseLessonRequest request
    ) {
        CourseUnit unit = findUnit(unitId);
        validateCourseLessonRequest(request);
        assertSessionNumberAvailable(unit.getInstructorLedCourse().getId(), request.getSessionNumber(), null);
        CourseLesson lesson = CourseLesson.builder()
                .courseUnit(unit)
                .sequenceNumber(request.getSessionNumber())
                .plannedSessionCount(request.getPlannedSessionCount() == null || request.getPlannedSessionCount() < 1 ? 1 : request.getPlannedSessionCount())
                .title(requireText(request.getTitle(), "Tiêu đề bài học không được để trống."))
                .description(trimOrNull(request.getDescription()))
                .learningObjectives(trimOrNull(request.getLearningObjectives()))
                .build();
        lesson = courseLessonRepository.save(lesson);
        synchronizeTotalSessions(unit.getInstructorLedCourse());
        return toCourseLessonResponse(lesson);
    }

    /**
     * Updates an existing course lesson.
     */
    @Override
    public CourseLessonResponse updateCourseLesson(
            Long lessonId,
            CourseLessonRequest request
    ) {
        CourseLesson lesson = findCourseLesson(lessonId);
        validateCourseLessonRequest(request);
        assertSessionNumberAvailable(
                lesson.getCourseUnit().getInstructorLedCourse().getId(),
                request.getSessionNumber(),
                lessonId
        );
        lesson.setSequenceNumber(request.getSessionNumber());
        lesson.setPlannedSessionCount(request.getPlannedSessionCount() == null || request.getPlannedSessionCount() < 1 ? 1 : request.getPlannedSessionCount());
        lesson.setTitle(requireText(request.getTitle(), "Tiêu đề bài học không được để trống."));
        lesson.setDescription(trimOrNull(request.getDescription()));
        lesson.setLearningObjectives(trimOrNull(request.getLearningObjectives()));
        lesson = courseLessonRepository.save(lesson);
        return toCourseLessonResponse(lesson);
    }

    /**
     * Deletes a course lesson and recalculates course totals.
     */
    @Override
    public void deleteCourseLesson(Long lessonId) {
        CourseLesson lesson = findCourseLesson(lessonId);
        InstructorLedCourse course = lesson.getCourseUnit().getInstructorLedCourse();
        courseLessonRepository.delete(lesson);
        courseLessonRepository.flush();
        synchronizeTotalSessions(course);
    }

    /** Attaches a published material to a unit while preventing duplicate links. */
    @Override
    public CourseUnitResponse attachMaterial(Long unitId, CourseUnitContentRefRequest request) {
        CourseUnit unit = findUnit(unitId);
        CenterMaterialLibraryItem material = materialRepository.findById(request.getResourceId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy học liệu trong kho."));
        requirePublishedResource(material.getStatus(), "Học liệu");
        if (contentRefRepository.existsByCourseUnitIdAndContentTypeAndLearningResourceId(
                unitId, CourseUnitContentType.MATERIAL, material.getId())) {
            throw new IllegalArgumentException("Học liệu này đã tồn tại trong Unit.");
        }
        contentRefRepository.save(CourseUnitContentRef.builder()
                .courseUnit(unit)
                .contentType(CourseUnitContentType.MATERIAL)
                .learningResource(material)
                .sequenceNumber(defaultInt(request.getDisplayOrder()))
                .build());
        return toUnitResponse(findUnit(unitId));
    }

    /**
     * Attaches a practice exercise from the Content Bank to a course unit.
     */
    @Override
    public CourseUnitResponse attachExercise(Long unitId, CourseUnitContentRefRequest request) {
        CourseUnit unit = findUnit(unitId);
        Long resolvedId = request.getResourceId();
        ContentBankItem exercise = contentBankItemRepository.findByIdAndBankType(resolvedId, ContentBankType.EXERCISE)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài tập trong ngân hàng."));
        requirePublishedResource(exercise.getStatus(), "Bài tập");
        if (contentRefRepository.existsByCourseUnitIdAndContentTypeAndContentBankItemId(
                unitId, CourseUnitContentType.EXERCISE, exercise.getId())) {
            throw new IllegalArgumentException("Bài tập này đã tồn tại trong Unit.");
        }
        contentRefRepository.save(CourseUnitContentRef.builder()
                .courseUnit(unit)
                .contentType(CourseUnitContentType.EXERCISE)
                .contentBankItem(exercise)
                .sequenceNumber(defaultInt(request.getDisplayOrder()))
                .build());
        return toUnitResponse(findUnit(unitId));
    }

    /**
     * Attaches an assessment item from the Content Bank to a course unit.
     */
    @Override
    public CourseUnitResponse attachAssessment(Long unitId, CourseUnitContentRefRequest request) {
        CourseUnit unit = findUnit(unitId);
        Long resolvedId = request.getResourceId();
        ContentBankItem assessment = contentBankItemRepository.findByIdAndBankType(resolvedId, ContentBankType.ASSESSMENT)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đề trong ngân hàng."));
        requirePublishedResource(assessment.getStatus(), "Đề đánh giá");
        if (contentRefRepository.existsByCourseUnitIdAndContentTypeAndContentBankItemId(
                unitId, CourseUnitContentType.ASSESSMENT, assessment.getId())) {
            throw new IllegalArgumentException("Đề đánh giá này đã tồn tại trong Unit.");
        }
        contentRefRepository.save(CourseUnitContentRef.builder()
                .courseUnit(unit)
                .contentType(CourseUnitContentType.ASSESSMENT)
                .contentBankItem(assessment)
                .sequenceNumber(defaultInt(request.getDisplayOrder()))
                .build());
        return toUnitResponse(findUnit(unitId));
    }

    /**
     * Attaches a flashcard set from the Content Bank to a course unit.
     */
    @Override
    public CourseUnitResponse attachFlashcard(Long unitId, CourseUnitContentRefRequest request) {
        CourseUnit unit = findUnit(unitId);
        Long resolvedId = request.getResourceId();
        ContentBankItem flashcardSet = contentBankItemRepository.findByIdAndBankType(resolvedId, ContentBankType.FLASHCARD)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bộ flashcard."));
        requirePublishedResource(flashcardSet.getStatus(), "Bộ flashcard");
        if (contentRefRepository.existsByCourseUnitIdAndContentTypeAndContentBankItemId(
                unitId, CourseUnitContentType.FLASHCARD, flashcardSet.getId())) {
            throw new IllegalArgumentException("Bộ flashcard này đã tồn tại trong Unit.");
        }
        contentRefRepository.save(CourseUnitContentRef.builder()
                .courseUnit(unit)
                .contentType(CourseUnitContentType.FLASHCARD)
                .contentBankItem(flashcardSet)
                .sequenceNumber(defaultInt(request.getDisplayOrder()))
                .build());
        return toUnitResponse(findUnit(unitId));
    }

    /** Detaches a unit resource by reference type and reference ID. */
    @Override
    public void detachReference(String type, Long referenceId) {
        CourseUnitContentRef ref = contentRefRepository.findById(referenceId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy liên kết nội dung."));
        if (!ref.getContentType().name().equals(normalizeRefType(type))) {
            throw new RuntimeException("Loại tài nguyên không khớp với liên kết.");
        }
        contentRefRepository.delete(ref);
    }

    /** Lists assessment-bank items by skill and assessment type. */
    @Override
    @Transactional(readOnly = true)
    public List<AssessmentBankItemResponse> listAssessmentBank(AssessmentSkill skill, AssessmentType type) {
        List<AssessmentBankItem> items;
        if (skill != null) {
            items = assessmentBankRepository.findBySkillOrderByUpdatedAtDescIdDesc(skill);
        } else if (type != null) {
            items = assessmentBankRepository.findByTypeOrderByUpdatedAtDescIdDesc(type);
        } else {
            items = assessmentBankRepository.findAllByOrderByUpdatedAtDescIdDesc();
        }
        return items.stream().map(this::toAssessmentResponse).toList();
    }

    /** Filters and paginates assessment-bank items for content management. */
    @Override
    @Transactional(readOnly = true)
    public Page<AssessmentBankItemResponse> pageAssessmentBank(
            AssessmentSkill skill,
            AssessmentType type,
            String status,
            String keyword,
            String examCategory,
            Pageable pageable
    ) {
        String normalizedStatus = StringUtils.hasText(status)
                ? status.trim().toUpperCase(Locale.ROOT)
                : "";
        String normalizedKeyword = StringUtils.hasText(keyword)
                ? "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%"
                : "";
        String normalizedExamCategory = StringUtils.hasText(examCategory)
                ? examCategory.trim().toUpperCase(Locale.ROOT)
                : "";
        return assessmentBankRepository.searchPage(
                        skill == null ? "" : skill.name(),
                        type == null ? "" : type.name(),
                        normalizedStatus,
                        normalizedKeyword,
                        normalizedExamCategory,
                        PageRequest.of(pageable.getPageNumber(), pageable.getPageSize())
                )
                .map(this::toAssessmentResponse);
    }

    /** Aggregates assessment counts by status after applying the current filters. */
    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getAssessmentBankStats(AssessmentSkill skill, AssessmentType type) {
        List<AssessmentBankItem> items = assessmentBankRepository.findAll().stream()
                .filter(item -> skill == null || item.getSkill() == skill)
                .filter(item -> type == null || item.getType() == type)
                .toList();
        return Map.of(
                "total", (long) items.size(),
                "published", items.stream().filter(item -> "PUBLISHED".equalsIgnoreCase(item.getStatus())).count(),
                "draft", items.stream().filter(item -> "DRAFT".equalsIgnoreCase(item.getStatus())).count(),
                "timed", items.stream().filter(item -> item.getTimeLimitMinutes() != null && item.getTimeLimitMinutes() > 0).count()
        );
    }

    /** Returns one assessment-bank item by ID. */
    @Override
    @Transactional(readOnly = true)
    public AssessmentBankItemResponse getAssessmentBankItem(Long id) {
        return toAssessmentResponse(findAssessment(id));
    }

    /** Lists published mock tests available to learners. */
    @Override
    @Transactional(readOnly = true)
    public List<AssessmentBankItemResponse> listPublishedMockTests() {
        return assessmentBankRepository
                .findByTypeAndStatusOrderByUpdatedAtDescIdDesc(
                        AssessmentType.MOCK_TEST,
                        "PUBLISHED"
                )
                .stream()
                .map(this::toAssessmentResponse)
                .toList();
    }

    /** Returns a published mock test and rejects non-public content. */
    @Override
    @Transactional(readOnly = true)
    public AssessmentBankItemResponse getPublishedMockTest(Long id) {
        AssessmentBankItem item = assessmentBankRepository
                .findByIdAndTypeAndStatus(id, AssessmentType.MOCK_TEST, "PUBLISHED")
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đề thi thử đã xuất bản."));
        return toAssessmentResponse(item);
    }

    /** Validates and creates an assessment-bank item. */
    @Override
    public AssessmentBankItemResponse createAssessmentBankItem(AssessmentBankItemRequest request) {
        validateAssessmentBankRequest(request);
        AssessmentRubric rubric = resolveAssessmentRubric(request.getRubricId(), request.getSkill());
        AssessmentBankItem item = AssessmentBankItem.builder()
                .title(requireText(request.getTitle(), "Tên đề không được để trống."))
                .description(trimOrNull(request.getDescription()))
                .type(request.getType())
                .skill(request.getSkill())
                .aiEvaluationMode(resolveAiEvaluationMode(request))
                .rubric(rubric)
                .instructions(trimOrNull(request.getInstructions()))
                .objectiveAnswerKey(trimOrNull(request.getObjectiveAnswerKey()))
                .uiConfigJson(trimOrNull(request.getUiConfigJson()))
                .passingScore(request.getPassingScore())
                .maxScore(request.getMaxScore() == null ? BigDecimal.TEN : request.getMaxScore())
                .timeLimitMinutes(request.getTimeLimitMinutes())
                .status(defaultText(request.getStatus(), "DRAFT").toUpperCase(Locale.ROOT))
                .build();
        return toAssessmentResponse(assessmentBankRepository.save(item));
    }

    /** Updates an assessment item, its grading configuration, and rubric. */
    @Override
    public AssessmentBankItemResponse updateAssessmentBankItem(Long id, AssessmentBankItemRequest request) {
        validateAssessmentBankRequest(request);
        AssessmentBankItem item = findAssessment(id);
        AssessmentRubric rubric = resolveAssessmentRubric(request.getRubricId(), request.getSkill());
        item.setTitle(requireText(request.getTitle(), "Tên đề không được để trống."));
        item.setDescription(trimOrNull(request.getDescription()));
        item.setType(request.getType());
        item.setSkill(request.getSkill());
        item.setAiEvaluationMode(resolveAiEvaluationMode(request));
        item.setRubric(rubric);
        item.setInstructions(trimOrNull(request.getInstructions()));
        item.setObjectiveAnswerKey(trimOrNull(request.getObjectiveAnswerKey()));
        item.setUiConfigJson(trimOrNull(request.getUiConfigJson()));
        item.setPassingScore(request.getPassingScore());
        item.setMaxScore(request.getMaxScore() == null ? BigDecimal.TEN : request.getMaxScore());
        item.setTimeLimitMinutes(request.getTimeLimitMinutes());
        item.setStatus(defaultText(request.getStatus(), "DRAFT").toUpperCase(Locale.ROOT));
        return toAssessmentResponse(assessmentBankRepository.save(item));
    }

    /** Archives an assessment item instead of deleting it physically. */
    @Override
    public void archiveAssessmentBankItem(Long id) {
        AssessmentBankItem item = findAssessment(id);
        item.setStatus("ARCHIVED");
        assessmentBankRepository.save(item);
    }

    /** Lists flashcard sets by most recent update. */
    @Override
    @Transactional(readOnly = true)
    public List<FlashcardSetResponse> listFlashcardSets() {
        return flashcardSetRepository.findAllByOrderByUpdatedAtDescIdDesc()
                .stream()
                .map(this::toFlashcardSetResponse)
                .toList();
    }

    /** Filters and paginates flashcard sets by keyword, exam, skill, and status. */
    @Override
    @Transactional(readOnly = true)
    public Page<FlashcardSetResponse> pageFlashcardSets(
            String keyword,
            String examCategory,
            String skill,
            String status,
            Pageable pageable
    ) {
        Specification<FlashcardSet> specification = (root, query, criteriaBuilder) ->
                criteriaBuilder.conjunction();
        if (StringUtils.hasText(keyword)) {
            String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, query, criteriaBuilder) -> criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("tags")), pattern)
            ));
        }
        if (StringUtils.hasText(examCategory)) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("examCategory"), examCategory.trim().toUpperCase(Locale.ROOT)));
        }
        if (StringUtils.hasText(skill)) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("skill"), skill.trim().toUpperCase(Locale.ROOT)));
        }
        if (StringUtils.hasText(status)) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("status"), status.trim().toUpperCase(Locale.ROOT)));
        }
        return flashcardSetRepository.findAll(specification, pageable).map(this::toFlashcardSetResponse);
    }

    /** Aggregates set and card counts after applying the current filters. */
    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getFlashcardSetStats(String examCategory, String skill) {
        List<FlashcardSet> sets = flashcardSetRepository.findAll().stream()
                .filter(set -> !StringUtils.hasText(examCategory)
                        || examCategory.equalsIgnoreCase(set.getExamCategory()))
                .filter(set -> !StringUtils.hasText(skill) || skill.equalsIgnoreCase(set.getSkill()))
                .toList();
        return Map.of(
                "total", (long) sets.size(),
                "published", sets.stream().filter(set -> "PUBLISHED".equalsIgnoreCase(set.getStatus())).count(),
                "draft", sets.stream().filter(set -> "DRAFT".equalsIgnoreCase(set.getStatus())).count(),
                "cards", sets.stream().mapToLong(set -> countFlashcards(set.getCardsJson())).sum()
        );
    }

    /** Counts cards from JSON and returns zero when legacy data cannot be parsed. */
    private long countFlashcards(String cardsJson) {
        if (!StringUtils.hasText(cardsJson)) {
            return 0;
        }
        try {
            JsonNode root = objectMapper.readTree(cardsJson);
            return root.isArray() ? root.size() : 0;
        } catch (Exception ignored) {
            return 0;
        }
    }

    /** Returns one flashcard set by ID. */
    @Override
    @Transactional(readOnly = true)
    public FlashcardSetResponse getFlashcardSet(Long id) {
        return toFlashcardSetResponse(findFlashcardSet(id));
    }

    /** Creates a flashcard set after normalizing its input. */
    @Override
    public FlashcardSetResponse createFlashcardSet(FlashcardSetRequest request) {
        FlashcardSet set = FlashcardSet.builder()
                .title(requireText(request.getTitle(), "Tên bộ flashcard không được để trống."))
                .description(trimOrNull(request.getDescription()))
                .examCategory(defaultText(request.getExamCategory(), "IELTS").toUpperCase(Locale.ROOT))
                .skill(trimUpperOrNull(request.getSkill()))
                .tags(trimOrNull(request.getTags()))
                .cardsJson(trimOrNull(request.getCardsJson()))
                .status(defaultText(request.getStatus(), "DRAFT").toUpperCase(Locale.ROOT))
                .build();
        return toFlashcardSetResponse(flashcardSetRepository.save(set));
    }

    /** Updates flashcard content and classification metadata. */
    @Override
    public FlashcardSetResponse updateFlashcardSet(Long id, FlashcardSetRequest request) {
        FlashcardSet set = findFlashcardSet(id);
        set.setTitle(requireText(request.getTitle(), "Tên bộ flashcard không được để trống."));
        set.setDescription(trimOrNull(request.getDescription()));
        set.setExamCategory(defaultText(request.getExamCategory(), "IELTS").toUpperCase(Locale.ROOT));
        set.setSkill(trimUpperOrNull(request.getSkill()));
        set.setTags(trimOrNull(request.getTags()));
        set.setCardsJson(trimOrNull(request.getCardsJson()));
        set.setStatus(defaultText(request.getStatus(), "DRAFT").toUpperCase(Locale.ROOT));
        return toFlashcardSetResponse(flashcardSetRepository.save(set));
    }

    /** Archives a flashcard set instead of deleting it physically. */
    @Override
    public void archiveFlashcardSet(Long id) {
        FlashcardSet set = findFlashcardSet(id);
        set.setStatus("ARCHIVED");
        flashcardSetRepository.save(set);
    }

    /** Synchronizes derived values before persisting a course. */
    private InstructorLedCourse saveAndSyncCourse(InstructorLedCourse course) {
        InstructorLedCourse saved = instructorLedCourseRepository.save(course);
        return saved;
    }

    /** Finds a course or raises a domain-friendly error. */
    private InstructorLedCourse findInstructorLedCourse(Long id) {
        return instructorLedCourseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học có giảng viên."));
    }

    /** Finds a unit by ID or stops when it does not exist. */
    private CourseUnit findUnit(Long id) {
        return unitRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Unit trong khóa học."));
    }

    /** Finds a lesson by ID or stops when it does not exist. */
    private CourseLesson findCourseLesson(Long id) {
        return courseLessonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài học trong khóa học."));
    }

    /** Finds an assessment-bank item by ID. */
    private AssessmentBankItem findAssessment(Long id) {
        return assessmentBankRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đề trong ngân hàng."));
    }

    /** Finds a flashcard set by ID. */
    private FlashcardSet findFlashcardSet(Long id) {
        return flashcardSetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bộ flashcard."));
    }

    /** Maps a course to a response and includes units only for detail views. */
    private InstructorLedCourseResponse toInstructorLedCourseResponse(InstructorLedCourse course, boolean includeUnits) {
        return InstructorLedCourseResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .code(course.getCode())
                .shortDescription(course.getShortDescription())
                .description(course.getDescription())
                .durationLabel(course.getDurationLabel())
                .baseTuitionFeeVnd(course.getBaseTuitionFeeVnd())
                .saleTuitionFeeVnd(course.getSaleTuitionFeeVnd())
                .examCategory(course.getExamType())
                .focusSkills(course.getFocusSkills())
                .targetBand(course.getTargetBand())
                .targetScore(course.getTargetScore())
                .entryLevel(course.getEntryLevel())
                .outcomes(course.getLearningOutcomes())
                .teacherGuide(course.getTeacherGuide())
                .totalSessions(resolveTotalSessions(course))
                .totalLessons(resolveTotalLessons(course))
                .totalUnits(course.getUnits() == null ? 0 : course.getUnits().size())
                .status(course.getPublicationStatus().name())
                .statusLabel(courseStatusLabel(course.getPublicationStatus().name()))
                .reviewNote(course.getReviewNote())
                .submittedByName(course.getCreatedBy() == null ? null : course.getCreatedBy().getFullName())
                .submittedAt(course.getSubmittedAt())
                .reviewedByName(course.getReviewedBy() == null ? null : course.getReviewedBy().getFullName())
                .reviewedAt(course.getReviewedAt())
                .classroomUsageCount(0)
                .activeClassroomCount(0)
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .units(includeUnits ? (course.getUnits() == null ? List.of() : course.getUnits().stream().map(this::toUnitResponse).toList()) : null)
                .usingClassrooms(includeUnits ? toClassroomUsages(course) : null)
                .build();
    }

    /** Describes classrooms using the course for archive-safety checks. */
    private List<InstructorLedCourseResponse.ClassroomUsage> toClassroomUsages(InstructorLedCourse course) {
        return List.of();
    }

    /** Validates the English profile, lesson structure, and assessments before publication. */
    private void validateReadyForPublish(InstructorLedCourse course) {
        validateEnglishProfile(
                course.getExamType(),
                course.getFocusSkills(),
                course.getTargetBand(),
                course.getTargetScore(),
                course.getEntryLevel()
        );
        if (!StringUtils.hasText(course.getLearningOutcomes())) {
            throw new RuntimeException("Khóa học phải mô tả chuẩn đầu ra tiếng Anh trước khi xuất bản.");
        }
        if (course.getUnits() == null || course.getUnits().isEmpty()) {
            throw new RuntimeException("Khóa học chưa có Unit nào. Hãy thêm nội dung trước khi xuất bản.");
        }
        validateStructuredLessons(course);
        boolean hasUnpublishedMaterial = course.getUnits().stream()
                .flatMap(unit -> unit.getContentRefs().stream())
                .filter(ref -> ref.getContentType() == CourseUnitContentType.MATERIAL)
                .map(CourseUnitContentRef::getLearningResource)
                .filter(java.util.Objects::nonNull)
                .anyMatch(material -> !"PUBLISHED".equalsIgnoreCase(material.getStatus()));
        if (hasUnpublishedMaterial) {
            throw new RuntimeException("Khóa học chỉ được sử dụng học liệu trung tâm đã xuất bản.");
        }
        validateFocusedSkillAssessments(course);
    }

    /** Ensures unit and lesson ordering is valid and contains no duplicates. */
    private void validateStructuredLessons(InstructorLedCourse course) {
        List<CourseLesson> courseLessons = course.getUnits().stream()
                .flatMap(unit -> unit.getLessons().stream())
                .sorted(Comparator.comparing(CourseLesson::getSequenceNumber))
                .toList();
        if (courseLessons.isEmpty()) {
            throw new RuntimeException("Khóa học chưa có bài học. Hãy cập nhật trước khi xuất bản.");
        }

        course.getUnits().stream()
                .filter(unit -> unit.getLessons().isEmpty())
                .findFirst()
                .ifPresent(unit -> {
                    throw new IllegalArgumentException(
                            "Unit “" + unit.getTitle() + "” chưa có buổi học. Hãy thêm ít nhất một buổi trước khi xuất bản."
                    );
                });

        Set<Integer> uniqueNumbers = new LinkedHashSet<>();
        for (CourseLesson courseLesson : courseLessons) {
            if (!uniqueNumbers.add(courseLesson.getSequenceNumber())) {
                throw new IllegalArgumentException(
                        "Bài học " + courseLesson.getSequenceNumber() + " đang bị trùng trong khóa học."
                );
            }
        }

        for (int index = 0; index < courseLessons.size(); index++) {
            int expectedNumber = index + 1;
            if (!Integer.valueOf(expectedNumber).equals(courseLessons.get(index).getSequenceNumber())) {
                String currentNumbers = courseLessons.stream()
                        .map(CourseLesson::getSequenceNumber)
                        .map(String::valueOf)
                        .collect(java.util.stream.Collectors.joining(", "));
                throw new IllegalArgumentException(
                        "Thứ tự bài học chưa liên tục. Khóa học hiện có bài "
                                + currentNumbers + " nhưng thiếu buổi " + expectedNumber + "."
                );
            }
        }

    }

    /** Validates required lesson fields and numeric limits. */
    private void validateCourseLessonRequest(CourseLessonRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Dữ liệu bài học không được để trống.");
        }
        if (request.getSessionNumber() == null || request.getSessionNumber() < 1) {
            throw new IllegalArgumentException("Thứ tự bài học phải bắt đầu từ 1.");
        }
        if (request.getPlannedSessionCount() != null && request.getPlannedSessionCount() < 1) {
            throw new IllegalArgumentException("Số buổi dự kiến phải từ 1 trở lên.");
        }
        if (request.getDisplayOrder() != null && request.getDisplayOrder() < 0) {
            throw new IllegalArgumentException("Thứ tự hiển thị không được âm.");
        }
        requireText(request.getTitle(), "Tiêu đề bài học không được để trống.");
    }

    /** Prevents lessons in the same course from sharing a session number. */
    private void assertSessionNumberAvailable(Long instructorLedCourseId, Integer sessionNumber, Long excludeId) {
        if (courseLessonRepository.existsDuplicateSequenceNumber(instructorLedCourseId, sessionNumber, excludeId)) {
            throw new IllegalArgumentException(
                    "Bài học số " + sessionNumber + " đã tồn tại trong khóa học."
            );
        }
    }

    /** Keeps the synchronization hook even though totals are currently derived from lessons. */
    private void synchronizeTotalSessions(InstructorLedCourse course) {
        // Session count is derived from CourseLesson and is not persisted on the course aggregate.
    }

    /** Sums planned sessions across every lesson in the course. */
    private int resolveTotalSessions(InstructorLedCourse course) {
        if (course != null && course.getUnits() != null) {
            return course.getUnits().stream()
                    .filter(unit -> unit.getLessons() != null)
                    .flatMap(unit -> unit.getLessons().stream())
                    .mapToInt(lesson -> lesson.getPlannedSessionCount() == null || lesson.getPlannedSessionCount() < 1 ? 1 : lesson.getPlannedSessionCount())
                    .sum();
        }
        return 0;
    }

    /** Counts all lessons across the course units. */
    private int resolveTotalLessons(InstructorLedCourse course) {
        if (course != null && course.getUnits() != null) {
            return course.getUnits().stream()
                    .filter(unit -> unit.getLessons() != null)
                    .mapToInt(unit -> unit.getLessons().size())
                    .sum();
        }
        return 0;
    }

    /**
     * Applies and validates standardized English exam profile (IELTS, TOEIC, General English).
     */
    private void applyEnglishProfile(InstructorLedCourse course, InstructorLedCourseRequest request) {
        String examCategory = normalizeExamCategory(request.getExamCategory());
        String focusSkills = normalizeFocusSkills(request.getFocusSkills());
        validateEnglishProfile(
                examCategory,
                focusSkills,
                request.getTargetBand(),
                request.getTargetScore(),
                request.getEntryLevel()
        );
        course.setExamType(examCategory);
        course.setFocusSkills(focusSkills);
        course.setTargetBand(request.getTargetBand());
        course.setTargetScore(request.getTargetScore());
        course.setEntryLevel(request.getEntryLevel().trim());
    }

    /** Normalizes and validates the supported exam category. */
    private String normalizeExamCategory(String value) {
        String normalized = defaultText(value, "IELTS").trim().toUpperCase(Locale.ROOT);
        if ("GENERAL".equals(normalized) || "COMMUNICATION".equals(normalized) || "FOUNDATION".equals(normalized)) {
            normalized = "GENERAL_ENGLISH";
        }
        if (!EXAM_CATEGORIES.contains(normalized)) {
            throw new IllegalArgumentException("Khóa học chỉ được thuộc IELTS, TOEIC hoặc General English.");
        }
        return normalized;
    }

    /** Normalizes focus skills, removes duplicates, and preserves domain ordering. */
    private String normalizeFocusSkills(String value) {
        LinkedHashSet<String> selected = new LinkedHashSet<>();
        if (StringUtils.hasText(value)) {
            Arrays.stream(value.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .map(skill -> skill.toUpperCase(Locale.ROOT))
                    .forEach(skill -> {
                        if (!SKILL_ORDER.contains(skill)) {
                            throw new IllegalArgumentException("Kỹ năng “" + skill + "” không thuộc phạm vi đào tạo tiếng Anh.");
                        }
                        selected.add(skill);
                    });
        }
        return SKILL_ORDER.stream().filter(selected::contains).collect(java.util.stream.Collectors.joining(","));
    }

    /** Validates entry and target levels against the selected exam scale. */
    private void validateEnglishProfile(
            String examCategory,
            String focusSkills,
            BigDecimal targetBand,
            Integer targetScore,
            String entryLevel
    ) {
        if (!StringUtils.hasText(entryLevel)) {
            throw new IllegalArgumentException("Hãy khai báo trình độ đầu vào của khóa học.");
        }
        if (!StringUtils.hasText(focusSkills)) {
            throw new IllegalArgumentException("Hãy chọn ít nhất một kỹ năng tiếng Anh trọng tâm.");
        }
        if ("IELTS".equals(examCategory)) {
            BigDecimal entryBand = parseBand(entryLevel, "Band IELTS đầu vào");
            if (targetBand == null) {
                throw new IllegalArgumentException("Khóa học IELTS phải có band mục tiêu.");
            }
            validateBand(targetBand, "Band IELTS mục tiêu");
            if (entryBand.compareTo(targetBand) > 0) {
                throw new IllegalArgumentException("Band IELTS đầu vào không thể cao hơn band mục tiêu.");
            }
            if (targetScore != null) {
                throw new IllegalArgumentException("Khóa học IELTS không sử dụng thang điểm TOEIC.");
            }
            return;
        }
        if ("TOEIC".equals(examCategory)) {
            int entryScore = parseToeicScore(entryLevel, "Điểm TOEIC đầu vào");
            if (targetScore == null || targetScore < 10 || targetScore > 990 || targetScore % 5 != 0) {
                throw new IllegalArgumentException("Điểm mục tiêu TOEIC phải từ 10 đến 990 và tăng theo bước 5.");
            }
            if (entryScore > targetScore) {
                throw new IllegalArgumentException("Điểm TOEIC đầu vào không thể cao hơn điểm mục tiêu.");
            }
            if (targetBand != null) {
                throw new IllegalArgumentException("Khóa học TOEIC không sử dụng band IELTS.");
            }
            return;
        }
        if (!CEFR_LEVELS.contains(entryLevel.trim().toUpperCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Trình độ đầu vào General English phải theo CEFR từ A1 đến C2.");
        }
        if (targetBand != null || targetScore != null) {
            throw new IllegalArgumentException("General English dùng chuẩn đầu ra mô tả, không dùng band IELTS hoặc điểm TOEIC.");
        }
    }

    /** Parses an IELTS band and validates its range immediately. */
    private BigDecimal parseBand(String value, String label) {
        try {
            BigDecimal band = new BigDecimal(value.trim());
            validateBand(band, label);
            return band;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(label + " phải là một band hợp lệ.");
        }
    }

    /** Ensures an IELTS band is between 0 and 9 in 0.5 increments. */
    private void validateBand(BigDecimal band, String label) {
        if (band.compareTo(BigDecimal.ZERO) < 0
                || band.compareTo(BigDecimal.valueOf(9)) > 0
                || band.multiply(BigDecimal.valueOf(2)).stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException(label + " phải từ 0 đến 9 và tăng theo bước 0.5.");
        }
    }

    /** Parses a TOEIC score between 10 and 990 in increments of 5. */
    private int parseToeicScore(String value, String label) {
        try {
            int score = Integer.parseInt(value.trim());
            if (score < 10 || score > 990 || score % 5 != 0) {
                throw new IllegalArgumentException(label + " phải từ 10 đến 990 và tăng theo bước 5.");
            }
            return score;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(label + " phải là một số nguyên hợp lệ.");
        }
    }

    /** Converts an exam code into a user-friendly label. */
    private String examCategoryLabel(String examCategory) {
        return "GENERAL_ENGLISH".equals(examCategory) ? "General English" : examCategory;
    }

    /** Ensures IELTS and TOEIC courses assess every configured focus skill. */
    private void validateFocusedSkillAssessments(InstructorLedCourse course) {
        if ("GENERAL_ENGLISH".equals(course.getExamType())) {
            return;
        }
        Set<AssessmentSkill> testedSkills = course.getUnits().stream()
                .flatMap(unit -> unit.getContentRefs().stream())
                .filter(ref -> ref.getContentType() == CourseUnitContentType.ASSESSMENT)
                .map(CourseUnitContentRef::getContentBankItem)
                .filter(java.util.Objects::nonNull)
                .map(ContentBankItem::getSkill)
                .filter(java.util.Objects::nonNull)
                .map(String::toUpperCase)
                .map(AssessmentSkill::valueOf)
                .collect(java.util.stream.Collectors.toSet());
        for (String skill : course.getFocusSkills().split(",")) {
            try {
                AssessmentSkill assessmentSkill = AssessmentSkill.valueOf(skill);
                if (!testedSkills.contains(assessmentSkill) && !testedSkills.contains(AssessmentSkill.MIXED)) {
                    throw new IllegalArgumentException(
                            "Khóa học chưa có bài đánh giá cho kỹ năng " + skillLabel(assessmentSkill) + "."
                    );
                }
            } catch (IllegalArgumentException exception) {
                if (exception.getMessage() != null && exception.getMessage().startsWith("Khóa học")) {
                    throw exception;
                }
            }
        }
    }

    /** Converts a skill enum into a label used in validation messages. */
    private String skillLabel(AssessmentSkill skill) {
        return switch (skill) {
            case LISTENING -> "Listening";
            case READING -> "Reading";
            case WRITING -> "Writing";
            case SPEAKING -> "Speaking";
            case VOCABULARY -> "Vocabulary";
            case GRAMMAR -> "Grammar";
            case MIXED -> "tổng hợp";
        };
    }

    /** Converts a publication status into its Vietnamese UI label. */
    private String courseStatusLabel(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        return switch (status) {
            case "DRAFT" -> "Bản nháp";
            case "PENDING_REVIEW" -> "Sẵn sàng xuất bản";
            case "PUBLISHED" -> "Đã xuất bản";
            case "REJECTED" -> "Bị từ chối";
            case "ARCHIVED" -> "Đã lưu trữ";
            default -> status;
        };
    }

    /** Normalizes legacy status values into the current publication enum. */
    private PackageStatus parsePublicationStatus(String status) {
        String normalized = defaultText(status, "DRAFT").trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "APPROVED" -> PackageStatus.PUBLISHED;
            case "PENDING_APPROVAL" -> PackageStatus.PENDING_REVIEW;
            default -> PackageStatus.valueOf(normalized);
        };
    }

    /** Converts a classroom offering status into its Vietnamese UI label. */
    private String offeringStatusLabel(ClassroomOfferingStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case DRAFT -> "Bản nháp";
            case UPCOMING -> "Sắp khai giảng";
            case ACTIVE -> "Đang diễn ra";
            case COMPLETED -> "Đã kết thúc";
            case CANCELLED -> "Đã hủy";
            case CLOSED -> "Đã đóng";
        };
    }

    /** Integration point for classroom usage checks; currently returns zero until a query is connected. */
    private long countActiveClassrooms(InstructorLedCourse course) {
        return 0L;
    }

    /**
     * Resolves course code from request or generates a unique slug based on title.
     */
    private String resolveNewCourseCode(InstructorLedCourseRequest request) {
        if (StringUtils.hasText(request.getCode())) {
            String requestedCode = normalizeCourseCode(request.getCode());
            if (instructorLedCourseRepository.existsByCodeIgnoreCase(requestedCode)) {
                throw new RuntimeException("Mã khóa học có giảng viên đã tồn tại.");
            }
            return requestedCode;
        }
        return uniqueCode(makeCourseCode(request.getTitle()));
    }

    /**
     * Generates a slugified uppercase code prefixed with ILC-.
     */
    private String makeCourseCode(String title) {
        String normalizedTitle = Normalizer.normalize(defaultText(title, "CURRICULUM"), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .replaceAll("[^A-Za-z0-9]+", "-")
                .replaceAll("^-+|-+$", "")
                .toUpperCase(Locale.ROOT);
        return normalizeCourseCode("ILC-" + defaultText(normalizedTitle, "COURSE"));
    }

    /**
     * Trims and normalizes course code within length limits.
     */
    private String normalizeCourseCode(String sourceCode) {
        String normalized = sourceCode.trim().toUpperCase(Locale.ROOT);
        return normalized.length() <= COURSE_CODE_MAX_LENGTH
                ? normalized
                : normalized.substring(0, COURSE_CODE_MAX_LENGTH);
    }

    /** Creates a unique copy code from the source course code. */
    private String uniqueCourseCode(String sourceCode) {
        return uniqueCode(sourceCode + "-COPY");
    }

    /**
     * Ensures code uniqueness by appending incremental numerical suffixes if duplicated.
     */
    private String uniqueCode(String sourceCode) {
        String base = normalizeCourseCode(sourceCode);
        String code = base;
        int index = 2;
        while (instructorLedCourseRepository.existsByCodeIgnoreCase(code)) {
            String suffix = "-" + index++;
            int baseLength = Math.min(base.length(), COURSE_CODE_MAX_LENGTH - suffix.length());
            code = base.substring(0, baseLength) + suffix;
        }
        return code;
    }

    /** Maps a unit with its lessons and four resource groups for the editor. */
    private CourseUnitResponse toUnitResponse(CourseUnit unit) {
        List<CourseUnitContentRef> refs = unit.getContentRefs() == null ? List.of() : unit.getContentRefs();
        return CourseUnitResponse.builder()
                .id(unit.getId())
                .programId(unit.getInstructorLedCourse().getId())
                .instructorLedCourseId(unit.getInstructorLedCourse().getId())
                .displayOrder(unit.getSequenceNumber())
                .title(unit.getTitle())
                .description(unit.getDescription())
                .sessionPlan(unit.getLearningObjectives())
                .lessons(unit.getLessons().stream()
                        .sorted(Comparator.comparing(CourseLesson::getSequenceNumber)
                                .thenComparing(CourseLesson::getId, Comparator.nullsLast(Long::compareTo)))
                        .map(this::toCourseLessonResponse)
                        .toList())
                .createdAt(unit.getCreatedAt())
                .updatedAt(unit.getUpdatedAt())
                .materials(toContentRefResponses(refs, CourseUnitContentType.MATERIAL))
                .exercises(toContentRefResponses(refs, CourseUnitContentType.EXERCISE))
                .assessments(toContentRefResponses(refs, CourseUnitContentType.ASSESSMENT))
                .flashcards(toContentRefResponses(refs, CourseUnitContentType.FLASHCARD))
                .build();
    }

    /** Maps a lesson and includes its unit and course context. */
    private CourseLessonResponse toCourseLessonResponse(CourseLesson lesson) {
        CourseUnit unit = lesson.getCourseUnit();
        return CourseLessonResponse.builder()
                .id(lesson.getId())
                .unitId(unit.getId())
                .unitTitle(unit.getTitle())
                .programId(unit.getInstructorLedCourse().getId())
                .instructorLedCourseId(unit.getInstructorLedCourse().getId())
                .sessionNumber(lesson.getSequenceNumber())
                .displayOrder(lesson.getSequenceNumber())
                .plannedSessionCount(lesson.getPlannedSessionCount() == null || lesson.getPlannedSessionCount() < 1 ? 1 : lesson.getPlannedSessionCount())
                .title(lesson.getTitle())
                .description(lesson.getDescription())
                .learningObjectives(lesson.getLearningObjectives())
                .createdAt(lesson.getCreatedAt())
                .updatedAt(lesson.getUpdatedAt())
                .build();
    }

    /** Filters unit references by type and maps them to responses. */
    private List<CourseUnitContentRefResponse> toContentRefResponses(
            List<CourseUnitContentRef> refs,
            CourseUnitContentType type
    ) {
        return refs.stream()
                .filter(ref -> ref.getContentType() == type)
                .map(this::toContentRefResponse)
                .toList();
    }

    /** Normalizes materials and bank items into one resource-reference response. */
    private CourseUnitContentRefResponse toContentRefResponse(CourseUnitContentRef ref) {
        CenterMaterialLibraryItem material = ref.getLearningResource();
        ContentBankItem item = ref.getContentBankItem();
        return CourseUnitContentRefResponse.builder()
                .id(ref.getId())
                .type(ref.getContentType().name())
                .resourceId(material != null ? material.getId() : item == null ? null : item.getId())
                .title(material != null ? material.getTitle() : item == null ? null : item.getTitle())
                .subtitle(material != null ? material.getMaterialType() : item == null ? null : item.getExamCategory())
                .skill(material != null ? material.getSkill() : item == null ? null : item.getSkill())
                .status(material != null ? material.getStatus() : item == null ? null : item.getStatus())
                .fileUrl(material == null ? null : material.getFileUrl())
                .displayOrder(ref.getSequenceNumber())
                .contentJson(item == null ? null : toJson(item.getContentData()))
                .build();
    }

    /** Maps an assessment-bank item and its grading configuration. */
    private AssessmentBankItemResponse toAssessmentResponse(AssessmentBankItem item) {
        return AssessmentBankItemResponse.builder()
                .id(item.getId())
                .title(item.getTitle())
                .description(item.getDescription())
                .type(item.getType())
                .skill(item.getSkill())
                .aiEvaluationMode(item.getAiEvaluationMode())
                .rubric(toRubricResponse(item.getRubric()))
                .instructions(item.getInstructions())
                .objectiveAnswerKey(item.getObjectiveAnswerKey())
                .uiConfigJson(item.getUiConfigJson())
                .passingScore(item.getPassingScore())
                .maxScore(item.getMaxScore())
                .timeLimitMinutes(item.getTimeLimitMinutes())
                .status(item.getStatus())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }

    /** Maps a rubric and its criteria, allowing assessments without a rubric. */
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
                .status(rubric.getStatus())
                .criteria(rubric.getCriteria().stream()
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

    /** Maps a flashcard-set entity to its management response. */
    private FlashcardSetResponse toFlashcardSetResponse(FlashcardSet set) {
        return FlashcardSetResponse.builder()
                .id(set.getId())
                .title(set.getTitle())
                .description(set.getDescription())
                .examCategory(set.getExamCategory())
                .skill(set.getSkill())
                .tags(set.getTags())
                .cardsJson(set.getCardsJson())
                .status(set.getStatus())
                .createdAt(set.getCreatedAt())
                .updatedAt(set.getUpdatedAt())
                .build();
    }

    /** Validates answers, test configuration, and skill-specific AI grading requirements. */
    private void validateAssessmentBankRequest(AssessmentBankItemRequest request) {
        if ((request.getSkill() == AssessmentSkill.LISTENING || request.getSkill() == AssessmentSkill.READING)
                && !StringUtils.hasText(request.getObjectiveAnswerKey())) {
            throw new RuntimeException("Đề Listening/Reading cần có đáp án khách quan.");
        }
        if ((request.getSkill() == AssessmentSkill.WRITING || request.getSkill() == AssessmentSkill.SPEAKING)
                && !StringUtils.hasText(request.getUiConfigJson())) {
            throw new RuntimeException("Đề Writing/Speaking cần có nội dung đề trong cấu hình.");
        }
        if (request.getSkill() == AssessmentSkill.WRITING || request.getSkill() == AssessmentSkill.SPEAKING) {
            if (request.getRubricId() == null) {
                throw new RuntimeException("Bài Writing/Speaking phải có bộ tiêu chí chấm.");
            }
        }
    }

    /** Derives grading behavior from skill so clients cannot persist incompatible modes. */
    private AiEvaluationMode resolveAiEvaluationMode(AssessmentBankItemRequest request) {
        if (request.getSkill() == AssessmentSkill.LISTENING
                || request.getSkill() == AssessmentSkill.READING
                || request.getSkill() == AssessmentSkill.WRITING
                || request.getSkill() == AssessmentSkill.SPEAKING) {
            return AiEvaluationMode.ESTIMATED_BAND;
        }
        return request.getAiEvaluationMode() == null
                ? AiEvaluationMode.EXPLAIN_ONLY
                : request.getAiEvaluationMode();
    }

    /** Resolves a published rubric and verifies that it matches the assessment skill. */
    private AssessmentRubric resolveAssessmentRubric(Long rubricId, AssessmentSkill skill) {
        if (rubricId == null) {
            return null;
        }
        AssessmentRubric rubric = assessmentRubricRepository.findById(rubricId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy rubric."));
        if (!"PUBLISHED".equalsIgnoreCase(rubric.getStatus())) {
            throw new RuntimeException("Bộ tiêu chí đã tạm ngưng.");
        }
        boolean requiresExactRubric = skill == AssessmentSkill.WRITING || skill == AssessmentSkill.SPEAKING;
        if (rubric.getSkill() != null
                && rubric.getSkill() != skill
                && (requiresExactRubric || rubric.getSkill() != AssessmentSkill.MIXED)) {
            throw new RuntimeException("Bộ tiêu chí không phù hợp với kỹ năng của nội dung.");
        }
        return rubric;
    }

    /** Normalizes a unit-reference type for detach routing. */
    private String normalizeRefType(String type) {
        return StringUtils.hasText(type) ? type.trim().toUpperCase(Locale.ROOT) : "";
    }

    /** Requires non-blank text and removes surrounding whitespace. */
    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new RuntimeException(message);
        }
        return value.trim();
    }

    /** Uses a fallback when the input text is blank. */
    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    /** Allows only published resources to be attached to a unit. */
    private void requirePublishedResource(String status, String resourceLabel) {
        if (!"PUBLISHED".equalsIgnoreCase(status)) {
            throw new IllegalArgumentException(resourceLabel + " phải được xuất bản trước khi gắn vào khóa học.");
        }
    }

    /** Trims optional text or normalizes it to null. */
    private String trimOrNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /** Serializes content configuration and converts JSON failures into domain errors. */
    private String toJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            throw new IllegalArgumentException("Không thể chuyển đổi cấu hình nội dung.", exception);
        }
    }

    /** Trims and uppercases optional text or returns null. */
    private String trimUpperOrNull(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : null;
    }

    /** Normalizes a nullable integer to zero. */
    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }
}
