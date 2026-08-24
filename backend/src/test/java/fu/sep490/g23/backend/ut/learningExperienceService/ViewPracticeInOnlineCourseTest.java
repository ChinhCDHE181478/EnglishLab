package fu.sep490.g23.backend.ut.learningExperienceService;

import fu.sep490.g23.backend.dto.response.classroom.ClassroomPracticeResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.assessment.AssessmentSubmission;
import fu.sep490.g23.backend.entity.assessment.CourseAssessment;
import fu.sep490.g23.backend.entity.assessment.ExerciseBankItem;
import fu.sep490.g23.backend.entity.assessment.enums.AiEvaluationMode;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentType;
import fu.sep490.g23.backend.entity.assessment.enums.SubmissionStatus;
import fu.sep490.g23.backend.entity.classroom.ClassroomOffering;
import fu.sep490.g23.backend.entity.course.CourseModule;
import fu.sep490.g23.backend.entity.course.LearningPackage;
import fu.sep490.g23.backend.entity.course.Lesson;
import fu.sep490.g23.backend.entity.course.LessonProgress;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.entity.course.enums.LessonProgressStatus;
import fu.sep490.g23.backend.entity.curriculum.CurriculumExerciseRef;
import fu.sep490.g23.backend.entity.curriculum.CurriculumProgram;
import fu.sep490.g23.backend.entity.curriculum.CurriculumUnit;
import fu.sep490.g23.backend.repository.assessment.AssessmentSubmissionRepository;
import fu.sep490.g23.backend.repository.assessment.CourseAssessmentRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomEnrollmentRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomOfferingRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomPracticeAttemptHistoryRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomPracticeAttemptRepository;
import fu.sep490.g23.backend.repository.course.LessonProgressRepository;
import fu.sep490.g23.backend.security.ClassroomAccessHelper;
import fu.sep490.g23.backend.service.assessment.AssessmentPassingThresholdResolver;
import fu.sep490.g23.backend.service.classroom.ClassroomRegistrationSupport;
import fu.sep490.g23.backend.service.classroom.impl.ClassroomPracticeServiceImpl;
import fu.sep490.g23.backend.service.course.CourseProgressionGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ViewPracticeInOnlineCourseTest {

    // ===== Mocks cho ClassroomPracticeServiceImpl (test listForLearner) =====
    @Mock private ClassroomOfferingRepository offeringRepository;
    @Mock private ClassroomEnrollmentRepository enrollmentRepository;
    @Mock private ClassroomPracticeAttemptRepository attemptRepository;
    @Mock private ClassroomPracticeAttemptHistoryRepository attemptHistoryRepository;
    @Mock private ClassroomAccessHelper accessHelper;

    @InjectMocks
    private ClassroomPracticeServiceImpl service;

    // ===== Mocks cho CourseProgressionGuard (test nhß║¡n diß╗çn Module Test) =====
    @Mock private LessonProgressRepository lessonProgressRepository;
    @Mock private CourseAssessmentRepository courseAssessmentRepository;
    @Mock private AssessmentSubmissionRepository assessmentSubmissionRepository;
    @Mock private AssessmentPassingThresholdResolver passingThresholdResolver;

    // Khß╗ƒi tß║ío thß╗º c├┤ng v├¼ @InjectMocks chß╗ë ├íp dß╗Ñng cho 1 SUT
    private CourseProgressionGuard guard;

    private User learner;
    private ClassroomOffering offering;
    private CurriculumUnit unitModule;
    private CurriculumExerciseRef exerciseRef;
    private Long offeringId;
    private Long moduleId;
    private String learnerEmail;

    // ===== Entities cho test CourseProgressionGuard =====
    private OnlineCourse onlineCourse;
    private CourseModule moduleForProgression;
    private CourseAssessment moduleTestAssessment;

    @BeforeEach
    void setUp() {
        // ===== Learner ─æ├ú ghi danh (PRE-3) =====
        learnerEmail = "learner@englishlab.com";
        learner = User.builder()
                .id(1L)
                .email(learnerEmail)
                .fullName("Nguyen Van A")
                .build();

        // ===== Course / Offering =====
        offeringId = 100L;
        // PRE-3: learner ─æ├ú ghi danh kh├│a hß╗ìc vß╗¢i trß║íng th├íi ACTIVE
        // PRE-4 / BR-57: module ─æang truy cß║¡p ─æ├ú ─æ╞░ß╗úc mß╗ƒ kh├│a (trß║íng th├íi AVAILABLE)
        LearningPackage learningPackage = LearningPackage.builder()
                .id(10L)
                .title("IELTS Foundation Course")
                .slug("ielts-foundation")
                .build();

        // ===== Module ─æ╞░ß╗úc mß╗ƒ kh├│a (PRE-4 / BR-57) vß╗¢i danh s├ích b├ái luyß╗çn tß║¡p =====
        moduleId = 500L;
        unitModule = CurriculumUnit.builder()
                .id(moduleId)
                .title("Module 1: Greetings & Introductions")
                .description("Luyß╗çn tß║¡p c├íc b├ái tß║¡p vß╗ü ch├áo hß╗Åi")
                .displayOrder(1)
                .exerciseRefs(new ArrayList<>())
                .build();

        // ===== Curriculum c├│ cß║Ñu h├¼nh b├ái luyß╗çn tß║¡p =====
        CurriculumProgram program = CurriculumProgram.builder()
                .id(50L)
                .title("IELTS Speaking Program")
                .build();
        program.setUnits(new ArrayList<>(List.of(unitModule)));

        unitModule.setProgram(program);

        // ===== Exercise trong module =====
        ExerciseBankItem exercise = ExerciseBankItem.builder()
                .id(701L)
                .title("N├│i vß╗ü bß║ún th├ón trong 2 ph├║t")
                .skill("SPEAKING")
                .exerciseType("SPEAKING_PRACTICE")
                .prompt("H├úy giß╗¢i thiß╗çu vß╗ü bß║ún th├ón bß║ín trong 2 ph├║t")
                .active(true)
                .build();
        CurriculumExerciseRef ref = CurriculumExerciseRef.builder()
                .id(700L)
                .unit(unitModule)
                .exercise(exercise)
                .displayOrder(1)
                .note("B├ái luyß╗çn tß║¡p ─æß║ºu ti├¬n cß╗ºa module")
                .build();
        unitModule.getExerciseRefs().add(ref);
        exerciseRef = ref;

        offering = ClassroomOffering.builder()
                .id(offeringId)
                .learningPackage(learningPackage)
                .curriculumProgram(program)
                .build();

        // ====== Entities cho test Case 2: Nhß║¡n diß╗çn Module Test ======
        // Course c├│ targetBand = 8.5 ΓåÆ threshold = 8.5 - 0.5 = 8.0 (theo AssessmentPassingThresholdResolver)
        onlineCourse = OnlineCourse.builder()
                .id(10L)
                .learningPackage(LearningPackage.builder()
                        .id(10L)
                        .title("IELTS Master")
                        .slug("ielts-master")
                        .build())
                .targetBand(8.5)
                .modules(new ArrayList<>())
                .build();

        // Module chß╗⌐a b├ái luyß╗çn tß║¡p cuß╗æi c├╣ng l├á Module Test
        Lesson lessonInModule = Lesson.builder()
                .id(700L)
                .title("Speaking Practice 1")
                .displayOrder(1)
                .build();
        moduleForProgression = CourseModule.builder()
                .id(800L)
                .onlineCourse(onlineCourse)
                .title("Module 1: Speaking Advanced")
                .displayOrder(1)
                .lessons(new ArrayList<>(List.of(lessonInModule)))
                .build();
        lessonInModule.setModule(moduleForProgression);
        onlineCourse.getModules().add(moduleForProgression);

        // B├ái test c├│ cß║Ñu h├¼nh targetScore = 8.0
        // progressKey = "" ─æß╗â CourseProgressionGuard.isAssessmentPassed d├╣ng nh├ính
        // findTopByAssessmentAndStudentOrderBySubmittedAtDesc (kh├┤ng qua progressKey)
        moduleTestAssessment = CourseAssessment.builder()
                .id(900L)
                .onlineCourse(onlineCourse)
                .module(moduleForProgression)
                .title("Module 1 Final Test - Writing")
                .type(AssessmentType.MODULE_TEST)
                .skill(AssessmentSkill.WRITING)
                .aiEvaluationMode(AiEvaluationMode.ESTIMATED_BAND)
                .passingScore(BigDecimal.valueOf(8.0))  // targetScore = 8.0
                .maxScore(BigDecimal.valueOf(9.0))
                .progressKey("")
                .active(true)
                .displayOrder(1)
                .build();

        // Khß╗ƒi tß║ío CourseProgressionGuard thß╗º c├┤ng
        guard = new CourseProgressionGuard(
                lessonProgressRepository,
                courseAssessmentRepository,
                assessmentSubmissionRepository,
                passingThresholdResolver
        );
    }

    /**
     * Test case: Lß║Ñy danh s├ích b├ái luyß╗çn tß║¡p trong hß╗ìc phß║ºn hß╗úp lß╗ç
     * Pre:
     *   - Learner ─æ├ú ghi danh kh├│a hß╗ìc (PRE-3)
     *   - Module ─æang truy cß║¡p ─æ├ú ─æ╞░ß╗úc mß╗ƒ kh├│a (PRE-4 / BR-57)
     *   - Module c├│ cß║Ñu h├¼nh danh s├ích b├ái luyß╗çn tß║¡p
     * Input: learner_id, course_id (=offeringId), module_id hß╗úp lß╗ç
     * Expected:
     *   - Service trß║ú vß╗ü danh s├ích c├íc b├ái luyß╗çn tß║¡p thuß╗Öc module.
     *   - Mß╗ùi item phß║ún ├ính ─æ├║ng th├┤ng tin unit, exercise, trß║íng th├íi completed=false.
     */
    @Test
    void listPracticeInOnlineCourse() {
        // Arrange
        when(accessHelper.requireUser(learnerEmail)).thenReturn(learner);
        when(enrollmentRepository.existsByStudentIdAndClassroomOfferingIdAndRegistrationStatusIn(
                learner.getId(), offeringId, ClassroomRegistrationSupport.HAS_LEARNING_ACCESS))
                .thenReturn(true);
        when(offeringRepository.findById(offeringId)).thenReturn(Optional.of(offering));
        when(attemptRepository.findByClassroomOfferingIdAndStudentId(offeringId, learner.getId()))
                .thenReturn(List.of());
        when(attemptHistoryRepository.findByClassroomOfferingIdAndStudentIdAndExerciseIdOrderByCompletedAtDesc(
                offeringId, learner.getId(), exerciseRef.getExercise().getId()))
                .thenReturn(List.of());

        // Act
        List<ClassroomPracticeResponse> response = service.listForLearner(offeringId, learnerEmail);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response).hasSize(1);

        ClassroomPracticeResponse item = response.get(0);
        assertThat(item.getClassroomOfferingId()).isEqualTo(offeringId);
        assertThat(item.getClassroomTitle()).isEqualTo("IELTS Foundation Course");
        assertThat(item.getUnitId()).isEqualTo(moduleId);
        assertThat(item.getUnitTitle()).isEqualTo("Module 1: Greetings & Introductions");
        assertThat(item.getUnitDisplayOrder()).isEqualTo(1);
        assertThat(item.getExerciseId()).isEqualTo(701L);
        assertThat(item.getTitle()).isEqualTo("N├│i vß╗ü bß║ún th├ón trong 2 ph├║t");
        assertThat(item.getSkill()).isEqualTo("SPEAKING");
        assertThat(item.getExerciseType()).isEqualTo("SPEAKING_PRACTICE");
        assertThat(item.getInstruction()).isEqualTo("H├úy giß╗¢i thiß╗çu vß╗ü bß║ún th├ón bß║ín trong 2 ph├║t");
        assertThat(item.getNote()).isEqualTo("B├ái luyß╗çn tß║¡p ─æß║ºu ti├¬n cß╗ºa module");
        assertThat(item.isCompleted()).isFalse();
        assertThat(item.getResponseText()).isNull();
        assertThat(item.getCompletedAt()).isNull();
        assertThat(item.getAttemptCount()).isZero();
        assertThat(item.getLastScorePercent()).isNull();
    }

    /**
     * Test case 2: Nhß║¡n diß╗çn b├ái kiß╗âm tra cuß╗æi hß╗ìc phß║ºn v├á t├¡nh ─æiß╗âm ─æß║ít
     * Pre:
     *   - Module chß╗⌐a b├ái tß║¡p luyß╗çn tß║¡p cuß╗æi c├╣ng l├á Module Test
     *   - B├ái test c├│ cß║Ñu h├¼nh targetScore = 8.0
     * Input: learner_id, module_id
     * Expected:
     *   - Hß╗ç thß╗æng nhß║¡n diß╗çn ─æ╞░ß╗úc b├ái Module Test thuß╗Öc module
     *   - T├¡nh ─æ╞░ß╗úc ng╞░ß╗íng ─æß║ít = 8.0
     *   - Submission c├│ aiScore = 8.0 ΓåÆ ─æß║ít y├¬u cß║ºu ΓåÆ canAdvancePastModule = true
     */
    @Test
    void recognizeModuleFinalTest_LearnerMeetsTargetScore() {
        // Arrange
        // ─Éß║úm bß║úo lesson ─æ├ú ho├án th├ánh (─æß║ºu v├áo ─æß╗â b├ái Module Test ─æ╞░ß╗úc ph├⌐p submit)
        Long lessonId = moduleForProgression.getLessons().get(0).getId();
        LessonProgress completedLessonProgress = new LessonProgress();
        when(lessonProgressRepository
                .findByStudentAndLessonIdInAndStatus(
                        learner,
                        Set.of(lessonId),
                        LessonProgressStatus.COMPLETED))
                .thenReturn(List.of(completedLessonProgress));

        // Nhß║¡n diß╗çn module c├│ 1 b├ái Module Test active
        when(courseAssessmentRepository.findByModuleAndActiveTrueOrderByDisplayOrderAscIdAsc(moduleForProgression))
                .thenReturn(List.of(moduleTestAssessment));

        // ===== Nhß║¡n diß╗çn submission cß╗ºa learner cho b├ái Module Test =====
        when(assessmentSubmissionRepository.findTopByAssessmentAndStudentOrderBySubmittedAtDesc(
                moduleTestAssessment, learner))
                .thenReturn(Optional.of(
                        AssessmentSubmission.builder()
                                .id(999L)
                                .assessment(moduleTestAssessment)
                                .student(learner)
                                .aiScore(BigDecimal.valueOf(8.0))  // Bß║░NG ng╞░ß╗íng ΓåÆ ─æß║ít
                                .status(SubmissionStatus.AI_EVALUATED)
                                .build()
                ));

        // ===== T├¡nh ─æiß╗âm ─æß║ít =====
        // targetScore = 8.0, score = 8.0 ΓåÆ 8.0.compareTo(8.0) >= 0 ΓåÆ ─æß║ít
        when(passingThresholdResolver.isScorePassing(BigDecimal.valueOf(8.0), moduleTestAssessment))
                .thenReturn(true);

        // Act
        boolean canAdvance = guard.canAdvancePastModule(learner, moduleForProgression);

        // Assert
        assertThat(canAdvance).isTrue();
    }

    /**
     * Test case 3: Hß╗ìc phß║ºn ch╞░a cß║Ñu h├¼nh b├ái luyß╗çn tß║¡p
     * Pre:
     *   - Learner ─æ├ú ghi danh kh├│a hß╗ìc (PRE-3)
     *   - Module ─æang truy cß║¡p ─æ├ú ─æ╞░ß╗úc mß╗ƒ kh├│a (PRE-4 / BR-57)
     *   - Module CH╞»A c├│ cß║Ñu h├¼nh danh s├ích b├ái luyß╗çn tß║¡p (exerciseRefs = [])
     * Input: learner_id, module_id (ch╞░a c├│ b├ái luyß╗çn tß║¡p)
     * Expected:
     *   - Service trß║ú vß╗ü danh s├ích rß╗ùng (empty list)
     *   - Kh├┤ng throw exception
     */
    @Test
    void listPracticeInOnlineCourse_ModuleWithoutPractice() {
        // Arrange
        // Tß║ío offering ri├¬ng c├│ curriculum chß╗ë chß╗⌐a 1 module rß╗ùng (kh├┤ng c├│ b├ái luyß╗çn tß║¡p)
        Long emptyModuleId = 600L;
        Long emptyOfferingId = 200L;

        CurriculumUnit emptyModule = CurriculumUnit.builder()
                .id(emptyModuleId)
                .title("Module 2: Grammar Basics")
                .description("Module ch╞░a cß║Ñu h├¼nh b├ái luyß╗çn tß║¡p")
                .displayOrder(1)
                .exerciseRefs(new ArrayList<>())
                .build();

        CurriculumProgram programEmpty = CurriculumProgram.builder()
                .id(51L)
                .title("Grammar Program")
                .build();
        programEmpty.setUnits(new ArrayList<>(List.of(emptyModule)));
        emptyModule.setProgram(programEmpty);

        ClassroomOffering emptyOffering = ClassroomOffering.builder()
                .id(emptyOfferingId)
                .learningPackage(LearningPackage.builder()
                        .id(11L)
                        .title("Grammar Basics")
                        .slug("grammar-basics")
                        .build())
                .curriculumProgram(programEmpty)
                .build();

        // Mock: learner hß╗úp lß╗ç v├á c├│ quyß╗ün truy cß║¡p offering n├áy
        when(accessHelper.requireUser(learnerEmail)).thenReturn(learner);
        when(enrollmentRepository.existsByStudentIdAndClassroomOfferingIdAndRegistrationStatusIn(
                learner.getId(), emptyOfferingId, ClassroomRegistrationSupport.HAS_LEARNING_ACCESS))
                .thenReturn(true);
        when(offeringRepository.findById(emptyOfferingId)).thenReturn(Optional.of(emptyOffering));
        when(attemptRepository.findByClassroomOfferingIdAndStudentId(emptyOfferingId, learner.getId()))
                .thenReturn(List.of());

        // Act
        List<ClassroomPracticeResponse> response = service.listForLearner(emptyOfferingId, learnerEmail);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response).isEmpty();
    }

    /**
     * Test case 4: Hß╗ìc vi├¬n ch╞░a ghi danh kh├│a hß╗ìc ΓÇö kh├┤ng thß╗â truy cß║¡p b├ái luyß╗çn tß║¡p
     * Pre:
     *   - Learner ch╞░a mua / ch╞░a ghi danh v├áo kh├│a hß╗ìc (PRE-3 KH├öNG thoß║ú)
     *   - Learner l├á user hß╗úp lß╗ç (─æ├ú ─æ─âng nhß║¡p)
     * Input: learner_id (ch╞░a ghi danh), course_id (=offeringId), module_id
     * Expected:
     *   - Service throw RuntimeException vß╗¢i message "Bß║ín kh├┤ng thuß╗Öc lß╗¢p hß╗ìc n├áy."
     *   - Kh├┤ng truy cß║¡p ─æ╞░ß╗úc danh s├ích b├ái luyß╗çn tß║¡p
     */
    @Test
    void listPracticeInOnlineCourse_LearnerNotEnrolled() {
        // Arrange
        // Learner l├á user hß╗úp lß╗ç (─æ├ú ─æ─âng nhß║¡p)
        when(accessHelper.requireUser(learnerEmail)).thenReturn(learner);

        // Nh╞░ng CH╞»A c├│ enrollment vß╗¢i trß║íng th├íi truy cß║¡p hß╗ìc tß║¡p cho offering n├áy
        // ΓåÆ ClassroomPracticeServiceImpl.requireLearnerAccess sß║╜ throw RuntimeException
        when(enrollmentRepository.existsByStudentIdAndClassroomOfferingIdAndRegistrationStatusIn(
                learner.getId(), offeringId, ClassroomRegistrationSupport.HAS_LEARNING_ACCESS))
                .thenReturn(false);

        // Act + Assert
        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> service.listForLearner(offeringId, learnerEmail)
        )
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Bß║ín kh├┤ng thuß╗Öc lß╗¢p hß╗ìc n├áy");
    }


    @Test
    void recognizeLockedModule_PreviousModuleTestNotPassed() {
        // Arrange ΓÇö Setup cho kß╗ïch bß║ún kh├│a hß╗ìc 2 module
        // Course c├│ targetBand = 8.5 ΓåÆ threshold = 8.0
        OnlineCourse multiModuleCourse = OnlineCourse.builder()
                .id(20L)
                .learningPackage(LearningPackage.builder()
                        .id(20L)
                        .title("IELTS Complete")
                        .slug("ielts-complete")
                        .build())
                .targetBand(8.5)
                .modules(new ArrayList<>())
                .build();

        // Module 1 ΓÇö ─æ├ú c├│ lesson nh╞░ng learner ch╞░a ho├án th├ánh
        Lesson module1Lesson = Lesson.builder()
                .id(901L)
                .title("Speaking Practice - Module 1")
                .displayOrder(1)
                .build();
        CourseModule module1 = CourseModule.builder()
                .id(901L)
                .onlineCourse(multiModuleCourse)
                .title("Module 1: Foundations")
                .displayOrder(1)
                .lessons(new ArrayList<>(List.of(module1Lesson)))
                .build();
        module1Lesson.setModule(module1);

        // Module 2 ΓÇö module ─æang bß╗ï kh├│a (v├¼ Module 1 ch╞░a ─æß║ít Module Test)
        Lesson module2Lesson = Lesson.builder()
                .id(902L)
                .title("Speaking Practice - Module 2")
                .displayOrder(1)
                .build();
        CourseModule module2 = CourseModule.builder()
                .id(902L)
                .onlineCourse(multiModuleCourse)
                .title("Module 2: Advanced")
                .displayOrder(2)
                .lessons(new ArrayList<>(List.of(module2Lesson)))
                .build();
        module2Lesson.setModule(module2);
        multiModuleCourse.getModules().add(module1);
        multiModuleCourse.getModules().add(module2);

        // Module Test cß╗ºa Module 1 (ch╞░a ─æß║ít)
        CourseAssessment module1Test = CourseAssessment.builder()
                .id(901L)
                .onlineCourse(multiModuleCourse)
                .module(module1)
                .title("Module 1 Final Test")
                .type(AssessmentType.MODULE_TEST)
                .skill(AssessmentSkill.WRITING)
                .aiEvaluationMode(AiEvaluationMode.ESTIMATED_BAND)
                .passingScore(BigDecimal.valueOf(8.0))
                .maxScore(BigDecimal.valueOf(9.0))
                .progressKey("")
                .active(true)
                .displayOrder(1)
                .build();

        // ===== Mock dependencies cho CourseProgressionGuard.canAdvancePastModule(module1) =====

        // 1) Lessons trong Module 1 CH╞»A ho├án th├ánh ΓåÆ trß║ú empty list
        Long module1LessonId = module1.getLessons().get(0).getId();
        when(lessonProgressRepository
                .findByStudentAndLessonIdInAndStatus(
                        learner,
                        Set.of(module1LessonId),
                        LessonProgressStatus.COMPLETED))
                .thenReturn(List.of()); // empty ΓåÆ lesson ch╞░a ho├án th├ánh

        // 2) courseAssessmentRepository kh├┤ng cß║ºn stubbing - guard return false sß╗¢m ß╗ƒ
        //    areModuleLessonsCompleted() tr╞░ß╗¢c khi truy cß║¡p v├áo courseAssessmentRepository.
        lenient().when(courseAssessmentRepository.findByModuleAndActiveTrueOrderByDisplayOrderAscIdAsc(module1))
                .thenReturn(List.of(module1Test));

        // Act ΓÇö gß╗ìi guard.canAdvancePastModule cho Module 1
        boolean canAdvanceModule1 = guard.canAdvancePastModule(learner, module1);

        // Assert ΓÇö Module 1 ch╞░a thß╗â v╞░ß╗út qua ΓåÆ Module 2 bß╗ï kh├│a
        assertThat(canAdvanceModule1).isFalse();
    }
}
