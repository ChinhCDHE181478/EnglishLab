package fu.sap490.g23.backend.seed;

import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.assessment.ExerciseBankItem;
import fu.sap490.g23.backend.entity.assessment.enums.AiEvaluationMode;
import fu.sap490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sap490.g23.backend.entity.assessment.enums.AssessmentType;
import fu.sap490.g23.backend.entity.classroom.*;
import fu.sap490.g23.backend.entity.classroom.enums.*;
import fu.sap490.g23.backend.entity.course.LearningPackage;
import fu.sap490.g23.backend.entity.course.PackageType;
import fu.sap490.g23.backend.entity.course.enums.PackageStatus;
import fu.sap490.g23.backend.entity.course.enums.PackageTypeCode;
import fu.sap490.g23.backend.entity.curriculum.*;
import fu.sap490.g23.backend.entity.enums.RoleEnum;
import fu.sap490.g23.backend.repository.UserRepository;
import fu.sap490.g23.backend.repository.assessment.ExerciseBankItemRepository;
import fu.sap490.g23.backend.repository.classroom.*;
import fu.sap490.g23.backend.repository.course.LearningPackageRepository;
import fu.sap490.g23.backend.repository.course.PackageTypeRepository;
import fu.sap490.g23.backend.repository.curriculum.CurriculumProgramRepository;
import fu.sap490.g23.backend.repository.curriculum.CurriculumUnitRepository;
import fu.sap490.g23.backend.repository.curriculum.FlashcardSetRepository;
import fu.sap490.g23.backend.repository.curriculum.AssessmentBankItemRepository;
import fu.sap490.g23.backend.service.user.UserRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Component
@Order(200)
@RequiredArgsConstructor
public class ToeicShowcaseClassroomSeeder implements CommandLineRunner {

    private static final String LEARNER_EMAIL = "0386852628z@gmail.com";
    private static final String TEACHER_EMAIL = "classroom.teacher1@englishlab.vn";
    private static final String CURRICULUM_SLUG = "toeic-650-complete-virtual-v1";
    private static final String TRAINING_SLUG = "toeic-650-complete-training";
    private static final String PACKAGE_SLUG = "toeic-650-showcase-class-0386852628z";
    private static final String CLASS_TITLE = "TOEIC 650 Complete - Lớp thực hành đầy đủ";
    private static final String MATERIAL_BASE_URL = "http://localhost:8080/demo/toeic/";
    private static final String LEGACY_MODULE_TEST_TITLE = "TOEIC Part 5 Mini Module Test - Workplace English";
    private static final String UNIT_PROGRESS_CHECK_TITLE = "TOEIC 650 Unit 5 Progress Check - Incomplete Sentences";
    private static final String UNIT_PROGRESS_CHECK_ANSWER_KEY = """
            {"1":"B","2":"C","3":"A","4":"D","5":"B","6":"A","7":"C","8":"D","9":"B","10":"A"}
            """;
    private static final String UNIT_PROGRESS_CHECK_UI_CONFIG = """
            {
              "durationMinutes": 15,
              "parts": [{
                "key": "part_5",
                "part": 5,
                "title": "Incomplete Sentences",
                "instructions": "Choose the word or phrase that best completes each sentence.",
                "questionGroups": [{
                  "title": "Questions 1-10",
                  "type": "single_choice",
                  "questions": [
                    {"number":1,"prompt":"The conference room must be ------- before the client presentation begins.","options":[{"value":"A","label":"prepare"},{"value":"B","label":"prepared"},{"value":"C","label":"preparing"},{"value":"D","label":"preparation"}]},
                    {"number":2,"prompt":"Ms. Ortega will review the contract ------- sending it to the legal department.","options":[{"value":"A","label":"during"},{"value":"B","label":"among"},{"value":"C","label":"before"},{"value":"D","label":"until"}]},
                    {"number":3,"prompt":"The new software allows employees to process customer orders more -------.","options":[{"value":"A","label":"efficiently"},{"value":"B","label":"efficient"},{"value":"C","label":"efficiency"},{"value":"D","label":"efficientest"}]},
                    {"number":4,"prompt":"Neither the manager nor the assistants ------- available for comment yesterday.","options":[{"value":"A","label":"is"},{"value":"B","label":"has been"},{"value":"C","label":"was"},{"value":"D","label":"were"}]},
                    {"number":5,"prompt":"Customers may request a full refund ------- they present the original receipt.","options":[{"value":"A","label":"despite"},{"value":"B","label":"provided that"},{"value":"C","label":"whereas"},{"value":"D","label":"because of"}]},
                    {"number":6,"prompt":"The marketing team has not ------- decided which design will be used for the campaign.","options":[{"value":"A","label":"yet"},{"value":"B","label":"ever"},{"value":"C","label":"soon"},{"value":"D","label":"still"}]},
                    {"number":7,"prompt":"All expense reports should be submitted ------- the end of the month.","options":[{"value":"A","label":"at"},{"value":"B","label":"from"},{"value":"C","label":"by"},{"value":"D","label":"between"}]},
                    {"number":8,"prompt":"The factory increased production to meet the ------- demand for electric vehicles.","options":[{"value":"A","label":"growth"},{"value":"B","label":"grew"},{"value":"C","label":"growingly"},{"value":"D","label":"growing"}]},
                    {"number":9,"prompt":"Mr. Patel is responsible for ------- that all safety procedures are followed.","options":[{"value":"A","label":"ensure"},{"value":"B","label":"ensuring"},{"value":"C","label":"ensured"},{"value":"D","label":"ensures"}]},
                    {"number":10,"prompt":"The shipment arrived on schedule ------- the severe weather conditions.","options":[{"value":"A","label":"despite"},{"value":"B","label":"because"},{"value":"C","label":"unless"},{"value":"D","label":"throughout"}]}
                  ]
                }]
              }]
            }
            """;

