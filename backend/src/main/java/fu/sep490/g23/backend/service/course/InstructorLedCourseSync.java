package fu.sep490.g23.backend.service.course;

import fu.sep490.g23.backend.entity.classroom.CenterMaterialLibraryItem;
import fu.sep490.g23.backend.entity.classroom.TrainingProgram;
import fu.sep490.g23.backend.entity.course.CourseLesson;
import fu.sep490.g23.backend.entity.course.CourseUnit;
import fu.sep490.g23.backend.entity.course.CourseUnitContentRef;
import fu.sep490.g23.backend.entity.course.InstructorLedCourse;
import fu.sep490.g23.backend.entity.course.InstructorLedCourseIdMap;
import fu.sep490.g23.backend.entity.course.enums.CourseUnitContentType;
import fu.sep490.g23.backend.entity.course.enums.InstructorLedCourseLegacyKind;
import fu.sep490.g23.backend.entity.course.enums.PackageStatus;
import fu.sep490.g23.backend.entity.curriculum.ContentBankItem;
import fu.sep490.g23.backend.entity.curriculum.CurriculumAssessmentRef;
import fu.sep490.g23.backend.entity.curriculum.CurriculumExerciseRef;
import fu.sep490.g23.backend.entity.curriculum.CurriculumFlashcardRef;
import fu.sep490.g23.backend.entity.curriculum.CurriculumMaterialRef;
import fu.sep490.g23.backend.entity.curriculum.CurriculumProgram;
import fu.sep490.g23.backend.entity.curriculum.CurriculumResourceRef;
import fu.sep490.g23.backend.entity.curriculum.CurriculumSessionPlan;
import fu.sep490.g23.backend.entity.curriculum.CurriculumUnit;
import fu.sep490.g23.backend.repository.course.CourseLessonRepository;
import fu.sep490.g23.backend.repository.course.CourseUnitContentRefRepository;
import fu.sep490.g23.backend.repository.course.CourseUnitRepository;
import fu.sep490.g23.backend.repository.course.InstructorLedCourseIdMapRepository;
import fu.sep490.g23.backend.repository.course.InstructorLedCourseRepository;
import fu.sep490.g23.backend.repository.curriculum.ContentBankItemRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Dual-writes legacy TrainingProgram / Curriculum* into Slice-5 canonical tables.
 * Preserves IDs: ILC.id = TP.id; unit/lesson/ref ids match curriculum_* ids.
 */
@Component
@RequiredArgsConstructor
public class InstructorLedCourseSync {

    private final InstructorLedCourseRepository instructorLedCourseRepository;
    private final InstructorLedCourseIdMapRepository idMapRepository;
    private final CourseUnitRepository courseUnitRepository;
    private final CourseLessonRepository courseLessonRepository;
    private final CourseUnitContentRefRepository contentRefRepository;
    private final ContentBankItemRepository contentBankItemRepository;
    private final InstructorLedCourseIdResolver idResolver;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public InstructorLedCourse syncFromTrainingProgram(TrainingProgram trainingProgram) {
        if (trainingProgram == null || trainingProgram.getId() == null) {
            return null;
        }
        CurriculumProgram curriculum = trainingProgram.getCurriculumProgram();
        InstructorLedCourse ilc = upsertInstructorLedCourse(trainingProgram, curriculum);
        upsertIdMap(InstructorLedCourseLegacyKind.TRAINING_PROGRAM, trainingProgram.getId(), ilc.getId());
        if (curriculum != null && curriculum.getId() != null) {
            boolean cpMapExisted = idMapRepository
                    .findByLegacyKindAndLegacyId(InstructorLedCourseLegacyKind.CURRICULUM_PROGRAM, curriculum.getId())
                    .isPresent();
            upsertIdMap(InstructorLedCourseLegacyKind.CURRICULUM_PROGRAM, curriculum.getId(), ilc.getId());
            if (!cpMapExisted) {
                syncCurriculumTree(curriculum, ilc);
            }
        }
        return ilc;
    }

