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

@Service
@RequiredArgsConstructor
@Transactional
public class InstructorLedCourseManagementServiceImpl implements InstructorLedCourseManagementService {

    private static final int PROGRAM_CODE_MAX_LENGTH = 120;
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

    private final InstructorLedCourseRepository programRepository;
    private final CourseUnitRepository unitRepository;
    private final CourseLessonRepository sessionPlanRepository;
    private final CourseUnitContentRefRepository contentRefRepository;
    private final ContentBankItemRepository contentBankItemRepository;
    private final CenterMaterialLibraryItemRepository materialRepository;
    private final ExerciseBankItemRepository exerciseRepository;
    private final AssessmentRubricRepository assessmentRubricRepository;
    private final AssessmentBankItemRepository assessmentBankRepository;
    private final FlashcardSetRepository flashcardSetRepository;
    private final ClassroomAccessHelper accessHelper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(readOnly = true)
    public List<InstructorLedCourseResponse> listPrograms() {
        List<InstructorLedCourse> programs = programRepository.findAllByOrderByUpdatedAtDescIdDesc();
        return programs.stream().map(program -> toProgramResponse(program, false)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InstructorLedCourseResponse> pagePrograms(
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
        return programRepository.findAll(specification, pageable)
                .map(program -> toProgramResponse(program, false));
    }

    @Override
    @Transactional(readOnly = true)
    public InstructorLedCourseResponse getProgram(Long id) {
        return toProgramResponse(findProgram(id), true);
    }

    /**
     * Creates a new instructor-led course in DRAFT status with a unique course code.
     */
    @Override
    public InstructorLedCourseResponse createProgram(InstructorLedCourseRequest request) {
        String code = resolveNewProgramCode(request);
        InstructorLedCourse program = InstructorLedCourse.builder()
                .title(requireText(request.getTitle(), "Tên giáo trình không được để trống."))
                .code(code)
                .shortDescription(trimOrNull(request.getShortDescription()))
                .description(trimOrNull(request.getDescription()))
                .durationLabel(trimOrNull(request.getDurationLabel()))
                .level(trimOrNull(request.getLevel()))
                .baseTuitionFeeVnd(request.getBaseTuitionFeeVnd() != null ? request.getBaseTuitionFeeVnd() : BigDecimal.ZERO)
                .saleTuitionFeeVnd(request.getSaleTuitionFeeVnd())
                .learningOutcomes(trimOrNull(request.getOutcomes()))
                .teacherGuide(trimOrNull(request.getTeacherGuide()))
                .publicationStatus(parsePublicationStatus(request.getStatus()))
                .build();
        applyEnglishProfile(program, request);

        // Disallow publishing new courses without units and lessons
        if (program.getPublicationStatus() == PackageStatus.PUBLISHED) {
            throw new RuntimeException("Giáo trình mới tạo chưa có Unit và buổi học nên chưa thể xuất bản. Hãy lưu nháp trước.");
        }
        return toProgramResponse(saveAndSyncProgram(program), true);
    }

    /**
     * Updates an existing instructor-led course metadata and publication status.
     */
    @Override
    public InstructorLedCourseResponse updateProgram(Long id, InstructorLedCourseRequest request) {
        InstructorLedCourse program = findProgram(id);
        String code = StringUtils.hasText(request.getCode())
                ? normalizeProgramCode(request.getCode())
                : program.getCode();
        if (!program.getCode().equalsIgnoreCase(code) && programRepository.existsByCodeIgnoreCase(code)) {
            throw new RuntimeException("Mã giáo trình đã tồn tại.");
        }
        program.setTitle(requireText(request.getTitle(), "Tên giáo trình không được để trống."));
        program.setCode(code);
        program.setShortDescription(trimOrNull(request.getShortDescription())) ;
        program.setDescription(trimOrNull(request.getDescription()));
        program.setDurationLabel(trimOrNull(request.getDurationLabel()));
        program.setLevel(trimOrNull(request.getLevel()));
        if (request.getBaseTuitionFeeVnd() != null) {
            program.setBaseTuitionFeeVnd(request.getBaseTuitionFeeVnd());
        }
        program.setSaleTuitionFeeVnd(request.getSaleTuitionFeeVnd());
        applyEnglishProfile(program, request);
        program.setLearningOutcomes(trimOrNull(request.getOutcomes()));
        program.setTeacherGuide(trimOrNull(request.getTeacherGuide()));
        PackageStatus previousStatus = program.getPublicationStatus();
        PackageStatus nextStatus = parsePublicationStatus(request.getStatus());

        // Validate course readiness before publishing
        if (nextStatus == PackageStatus.PUBLISHED && previousStatus != PackageStatus.PUBLISHED) {
            validateReadyForPublish(program);
        }
        program.setPublicationStatus(nextStatus);
        return toProgramResponse(saveAndSyncProgram(program), true);
    }

    @Override
    public void archiveProgram(Long id) {
        InstructorLedCourse program = findProgram(id);
        long activeClassrooms = countActiveClassrooms(program);
        if (activeClassrooms > 0) {
            throw new RuntimeException(
                    "Không thể lưu trữ: giáo trình đang được " + activeClassrooms
                            + " lớp sắp khai giảng hoặc đang diễn ra sử dụng.");
        }
        program.setPublicationStatus(PackageStatus.ARCHIVED);
        saveAndSyncProgram(program);
    }

    @Override
    public InstructorLedCourseResponse cloneProgram(Long id) {
        InstructorLedCourse source = findProgram(id);
        InstructorLedCourse clone = InstructorLedCourse.builder()
                .title(source.getTitle() + " (Bản sao)")
                .code(uniqueProgramCode(source.getCode()))
                .shortDescription(source.getShortDescription())
                .description(source.getDescription())
                .durationLabel(source.getDurationLabel())
                .level(source.getLevel())
                .baseTuitionFeeVnd(source.getBaseTuitionFeeVnd())
                .saleTuitionFeeVnd(source.getSaleTuitionFeeVnd())
                .examType(source.getExamType())
                .focusSkills(source.getFocusSkills())
                .targetBand(source.getTargetBand())
                .targetScore(source.getTargetScore())
                .entryLevel(source.getEntryLevel())
                .entryPlacementLevel(source.getEntryPlacementLevel())
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
            unit.getLessons().forEach(sessionPlan -> unitClone.addLesson(CourseLesson.builder()
                    .sequenceNumber(sessionPlan.getSequenceNumber())
                    .title(sessionPlan.getTitle())
                    .description(sessionPlan.getDescription())
                    .learningObjectives(sessionPlan.getLearningObjectives())
                    .build()));
        }
        InstructorLedCourse saved = programRepository.save(clone);
        return toProgramResponse(saved, true);
    }

    @Override
    public InstructorLedCourseResponse publishProgram(Long id, String actorEmail) {
        InstructorLedCourse program = findProgram(id);
        if (program.getPublicationStatus() == PackageStatus.PUBLISHED) {
            throw new RuntimeException("Giáo trình đã được xuất bản.");
        }
        validateReadyForPublish(program);
        User actor = accessHelper.requireUser(actorEmail);
        program.setPublicationStatus(PackageStatus.PUBLISHED);
        program.setReviewNote(null);
        program.setCreatedBy(actor);
        program.setSubmittedAt(LocalDateTime.now());
        program.setReviewedBy(actor);
        program.setReviewedAt(LocalDateTime.now());
        program = saveAndSyncProgram(program);
        return toProgramResponse(program, true);
    }

    /**
     * Creates a new course unit in the instructor-led course.
     */
    @Override
    public CourseUnitResponse createUnit(Long programId, CourseUnitRequest request) {
        InstructorLedCourse program = findProgram(programId);
        CourseUnit unit = CourseUnit.builder()
                .instructorLedCourse(program)
                .sequenceNumber(defaultInt(request.getDisplayOrder()))
                .title(requireText(request.getTitle(), "Tên Unit không được để trống."))
                .description(trimOrNull(request.getDescription()))
                .learningObjectives(trimOrNull(request.getSessionPlan()))
                .build();
        program.addUnit(unit);
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
        InstructorLedCourse program = unit.getInstructorLedCourse();
        unitRepository.delete(unit);
        unitRepository.flush();
        synchronizeTotalSessions(program);
    }

    /**
     * Creates a new lesson/session plan under a specified course unit.
     */
    @Override
    public CourseLessonResponse createSessionPlan(
            Long unitId,
            CourseLessonRequest request
    ) {
        CourseUnit unit = findUnit(unitId);
        validateSessionPlanRequest(request);
        assertSessionNumberAvailable(unit.getInstructorLedCourse().getId(), request.getSessionNumber(), null);
        CourseLesson sessionPlan = CourseLesson.builder()
                .courseUnit(unit)
                .sequenceNumber(request.getSessionNumber())
                .plannedSessionCount(request.getPlannedSessionCount() == null || request.getPlannedSessionCount() < 1 ? 1 : request.getPlannedSessionCount())
                .title(requireText(request.getTitle(), "Tiêu đề bài học không được để trống."))
                .description(trimOrNull(request.getDescription()))
                .learningObjectives(trimOrNull(request.getLearningObjectives()))
                .build();
        sessionPlan = sessionPlanRepository.save(sessionPlan);
        synchronizeTotalSessions(unit.getInstructorLedCourse());
        return toSessionPlanResponse(sessionPlan);
    }

    /**
     * Updates an existing lesson/session plan.
     */
    @Override
    public CourseLessonResponse updateSessionPlan(
            Long sessionPlanId,
            CourseLessonRequest request
    ) {
        CourseLesson sessionPlan = findSessionPlan(sessionPlanId);
        validateSessionPlanRequest(request);
        assertSessionNumberAvailable(
                sessionPlan.getCourseUnit().getInstructorLedCourse().getId(),
                request.getSessionNumber(),
                sessionPlanId
        );
        sessionPlan.setSequenceNumber(request.getSessionNumber());
        sessionPlan.setPlannedSessionCount(request.getPlannedSessionCount() == null || request.getPlannedSessionCount() < 1 ? 1 : request.getPlannedSessionCount());
        sessionPlan.setTitle(requireText(request.getTitle(), "Tiêu đề bài học không được để trống."));
        sessionPlan.setDescription(trimOrNull(request.getDescription()));
        sessionPlan.setLearningObjectives(trimOrNull(request.getLearningObjectives()));
        sessionPlan = sessionPlanRepository.save(sessionPlan);
        return toSessionPlanResponse(sessionPlan);
    }

    /**
     * Deletes a lesson/session plan and recalculates course totals.
     */
    @Override
    public void deleteSessionPlan(Long sessionPlanId) {
        CourseLesson sessionPlan = findSessionPlan(sessionPlanId);
        InstructorLedCourse program = sessionPlan.getCourseUnit().getInstructorLedCourse();
        sessionPlanRepository.delete(sessionPlan);
        sessionPlanRepository.flush();
        synchronizeTotalSessions(program);
    }

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

    @Override
    public void detachReference(String type, Long referenceId) {
        CourseUnitContentRef ref = contentRefRepository.findById(referenceId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy liên kết nội dung."));
        if (!ref.getContentType().name().equals(normalizeRefType(type))) {
            throw new RuntimeException("Loại tài nguyên không khớp với liên kết.");
        }
        contentRefRepository.delete(ref);
    }

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

    @Override
    @Transactional(readOnly = true)
    public AssessmentBankItemResponse getAssessmentBankItem(Long id) {
        return toAssessmentResponse(findAssessment(id));
    }

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

    @Override
    @Transactional(readOnly = true)
    public AssessmentBankItemResponse getPublishedMockTest(Long id) {
        AssessmentBankItem item = assessmentBankRepository
                .findByIdAndTypeAndStatus(id, AssessmentType.MOCK_TEST, "PUBLISHED")
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đề thi thử đã xuất bản."));
        return toAssessmentResponse(item);
    }

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

    @Override
    public void archiveAssessmentBankItem(Long id) {
        AssessmentBankItem item = findAssessment(id);
        item.setStatus("ARCHIVED");
        assessmentBankRepository.save(item);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FlashcardSetResponse> listFlashcardSets() {
        return flashcardSetRepository.findAllByOrderByUpdatedAtDescIdDesc()
                .stream()
                .map(this::toFlashcardSetResponse)
                .toList();
    }

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

    @Override
    @Transactional(readOnly = true)
    public FlashcardSetResponse getFlashcardSet(Long id) {
        return toFlashcardSetResponse(findFlashcardSet(id));
    }

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

    @Override
    public void archiveFlashcardSet(Long id) {
        FlashcardSet set = findFlashcardSet(id);
        set.setStatus("ARCHIVED");
        flashcardSetRepository.save(set);
    }

    private InstructorLedCourse saveAndSyncProgram(InstructorLedCourse program) {
        InstructorLedCourse saved = programRepository.save(program);
        return saved;
    }

    private InstructorLedCourse findProgram(Long id) {
        return programRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giáo trình."));
    }

    private CourseUnit findUnit(Long id) {
        return unitRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Unit trong giáo trình."));
    }

    private CourseLesson findSessionPlan(Long id) {
        return sessionPlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài học trong giáo trình."));
    }

    private AssessmentBankItem findAssessment(Long id) {
        return assessmentBankRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đề trong ngân hàng."));
    }

    private FlashcardSet findFlashcardSet(Long id) {
        return flashcardSetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bộ flashcard."));
    }

    private InstructorLedCourseResponse toProgramResponse(InstructorLedCourse program, boolean includeUnits) {
        return InstructorLedCourseResponse.builder()
                .id(program.getId())
                .title(program.getTitle())
                .code(program.getCode())
                .shortDescription(program.getShortDescription())
                .description(program.getDescription())
                .durationLabel(program.getDurationLabel())
                .level(program.getLevel())
                .baseTuitionFeeVnd(program.getBaseTuitionFeeVnd())
                .saleTuitionFeeVnd(program.getSaleTuitionFeeVnd())
                .examCategory(program.getExamType())
                .focusSkills(program.getFocusSkills())
                .targetBand(program.getTargetBand())
                .targetScore(program.getTargetScore())
                .entryLevel(program.getEntryLevel())
                .entryPlacementLevel(program.getEntryPlacementLevel())
                .outcomes(program.getLearningOutcomes())
                .teacherGuide(program.getTeacherGuide())
                .totalSessions(resolveTotalSessions(program))
                .totalLessons(resolveTotalLessons(program))
                .totalUnits(program.getUnits() == null ? 0 : program.getUnits().size())
                .status(program.getPublicationStatus().name())
                .statusLabel(programStatusLabel(program.getPublicationStatus().name()))
                .reviewNote(program.getReviewNote())
                .submittedByName(program.getCreatedBy() == null ? null : program.getCreatedBy().getFullName())
                .submittedAt(program.getSubmittedAt())
                .reviewedByName(program.getReviewedBy() == null ? null : program.getReviewedBy().getFullName())
                .reviewedAt(program.getReviewedAt())
                .classroomUsageCount(0)
                .activeClassroomCount(0)
                .createdAt(program.getCreatedAt())
                .updatedAt(program.getUpdatedAt())
                .units(includeUnits ? (program.getUnits() == null ? List.of() : program.getUnits().stream().map(this::toUnitResponse).toList()) : null)
                .usingClassrooms(includeUnits ? toClassroomUsages(program) : null)
                .build();
    }

    private List<InstructorLedCourseResponse.ClassroomUsage> toClassroomUsages(InstructorLedCourse program) {
        return List.of();
    }

    private void validateReadyForPublish(InstructorLedCourse program) {
        if (("IELTS".equals(program.getExamType()) || "TOEIC".equals(program.getExamType()))
                && program.getEntryPlacementLevel() == null) {
            throw new IllegalArgumentException("Hãy chọn trình độ Placement đầu vào trước khi xuất bản giáo trình.");
        }
        validateEnglishProfile(
                program.getExamType(),
                program.getFocusSkills(),
                program.getTargetBand(),
                program.getTargetScore(),
                program.getEntryLevel()
        );
        if (!StringUtils.hasText(program.getLearningOutcomes())) {
            throw new RuntimeException("Giáo trình phải mô tả chuẩn đầu ra tiếng Anh trước khi xuất bản.");
        }
        if (program.getUnits() == null || program.getUnits().isEmpty()) {
            throw new RuntimeException("Giáo trình chưa có Unit nào. Hãy thêm nội dung trước khi xuất bản.");
        }
        validateStructuredLessons(program);
        boolean hasUnpublishedMaterial = program.getUnits().stream()
                .flatMap(unit -> unit.getContentRefs().stream())
                .filter(ref -> ref.getContentType() == CourseUnitContentType.MATERIAL)
                .map(CourseUnitContentRef::getLearningResource)
                .filter(java.util.Objects::nonNull)
                .anyMatch(material -> !"PUBLISHED".equalsIgnoreCase(material.getStatus()));
        if (hasUnpublishedMaterial) {
            throw new RuntimeException("Giáo trình chỉ được sử dụng học liệu trung tâm đã xuất bản.");
        }
        validateFocusedSkillAssessments(program);
    }

    private void validateStructuredLessons(InstructorLedCourse program) {
        List<CourseLesson> courseLessons = program.getUnits().stream()
                .flatMap(unit -> unit.getLessons().stream())
                .sorted(Comparator.comparing(CourseLesson::getSequenceNumber))
                .toList();
        if (courseLessons.isEmpty()) {
            throw new RuntimeException("Giáo trình chưa có bài học. Hãy cập nhật trước khi xuất bản.");
        }

        program.getUnits().stream()
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
                        "Buổi " + courseLesson.getSequenceNumber() + " đang bị trùng trong giáo trình."
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
                        "Thứ tự buổi học chưa liên tục. Giáo trình hiện có buổi "
                                + currentNumbers + " nhưng thiếu buổi " + expectedNumber + "."
                );
            }
        }

    }