    private static final List<UnitSeed> UNIT_SEEDS = List.of(
            new UnitSeed("Photographs & mô tả hành động", "Listening Part 1", "unit-1-photographs.txt", "present continuous,location,photographs"),
            new UnitSeed("Question-Response", "Listening Part 2", "unit-2-question-response.txt", "question words,indirect responses"),
            new UnitSeed("Conversations", "Listening Part 3", "unit-3-conversations.txt", "scheduling,delivery,customer service"),
            new UnitSeed("Short Talks", "Listening Part 4", "unit-4-talks.txt", "announcements,voicemail,next action"),
            new UnitSeed("Incomplete Sentences", "Reading Part 5", "unit-5-incomplete-sentences.txt", "grammar,word forms,collocations"),
            new UnitSeed("Text Completion", "Reading Part 6", "unit-6-text-completion.txt", "email,memo,connectors"),
            new UnitSeed("Reading Comprehension", "Reading Part 7", "unit-7-reading-comprehension.txt", "single,double,triple passages"),
            new UnitSeed("Full Test Strategy", "TOEIC 2-hour simulation", "unit-8-mock-test.txt", "pacing,error log,test strategy")
    );

    private final UserRepository userRepository;
    private final UserRoleService userRoleService;
    private final PasswordEncoder passwordEncoder;
    private final DemoLearnerOnboardingSupport onboardingSupport;
    private final PackageTypeRepository packageTypeRepository;
    private final LearningPackageRepository learningPackageRepository;
    private final CurriculumProgramRepository curriculumProgramRepository;
    private final CurriculumUnitRepository curriculumUnitRepository;
    private final CenterMaterialLibraryItemRepository centerMaterialRepository;
    private final ExerciseBankItemRepository exerciseRepository;
    private final FlashcardSetRepository flashcardSetRepository;
    private final AssessmentBankItemRepository assessmentBankItemRepository;
    private final TrainingProgramRepository trainingProgramRepository;
    private final ClassroomOfferingRepository offeringRepository;
    private final ClassroomSessionRepository sessionRepository;
    private final ClassroomEnrollmentRepository enrollmentRepository;
    private final ClassroomTeacherAssignmentRepository teacherAssignmentRepository;
    private final ClassroomSyllabusItemRepository syllabusRepository;
    private final ClassroomMaterialRepository classroomMaterialRepository;
    private final ClassroomHomeworkRepository homeworkRepository;
    private final ClassroomAnnouncementRepository announcementRepository;
    private final ClassroomGradebookEntryRepository gradebookRepository;

    @Value("${app.seed.showcase-classroom.enabled:true}")
    private boolean enabled;

    @Override
    @Transactional
    public void run(String... args) {
        if (!enabled) {
            return;
        }

        User learner = ensureUser(LEARNER_EMAIL, "Học viên EnglishLab", RoleEnum.LEARNER);
        User teacher = ensureUser(TEACHER_EMAIL, "Nguyễn Văn Teacher", RoleEnum.TEACHER);
        PackageType classroomType = packageTypeRepository.findByCode(PackageTypeCode.CLASSROOM)
                .orElseGet(() -> packageTypeRepository.save(PackageType.builder()
                        .code(PackageTypeCode.CLASSROOM)
                        .name("Classroom")
                        .description("Lớp học trực tiếp hoặc trực tuyến có giáo viên.")
                        .active(true)
                        .build()));

        CurriculumProgram curriculum = ensureCurriculum(teacher);
        List<CurriculumUnit> units = curriculumUnitRepository.findByProgramIdOrderByDisplayOrderAscIdAsc(curriculum.getId());
        synchronizeCurriculumResources(units, teacher);
        units = curriculumUnitRepository.findByProgramIdOrderByDisplayOrderAscIdAsc(curriculum.getId());
        TrainingProgram trainingProgram = ensureTrainingProgram(curriculum);
        ClassroomOffering offering = ensureOffering(classroomType, trainingProgram, curriculum, teacher);

        ensureTeacherAssignment(offering, teacher);
        ensureEnrollment(offering, learner, teacher);
        List<ClassroomSession> sessions = ensureSessions(offering, teacher, units);
        ensureSyllabus(offering, units, sessions);
        ensureClassroomMaterials(offering, units, teacher);
        AssessmentBankItem unitProgressCheck = ensureUnitProgressCheckBankItem();
        ensureCurriculumAssessment(units.get(4), unitProgressCheck);
        ensureHomework(offering, units, sessions, teacher, unitProgressCheck);
        ensureAnnouncement(offering, teacher);
        ensureGradebook(offering, learner, teacher);
    }

    private CurriculumProgram ensureCurriculum(User teacher) {
        CurriculumProgram program = curriculumProgramRepository.findBySlug(CURRICULUM_SLUG)
                .orElseGet(() -> curriculumProgramRepository.save(CurriculumProgram.builder()
                        .title("TOEIC 650 Complete - Virtual Curriculum")
                        .code("EL-TOEIC-650-V1")
                        .slug(CURRICULUM_SLUG)
                        .deliveryMode(ClassroomDeliveryMode.VIRTUAL)
                        .examCategory("TOEIC")
                        .targetScore(650)
                        .entryLevel("TOEIC 350+ hoặc CEFR A2")
                        .outcomes("Nắm đủ 7 Part TOEIC; đạt mục tiêu 650; tự review lỗi theo kỹ năng.")
                        .teacherGuide("Mỗi unit gồm tài liệu trung tâm, luyện tập, flashcard và bài giao có deadline.")
                        .interactionActivities("Live practice, pair work, answer review, error log và mock test.")
                        .totalSessions(8)
                        .status("APPROVED")
                        .virtualPlatform("LARK")
                        .recordingAllowed(true)
                        .recordingAvailableDays(30)
                        .materialsDownloadable(true)
                        .deviceCheckRequired(true)
                        .micRequired(false)
                        .speakerRequired(true)
                        .autoAttendanceEnabled(true)
                        .displayOrder(1)
                        .reviewedBy(teacher)
                        .reviewedAt(LocalDateTime.now())
                        .build()));

        if (curriculumUnitRepository.findByProgramIdOrderByDisplayOrderAscIdAsc(program.getId()).isEmpty()) {
            for (int index = 0; index < UNIT_SEEDS.size(); index++) {
                UnitSeed seed = UNIT_SEEDS.get(index);
                CenterMaterialLibraryItem material = ensureCenterMaterial(seed, index + 1, teacher);
                ExerciseBankItem exercise = ensureExercise(seed, index + 1, teacher);
                FlashcardSet flashcards = ensureFlashcards(seed, index + 1);

                CurriculumUnit unit = CurriculumUnit.builder()
                        .program(program)
                        .displayOrder(index + 1)
                        .title("Unit " + (index + 1) + " - " + seed.title())
                        .description(seed.description())
                        .sessionPlan("Warm-up 10 phút; chiến thuật 25 phút; guided practice 35 phút; review 20 phút.")
                        .build();
                unit.getMaterialRefs().add(CurriculumMaterialRef.builder()
                        .unit(unit).material(material).displayOrder(1).note("Tài liệu chuẩn của trung tâm").build());
                unit.getExerciseRefs().add(CurriculumExerciseRef.builder()
                        .unit(unit).exercise(exercise).displayOrder(1).note("Bài luyện tập bắt buộc").build());
                unit.getFlashcardRefs().add(CurriculumFlashcardRef.builder()
                        .unit(unit).flashcardSet(flashcards).displayOrder(1).note("Ôn trước và sau buổi học").build());
                curriculumUnitRepository.save(unit);
            }
        }
        return program;
    }