    @Transactional
    public void syncCurriculumProgramFields(CurriculumProgram curriculum) {
        if (curriculum == null || curriculum.getId() == null) {
            return;
        }
        idResolver.resolveFromCurriculumProgramId(curriculum.getId()).ifPresent(ilc -> {
            applyCurriculumFields(ilc, curriculum);
            instructorLedCourseRepository.save(ilc);
        });
    }

    @Transactional
    public void syncCurriculumProgramTree(CurriculumProgram curriculum) {
        if (curriculum == null || curriculum.getId() == null) {
            return;
        }
        idResolver.resolveFromCurriculumProgramId(curriculum.getId())
                .ifPresent(ilc -> syncCurriculumTree(curriculum, ilc));
    }

    @Transactional
    public void syncUnit(CurriculumUnit unit) {
        if (unit == null || unit.getId() == null || unit.getProgram() == null) {
            return;
        }
        idResolver.resolveFromCurriculumProgramId(unit.getProgram().getId())
                .ifPresent(ilc -> upsertCourseUnit(unit, ilc));
    }

    @Transactional
    public void deleteUnit(Long unitId) {
        if (unitId == null) {
            return;
        }
        contentRefRepository.deleteAll(
                contentRefRepository.findByCourseUnitIdOrderBySequenceNumberAscIdAsc(unitId));
        courseLessonRepository.deleteAll(
                courseLessonRepository.findByCourseUnitIdOrderBySequenceNumberAscIdAsc(unitId));
        if (courseUnitRepository.existsById(unitId)) {
            courseUnitRepository.deleteById(unitId);
        }
    }

    @Transactional
    public void syncSessionPlan(CurriculumSessionPlan sessionPlan) {
        if (sessionPlan == null || sessionPlan.getId() == null || sessionPlan.getUnit() == null) {
            return;
        }
        syncUnit(sessionPlan.getUnit());
        if (!courseUnitRepository.existsById(sessionPlan.getUnit().getId())) {
            return;
        }
        upsertCourseLesson(sessionPlan);
    }

    @Transactional
    public void deleteSessionPlan(Long sessionPlanId) {
        if (sessionPlanId != null && courseLessonRepository.existsById(sessionPlanId)) {
            courseLessonRepository.deleteById(sessionPlanId);
        }
    }

    @Transactional
    public void syncResourceRef(CurriculumResourceRef ref) {
        if (ref == null || ref.getId() == null || ref.getUnit() == null) {
            return;
        }
        syncUnit(ref.getUnit());
        if (!courseUnitRepository.existsById(ref.getUnit().getId())) {
            return;
        }
        upsertContentRef(ref);
    }

    @Transactional
    public void deleteResourceRef(Long refId) {
        if (refId != null && contentRefRepository.existsById(refId)) {
            contentRefRepository.deleteById(refId);
        }
    }

    private void syncCurriculumTree(CurriculumProgram curriculum, InstructorLedCourse ilc) {
        applyCurriculumFields(ilc, curriculum);
        instructorLedCourseRepository.save(ilc);
        if (curriculum.getUnits() == null) {
            return;
        }
        for (CurriculumUnit unit : curriculum.getUnits()) {
            upsertCourseUnit(unit, ilc);
            if (unit.getSessionPlans() != null) {
                unit.getSessionPlans().forEach(this::upsertCourseLesson);
            }
            syncUnitContentRefs(unit);
        }
    }

    private void syncUnitContentRefs(CurriculumUnit unit) {
        if (unit.getMaterialRefs() != null) {
            unit.getMaterialRefs().forEach(this::upsertContentRef);
        }
        if (unit.getExerciseRefs() != null) {
            unit.getExerciseRefs().forEach(this::upsertContentRef);
        }
        if (unit.getAssessmentRefs() != null) {
            unit.getAssessmentRefs().forEach(this::upsertContentRef);
        }
        if (unit.getFlashcardRefs() != null) {
            unit.getFlashcardRefs().forEach(this::upsertContentRef);
        }
    }

