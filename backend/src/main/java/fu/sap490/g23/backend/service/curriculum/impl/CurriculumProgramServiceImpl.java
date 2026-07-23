package fu.sap490.g23.backend.service.curriculum.impl;

import fu.sap490.g23.backend.dto.request.curriculum.AssessmentBankItemRequest;
import fu.sap490.g23.backend.dto.request.curriculum.CurriculumProgramRequest;
import fu.sap490.g23.backend.dto.request.curriculum.CurriculumReferenceRequest;
import fu.sap490.g23.backend.dto.request.curriculum.CurriculumUnitRequest;
import fu.sap490.g23.backend.dto.request.curriculum.FlashcardSetRequest;
import fu.sap490.g23.backend.dto.response.assessment.AssessmentRubricResponse;
import fu.sap490.g23.backend.dto.response.assessment.RubricCriterionResponse;
import fu.sap490.g23.backend.dto.response.curriculum.AssessmentBankItemResponse;
import fu.sap490.g23.backend.dto.response.curriculum.CurriculumProgramResponse;
import fu.sap490.g23.backend.dto.response.curriculum.CurriculumReferenceResponse;
import fu.sap490.g23.backend.dto.response.curriculum.CurriculumUnitResponse;
import fu.sap490.g23.backend.dto.response.curriculum.FlashcardSetResponse;
import fu.sap490.g23.backend.entity.assessment.AssessmentRubric;
import fu.sap490.g23.backend.entity.assessment.ExerciseBankItem;
import fu.sap490.g23.backend.entity.assessment.enums.AiEvaluationMode;
import fu.sap490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sap490.g23.backend.entity.assessment.enums.AssessmentType;
import fu.sap490.g23.backend.entity.classroom.CenterMaterialLibraryItem;
import fu.sap490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import fu.sap490.g23.backend.entity.classroom.enums.ClassroomOfferingStatus;
import fu.sap490.g23.backend.entity.curriculum.AssessmentBankItem;
import fu.sap490.g23.backend.entity.curriculum.CurriculumAssessmentRef;
import fu.sap490.g23.backend.entity.curriculum.CurriculumExerciseRef;
import fu.sap490.g23.backend.entity.curriculum.CurriculumFlashcardRef;
import fu.sap490.g23.backend.entity.curriculum.CurriculumMaterialRef;
import fu.sap490.g23.backend.entity.curriculum.CurriculumProgram;
import fu.sap490.g23.backend.entity.curriculum.CurriculumUnit;
import fu.sap490.g23.backend.entity.curriculum.FlashcardSet;
import fu.sap490.g23.backend.repository.assessment.AssessmentRubricRepository;
import fu.sap490.g23.backend.repository.assessment.ExerciseBankItemRepository;
import fu.sap490.g23.backend.repository.classroom.CenterMaterialLibraryItemRepository;
import fu.sap490.g23.backend.repository.curriculum.AssessmentBankItemRepository;
import fu.sap490.g23.backend.repository.curriculum.CurriculumAssessmentRefRepository;
import fu.sap490.g23.backend.repository.curriculum.CurriculumExerciseRefRepository;
import fu.sap490.g23.backend.repository.curriculum.CurriculumFlashcardRefRepository;
import fu.sap490.g23.backend.repository.curriculum.CurriculumMaterialRefRepository;
import fu.sap490.g23.backend.repository.curriculum.CurriculumProgramRepository;
import fu.sap490.g23.backend.repository.curriculum.CurriculumUnitRepository;
import fu.sap490.g23.backend.repository.curriculum.FlashcardSetRepository;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.classroom.ClassroomOffering;
import fu.sap490.g23.backend.security.ClassroomAccessHelper;
import fu.sap490.g23.backend.service.curriculum.CurriculumProgramService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional
public class CurriculumProgramServiceImpl implements CurriculumProgramService {

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    private final CurriculumProgramRepository programRepository;
    private final CurriculumUnitRepository unitRepository;
    private final CurriculumMaterialRefRepository materialRefRepository;
    private final CurriculumExerciseRefRepository exerciseRefRepository;
    private final CurriculumAssessmentRefRepository assessmentRefRepository;
    private final CurriculumFlashcardRefRepository flashcardRefRepository;
    private final CenterMaterialLibraryItemRepository materialRepository;
    private final ExerciseBankItemRepository exerciseRepository;
    private final AssessmentRubricRepository assessmentRubricRepository;
    private final AssessmentBankItemRepository assessmentBankRepository;
    private final FlashcardSetRepository flashcardSetRepository;
    private final ClassroomAccessHelper accessHelper;