    private void synchronizeCurriculumResources(List<CurriculumUnit> units, User teacher) {
        for (int index = 0; index < Math.min(units.size(), UNIT_SEEDS.size()); index++) {
            CurriculumUnit unit = units.get(index);
            UnitSeed seed = UNIT_SEEDS.get(index);
            int unitNumber = index + 1;
            CenterMaterialLibraryItem material = ensureCenterMaterial(seed, unitNumber, teacher);
            ExerciseBankItem exercise = ensureExercise(seed, unitNumber, teacher);
            FlashcardSet flashcards = ensureFlashcards(seed, unitNumber);

            if (unit.getMaterialRefs().stream().noneMatch(ref -> ref.getMaterial().getId().equals(material.getId()))) {
                unit.getMaterialRefs().add(CurriculumMaterialRef.builder()
                        .unit(unit).material(material).displayOrder(1).note("Tài liệu chuẩn của trung tâm").build());
            }
            if (unit.getExerciseRefs().stream().noneMatch(ref -> ref.getExercise().getId().equals(exercise.getId()))) {
                unit.getExerciseRefs().add(CurriculumExerciseRef.builder()
                        .unit(unit).exercise(exercise).displayOrder(1).note("Bài luyện tập bắt buộc").build());
            }
            if (unit.getFlashcardRefs().stream().noneMatch(ref -> ref.getFlashcardSet().getId().equals(flashcards.getId()))) {
                unit.getFlashcardRefs().add(CurriculumFlashcardRef.builder()
                        .unit(unit).flashcardSet(flashcards).displayOrder(1).note("Ôn trước và sau buổi học").build());
            }
            curriculumUnitRepository.save(unit);
        }
    }

    private CenterMaterialLibraryItem ensureCenterMaterial(UnitSeed seed, int unitNumber, User teacher) {
        String title = "TOEIC 650 Unit " + unitNumber + " - " + seed.title();
        return centerMaterialRepository.findAllByOrderByUpdatedAtDescIdDesc().stream()
                .filter(item -> title.equalsIgnoreCase(item.getTitle()))
                .findFirst()
                .orElseGet(() -> centerMaterialRepository.save(CenterMaterialLibraryItem.builder()
                        .title(title)
                        .description(seed.description())
                        .fileUrl(MATERIAL_BASE_URL + seed.fileName())
                        .fileType("TXT")
                        .materialType("LESSON_NOTE")
                        .provider("EnglishLab")
                        .examCategory("TOEIC")
                        .toeicScoreMin(350)
                        .toeicScoreMax(750)
                        .skill(unitNumber <= 4 ? "LISTENING" : "READING")
                        .tags(seed.tags())
                        .status("PUBLISHED")
                        .createdBy(teacher)
                        .updatedBy(teacher)
                        .build()));
    }

    private ExerciseBankItem ensureExercise(UnitSeed seed, int unitNumber, User teacher) {
        String title = "TOEIC 650 Unit " + unitNumber + " Practice";
        ExerciseBankItem exercise = exerciseRepository.findAllByOrderByUpdatedAtDesc().stream()
                .filter(item -> title.equalsIgnoreCase(item.getTitle()))
                .findFirst()
                .orElseGet(() -> ExerciseBankItem.builder()
                        .title(title)
                        .skill(unitNumber <= 4 ? "LISTENING" : "READING")
                        .level("TOEIC 350-650")
                        .prompt("Hoàn thành bài luyện " + seed.title() + " và ghi lại lý do cho mỗi câu sai.")
                        .answerKey("Đáp án và giải thích được review trong buổi học kế tiếp.")
                        .explanation("Phân loại lỗi theo từ vựng, ngữ pháp, chi tiết, suy luận hoặc quản lý thời gian.")
                        .tags(seed.tags())
                        .active(true)
                        .createdBy(teacher)
                        .build());
        exercise.setExerciseType("PRACTICE");
        return exerciseRepository.save(exercise);
    }

    private FlashcardSet ensureFlashcards(UnitSeed seed, int unitNumber) {
        String title = "TOEIC 650 Unit " + unitNumber + " Flashcards";
        FlashcardSet set = flashcardSetRepository.findByTitleIgnoreCase(title)
                .orElseGet(() -> FlashcardSet.builder().title(title).build());
        set.setDescription("Từ vựng trọng tâm cho " + seed.title());
        set.setExamCategory("TOEIC");
        set.setSkill(unitNumber <= 4 ? "LISTENING" : "READING");
        set.setTags(seed.tags());
        set.setCardsJson(flashcardsJson(unitNumber));
        set.setStatus("PUBLISHED");
        set.setDisplayOrder(unitNumber);
        return flashcardSetRepository.save(set);
    }