    private InstructorLedCourse upsertInstructorLedCourse(TrainingProgram tp, CurriculumProgram cp) {
        if (!instructorLedCourseRepository.existsById(tp.getId())) {
            insertInstructorLedCourseRow(tp.getId());
            entityManager.flush();
        }
        InstructorLedCourse ilc = instructorLedCourseRepository.findById(tp.getId()).orElseThrow();
        ilc.setCode(tp.getCode());
        ilc.setSlug(tp.getSlug());
        ilc.setTitle(resolveTitle(tp, cp));
        ilc.setShortDescription(tp.getShortDescription());
        ilc.setDescription(tp.getDescription() != null ? tp.getDescription() : (cp == null ? null : cp.getOutcomes()));
        ilc.setBaseTuitionFeeVnd(tp.getPrice() == null ? BigDecimal.ZERO : tp.getPrice());
        ilc.setSaleTuitionFeeVnd(tp.getSalePrice());
        ilc.setDurationLabel(tp.getDuration());
        ilc.setThumbnailUrl(tp.getThumbnailUrl());
        ilc.setPublicationStatus(toPublicationStatus(tp.getStatus()));
        ilc.setFeatured(tp.isFeatured());
        ilc.setDisplayOrder(resolveDisplayOrder(tp, cp));
        applyCurriculumFields(ilc, cp);
        return instructorLedCourseRepository.save(ilc);
    }

    private void applyCurriculumFields(InstructorLedCourse ilc, CurriculumProgram cp) {
        if (cp == null) {
            return;
        }
        ilc.setExamType(blankToDefault(cp.getExamCategory(), "IELTS"));
        ilc.setProgramTrack(cp.getProgramTrack());
        ilc.setLevel(cp.getEntryLevel());
        ilc.setEntryLevel(cp.getEntryLevel());
        ilc.setEntryPlacementLevel(cp.getEntryPlacementLevel());
        ilc.setFocusSkills(cp.getFocusSkills());
        ilc.setTargetBand(cp.getTargetBand());
        ilc.setTargetScore(cp.getTargetScore());
        ilc.setLearningOutcomes(cp.getOutcomes());
        ilc.setTeacherGuide(cp.getTeacherGuide());
        ilc.setCreatedBy(cp.getSubmittedBy());
        ilc.setSubmittedAt(cp.getSubmittedAt());
        ilc.setReviewedBy(cp.getReviewedBy());
        ilc.setReviewedAt(cp.getReviewedAt());
        ilc.setReviewNote(cp.getReviewNote());
    }

    private void upsertCourseUnit(CurriculumUnit unit, InstructorLedCourse ilc) {
        if (!courseUnitRepository.existsById(unit.getId())) {
            insertCourseUnitRow(unit.getId(), ilc.getId());
            entityManager.flush();
        }
        CourseUnit courseUnit = courseUnitRepository.findById(unit.getId()).orElseThrow();
        courseUnit.setInstructorLedCourse(ilc);
        courseUnit.setCode("CU-" + unit.getId());
        courseUnit.setTitle(unit.getTitle());
        courseUnit.setDescription(unit.getDescription());
        courseUnit.setLearningObjectives(null);
        courseUnit.setSequenceNumber(unit.getDisplayOrder() == null ? 0 : unit.getDisplayOrder());
        courseUnitRepository.save(courseUnit);
    }

    private void upsertCourseLesson(CurriculumSessionPlan plan) {
        if (plan.getId() == null || plan.getUnit() == null || plan.getUnit().getId() == null) {
            return;
        }
        if (!courseLessonRepository.existsById(plan.getId())) {
            insertCourseLessonRow(plan.getId(), plan.getUnit().getId());
            entityManager.flush();
        }
        CourseLesson lesson = courseLessonRepository.findById(plan.getId()).orElseThrow();
        lesson.setCourseUnit(courseUnitRepository.getReferenceById(plan.getUnit().getId()));
        lesson.setCode("CL-" + plan.getId());
        lesson.setTitle(plan.getTitle());
        lesson.setDescription(plan.getDescription());
        lesson.setLearningObjectives(plan.getLearningObjectives());
        lesson.setSequenceNumber(plan.getSessionNumber() != null
                ? plan.getSessionNumber()
                : (plan.getDisplayOrder() == null ? 0 : plan.getDisplayOrder()));
        lesson.setEstimatedDurationMinutes(null);
        courseLessonRepository.save(lesson);
    }