    private void validateSessionPlanRequest(CourseLessonRequest request) {
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

    private void assertSessionNumberAvailable(Long programId, Integer sessionNumber, Long excludeId) {
        if (sessionPlanRepository.existsDuplicateSequenceNumber(programId, sessionNumber, excludeId)) {
            throw new IllegalArgumentException(
                    "Bài học số " + sessionNumber + " đã tồn tại trong giáo trình."
            );
        }
    }

    private void synchronizeTotalSessions(InstructorLedCourse program) {
        // Session count is derived from CourseLesson and is not persisted on the course aggregate.
    }

    private int resolveTotalSessions(InstructorLedCourse program) {
        if (program != null && program.getUnits() != null) {
            return program.getUnits().stream()
                    .filter(unit -> unit.getLessons() != null)
                    .flatMap(unit -> unit.getLessons().stream())
                    .mapToInt(lesson -> lesson.getPlannedSessionCount() == null || lesson.getPlannedSessionCount() < 1 ? 1 : lesson.getPlannedSessionCount())
                    .sum();
        }
        return 0;
    }

    private int resolveTotalLessons(InstructorLedCourse program) {
        if (program != null && program.getUnits() != null) {
            return program.getUnits().stream()
                    .filter(unit -> unit.getLessons() != null)
                    .mapToInt(unit -> unit.getLessons().size())
                    .sum();
        }
        return 0;
    }

    /**
     * Applies and validates standardized English exam profile (IELTS, TOEIC, General English).
     */
    private void applyEnglishProfile(InstructorLedCourse program, InstructorLedCourseRequest request) {
        String examCategory = normalizeExamCategory(request.getExamCategory());
        String focusSkills = normalizeFocusSkills(request.getFocusSkills());
        validateEnglishProfile(
                examCategory,
                focusSkills,
                request.getTargetBand(),
                request.getTargetScore(),
                request.getEntryLevel()
        );
        program.setExamType(examCategory);
        program.setFocusSkills(focusSkills);
        program.setTargetBand(request.getTargetBand());
        program.setTargetScore(request.getTargetScore());
        program.setEntryLevel(request.getEntryLevel().trim());
        program.setEntryPlacementLevel(request.getEntryPlacementLevel());
    }

    private String normalizeExamCategory(String value) {
        String normalized = defaultText(value, "IELTS").trim().toUpperCase(Locale.ROOT);
        if ("GENERAL".equals(normalized) || "COMMUNICATION".equals(normalized) || "FOUNDATION".equals(normalized)) {
            normalized = "GENERAL_ENGLISH";
        }
        if (!EXAM_CATEGORIES.contains(normalized)) {
            throw new IllegalArgumentException("Chương trình chỉ được thuộc IELTS, TOEIC hoặc General English.");
        }
        return normalized;
    }

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

    private void validateEnglishProfile(
            String examCategory,
            String focusSkills,
            BigDecimal targetBand,
            Integer targetScore,
            String entryLevel
    ) {
        if (!StringUtils.hasText(entryLevel)) {
            throw new IllegalArgumentException("Hãy khai báo trình độ đầu vào của chương trình.");
        }
        if (!StringUtils.hasText(focusSkills)) {
            throw new IllegalArgumentException("Hãy chọn ít nhất một kỹ năng tiếng Anh trọng tâm.");
        }
        if ("IELTS".equals(examCategory)) {
            BigDecimal entryBand = parseBand(entryLevel, "Band IELTS đầu vào");
            if (targetBand == null) {
                throw new IllegalArgumentException("Chương trình IELTS phải có band mục tiêu.");
            }
            validateBand(targetBand, "Band IELTS mục tiêu");
            if (entryBand.compareTo(targetBand) > 0) {
                throw new IllegalArgumentException("Band IELTS đầu vào không thể cao hơn band mục tiêu.");
            }
            if (targetScore != null) {
                throw new IllegalArgumentException("Chương trình IELTS không sử dụng thang điểm TOEIC.");
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
                throw new IllegalArgumentException("Chương trình TOEIC không sử dụng band IELTS.");
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

    private BigDecimal parseBand(String value, String label) {
        try {
            BigDecimal band = new BigDecimal(value.trim());
            validateBand(band, label);
            return band;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(label + " phải là một band hợp lệ.");
        }
    }

    private void validateBand(BigDecimal band, String label) {
        if (band.compareTo(BigDecimal.ZERO) < 0
                || band.compareTo(BigDecimal.valueOf(9)) > 0
                || band.multiply(BigDecimal.valueOf(2)).stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException(label + " phải từ 0 đến 9 và tăng theo bước 0.5.");
        }
    }

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

    private String examCategoryLabel(String examCategory) {
        return "GENERAL_ENGLISH".equals(examCategory) ? "General English" : examCategory;
    }

    private void validateFocusedSkillAssessments(InstructorLedCourse program) {
        if ("GENERAL_ENGLISH".equals(program.getExamType())) {
            return;
        }
        Set<AssessmentSkill> testedSkills = program.getUnits().stream()
                .flatMap(unit -> unit.getContentRefs().stream())
                .filter(ref -> ref.getContentType() == CourseUnitContentType.ASSESSMENT)
                .map(CourseUnitContentRef::getContentBankItem)
                .filter(java.util.Objects::nonNull)
                .map(ContentBankItem::getSkill)
                .filter(java.util.Objects::nonNull)
                .map(String::toUpperCase)
                .map(AssessmentSkill::valueOf)
                .collect(java.util.stream.Collectors.toSet());
        for (String skill : program.getFocusSkills().split(",")) {
            try {
                AssessmentSkill assessmentSkill = AssessmentSkill.valueOf(skill);
                if (!testedSkills.contains(assessmentSkill) && !testedSkills.contains(AssessmentSkill.MIXED)) {
                    throw new IllegalArgumentException(
                            "Giáo trình chưa có bài đánh giá cho kỹ năng " + skillLabel(assessmentSkill) + "."
                    );
                }
            } catch (IllegalArgumentException exception) {
                if (exception.getMessage() != null && exception.getMessage().startsWith("Giáo trình")) {
                    throw exception;
                }
            }
        }
    }

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

    private String programStatusLabel(String status) {
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

    private PackageStatus parsePublicationStatus(String status) {
        String normalized = defaultText(status, "DRAFT").trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "APPROVED" -> PackageStatus.PUBLISHED;
            case "PENDING_APPROVAL" -> PackageStatus.PENDING_REVIEW;
            default -> PackageStatus.valueOf(normalized);
        };
    }

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

    private long countActiveClassrooms(InstructorLedCourse program) {
        return 0L;
    }

    /**
     * Resolves course code from request or generates a unique slug based on title.
     */
    private String resolveNewProgramCode(InstructorLedCourseRequest request) {
        if (StringUtils.hasText(request.getCode())) {
            String requestedCode = normalizeProgramCode(request.getCode());
            if (programRepository.existsByCodeIgnoreCase(requestedCode)) {
                throw new RuntimeException("Mã giáo trình đã tồn tại.");
            }
            return requestedCode;
        }
        return uniqueCode(makeProgramCode(request.getTitle()));
    }

    /**
     * Generates a slugified uppercase code prefixed with ILC-.
     */
    private String makeProgramCode(String title) {
        String normalizedTitle = Normalizer.normalize(defaultText(title, "CURRICULUM"), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .replaceAll("[^A-Za-z0-9]+", "-")
                .replaceAll("^-+|-+$", "")
                .toUpperCase(Locale.ROOT);
        return normalizeProgramCode("ILC-" + defaultText(normalizedTitle, "COURSE"));
    }

    /**
     * Trims and normalizes course code within length limits.
     */
    private String normalizeProgramCode(String sourceCode) {
        String normalized = sourceCode.trim().toUpperCase(Locale.ROOT);
        return normalized.length() <= PROGRAM_CODE_MAX_LENGTH
                ? normalized
                : normalized.substring(0, PROGRAM_CODE_MAX_LENGTH);
    }

    private String uniqueProgramCode(String sourceCode) {
        return uniqueCode(sourceCode + "-COPY");
    }

    /**
     * Ensures code uniqueness by appending incremental numerical suffixes if duplicated.
     */
    private String uniqueCode(String sourceCode) {
        String base = normalizeProgramCode(sourceCode);
        String code = base;
        int index = 2;
        while (programRepository.existsByCodeIgnoreCase(code)) {
            String suffix = "-" + index++;
            int baseLength = Math.min(base.length(), PROGRAM_CODE_MAX_LENGTH - suffix.length());
            code = base.substring(0, baseLength) + suffix;
        }
        return code;
    }

    private CourseUnitResponse toUnitResponse(CourseUnit unit) {
        List<CourseUnitContentRef> refs = unit.getContentRefs() == null ? List.of() : unit.getContentRefs();
        return CourseUnitResponse.builder()
                .id(unit.getId())
                .programId(unit.getInstructorLedCourse().getId())
                .displayOrder(unit.getSequenceNumber())
                .title(unit.getTitle())
                .description(unit.getDescription())
                .sessionPlan(unit.getLearningObjectives())
                .lessons(unit.getLessons().stream()
                        .sorted(Comparator.comparing(CourseLesson::getSequenceNumber)
                                .thenComparing(CourseLesson::getId, Comparator.nullsLast(Long::compareTo)))
                        .map(this::toSessionPlanResponse)
                        .toList())
                .createdAt(unit.getCreatedAt())
                .updatedAt(unit.getUpdatedAt())
                .materials(toContentRefResponses(refs, CourseUnitContentType.MATERIAL))
                .exercises(toContentRefResponses(refs, CourseUnitContentType.EXERCISE))
                .assessments(toContentRefResponses(refs, CourseUnitContentType.ASSESSMENT))
                .flashcards(toContentRefResponses(refs, CourseUnitContentType.FLASHCARD))
                .build();
    }

    private CourseLessonResponse toSessionPlanResponse(CourseLesson sessionPlan) {
        CourseUnit unit = sessionPlan.getCourseUnit();
        return CourseLessonResponse.builder()
                .id(sessionPlan.getId())
                .unitId(unit.getId())
                .unitTitle(unit.getTitle())
                .programId(unit.getInstructorLedCourse().getId())
                .sessionNumber(sessionPlan.getSequenceNumber())
                .displayOrder(sessionPlan.getSequenceNumber())
                .plannedSessionCount(sessionPlan.getPlannedSessionCount() == null || sessionPlan.getPlannedSessionCount() < 1 ? 1 : sessionPlan.getPlannedSessionCount())
                .title(sessionPlan.getTitle())
                .description(sessionPlan.getDescription())
                .learningObjectives(sessionPlan.getLearningObjectives())
                .createdAt(sessionPlan.getCreatedAt())
                .updatedAt(sessionPlan.getUpdatedAt())
                .build();
    }

    private List<CourseUnitContentRefResponse> toContentRefResponses(
            List<CourseUnitContentRef> refs,
            CourseUnitContentType type
    ) {
        return refs.stream()
                .filter(ref -> ref.getContentType() == type)
                .map(this::toContentRefResponse)
                .toList();
    }

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

    private void validateAssessmentBankRequest(AssessmentBankItemRequest request) {
        if ((request.getSkill() == AssessmentSkill.LISTENING || request.getSkill() == AssessmentSkill.READING)
                && !StringUtils.hasText(request.getObjectiveAnswerKey())) {
            throw new RuntimeException("Đề Listening/Reading cần có đáp án khách quan.");
        }
        if ((request.getSkill() == AssessmentSkill.WRITING || request.getSkill() == AssessmentSkill.SPEAKING)
                && !StringUtils.hasText(request.getUiConfigJson())) {
            throw new RuntimeException("Đề Writing/Speaking cần có nội dung đề trong cấu hình.");
        }
        if (request.getType() == AssessmentType.MODULE_TEST
                && (request.getSkill() == AssessmentSkill.WRITING || request.getSkill() == AssessmentSkill.SPEAKING)) {
            if (resolveAiEvaluationMode(request) == AiEvaluationMode.NONE) {
                throw new RuntimeException("Module Test Writing/Speaking phải bật chấm bằng AI.");
            }
            if (request.getRubricId() == null) {
                throw new RuntimeException("Module Test Writing/Speaking phải có bộ tiêu chí chấm.");
            }
        }
    }

    private AiEvaluationMode resolveAiEvaluationMode(AssessmentBankItemRequest request) {
        if (request.getAiEvaluationMode() != null) return request.getAiEvaluationMode();
        return request.getSkill() == AssessmentSkill.WRITING || request.getSkill() == AssessmentSkill.SPEAKING
                ? AiEvaluationMode.RUBRIC_FEEDBACK
                : AiEvaluationMode.EXPLAIN_ONLY;
    }

    private AssessmentRubric resolveAssessmentRubric(Long rubricId, AssessmentSkill skill) {
        if (rubricId == null) {
            return null;
        }
        AssessmentRubric rubric = assessmentRubricRepository.findById(rubricId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy rubric."));
        if (!"PUBLISHED".equalsIgnoreCase(rubric.getStatus())) {
            throw new RuntimeException("Rubric đã tạm ngưng.");
        }
        if (rubric.getSkill() != null && rubric.getSkill() != skill && rubric.getSkill() != AssessmentSkill.MIXED) {
            throw new RuntimeException("Rubric không phù hợp với kỹ năng của nội dung.");
        }
        return rubric;
    }

    private String normalizeRefType(String type) {
        return StringUtils.hasText(type) ? type.trim().toUpperCase(Locale.ROOT) : "";
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new RuntimeException(message);
        }
        return value.trim();
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private void requirePublishedResource(String status, String resourceLabel) {
        if (!"PUBLISHED".equalsIgnoreCase(status)) {
            throw new IllegalArgumentException(resourceLabel + " phải được xuất bản trước khi gắn vào khóa học.");
        }
    }

    private String trimOrNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String toJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            throw new IllegalArgumentException("Không thể chuyển đổi cấu hình nội dung.", exception);
        }
    }

    private String trimUpperOrNull(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : null;
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }
}