    private String flashcardsJson(int unitNumber) {
        return switch (unitNumber) {
            case 1 -> """
                    [{"front":"in the foreground","back":"ở tiền cảnh","example":"A bicycle is parked in the foreground."},
                     {"front":"be seated","back":"đang ngồi","example":"Several people are seated around a table."},
                     {"front":"be stacked","back":"được xếp chồng","example":"Boxes are stacked beside the wall."},
                     {"front":"railing","back":"lan can","example":"A man is leaning against a railing."},
                     {"front":"pedestrian","back":"người đi bộ","example":"Pedestrians are crossing the street."},
                     {"front":"overlook","back":"nhìn ra, hướng ra","example":"The windows overlook the garden."}]
                    """;
            case 2 -> """
                    [{"front":"available","back":"có sẵn, rảnh","example":"Is the manager available this afternoon?"},
                     {"front":"reschedule","back":"đổi lịch","example":"Could we reschedule the appointment?"},
                     {"front":"extension","back":"số máy lẻ; gia hạn","example":"What is Mr. Lee's extension?"},
                     {"front":"department","back":"phòng ban","example":"Which department handles refunds?"},
                     {"front":"confirm","back":"xác nhận","example":"Please confirm the delivery date."},
                     {"front":"appointment","back":"cuộc hẹn","example":"When is your appointment?"}]
                    """;
            case 3 -> """
                    [{"front":"shipment","back":"lô hàng","example":"The shipment will arrive on Friday."},
                     {"front":"place an order","back":"đặt hàng","example":"I'd like to place an order for office chairs."},
                     {"front":"in stock","back":"còn hàng","example":"The blue model is currently in stock."},
                     {"front":"complimentary","back":"miễn phí","example":"Guests receive a complimentary breakfast."},
                     {"front":"invoice","back":"hóa đơn","example":"The invoice was sent by email."},
                     {"front":"replacement","back":"sản phẩm thay thế","example":"We can send a replacement tomorrow."}]
                    """;
            case 4 -> """
                    [{"front":"announcement","back":"thông báo","example":"The announcement concerns a gate change."},
                     {"front":"be advised","back":"được thông báo, lưu ý","example":"Passengers are advised to arrive early."},
                     {"front":"maintenance","back":"bảo trì","example":"The elevator is closed for maintenance."},
                     {"front":"venue","back":"địa điểm tổ chức","example":"The event venue has changed."},
                     {"front":"proceed to","back":"di chuyển đến","example":"Please proceed to platform six."},
                     {"front":"delay","back":"sự trì hoãn","example":"We apologize for the delay."}]
                    """;
            case 5 -> """
                    [{"front":"efficiently","back":"một cách hiệu quả","example":"The new system processes orders efficiently."},
                     {"front":"provided that","back":"miễn là, với điều kiện là","example":"A refund is available provided that you have a receipt."},
                     {"front":"be responsible for","back":"chịu trách nhiệm về","example":"She is responsible for training new staff."},
                     {"front":"prior to","back":"trước khi","example":"Submit the form prior to departure."},
                     {"front":"approximately","back":"xấp xỉ","example":"The repair will take approximately two hours."},
                     {"front":"despite","back":"mặc dù, bất chấp","example":"The shipment arrived despite the storm."}]
                    """;
            case 6 -> """
                    [{"front":"furthermore","back":"hơn nữa","example":"Furthermore, the service includes free delivery."},
                     {"front":"therefore","back":"vì vậy","example":"The road is closed; therefore, use the east entrance."},
                     {"front":"in response to","back":"để phản hồi","example":"I am writing in response to your inquiry."},
                     {"front":"enclosed","back":"được đính kèm","example":"Please find the enclosed application form."},
                     {"front":"regarding","back":"về việc","example":"We received your message regarding the invoice."},
                     {"front":"otherwise","back":"nếu không thì","example":"Pay by Friday; otherwise, a fee will apply."}]
                    """;
            case 7 -> """
                    [{"front":"according to","back":"theo như","example":"According to the notice, the store closes at six."},
                     {"front":"indicate","back":"chỉ ra, cho biết","example":"The article indicates that sales increased."},
                     {"front":"imply","back":"ngụ ý","example":"What is implied about the new policy?"},
                     {"front":"most likely","back":"có khả năng nhất","example":"Who most likely wrote the email?"},
                     {"front":"purpose","back":"mục đích","example":"What is the purpose of the memo?"},
                     {"front":"attached","back":"được đính kèm","example":"The revised schedule is attached."}]
                    """;
            default -> """
                    [{"front":"pacing","back":"phân bổ tốc độ làm bài","example":"Good pacing leaves enough time for Part 7."},
                     {"front":"eliminate","back":"loại trừ","example":"Eliminate choices that contradict the passage."},
                     {"front":"distractor","back":"phương án gây nhiễu","example":"A distractor may repeat a word from the audio."},
                     {"front":"review","back":"xem lại","example":"Review marked questions before submitting."},
                     {"front":"accuracy","back":"độ chính xác","example":"Balance speed with accuracy."},
                     {"front":"time allocation","back":"phân bổ thời gian","example":"Plan your time allocation for every part."}]
                    """;
        };
    }

    private AssessmentBankItem ensureUnitProgressCheckBankItem() {
        AssessmentBankItem item = assessmentBankItemRepository.findAllByOrderByUpdatedAtDescIdDesc().stream()
                .filter(existing -> UNIT_PROGRESS_CHECK_TITLE.equalsIgnoreCase(existing.getTitle()))
                .findFirst()
                .orElseGet(() -> assessmentBankItemRepository.findAllByOrderByUpdatedAtDescIdDesc().stream()
                        .filter(existing -> LEGACY_MODULE_TEST_TITLE.equalsIgnoreCase(existing.getTitle()))
                        .findFirst()
                        .orElseGet(AssessmentBankItem::new));
        item.setTitle(UNIT_PROGRESS_CHECK_TITLE);
        item.setDescription("Bài kiểm tra tiến độ Reading Part 5 bắt buộc của Unit 5 dành cho lớp học có giáo viên.");
        item.setType(AssessmentType.QUIZ);
        item.setSkill(AssessmentSkill.READING);
        item.setAiEvaluationMode(AiEvaluationMode.NONE);
        item.setRubric(null);
        item.setInstructions("Hoàn thành 10 câu Incomplete Sentences trong 15 phút.");
        item.setObjectiveAnswerKey(UNIT_PROGRESS_CHECK_ANSWER_KEY);
        item.setUiConfigJson(UNIT_PROGRESS_CHECK_UI_CONFIG);
        item.setPassingScore(BigDecimal.valueOf(7));
        item.setMaxScore(BigDecimal.TEN);
        item.setTimeLimitMinutes(15);
        item.setStatus("PUBLISHED");
        item.setDisplayOrder(1);
        item.setActive(true);
        return assessmentBankItemRepository.save(item);
    }