    private void upsertContentRef(CurriculumResourceRef ref) {
        ContentRefTarget target = resolveContentRefTarget(ref);
        if (target == null) {
            return;
        }

        int sequence = ref.getDisplayOrder() == null ? 0 : ref.getDisplayOrder();
        if (!contentRefRepository.existsById(ref.getId())) {
            insertContentRefRow(
                    ref.getId(),
                    ref.getUnit().getId(),
                    target.contentType().name(),
                    sequence,
                    ref.getNote(),
                    target.learningResourceId(),
                    target.contentBankItemId()
            );
            entityManager.flush();
            return;
        }

        CourseUnitContentRef contentRef = contentRefRepository.findById(ref.getId()).orElseThrow();
        contentRef.setCourseUnit(courseUnitRepository.getReferenceById(ref.getUnit().getId()));
        contentRef.setContentType(target.contentType());
        contentRef.setSequenceNumber(sequence);
        contentRef.setNote(ref.getNote());
        if (target.learningResourceId() != null) {
            contentRef.setLearningResource(
                    entityManager.getReference(CenterMaterialLibraryItem.class, target.learningResourceId()));
            contentRef.setContentBankItem(null);
        } else {
            contentRef.setLearningResource(null);
            ContentBankItem bankItem = contentBankItemRepository.findById(target.contentBankItemId()).orElse(null);
            contentRef.setContentBankItem(bankItem);
        }
        contentRefRepository.save(contentRef);
    }

    private ContentRefTarget resolveContentRefTarget(CurriculumResourceRef ref) {
        if (ref instanceof CurriculumMaterialRef materialRef) {
            if (materialRef.getMaterial() == null || materialRef.getMaterial().getId() == null) {
                return null;
            }
            return new ContentRefTarget(
                    CourseUnitContentType.MATERIAL,
                    materialRef.getMaterial().getId(),
                    null
            );
        }
        if (ref instanceof CurriculumExerciseRef exerciseRef) {
            if (exerciseRef.getExercise() == null || exerciseRef.getExercise().getId() == null) {
                return null;
            }
            return new ContentRefTarget(
                    CourseUnitContentType.EXERCISE,
                    null,
                    exerciseRef.getExercise().getId()
            );
        }
        if (ref instanceof CurriculumAssessmentRef assessmentRef) {
            if (assessmentRef.getAssessment() == null || assessmentRef.getAssessment().getId() == null) {
                return null;
            }
            return new ContentRefTarget(
                    CourseUnitContentType.ASSESSMENT,
                    null,
                    assessmentRef.getAssessment().getId()
            );
        }
        if (ref instanceof CurriculumFlashcardRef flashcardRef) {
            if (flashcardRef.getFlashcardSet() == null || flashcardRef.getFlashcardSet().getId() == null) {
                return null;
            }
            return new ContentRefTarget(
                    CourseUnitContentType.FLASHCARD,
                    null,
                    flashcardRef.getFlashcardSet().getId()
            );
        }
        return null;
    }

    private void upsertIdMap(InstructorLedCourseLegacyKind kind, Long legacyId, Long ilcId) {
        InstructorLedCourseIdMap map = idMapRepository.findByLegacyKindAndLegacyId(kind, legacyId)
                .orElseGet(() -> InstructorLedCourseIdMap.builder()
                        .legacyKind(kind)
                        .legacyId(legacyId)
                        .build());
        map.setInstructorLedCourseId(ilcId);
        idMapRepository.save(map);
    }

    private static String resolveTitle(TrainingProgram tp, CurriculumProgram cp) {
        if (StringUtils.hasText(tp.getTitle())) {
            return tp.getTitle().trim();
        }
        return cp != null ? cp.getTitle() : tp.getTitle();
    }