    @Override
    @Transactional(readOnly = true)
    public List<CurriculumProgramResponse> listPrograms(ClassroomDeliveryMode deliveryMode) {
        List<CurriculumProgram> programs = deliveryMode == null
                ? programRepository.findAllByOrderByDisplayOrderAscUpdatedAtDescIdDesc()
                : programRepository.findByDeliveryModeOrderByDisplayOrderAscUpdatedAtDescIdDesc(deliveryMode);
        return programs.stream().map(program -> toProgramResponse(program, false)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CurriculumProgramResponse getProgram(Long id) {
        return toProgramResponse(findProgram(id), true);
    }

    @Override
    public CurriculumProgramResponse createProgram(CurriculumProgramRequest request) {
        String code = requireText(request.getCode(), "Mã giáo trình không được để trống.").toUpperCase(Locale.ROOT);
        if (programRepository.existsByCodeIgnoreCase(code)) {
            throw new RuntimeException("Mã giáo trình đã tồn tại.");
        }
        String slug = uniqueProgramSlug(StringUtils.hasText(request.getSlug()) ? request.getSlug() : request.getTitle(), null);
        CurriculumProgram program = CurriculumProgram.builder()
                .title(requireText(request.getTitle(), "Tên giáo trình không được để trống."))
                .code(code)
                .slug(slug)
                .deliveryMode(request.getDeliveryMode())
                .examCategory(defaultText(request.getExamCategory(), "IELTS").toUpperCase(Locale.ROOT))
                .targetBand(request.getTargetBand())
                .targetScore(request.getTargetScore())
                .entryLevel(trimOrNull(request.getEntryLevel()))
                .outcomes(trimOrNull(request.getOutcomes()))
                .teacherGuide(trimOrNull(request.getTeacherGuide()))
                .interactionActivities(trimOrNull(request.getInteractionActivities()))
                .totalSessions(defaultInt(request.getTotalSessions()))
                .status(defaultText(request.getStatus(), "DRAFT").toUpperCase(Locale.ROOT))
                .displayOrder(defaultInt(request.getDisplayOrder()))
                .build();
        applyVirtualConfig(program, request);
        if ("PUBLISHED".equals(program.getStatus())) {
            throw new RuntimeException("Giáo trình mới tạo chưa có unit/buổi học nên chưa thể xuất bản. Hãy lưu nháp trước.");
        }
        return toProgramResponse(programRepository.save(program), true);
    }

    @Override
    public CurriculumProgramResponse updateProgram(Long id, CurriculumProgramRequest request) {
        CurriculumProgram program = findProgram(id);
        String code = requireText(request.getCode(), "Mã giáo trình không được để trống.").toUpperCase(Locale.ROOT);
        if (!program.getCode().equalsIgnoreCase(code) && programRepository.existsByCodeIgnoreCase(code)) {
            throw new RuntimeException("Mã giáo trình đã tồn tại.");
        }
        program.setTitle(requireText(request.getTitle(), "Tên giáo trình không được để trống."));
        program.setCode(code);
        program.setSlug(uniqueProgramSlug(StringUtils.hasText(request.getSlug()) ? request.getSlug() : request.getTitle(), id));
        program.setDeliveryMode(request.getDeliveryMode());
        program.setExamCategory(defaultText(request.getExamCategory(), "IELTS").toUpperCase(Locale.ROOT));
        program.setTargetBand(request.getTargetBand());
        program.setTargetScore(request.getTargetScore());
        program.setEntryLevel(trimOrNull(request.getEntryLevel()));
        program.setOutcomes(trimOrNull(request.getOutcomes()));
        program.setTeacherGuide(trimOrNull(request.getTeacherGuide()));
        program.setInteractionActivities(trimOrNull(request.getInteractionActivities()));
        program.setTotalSessions(defaultInt(request.getTotalSessions()));
        String previousStatus = program.getStatus();
        String nextStatus = defaultText(request.getStatus(), "DRAFT").toUpperCase(Locale.ROOT);
        if ("PUBLISHED".equals(nextStatus) && !"PUBLISHED".equals(previousStatus)) {
            validateReadyForPublish(program);
        }
        program.setStatus(nextStatus);
        program.setDisplayOrder(defaultInt(request.getDisplayOrder()));
        applyVirtualConfig(program, request);
        return toProgramResponse(programRepository.save(program), true);
    }

    @Override
    public void archiveProgram(Long id) {
        CurriculumProgram program = findProgram(id);
        long activeClassrooms = countActiveClassrooms(program);
        if (activeClassrooms > 0) {
            throw new RuntimeException(
                    "Không thể lưu trữ: giáo trình đang được " + activeClassrooms
                            + " lớp sắp khai giảng hoặc đang diễn ra sử dụng.");
        }
        program.setStatus("ARCHIVED");
        programRepository.save(program);
    }

    @Override
    public CurriculumProgramResponse cloneProgram(Long id) {
        CurriculumProgram source = findProgram(id);
        CurriculumProgram clone = CurriculumProgram.builder()
                .title(source.getTitle() + " (Bản sao)")
                .code(uniqueProgramCode(source.getCode()))
                .slug(uniqueProgramSlug(source.getSlug() + "-copy", null))
                .deliveryMode(source.getDeliveryMode())
                .examCategory(source.getExamCategory())
                .targetBand(source.getTargetBand())
                .targetScore(source.getTargetScore())
                .entryLevel(source.getEntryLevel())
                .outcomes(source.getOutcomes())
                .teacherGuide(source.getTeacherGuide())
                .interactionActivities(source.getInteractionActivities())
                .totalSessions(source.getTotalSessions())
                .status("DRAFT")
                .displayOrder(source.getDisplayOrder())
                .virtualPlatform(source.getVirtualPlatform())
                .recordingAllowed(source.getRecordingAllowed())
                .recordingAvailableDays(source.getRecordingAvailableDays())
                .materialsDownloadable(source.getMaterialsDownloadable())
                .sessionOpenBeforeMinutes(source.getSessionOpenBeforeMinutes())
                .sessionCloseAfterMinutes(source.getSessionCloseAfterMinutes())
                .deviceCheckRequired(source.getDeviceCheckRequired())
                .micRequired(source.getMicRequired())
                .speakerRequired(source.getSpeakerRequired())
                .cameraRequired(source.getCameraRequired())
                .autoAttendanceEnabled(source.getAutoAttendanceEnabled())
                .minAttendanceMinutes(source.getMinAttendanceMinutes())
                .build();

        for (CurriculumUnit unit : source.getUnits()) {
            CurriculumUnit unitClone = CurriculumUnit.builder()
                    .displayOrder(unit.getDisplayOrder())
                    .title(unit.getTitle())
                    .description(unit.getDescription())
                    .sessionPlan(unit.getSessionPlan())
                    .build();
            clone.addUnit(unitClone);
            unit.getMaterialRefs().forEach(ref -> unitClone.getMaterialRefs().add(CurriculumMaterialRef.builder()
                    .unit(unitClone)
                    .material(ref.getMaterial())
                    .displayOrder(ref.getDisplayOrder())
                    .note(ref.getNote())
                    .build()));
            unit.getExerciseRefs().forEach(ref -> unitClone.getExerciseRefs().add(CurriculumExerciseRef.builder()
                    .unit(unitClone)
                    .exercise(ref.getExercise())
                    .displayOrder(ref.getDisplayOrder())
                    .note(ref.getNote())
                    .build()));
            unit.getAssessmentRefs().forEach(ref -> unitClone.getAssessmentRefs().add(CurriculumAssessmentRef.builder()
                    .unit(unitClone)
                    .assessment(ref.getAssessment())
                    .displayOrder(ref.getDisplayOrder())
                    .note(ref.getNote())
                    .build()));
            unit.getFlashcardRefs().forEach(ref -> unitClone.getFlashcardRefs().add(CurriculumFlashcardRef.builder()
                    .unit(unitClone)
                    .flashcardSet(ref.getFlashcardSet())
                    .displayOrder(ref.getDisplayOrder())
                    .note(ref.getNote())
                    .build()));
        }
        return toProgramResponse(programRepository.save(clone), true);
    }

    @Override
    public CurriculumProgramResponse publishProgram(Long id, String actorEmail) {
        CurriculumProgram program = findProgram(id);
        if ("PUBLISHED".equals(program.getStatus())) {
            throw new RuntimeException("Giáo trình đã được xuất bản.");
        }
        validateReadyForPublish(program);
        User actor = accessHelper.requireUser(actorEmail);
        program.setStatus("PUBLISHED");
        program.setReviewNote(null);
        program.setSubmittedBy(actor);
        program.setSubmittedAt(LocalDateTime.now());
        program.setReviewedBy(actor);
        program.setReviewedAt(LocalDateTime.now());
        program = programRepository.save(program);
        return toProgramResponse(program, true);
    }

    @Override
    public CurriculumUnitResponse createUnit(Long programId, CurriculumUnitRequest request) {
        CurriculumProgram program = findProgram(programId);
        CurriculumUnit unit = CurriculumUnit.builder()
                .program(program)
                .displayOrder(defaultInt(request.getDisplayOrder()))
                .title(requireText(request.getTitle(), "Tên unit/buổi học không được để trống."))
                .description(trimOrNull(request.getDescription()))
                .sessionPlan(trimOrNull(request.getSessionPlan()))
                .build();
        return toUnitResponse(unitRepository.save(unit));
    }

    @Override
    public CurriculumUnitResponse updateUnit(Long unitId, CurriculumUnitRequest request) {
        CurriculumUnit unit = findUnit(unitId);
        unit.setDisplayOrder(defaultInt(request.getDisplayOrder()));
        unit.setTitle(requireText(request.getTitle(), "Tên unit/buổi học không được để trống."));
        unit.setDescription(trimOrNull(request.getDescription()));
        unit.setSessionPlan(trimOrNull(request.getSessionPlan()));
        return toUnitResponse(unitRepository.save(unit));
    }

    @Override
    public void deleteUnit(Long unitId) {
        unitRepository.delete(findUnit(unitId));
    }

    @Override
    public CurriculumUnitResponse attachMaterial(Long unitId, CurriculumReferenceRequest request) {
        CurriculumUnit unit = findUnit(unitId);
        CenterMaterialLibraryItem material = materialRepository.findById(request.getResourceId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy học liệu trong kho."));
        if (!materialRefRepository.existsByUnitIdAndMaterialId(unitId, material.getId())) {
            materialRefRepository.save(CurriculumMaterialRef.builder()
                    .unit(unit)
                    .material(material)
                    .displayOrder(defaultInt(request.getDisplayOrder()))
                    .note(trimOrNull(request.getNote()))
                    .build());
        }
        return toUnitResponse(findUnit(unitId));
    }

    @Override
    public CurriculumUnitResponse attachExercise(Long unitId, CurriculumReferenceRequest request) {
        CurriculumUnit unit = findUnit(unitId);
        ExerciseBankItem exercise = exerciseRepository.findById(request.getResourceId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài tập trong ngân hàng."));
        if (!exerciseRefRepository.existsByUnitIdAndExerciseId(unitId, exercise.getId())) {
            exerciseRefRepository.save(CurriculumExerciseRef.builder()
                    .unit(unit)
                    .exercise(exercise)
                    .displayOrder(defaultInt(request.getDisplayOrder()))
                    .note(trimOrNull(request.getNote()))
                    .build());
        }
        return toUnitResponse(findUnit(unitId));
    }

    @Override
    public CurriculumUnitResponse attachAssessment(Long unitId, CurriculumReferenceRequest request) {
        CurriculumUnit unit = findUnit(unitId);
        AssessmentBankItem assessment = assessmentBankRepository.findById(request.getResourceId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đề trong ngân hàng."));
        if (!assessmentRefRepository.existsByUnitIdAndAssessmentId(unitId, assessment.getId())) {
            assessmentRefRepository.save(CurriculumAssessmentRef.builder()
                    .unit(unit)
                    .assessment(assessment)
                    .displayOrder(defaultInt(request.getDisplayOrder()))
                    .note(trimOrNull(request.getNote()))
                    .build());
        }
        return toUnitResponse(findUnit(unitId));
    }

    @Override
    public CurriculumUnitResponse attachFlashcard(Long unitId, CurriculumReferenceRequest request) {
        CurriculumUnit unit = findUnit(unitId);
        FlashcardSet flashcardSet = flashcardSetRepository.findById(request.getResourceId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bộ flashcard."));
        if (!flashcardRefRepository.existsByUnitIdAndFlashcardSetId(unitId, flashcardSet.getId())) {
            flashcardRefRepository.save(CurriculumFlashcardRef.builder()
                    .unit(unit)
                    .flashcardSet(flashcardSet)
                    .displayOrder(defaultInt(request.getDisplayOrder()))
                    .note(trimOrNull(request.getNote()))
                    .build());
        }
        return toUnitResponse(findUnit(unitId));
    }

    @Override
    public void detachReference(String type, Long referenceId) {
        switch (normalizeRefType(type)) {
            case "MATERIAL" -> materialRefRepository.deleteById(referenceId);
            case "EXERCISE" -> exerciseRefRepository.deleteById(referenceId);
            case "ASSESSMENT" -> assessmentRefRepository.deleteById(referenceId);
            case "FLASHCARD" -> flashcardRefRepository.deleteById(referenceId);
            default -> throw new RuntimeException("Loại tài nguyên không hợp lệ.");
        }
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
    public AssessmentBankItemResponse getAssessmentBankItem(Long id) {
        return toAssessmentResponse(findAssessment(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssessmentBankItemResponse> listPublishedMockTests() {
        return assessmentBankRepository
                .findByTypeAndStatusAndActiveTrueOrderByDisplayOrderAscUpdatedAtDescIdDesc(
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
                .findByIdAndTypeAndStatusAndActiveTrue(id, AssessmentType.MOCK_TEST, "PUBLISHED")
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
                .aiEvaluationMode(request.getAiEvaluationMode() == null ? AiEvaluationMode.NONE : request.getAiEvaluationMode())
                .rubric(rubric)
                .instructions(trimOrNull(request.getInstructions()))
                .objectiveAnswerKey(trimOrNull(request.getObjectiveAnswerKey()))
                .uiConfigJson(trimOrNull(request.getUiConfigJson()))
                .passingScore(request.getPassingScore())
                .maxScore(request.getMaxScore() == null ? BigDecimal.TEN : request.getMaxScore())
                .timeLimitMinutes(request.getTimeLimitMinutes())
                .status(defaultText(request.getStatus(), "DRAFT").toUpperCase(Locale.ROOT))
                .displayOrder(defaultInt(request.getDisplayOrder()))
                .active(true)
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
        item.setAiEvaluationMode(request.getAiEvaluationMode() == null ? AiEvaluationMode.NONE : request.getAiEvaluationMode());
        item.setRubric(rubric);
        item.setInstructions(trimOrNull(request.getInstructions()));
        item.setObjectiveAnswerKey(trimOrNull(request.getObjectiveAnswerKey()));
        item.setUiConfigJson(trimOrNull(request.getUiConfigJson()));
        item.setPassingScore(request.getPassingScore());
        item.setMaxScore(request.getMaxScore() == null ? BigDecimal.TEN : request.getMaxScore());
        item.setTimeLimitMinutes(request.getTimeLimitMinutes());
        item.setStatus(defaultText(request.getStatus(), "DRAFT").toUpperCase(Locale.ROOT));
        item.setDisplayOrder(defaultInt(request.getDisplayOrder()));
        item.setActive(!"ARCHIVED".equalsIgnoreCase(item.getStatus()));
        return toAssessmentResponse(assessmentBankRepository.save(item));
    }

    @Override
    public void archiveAssessmentBankItem(Long id) {
        AssessmentBankItem item = findAssessment(id);
        item.setStatus("ARCHIVED");
        item.setActive(false);
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
                .displayOrder(defaultInt(request.getDisplayOrder()))
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
        set.setDisplayOrder(defaultInt(request.getDisplayOrder()));
        return toFlashcardSetResponse(flashcardSetRepository.save(set));
    }

    @Override
    public void archiveFlashcardSet(Long id) {
        FlashcardSet set = findFlashcardSet(id);
        set.setStatus("ARCHIVED");
        flashcardSetRepository.save(set);
    }

    private CurriculumProgram findProgram(Long id) {
        return programRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giáo trình."));
    }

    private CurriculumUnit findUnit(Long id) {
        return unitRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy unit/buổi học trong giáo trình."));
    }

    private AssessmentBankItem findAssessment(Long id) {
        return assessmentBankRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đề trong ngân hàng."));
    }

    private FlashcardSet findFlashcardSet(Long id) {
        return flashcardSetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bộ flashcard."));
    }

    private CurriculumProgramResponse toProgramResponse(CurriculumProgram program, boolean includeUnits) {
        return CurriculumProgramResponse.builder()
                .id(program.getId())
                .title(program.getTitle())
                .code(program.getCode())
                .slug(program.getSlug())
                .deliveryMode(program.getDeliveryMode())
                .deliveryModeLabel(deliveryModeLabel(program.getDeliveryMode()))
                .examCategory(program.getExamCategory())
                .targetBand(program.getTargetBand())
                .targetScore(program.getTargetScore())
                .entryLevel(program.getEntryLevel())
                .outcomes(program.getOutcomes())
                .teacherGuide(program.getTeacherGuide())
                .interactionActivities(program.getInteractionActivities())
                .totalSessions(program.getTotalSessions())
                .totalUnits(program.getUnits().size())
                .status(program.getStatus())
                .statusLabel(programStatusLabel(program.getStatus()))
                .reviewNote(program.getReviewNote())
                .submittedByName(program.getSubmittedBy() == null ? null : program.getSubmittedBy().getFullName())
                .submittedAt(program.getSubmittedAt())
                .reviewedByName(program.getReviewedBy() == null ? null : program.getReviewedBy().getFullName())
                .reviewedAt(program.getReviewedAt())
                .displayOrder(program.getDisplayOrder())
                .classroomUsageCount(program.getClassroomOfferings().size())
                .activeClassroomCount((int) countActiveClassrooms(program))
                .virtualPlatform(program.getVirtualPlatform())
                .recordingAllowed(program.getRecordingAllowed())
                .recordingAvailableDays(program.getRecordingAvailableDays())
                .materialsDownloadable(program.getMaterialsDownloadable())
                .sessionOpenBeforeMinutes(program.getSessionOpenBeforeMinutes())
                .sessionCloseAfterMinutes(program.getSessionCloseAfterMinutes())
                .deviceCheckRequired(program.getDeviceCheckRequired())
                .micRequired(program.getMicRequired())
                .speakerRequired(program.getSpeakerRequired())
                .cameraRequired(program.getCameraRequired())
                .autoAttendanceEnabled(program.getAutoAttendanceEnabled())
                .minAttendanceMinutes(program.getMinAttendanceMinutes())
                .createdAt(program.getCreatedAt())
                .updatedAt(program.getUpdatedAt())
                .units(includeUnits ? program.getUnits().stream().map(this::toUnitResponse).toList() : null)
                .usingClassrooms(includeUnits ? toClassroomUsages(program) : null)
                .build();
    }

    private List<CurriculumProgramResponse.ClassroomUsage> toClassroomUsages(CurriculumProgram program) {
        return program.getClassroomOfferings().stream()
                .sorted(Comparator.comparing(
                        ClassroomOffering::getStartDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(offering -> CurriculumProgramResponse.ClassroomUsage.builder()
                        .id(offering.getId())
                        .title(offering.getLearningPackage() == null ? null : offering.getLearningPackage().getTitle())
                        .status(offering.getStatus() == null ? null : offering.getStatus().name())
                        .statusLabel(offeringStatusLabel(offering.getStatus()))
                        .startDate(offering.getStartDate())
                        .build())
                .toList();
    }

    private void applyVirtualConfig(CurriculumProgram program, CurriculumProgramRequest request) {
        if (program.getDeliveryMode() != ClassroomDeliveryMode.VIRTUAL) {
            return;
        }
        program.setVirtualPlatform(trimUpperOrNull(request.getVirtualPlatform()));
        program.setRecordingAllowed(request.getRecordingAllowed());
        program.setRecordingAvailableDays(request.getRecordingAvailableDays());
        program.setMaterialsDownloadable(request.getMaterialsDownloadable());
        program.setSessionOpenBeforeMinutes(request.getSessionOpenBeforeMinutes());
        program.setSessionCloseAfterMinutes(request.getSessionCloseAfterMinutes());
        program.setDeviceCheckRequired(request.getDeviceCheckRequired());
        program.setMicRequired(request.getMicRequired());
        program.setSpeakerRequired(request.getSpeakerRequired());
        program.setCameraRequired(request.getCameraRequired());
        program.setAutoAttendanceEnabled(request.getAutoAttendanceEnabled());
        program.setMinAttendanceMinutes(request.getMinAttendanceMinutes());
    }

    private void validateReadyForPublish(CurriculumProgram program) {
        if (program.getUnits() == null || program.getUnits().isEmpty()) {
            throw new RuntimeException("Giáo trình chưa có unit/buổi học nào. Hãy thêm nội dung trước khi xuất bản.");
        }
        if (program.getTotalSessions() == null || program.getTotalSessions() <= 0) {
            throw new RuntimeException("Giáo trình chưa khai báo số buổi học. Hãy cập nhật trước khi xuất bản.");
        }
        boolean hasUnpublishedMaterial = program.getUnits().stream()
                .flatMap(unit -> unit.getMaterialRefs().stream())
                .map(CurriculumMaterialRef::getMaterial)
                .anyMatch(material -> !"PUBLISHED".equalsIgnoreCase(material.getStatus()));
        if (hasUnpublishedMaterial) {
            throw new RuntimeException("Giáo trình chỉ được sử dụng học liệu trung tâm đã xuất bản.");
        }
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

    private long countActiveClassrooms(CurriculumProgram program) {
        return program.getClassroomOfferings().stream()
                .filter(offering -> offering.getStatus() == ClassroomOfferingStatus.UPCOMING
                        || offering.getStatus() == ClassroomOfferingStatus.ACTIVE)
                .count();
    }

    private String uniqueProgramCode(String sourceCode) {
        String base = sourceCode + "-COPY";
        String code = base;
        int index = 2;
        while (programRepository.existsByCodeIgnoreCase(code)) {
            code = base + "-" + index++;
        }
        return code;
    }

    private CurriculumUnitResponse toUnitResponse(CurriculumUnit unit) {
        return CurriculumUnitResponse.builder()
                .id(unit.getId())
                .programId(unit.getProgram().getId())
                .displayOrder(unit.getDisplayOrder())
                .title(unit.getTitle())
                .description(unit.getDescription())
                .sessionPlan(unit.getSessionPlan())
                .createdAt(unit.getCreatedAt())
                .updatedAt(unit.getUpdatedAt())
                .materials(unit.getMaterialRefs().stream().map(this::toMaterialRef).toList())
                .exercises(unit.getExerciseRefs().stream().map(this::toExerciseRef).toList())
                .assessments(unit.getAssessmentRefs().stream().map(this::toAssessmentRef).toList())
                .flashcards(unit.getFlashcardRefs().stream().map(this::toFlashcardRef).toList())
                .build();
    }

    private CurriculumReferenceResponse toMaterialRef(CurriculumMaterialRef ref) {
        CenterMaterialLibraryItem material = ref.getMaterial();
        return CurriculumReferenceResponse.builder()
                .id(ref.getId())
                .type("MATERIAL")
                .resourceId(material.getId())
                .title(material.getTitle())
                .subtitle(material.getMaterialType())
                .skill(material.getSkill())
                .status(material.getStatus())
                .fileUrl(material.getFileUrl())
                .displayOrder(ref.getDisplayOrder())
                .note(ref.getNote())
                .build();
    }

    private CurriculumReferenceResponse toExerciseRef(CurriculumExerciseRef ref) {
        ExerciseBankItem exercise = ref.getExercise();
        return CurriculumReferenceResponse.builder()
                .id(ref.getId())
                .type("EXERCISE")
                .resourceId(exercise.getId())
                .title(exercise.getTitle())
                .subtitle(exercise.getExerciseType())
                .skill(exercise.getSkill())
                .status(exercise.isActive() ? "ACTIVE" : "INACTIVE")
                .displayOrder(ref.getDisplayOrder())
                .note(ref.getNote())
                .build();
    }

    private CurriculumReferenceResponse toAssessmentRef(CurriculumAssessmentRef ref) {
        AssessmentBankItem assessment = ref.getAssessment();
        return CurriculumReferenceResponse.builder()
                .id(ref.getId())
                .type("ASSESSMENT")
                .resourceId(assessment.getId())
                .title(assessment.getTitle())
                .subtitle(assessment.getType() == null ? null : assessment.getType().name())
                .skill(assessment.getSkill() == null ? null : assessment.getSkill().name())
                .status(assessment.getStatus())
                .displayOrder(ref.getDisplayOrder())
                .note(ref.getNote())
                .build();
    }

    private CurriculumReferenceResponse toFlashcardRef(CurriculumFlashcardRef ref) {
        FlashcardSet set = ref.getFlashcardSet();
        return CurriculumReferenceResponse.builder()
                .id(ref.getId())
                .type("FLASHCARD")
                .resourceId(set.getId())
                .title(set.getTitle())
                .subtitle(set.getExamCategory())
                .skill(set.getSkill())
                .status(set.getStatus())
                .displayOrder(ref.getDisplayOrder())
                .note(ref.getNote())
                .contentJson(set.getCardsJson())
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
                .displayOrder(item.getDisplayOrder())
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
                .active(rubric.isActive())
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
                .displayOrder(set.getDisplayOrder())
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
    }

    private AssessmentRubric resolveAssessmentRubric(Long rubricId, AssessmentSkill skill) {
        if (rubricId == null) {
            return null;
        }
        AssessmentRubric rubric = assessmentRubricRepository.findById(rubricId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy rubric."));
        if (!rubric.isActive()) {
            throw new RuntimeException("Rubric đã tạm ngưng.");
        }
        if (rubric.getSkill() != null && rubric.getSkill() != skill && rubric.getSkill() != AssessmentSkill.MIXED) {
            throw new RuntimeException("Rubric không phù hợp với kỹ năng của nội dung.");
        }
        return rubric;
    }

    private String uniqueProgramSlug(String source, Long currentId) {
        String baseSlug = toSlug(source);
        String slug = baseSlug;
        int index = 2;
        while (programRepository.findBySlug(slug)
                .filter(existing -> currentId == null || !existing.getId().equals(currentId))
                .isPresent()) {
            slug = baseSlug + "-" + index++;
        }
        return slug;
    }

    private String toSlug(String input) {
        String source = StringUtils.hasText(input) ? input.trim() : "curriculum";
        String nowhitespace = WHITESPACE.matcher(source).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        slug = slug.replaceAll("-+", "-").toLowerCase(Locale.ENGLISH);
        return slug.isBlank() ? "curriculum" : slug;
    }

    private String normalizeRefType(String type) {
        return StringUtils.hasText(type) ? type.trim().toUpperCase(Locale.ROOT) : "";
    }

    private String deliveryModeLabel(ClassroomDeliveryMode mode) {
        if (mode == null) {
            return null;
        }
        return switch (mode) {
            case OFFLINE -> "Tại trung tâm";
            case VIRTUAL -> "Trực tuyến với giảng viên";
        };
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

    private String trimOrNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String trimUpperOrNull(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : null;
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }
}