    private void ensureCurriculumAssessment(CurriculumUnit unit, AssessmentBankItem assessment) {
        CurriculumAssessmentRef attached = unit.getAssessmentRefs().stream()
                .filter(ref -> ref.getAssessment().getId().equals(assessment.getId())
                        || LEGACY_MODULE_TEST_TITLE.equalsIgnoreCase(ref.getAssessment().getTitle())
                        || UNIT_PROGRESS_CHECK_TITLE.equalsIgnoreCase(ref.getAssessment().getTitle()))
                .findFirst()
                .orElse(null);
        if (attached != null) {
            attached.setAssessment(assessment);
            attached.setDisplayOrder(1);
            attached.setNote("Bài kiểm tra tiến độ Reading bắt buộc của Unit 5");
            curriculumUnitRepository.save(unit);
            return;
        }
        unit.getAssessmentRefs().add(CurriculumAssessmentRef.builder()
                .unit(unit)
                .assessment(assessment)
                .displayOrder(1)
                .note("Bài kiểm tra tiến độ Reading bắt buộc của Unit 5")
                .build());
        curriculumUnitRepository.save(unit);
    }

    private TrainingProgram ensureTrainingProgram(CurriculumProgram curriculum) {
        return trainingProgramRepository.findBySlug(TRAINING_SLUG)
                .orElseGet(() -> trainingProgramRepository.save(TrainingProgram.builder()
                        .title("TOEIC 650 Complete - Training Program")
                        .code("TP-TOEIC-650-COMPLETE")
                        .slug(TRAINING_SLUG)
                        .deliveryMode(ClassroomDeliveryMode.VIRTUAL)
                        .curriculumProgram(curriculum)
                        .shortDescription("Lớp 8 buổi bám sát khung TOEIC 7 Part.")
                        .description("Bao gồm tài liệu trung tâm, flashcard theo unit, bài trực tiếp và bài nộp file.")
                        .entryLevel("TOEIC 350+")
                        .targetScore("650+")
                        .targetOutcome("Hoàn thành đủ 7 Part và một full test 200 câu.")
                        .defaultCapacity(16)
                        .price(BigDecimal.valueOf(3_900_000))
                        .salePrice(BigDecimal.valueOf(3_490_000))
                        .duration("8 tuần")
                        .studyMode("Virtual · Lark")
                        .syllabusSummary("8 unit · 8 buổi live · flashcard và homework theo unit")
                        .programOutcomes(curriculum.getOutcomes())
                        .teacherGuide(curriculum.getTeacherGuide())
                        .interactionActivities(curriculum.getInteractionActivities())
                        .status(PackageStatus.PUBLISHED)
                        .displayOrder(1)
                        .featured(true)
                        .build()));
    }

    private ClassroomOffering ensureOffering(
            PackageType classroomType,
            TrainingProgram trainingProgram,
            CurriculumProgram curriculum,
            User teacher
    ) {
        ClassroomOffering offering = offeringRepository.findByLearningPackageSlug(PACKAGE_SLUG)
                .orElseGet(() -> {
                    LearningPackage learningPackage = learningPackageRepository.save(LearningPackage.builder()
                            .packageType(classroomType)
                            .title(CLASS_TITLE)
                            .slug(PACKAGE_SLUG)
                            .shortDescription("Lớp mẫu TOEIC đầy đủ cho học viên 0386852628z@gmail.com")
                            .description("Lớp thực hành bám khung chương trình, có tài liệu, flashcard và hai hình thức giao bài.")
                            .targetScore("TOEIC 650+")
                            .duration("8 tuần")
                            .studyMode("Virtual")
                            .price(BigDecimal.valueOf(3_900_000))
                            .salePrice(BigDecimal.valueOf(3_490_000))
                            .status(PackageStatus.PUBLISHED)
                            .featured(true)
                            .createdBy(teacher)
                            .build());
                    return ClassroomOffering.builder()
                            .learningPackage(learningPackage)
                            .deliveryMode(ClassroomDeliveryMode.VIRTUAL)
                            .trainingProgram(trainingProgram)
                            .curriculumProgram(curriculum)
                            .status(ClassroomOfferingStatus.ACTIVE)
                            .entryLevel("TOEIC 350+")
                            .targetOutcome("TOEIC 650+")
                            .maxCapacity(16)
                            .startDate(LocalDate.now().minusWeeks(3))
                            .endDate(LocalDate.now().plusWeeks(5))
                            .primaryTeacher(teacher)
                            .defaultLarkMeetingUrl("https://meet.larksuite.com/s/englishlab-toeic-650-showcase")
                            .larkMeetingStatus(LarkMeetingStatus.OPEN)
                            .recordingVisible(false)
                            .syllabusSummary(trainingProgram.getSyllabusSummary())
                            .programOutcomes(trainingProgram.getProgramOutcomes())
                            .teacherGuide(trainingProgram.getTeacherGuide())
                            .interactionActivities(trainingProgram.getInteractionActivities())
                            .build();
                });
        if (!StringUtils.hasText(offering.getRecordingUrl()) || isDemoRecordingUrl(offering.getRecordingUrl())) {
            offering.setRecordingUrl(null);
            offering.setRecordingVisible(false);
        }
        return offeringRepository.save(offering);
    }

    private void ensureTeacherAssignment(ClassroomOffering offering, User teacher) {
        teacherAssignmentRepository.findByClassroomOfferingIdAndTeacherId(offering.getId(), teacher.getId())
                .orElseGet(() -> teacherAssignmentRepository.save(ClassroomTeacherAssignment.builder()
                        .classroomOffering(offering)
                        .teacher(teacher)
                        .role(ClassroomTeacherRole.PRIMARY)
                        .effectiveFrom(offering.getStartDate())
                        .reason("Giáo viên phụ trách lớp TOEIC showcase")
                        .build()));
    }