    private static int resolveDisplayOrder(TrainingProgram tp, CurriculumProgram cp) {
        if (tp.getDisplayOrder() != null) {
            return tp.getDisplayOrder();
        }
        if (cp != null && cp.getDisplayOrder() != null) {
            return cp.getDisplayOrder();
        }
        return 0;
    }

    private static PackageStatus toPublicationStatus(PackageStatus status) {
        if (status == null) {
            return PackageStatus.DRAFT;
        }
        return switch (status) {
            case PUBLISHED -> PackageStatus.PUBLISHED;
            case ARCHIVED -> PackageStatus.ARCHIVED;
            default -> status;
        };
    }

    private static String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private void insertInstructorLedCourseRow(Long id) {
        LocalDateTime now = LocalDateTime.now();
        entityManager.createNativeQuery("""
                INSERT INTO instructor_led_courses (
                    id, code, slug, title, exam_type, base_tuition_fee_vnd,
                    publication_status, featured, display_order, created_at, updated_at
                ) VALUES (
                    :id, :code, :slug, :title, 'IELTS', 0,
                    'DRAFT', false, 0, :now, :now
                )
                ON CONFLICT (id) DO NOTHING
                """)
                .setParameter("id", id)
                .setParameter("code", "ILC-TMP-" + id)
                .setParameter("slug", "ilc-tmp-" + id)
                .setParameter("title", "Temporary ILC " + id)
                .setParameter("now", now)
                .executeUpdate();
    }

    private void insertCourseUnitRow(Long id, Long ilcId) {
        LocalDateTime now = LocalDateTime.now();
        entityManager.createNativeQuery("""
                INSERT INTO course_units (
                    id, instructor_led_course_id, code, title, sequence_number, created_at, updated_at
                ) VALUES (
                    :id, :ilcId, :code, :title, 0, :now, :now
                )
                ON CONFLICT (id) DO NOTHING
                """)
                .setParameter("id", id)
                .setParameter("ilcId", ilcId)
                .setParameter("code", "CU-" + id)
                .setParameter("title", "Unit " + id)
                .setParameter("now", now)
                .executeUpdate();
    }

    private void insertCourseLessonRow(Long id, Long unitId) {
        LocalDateTime now = LocalDateTime.now();
        entityManager.createNativeQuery("""
                INSERT INTO course_lessons (
                    id, course_unit_id, code, title, sequence_number, created_at, updated_at
                ) VALUES (
                    :id, :unitId, :code, :title, 0, :now, :now
                )
                ON CONFLICT (id) DO NOTHING
                """)
                .setParameter("id", id)
                .setParameter("unitId", unitId)
                .setParameter("code", "CL-" + id)
                .setParameter("title", "Lesson " + id)
                .setParameter("now", now)
                .executeUpdate();
    }

    private void insertContentRefRow(
            Long id,
            Long unitId,
            String contentType,
            int sequenceNumber,
            String note,
            Long learningResourceId,
            Long contentBankItemId
    ) {
        LocalDateTime now = LocalDateTime.now();
        entityManager.createNativeQuery("""
                INSERT INTO course_unit_content_refs (
                    id, course_unit_id, content_type, sequence_number, note,
                    learning_resource_id, content_bank_item_id, created_at, updated_at
                ) VALUES (
                    :id, :unitId, :contentType, :sequenceNumber, :note,
                    :learningResourceId, :contentBankItemId, :now, :now
                )
                ON CONFLICT (id) DO NOTHING
                """)
                .setParameter("id", id)
                .setParameter("unitId", unitId)
                .setParameter("contentType", contentType)
                .setParameter("sequenceNumber", sequenceNumber)
                .setParameter("note", note)
                .setParameter("learningResourceId", learningResourceId)
                .setParameter("contentBankItemId", contentBankItemId)
                .setParameter("now", now)
                .executeUpdate();
    }

    private record ContentRefTarget(
            CourseUnitContentType contentType,
            Long learningResourceId,
            Long contentBankItemId
    ) {
    }
}