    private void ensureEnrollment(ClassroomOffering offering, User learner, User teacher) {
        enrollmentRepository.findByStudentIdAndClassroomOfferingId(learner.getId(), offering.getId())
                .orElseGet(() -> enrollmentRepository.save(ClassroomEnrollment.builder()
                        .student(learner)
                        .classroomOffering(offering)
                        .status(ClassroomEnrollmentStatus.ENROLLED)
                        .registrationStatus(ClassroomRegistrationStatus.ASSIGNED)
                        .tuitionAmountDue(BigDecimal.valueOf(3_490_000))
                        .tuitionAmountPaid(BigDecimal.valueOf(3_490_000))
                        .tuitionDepositPaid(BigDecimal.ZERO)
                        .tuitionSettlementType(TuitionSettlementType.NONE)
                        .enrolledAt(LocalDateTime.now().minusWeeks(3))
                        .assignedAt(LocalDateTime.now().minusWeeks(3))
                        .assignedBy(teacher)
                        .confirmedAt(LocalDateTime.now().minusWeeks(3))
                        .confirmedBy(teacher)
                        .assignmentNote("Tài khoản học viên showcase theo yêu cầu sản phẩm")
                        .build()));
    }

    private List<ClassroomSession> ensureSessions(ClassroomOffering offering, User teacher, List<CurriculumUnit> units) {
        List<ClassroomSession> existing = sessionRepository
                .findByClassroomOfferingIdOrderBySessionDateAscStartTimeAsc(offering.getId());
        if (!existing.isEmpty()) {
            existing.stream()
                    .filter(session -> isDemoRecordingUrl(session.getRecordingUrl()))
                    .forEach(session -> {
                        session.setRecordingUrl(null);
                        session.setRecordingVisible(false);
                    });
            sessionRepository.saveAll(existing);
            return existing;
        }
        for (int index = 0; index < units.size(); index++) {
            ClassroomSessionStatus status = index < 3
                    ? ClassroomSessionStatus.COMPLETED
                    : index == 3 ? ClassroomSessionStatus.OPEN : ClassroomSessionStatus.SCHEDULED;
            sessionRepository.save(ClassroomSession.builder()
                    .classroomOffering(offering)
                    .sessionDate(LocalDate.now().minusWeeks(3).plusWeeks(index))
                    .startTime(LocalTime.of(19, 30))
                    .endTime(LocalTime.of(21, 0))
                    .teacher(teacher)
                    .status(status)
                    .deliveryMode(ClassroomDeliveryMode.VIRTUAL)
                    .larkMeetingUrl(offering.getDefaultLarkMeetingUrl())
                    .larkMeetingStatus(status == ClassroomSessionStatus.COMPLETED
                            ? LarkMeetingStatus.ENDED
                            : status == ClassroomSessionStatus.OPEN ? LarkMeetingStatus.OPEN : LarkMeetingStatus.SCHEDULED)
                    .larkSyncStatus("DEMO")
                    .recordingVisible(false)
                    .recordingUrl(null)
                    .sessionContent(units.get(index).getTitle())
                    .note("Buổi học theo khung chương trình TOEIC 650 Complete")
                    .build());
        }
        return sessionRepository.findByClassroomOfferingIdOrderBySessionDateAscStartTimeAsc(offering.getId());
    }

    private boolean isDemoRecordingUrl(String url) {
        return StringUtils.hasText(url) && url.contains("example.com/recordings/");
    }

    private void ensureSyllabus(ClassroomOffering offering, List<CurriculumUnit> units, List<ClassroomSession> sessions) {
        if (!syllabusRepository.findByClassroomOfferingIdOrderByDisplayOrderAsc(offering.getId()).isEmpty()) {
            return;
        }
        for (int index = 0; index < units.size(); index++) {
            CurriculumUnit unit = units.get(index);
            ClassroomSession session = index < sessions.size() ? sessions.get(index) : null;
            syllabusRepository.save(ClassroomSyllabusItem.builder()
                    .classroomOffering(offering)
                    .title(unit.getTitle())
                    .description(unit.getDescription())
                    .displayOrder(index + 1)
                    .sessionNumber(index + 1)
                    .linkedSessionId(session == null ? null : session.getId())
                    .sessionPlan(unit.getSessionPlan())
                    .homeworkNotes("Hoàn thành bài tập và flashcard gắn với unit.")
                    .quizNotes("Bài soạn trực tiếp được quản lý chung trong mục Bài tập.")
                    .teacherNotes("Review error log đầu buổi kế tiếp.")
                    .reviewStatus(ContentReviewStatus.APPROVED)
                    .status("PUBLISHED")
                    .build());
        }
    }

    private void ensureClassroomMaterials(ClassroomOffering offering, List<CurriculumUnit> units, User teacher) {
        for (CurriculumUnit unit : units) {
            for (CurriculumMaterialRef ref : unit.getMaterialRefs()) {
                CenterMaterialLibraryItem material = ref.getMaterial();
                if (classroomMaterialRepository.existsByClassroomOfferingIdAndCenterMaterialIdAndSessionIsNull(
                        offering.getId(), material.getId())) {
                    continue;
                }
                classroomMaterialRepository.save(ClassroomMaterial.builder()
                        .classroomOffering(offering)
                        .title(material.getTitle())
                        .fileUrl(material.getFileUrl())
                        .fileType(material.getFileType())
                        .description(material.getDescription())
                        .materialType(material.getMaterialType())
                        .provider(material.getProvider())
                        .visibility("LEARNERS_IN_CLASS")
                        .sourceType("CENTER_LIBRARY")
                        .centerMaterialId(material.getId())
                        .uploadedBy(teacher)
                        .reviewStatus(ContentReviewStatus.APPROVED)
                        .build());
            }
        }
    }

    private void ensureHomework(
            ClassroomOffering offering,
            List<CurriculumUnit> units,
            List<ClassroomSession> sessions,
            User teacher,
            AssessmentBankItem unitProgressCheck
    ) {
        ensureHomeworkItem(offering, "Unit 1 Quiz - Photographs", ClassroomHomework.builder()
                .classroomOffering(offering)
                .session(sessions.get(0))
                .curriculumUnit(units.get(0))
                .title("Unit 1 Quiz - Photographs")
                .instruction("Chọn đáp án đúng trực tiếp trên website. Đây là bài theo format trung tâm.")
                .deadline(LocalDateTime.now().plusDays(3))
                .maxScore(BigDecimal.TEN)
                .activityType(HomeworkActivityType.SKILL_PRACTICE)
                .activityConfigJson("""
                        {"questions":[
                          {"number":1,"prompt":"The technicians are ------- the equipment.","options":[{"value":"A","label":"inspect"},{"value":"B","label":"inspecting"},{"value":"C","label":"inspected"},{"value":"D","label":"inspection"}]},
                          {"number":2,"prompt":"Which phrase best describes a workplace photograph?","options":[{"value":"A","label":"A desk is positioned near a window."},{"value":"B","label":"The deadline was extended yesterday."},{"value":"C","label":"Please submit the invoice."},{"value":"D","label":"The meeting begins at two."}]},
                          {"number":3,"prompt":"Some boxes ------- on the shelves.","options":[{"value":"A","label":"have arranged"},{"value":"B","label":"are arranging"},{"value":"C","label":"are arranged"},{"value":"D","label":"arrange"}]}],
                         "answerKey":{"1":"B","2":"A","3":"C"}}
                        """)
                .gradingMode(HomeworkGradingMode.TEACHER)
                .skill(AssessmentSkill.LISTENING)
                .status(HomeworkStatus.OPEN)
                .createdBy(teacher)
                .build());

        ensureHomeworkItem(offering, "Unit 2 Worksheet - Nộp file", ClassroomHomework.builder()
                .classroomOffering(offering)
                .session(sessions.get(1))
                .curriculumUnit(units.get(1))
                .title("Unit 2 Worksheet - Nộp file")
                .instruction("Tải file đề, hoàn thành trên máy và nộp lại file đáp án cho giáo viên.")
                .deadline(LocalDateTime.now().plusDays(5))
                .maxScore(BigDecimal.TEN)
                .attachmentUrl(MATERIAL_BASE_URL + "unit-2-question-response.txt")
                .activityType(HomeworkActivityType.FILE_RESPONSE)
                .gradingMode(HomeworkGradingMode.TEACHER)
                .skill(AssessmentSkill.LISTENING)
                .status(HomeworkStatus.OPEN)
                .createdBy(teacher)
                .build());

        Long flashcardId = units.get(2).getFlashcardRefs().getFirst().getFlashcardSet().getId();
        ensureHomeworkItem(offering, "Unit 3 Flashcard Review", ClassroomHomework.builder()
                .classroomOffering(offering)
                .session(sessions.get(2))
                .curriculumUnit(units.get(2))
                .title("Unit 3 Flashcard Review")
                .instruction("Ôn toàn bộ flashcard Unit 3 trước buổi Conversations tiếp theo.")
                .deadline(LocalDateTime.now().plusDays(2))
                .maxScore(BigDecimal.TEN)
                .activityType(HomeworkActivityType.FLASHCARD_REVIEW)
                .activityConfigJson("{\"flashcardSetIds\":[" + flashcardId + "]}")
                .gradingMode(HomeworkGradingMode.TEACHER)
                .skill(AssessmentSkill.VOCABULARY)
                .status(HomeworkStatus.OPEN)
                .createdBy(teacher)
                .build());

        ensureHomeworkItem(offering, "Unit 4 Short Talks - Listening Summary", ClassroomHomework.builder()
                .classroomOffering(offering)
                .session(sessions.get(3))
                .curriculumUnit(units.get(3))
                .title("Unit 4 Short Talks - Listening Summary")
                .instruction("Nghe lại nội dung Short Talks trong buổi học và viết tóm tắt 120-150 từ, gồm mục đích và hành động tiếp theo.")
                .deadline(LocalDateTime.now().plusDays(5))
                .maxScore(BigDecimal.TEN)
                .allowResubmission(true)
                .activityType(HomeworkActivityType.TEXT_RESPONSE)
                .gradingMode(HomeworkGradingMode.TEACHER)
                .skill(AssessmentSkill.LISTENING)
                .status(HomeworkStatus.OPEN)
                .createdBy(teacher)
                .build());

        ensureUnitProgressCheckHomework(offering, units.get(4), sessions.get(4), teacher, unitProgressCheck);

        ensureHomeworkItem(offering, "Unit 6 Text Completion - System Practice", ClassroomHomework.builder()
                .classroomOffering(offering)
                .session(sessions.get(5))
                .curriculumUnit(units.get(5))
                .title("Unit 6 Text Completion - System Practice")
                .instruction("Hoàn thành bài Text Completion trực tiếp trên website và đánh dấu câu cần giáo viên giải thích.")
                .deadline(LocalDateTime.now().plusDays(8))
                .maxScore(BigDecimal.TEN)
                .allowResubmission(true)
                .activityType(HomeworkActivityType.SKILL_PRACTICE)
                .activityConfigJson("""
                        {"durationMinutes":10,"questions":[
                          {"number":1,"prompt":"Thank you for your interest in the position. -------, we would like to invite you to an interview.","options":[{"value":"A","label":"However"},{"value":"B","label":"Therefore"},{"value":"C","label":"Otherwise"},{"value":"D","label":"Meanwhile"}]},
                          {"number":2,"prompt":"Please complete the enclosed form and return ------- by Friday.","options":[{"value":"A","label":"it"},{"value":"B","label":"its"},{"value":"C","label":"itself"},{"value":"D","label":"them"}]},
                          {"number":3,"prompt":"The orientation session ------- in Conference Room B at 9:00 A.M.","options":[{"value":"A","label":"held"},{"value":"B","label":"will hold"},{"value":"C","label":"will be held"},{"value":"D","label":"holding"}]},
                          {"number":4,"prompt":"Contact Ms. Rivera if you have any questions ------- the schedule.","options":[{"value":"A","label":"regarding"},{"value":"B","label":"between"},{"value":"C","label":"through"},{"value":"D","label":"beside"}]}],
                         "answerKey":{"1":"B","2":"A","3":"C","4":"A"}}
                        """)
                .gradingMode(HomeworkGradingMode.TEACHER)
                .skill(AssessmentSkill.READING)
                .status(HomeworkStatus.OPEN)
                .createdBy(teacher)
                .build());

        ensureHomeworkItem(offering, "Unit 7 Error Log - Reading", ClassroomHomework.builder()
                .classroomOffering(offering)
                .session(sessions.get(6))
                .curriculumUnit(units.get(6))
                .title("Unit 7 Error Log - Reading")
                .instruction("Viết 150-200 từ phân tích ba lỗi Reading gần nhất và kế hoạch tránh lặp lại.")
                .deadline(LocalDateTime.now().plusDays(10))
                .maxScore(BigDecimal.TEN)
                .allowResubmission(true)
                .activityType(HomeworkActivityType.TEXT_RESPONSE)
                .gradingMode(HomeworkGradingMode.TEACHER)
                .skill(AssessmentSkill.WRITING)
                .status(HomeworkStatus.OPEN)
                .createdBy(teacher)
                .build());

        ensureHomeworkItem(offering, "Unit 8 Full Test Strategy - Nộp kế hoạch", ClassroomHomework.builder()
                .classroomOffering(offering)
                .session(sessions.get(7))
                .curriculumUnit(units.get(7))
                .title("Unit 8 Full Test Strategy - Nộp kế hoạch")
                .instruction("Tải mẫu chiến lược, điền kế hoạch phân bổ 120 phút và nộp lại file hoàn chỉnh cho giáo viên.")
                .deadline(LocalDateTime.now().plusDays(14))
                .maxScore(BigDecimal.TEN)
                .allowResubmission(true)
                .attachmentUrl(MATERIAL_BASE_URL + "unit-8-mock-test.txt")
                .activityType(HomeworkActivityType.FILE_RESPONSE)
                .gradingMode(HomeworkGradingMode.TEACHER)
                .skill(AssessmentSkill.MIXED)
                .status(HomeworkStatus.OPEN)
                .createdBy(teacher)
                .build());
    }

    private void ensureUnitProgressCheckHomework(
            ClassroomOffering offering,
            CurriculumUnit unit,
            ClassroomSession session,
            User teacher,
            AssessmentBankItem assessment
    ) {
        String legacyTitle = "Unit 5 Module Test - Incomplete Sentences";
        String title = "Unit 5 Progress Check - Incomplete Sentences";
        ClassroomHomework homework = homeworkRepository.findByClassroomOfferingIdOrderByCreatedAtDesc(offering.getId()).stream()
                .filter(item -> title.equalsIgnoreCase(item.getTitle()) || legacyTitle.equalsIgnoreCase(item.getTitle()))
                .findFirst()
                .orElseGet(ClassroomHomework::new);
        boolean isNew = homework.getId() == null;
        homework.setClassroomOffering(offering);
        homework.setSession(session);
        homework.setCurriculumUnit(unit);
        homework.setTitle(title);
        homework.setInstruction("Bài kiểm tra tiến độ bắt buộc của Unit 5. Hoàn thành trực tiếp trên website theo giao diện Reading.");
        if (isNew || homework.getDeadline() == null) {
            homework.setDeadline(LocalDateTime.now().plusDays(7));
        }
        homework.setMaxScore(assessment.getMaxScore() == null ? BigDecimal.TEN : assessment.getMaxScore());
        homework.setAllowResubmission(true);
        homework.setActivityType(HomeworkActivityType.SKILL_PRACTICE);
        homework.setActivityConfigJson(assessment.getUiConfigJson());
        homework.setGradingMode(HomeworkGradingMode.TEACHER);
        homework.setSkill(AssessmentSkill.READING);
        homework.setStatus(HomeworkStatus.OPEN);
        if (isNew) {
            homework.setCreatedBy(teacher);
        }
        homeworkRepository.save(homework);
    }

    private void ensureHomeworkItem(ClassroomOffering offering, String title, ClassroomHomework item) {
        boolean exists = homeworkRepository.findByClassroomOfferingIdOrderByCreatedAtDesc(offering.getId()).stream()
                .anyMatch(existing -> title.equalsIgnoreCase(existing.getTitle()));
        if (!exists) {
            homeworkRepository.save(item);
        }
    }

    private void ensureAnnouncement(ClassroomOffering offering, User teacher) {
        boolean exists = announcementRepository.findByClassroomOfferingIdOrderByCreatedAtDesc(offering.getId()).stream()
                .anyMatch(item -> "Chào mừng đến lớp TOEIC 650 Complete".equalsIgnoreCase(item.getTitle()));
        if (!exists) {
            announcementRepository.save(ClassroomAnnouncement.builder()
                    .classroomOffering(offering)
                    .title("Chào mừng đến lớp TOEIC 650 Complete")
                    .content("Tài liệu, flashcard và bài tập của từng unit đã được mở. Quiz và bài file đều nằm trong mục Bài tập.")
                    .createdBy(teacher)
                    .build());
        }
    }

    private void ensureGradebook(ClassroomOffering offering, User learner, User teacher) {
        gradebookRepository.findByClassroomOfferingIdAndStudentId(offering.getId(), learner.getId())
                .orElseGet(() -> gradebookRepository.save(ClassroomGradebookEntry.builder()
                        .classroomOffering(offering)
                        .student(learner)
                        .attendancePercent(BigDecimal.valueOf(100))
                        .participationScore(BigDecimal.valueOf(8.5))
                        .status(GradebookEntryStatus.PENDING)
                        .updatedBy(teacher)
                        .build()));
    }

    private User ensureUser(String email, String fullName, RoleEnum role) {
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User created = User.builder()
                    .email(email)
                    .fullName(fullName)
                    .password(passwordEncoder.encode("Password123!"))
                    .emailVerified(true)
                    .build();
            userRoleService.assignRole(created, role);
            return userRepository.save(created);
        });
        userRoleService.ensureRole(user, role);
        return role == RoleEnum.LEARNER ? onboardingSupport.ensureReady(user) : user;
    }

    private record UnitSeed(String title, String description, String fileName, String tags) {
    }
}
