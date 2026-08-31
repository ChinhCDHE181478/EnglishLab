package fu.sep490.g23.backend.seed;
import fu.sep490.g23.backend.entity.classroom.ClassroomMaterial;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomTeacherRole;
import fu.sep490.g23.backend.entity.classroom.ClassroomTeacherAssignment;
import fu.sep490.g23.backend.entity.classroom.enums.GradebookEntryStatus;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomSessionStatus;
import fu.sep490.g23.backend.entity.classroom.ClassroomGradebookEntry;
import fu.sep490.g23.backend.entity.classroom.enums.HomeworkGradingMode;
import fu.sep490.g23.backend.entity.classroom.enums.TuitionSettlementType;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomRegistrationStatus;
import fu.sep490.g23.backend.entity.classroom.enums.HomeworkActivityType;
import fu.sep490.g23.backend.entity.classroom.ClassEnrollment;
import fu.sep490.g23.backend.entity.classroom.enums.ContentReviewStatus;
import fu.sep490.g23.backend.entity.classroom.ClassroomHomeworkSubmission;
import fu.sep490.g23.backend.entity.classroom.ClassroomAnnouncement;
import fu.sep490.g23.backend.entity.classroom.ClassroomSyllabusItem;
import fu.sep490.g23.backend.dto.request.curriculum.CourseUnitContentRefRequest;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomOfferingStatus;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import fu.sep490.g23.backend.entity.classroom.enums.HomeworkStatus;
import fu.sep490.g23.backend.repository.classroom.ClassroomHomeworkSubmissionRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomGradebookEntryRepository;
import fu.sep490.g23.backend.repository.classroom.CenterMaterialLibraryItemRepository;
import fu.sep490.g23.backend.entity.course.CourseUnit;
import fu.sep490.g23.backend.repository.classroom.ClassEnrollmentRepository;
import fu.sep490.g23.backend.entity.classroom.CenterMaterialLibraryItem;
import fu.sep490.g23.backend.entity.classroom.enums.HomeworkSubmissionStatus;
import fu.sep490.g23.backend.entity.classroom.ClassroomHomework;
import fu.sep490.g23.backend.repository.classroom.ClassroomSyllabusItemRepository;
import fu.sep490.g23.backend.entity.classroom.ClassSchedule;
import fu.sep490.g23.backend.repository.classroom.ClassroomTeacherAssignmentRepository;
import fu.sep490.g23.backend.entity.course.InstructorLedCourse;
import fu.sep490.g23.backend.entity.classroom.ClassSection;
import fu.sep490.g23.backend.entity.curriculum.FlashcardSet;
import fu.sep490.g23.backend.repository.classroom.ClassroomAnnouncementRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomMaterialRepository;
import fu.sep490.g23.backend.entity.curriculum.AssessmentBankItem;
import fu.sep490.g23.backend.repository.classroom.ClassSectionRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomHomeworkRepository;
import fu.sep490.g23.backend.repository.classroom.ClassScheduleRepository;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.assessment.ExerciseBankItem;
import fu.sep490.g23.backend.entity.assessment.enums.AiEvaluationMode;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentType;
import fu.sep490.g23.backend.entity.classroom.*;
import fu.sep490.g23.backend.entity.classroom.enums.*;
import fu.sep490.g23.backend.entity.classroom.*;
import fu.sep490.g23.backend.entity.course.enums.PackageStatus;
import fu.sep490.g23.backend.entity.curriculum.*;
import fu.sep490.g23.backend.entity.curriculum.*;
import fu.sep490.g23.backend.entity.enums.RoleCodes;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.assessment.ExerciseBankItemRepository;
import fu.sep490.g23.backend.repository.classroom.*;
import fu.sep490.g23.backend.repository.course.InstructorLedCourseRepository;
import fu.sep490.g23.backend.repository.course.CourseUnitRepository;
import fu.sep490.g23.backend.repository.course.CourseUnitContentRefRepository;
import fu.sep490.g23.backend.entity.course.enums.CourseUnitContentType;
import fu.sep490.g23.backend.service.curriculum.InstructorLedCourseManagementService;
import fu.sep490.g23.backend.repository.curriculum.FlashcardSetRepository;
import fu.sep490.g23.backend.repository.curriculum.AssessmentBankItemRepository;
import fu.sep490.g23.backend.service.user.UserRoleService;
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
    private static final String DEMO_LEARNER_TWO_EMAIL = "classroom.learner2@englishlab.vn";
    private static final String DEMO_LEARNER_THREE_EMAIL = "classroom.learner3@englishlab.vn";
    private static final String DEMO_LEARNER_FOUR_EMAIL = "classroom.learner4@englishlab.vn";
    private static final String TEACHER_EMAIL = "classroom.teacher1@englishlab.vn";
    private static final String CURRICULUM_SLUG = "toeic-650-complete-virtual-v1";
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
    private final InstructorLedCourseRepository instructorLedCourseRepository;
    private final CourseUnitRepository courseUnitRepository;
    private final CourseUnitContentRefRepository courseUnitContentRefRepository;
    private final CenterMaterialLibraryItemRepository centerMaterialRepository;
    private final ExerciseBankItemRepository exerciseRepository;
    private final FlashcardSetRepository flashcardSetRepository;
    private final AssessmentBankItemRepository assessmentBankItemRepository;
    private final ClassSectionRepository offeringRepository;
    private final ClassScheduleRepository sessionRepository;
    private final ClassEnrollmentRepository enrollmentRepository;
    private final ClassroomTeacherAssignmentRepository teacherAssignmentRepository;
    private final ClassroomSyllabusItemRepository syllabusRepository;
    private final ClassroomMaterialRepository classroomMaterialRepository;
    private final ClassroomHomeworkRepository homeworkRepository;
    private final ClassroomHomeworkSubmissionRepository homeworkSubmissionRepository;
    private final ClassroomAnnouncementRepository announcementRepository;
    private final ClassroomGradebookEntryRepository gradebookRepository;
    private final InstructorLedCourseManagementService instructorLedCourseManagementService;

    @Value("${app.seed.test.enabled:false}")
    private boolean enabled;

    @Value("${app.seed.sheet.enabled:false}")
    private boolean sheetEnabled;

    @Override
    public void run(String... args) {
        if (!enabled || sheetEnabled) {
            return;
        }

        User learner = ensureUser(LEARNER_EMAIL, "Học viên EnglishLab", RoleCodes.LEARNER);
        User learnerTwo = ensureUser(DEMO_LEARNER_TWO_EMAIL, "Phạm Minh Anh", RoleCodes.LEARNER);
        User learnerThree = ensureUser(DEMO_LEARNER_THREE_EMAIL, "Hoàng Gia Huy", RoleCodes.LEARNER);
        User learnerFour = ensureUser(DEMO_LEARNER_FOUR_EMAIL, "Trần Ngọc Mai", RoleCodes.LEARNER);
        List<User> learners = List.of(learner, learnerTwo, learnerThree, learnerFour);
        User teacher = ensureUser(TEACHER_EMAIL, "Nguyễn Văn Teacher", RoleCodes.TEACHER);
        InstructorLedCourse curriculum = ensureCurriculum(teacher);
        List<CourseUnit> units = courseUnitRepository.findByInstructorLedCourseIdOrderBySequenceNumberAscIdAsc(curriculum.getId());
        synchronizeCourseResources(units, teacher);
        units = courseUnitRepository.findByInstructorLedCourseIdOrderBySequenceNumberAscIdAsc(curriculum.getId());
        ClassSection offering = ensureOffering(curriculum, teacher);

        ensureTeacherAssignment(offering, teacher);
        learners.forEach(student -> ensureEnrollment(offering, student, teacher));
        List<ClassSchedule> schedules = ensureSessions(offering, teacher, units);
        ensureSyllabus(offering, units, schedules);
        ensureClassroomMaterials(offering, units, teacher);
        AssessmentBankItem unitProgressCheck = ensureUnitProgressCheckBankItem();
        ensureCurriculumAssessment(units.get(4), unitProgressCheck);
        ensureHomework(offering, units, schedules, teacher, unitProgressCheck);
        ensureHomeworkSubmissions(offering, learnerTwo, learnerThree, learnerFour, teacher);
        ensureAnnouncement(offering, teacher);
        learners.forEach(student -> ensureGradebook(offering, student, teacher));
    }

    private InstructorLedCourse ensureCurriculum(User teacher) {
        InstructorLedCourse program = instructorLedCourseRepository.findBySlug(CURRICULUM_SLUG)
                .orElseGet(() -> instructorLedCourseRepository.save(InstructorLedCourse.builder()
                        .title("TOEIC 650 Complete - Virtual Curriculum")
                        .code("EL-TOEIC-650-V1")
                        .slug(CURRICULUM_SLUG)
                        .examType("TOEIC")
                        .targetScore(650)
                        .entryLevel("TOEIC 350+ hoặc CEFR A2")
                        .learningOutcomes("Nắm đủ 7 Part TOEIC; đạt mục tiêu 650; tự review lỗi theo kỹ năng.")
                        .teacherGuide("Mỗi unit gồm tài liệu trung tâm, luyện tập, flashcard và bài giao có deadline.")
                        .shortDescription("Lớp 8 buổi bám sát khung TOEIC 7 Part.")
                        .description("Triển khai TOEIC 650 với giáo viên trong 8 tuần.")
                        .baseTuitionFeeVnd(BigDecimal.valueOf(3_900_000))
                        .saleTuitionFeeVnd(BigDecimal.valueOf(3_490_000))
                        .durationLabel("8 tuần")
                        .publicationStatus(PackageStatus.PUBLISHED)
                        .reviewedBy(teacher)
                        .reviewedAt(LocalDateTime.now())
                        .build()));

        if (courseUnitRepository.findByInstructorLedCourseIdOrderBySequenceNumberAscIdAsc(program.getId()).isEmpty()) {
            for (int index = 0; index < UNIT_SEEDS.size(); index++) {
                UnitSeed seed = UNIT_SEEDS.get(index);
                CenterMaterialLibraryItem material = ensureCenterMaterial(seed, index + 1, teacher);
                ExerciseBankItem exercise = ensureExercise(seed, index + 1, teacher);
                FlashcardSet flashcards = ensureFlashcards(seed, index + 1);

                CourseUnit unit = CourseUnit.builder()
                        .instructorLedCourse(program)
                        .sequenceNumber(index + 1)
                        .title("Unit " + (index + 1) + " - " + seed.title())
                        .description(seed.description())
                        .learningObjectives("Warm-up 10 phút; chiến thuật 25 phút; guided practice 35 phút; review 20 phút.")
                        .build();
                courseUnitRepository.save(unit);
                attachUnitResources(unit, material, exercise, flashcards);
            }
        }
        return program;
    }

    private void synchronizeCourseResources(List<CourseUnit> units, User teacher) {
        for (int index = 0; index < Math.min(units.size(), UNIT_SEEDS.size()); index++) {
            CourseUnit unit = units.get(index);
            UnitSeed seed = UNIT_SEEDS.get(index);
            int unitNumber = index + 1;
            CenterMaterialLibraryItem material = ensureCenterMaterial(seed, unitNumber, teacher);
            ExerciseBankItem exercise = ensureExercise(seed, unitNumber, teacher);
            FlashcardSet flashcards = ensureFlashcards(seed, unitNumber);

            attachUnitResources(unit, material, exercise, flashcards);
        }
    }

    private void attachUnitResources(
            CourseUnit unit,
            CenterMaterialLibraryItem material,
            ExerciseBankItem exercise,
            FlashcardSet flashcards
    ) {
        try {
            instructorLedCourseManagementService.attachMaterial(unit.getId(), contentRef(material.getId(), "Tài liệu chuẩn của trung tâm"));
        } catch (Exception ignored) {}
        try {
            instructorLedCourseManagementService.attachExercise(unit.getId(), contentRef(exercise.getId(), "Bài luyện tập bắt buộc"));
        } catch (Exception ignored) {}
        try {
            instructorLedCourseManagementService.attachFlashcard(unit.getId(), contentRef(flashcards.getId(), "Ôn trước và sau buổi học"));
        } catch (Exception ignored) {}
    }

    private CourseUnitContentRefRequest contentRef(Long resourceId, String note) {
        CourseUnitContentRefRequest request = new CourseUnitContentRefRequest();
        request.setResourceId(resourceId);
        request.setDisplayOrder(1);
        request.setNote(note);
        return request;
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
        if (exercise.getPrompt() == null || !exercise.getPrompt().trim().startsWith("{")) {
            exercise.setPrompt(buildSystemPracticeConfig(seed, unitNumber));
            exercise.setAnswerKey("{\"1\":\"B\",\"2\":\"A\",\"3\":\"C\"}");
            exercise.setExplanation("Xem lại đáp án, xác định nguyên nhân câu sai và thực hiện một lượt mới để cải thiện kết quả.");
        }
        return exerciseRepository.save(exercise);
    }

    private String buildSystemPracticeConfig(UnitSeed seed, int unitNumber) {
        String skill = unitNumber <= 4 ? "LISTENING" : "READING";
        String type = unitNumber <= 4 ? "ielts_listening_exam" : "ielts_reading_exam";
        return """
                {
                  "type":"%s",
                  "key":"toeic-650-unit-%d-practice",
                  "title":"TOEIC 650 Unit %d Practice - %s",
                  "durationMinutes":10,
                  "rules":["Mỗi câu chỉ chọn một đáp án","Có thể luyện lại nhiều lần","Kết quả không tính vào bảng điểm lớp"],
                  "parts":[{
                    "key":"part_1",
                    "partNumber":%d,
                    "title":"%s Practice",
                    "questionRange":"Questions 1-3",
                    "passage":{"title":"Practice instructions","paragraphs":[{"label":"%s","text":"%s"}]},
                    "questionGroups":[{
                      "type":"single_choice",
                      "title":"Choose the best answer",
                      "instructions":"Select one answer for each question.",
                      "questions":[
                        {"number":1,"prompt":"Which option best matches the target skill for this unit?","options":[{"value":"A","label":"Ignore the context"},{"value":"B","label":"Identify key words and context"},{"value":"C","label":"Choose the longest option"},{"value":"D","label":"Skip every difficult item"}]},
                        {"number":2,"prompt":"What should you do before confirming an answer?","options":[{"value":"A","label":"Check the evidence in the question"},{"value":"B","label":"Change the answer randomly"},{"value":"C","label":"Look only at one word"},{"value":"D","label":"Leave the question blank"}]},
                        {"number":3,"prompt":"Which review method is most useful after the practice?","options":[{"value":"A","label":"Forget all incorrect answers"},{"value":"B","label":"Repeat without checking"},{"value":"C","label":"Classify errors and review explanations"},{"value":"D","label":"Memorize option letters only"}]}
                      ]
                    }]
                  }]
                }
                """.formatted(type, unitNumber, unitNumber, seed.title(), unitNumber, skill, seed.title(), seed.description());
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

    private void ensureCurriculumAssessment(CourseUnit unit, AssessmentBankItem assessment) {
        instructorLedCourseManagementService.attachAssessment(
                unit.getId(),
                contentRef(assessment.getId(), "Bài kiểm tra tiến độ Reading bắt buộc của Unit 5")
        );
    }

    private ClassSection ensureOffering(
            InstructorLedCourse curriculum,
            User teacher
    ) {
        ClassSection offering = offeringRepository.findByCode(PACKAGE_SLUG)
                .orElseGet(() -> {
                    return ClassSection.builder()
                            .name(CLASS_TITLE)
                            .code(PACKAGE_SLUG)
                            .instructorLedCourse(curriculum)
                            .tuitionFeeVnd(BigDecimal.valueOf(3_490_000))
                            .deliveryMode(ClassroomDeliveryMode.VIRTUAL)
                            
                            
                            .status(ClassroomOfferingStatus.ACTIVE)
                            .entryLevel("TOEIC 350+")
                            .targetOutcome("TOEIC 650+")
                            .capacity(16)
                            .startDate(LocalDate.now().minusWeeks(3))
                            .plannedEndDate(LocalDate.now().plusWeeks(5))
                            .primaryTeacher(teacher)
                            .syllabusSummary(curriculum.getLearningOutcomes())
                            .programOutcomes(curriculum.getLearningOutcomes())
                            .teacherGuide(curriculum.getTeacherGuide())
                            .interactionActivities(null)
                            .build();
                });
        offering.setInstructorLedCourse(curriculum);
        return offeringRepository.save(offering);
    }

    private void ensureTeacherAssignment(ClassSection offering, User teacher) {
        if (teacherAssignmentRepository.findAllByClassSectionIdAndTeacherId(offering.getId(), teacher.getId()).isEmpty()) {
            teacherAssignmentRepository.save(ClassroomTeacherAssignment.builder()
                        .classSection(offering)
                        .teacher(teacher)
                        .role(ClassroomTeacherRole.PRIMARY)
                        .effectiveFrom(offering.getStartDate())
                        .reason("Giáo viên phụ trách lớp TOEIC showcase")
                        .build());
        }
    }

    private void ensureEnrollment(ClassSection offering, User learner, User teacher) {
        enrollmentRepository.findByStudentIdAndClassSectionId(learner.getId(), offering.getId())
                .orElseGet(() -> enrollmentRepository.save(ClassEnrollment.builder()
                        .student(learner)
                        .classSection(offering)
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

    private List<ClassSchedule> ensureSessions(ClassSection offering, User teacher, List<CourseUnit> units) {
        List<ClassSchedule> existing = sessionRepository
                .findByClassSectionIdOrderBySessionDateAscStartTimeAsc(offering.getId());
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
            sessionRepository.save(ClassSchedule.builder()
                    .classSection(offering)
                    .sessionDate(LocalDate.now().minusWeeks(3).plusWeeks(index))
                    .startTime(LocalTime.of(19, 30))
                    .endTime(LocalTime.of(21, 0))
                    .teacher(teacher)
                    .status(status)
                    .deliveryModeOverride(null)
                    .recordingVisible(false)
                    .recordingUrl(null)
                    .sessionContent(units.get(index).getTitle())
                    .note("Buổi học theo khung chương trình TOEIC 650 Complete")
                    .build());
        }
        return sessionRepository.findByClassSectionIdOrderBySessionDateAscStartTimeAsc(offering.getId());
    }

    private boolean isDemoRecordingUrl(String url) {
        return StringUtils.hasText(url) && url.contains("example.com/recordings/");
    }

    private void ensureSyllabus(ClassSection offering, List<CourseUnit> units, List<ClassSchedule> schedules) {
        if (!syllabusRepository.findByClassSectionIdOrderByDisplayOrderAsc(offering.getId()).isEmpty()) {
            return;
        }
        for (int index = 0; index < units.size(); index++) {
            CourseUnit unit = units.get(index);
            ClassSchedule session = index < schedules.size() ? schedules.get(index) : null;
            syllabusRepository.save(ClassroomSyllabusItem.builder()
                    .classSection(offering)
                    .title(unit.getTitle())
                    .description(unit.getDescription())
                    .displayOrder(index + 1)
                    .displayOrder(index + 1)
                    .linkedSessionId(session == null ? null : session.getId())
                    .sessionPlan(unit.getLearningObjectives())
                    .homeworkNotes("Hoàn thành bài tập và flashcard gắn với unit.")
                    .quizNotes("Bài soạn trực tiếp được quản lý chung trong mục Bài tập.")
                    .teacherNotes("Review error log đầu buổi kế tiếp.")
                    .reviewStatus(ContentReviewStatus.APPROVED)
                    .status("PUBLISHED")
                    .build());
        }
    }

    private void ensureClassroomMaterials(ClassSection offering, List<CourseUnit> units, User teacher) {
        for (CourseUnit unit : units) {
            for (var ref : courseUnitContentRefRepository.findByCourseUnitIdOrderBySequenceNumberAscIdAsc(unit.getId())) {
                if (ref.getContentType() != CourseUnitContentType.MATERIAL || ref.getLearningResource() == null) {
                    continue;
                }
                CenterMaterialLibraryItem material = ref.getLearningResource();
                if (classroomMaterialRepository.existsByClassSectionIdAndCenterMaterialIdAndSessionIsNull(
                        offering.getId(), material.getId())) {
                    continue;
                }
                classroomMaterialRepository.save(ClassroomMaterial.builder()
                        .classSection(offering)
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
            ClassSection offering,
            List<CourseUnit> units,
            List<ClassSchedule> schedules,
            User teacher,
            AssessmentBankItem unitProgressCheck
    ) {
        ensureHomeworkItem(offering, "Unit 1 Quiz - Photographs", ClassroomHomework.builder()
                .classSection(offering)
                .session(schedules.get(0))
                .courseUnit(units.get(0))
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
                .gradingMode(HomeworkGradingMode.AUTO)
                .skill(AssessmentSkill.LISTENING)
                .status(HomeworkStatus.OPEN)
                .createdBy(teacher)
                .build());

        ensureHomeworkItem(offering, "Unit 2 Worksheet - Nộp file", ClassroomHomework.builder()
                .classSection(offering)
                .session(schedules.get(1))
                .courseUnit(units.get(1))
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

        Long flashcardId = courseUnitContentRefRepository.findByCourseUnitIdOrderBySequenceNumberAscIdAsc(units.get(2).getId()).stream()
                .filter(ref -> ref.getContentType() == CourseUnitContentType.FLASHCARD && ref.getContentBankItem() != null)
                .map(ref -> ref.getContentBankItem().getId())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Thiếu flashcard cho Unit 3."));
        ensureHomeworkItem(offering, "Unit 3 Flashcard Review", ClassroomHomework.builder()
                .classSection(offering)
                .session(schedules.get(2))
                .courseUnit(units.get(2))
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
                .classSection(offering)
                .session(schedules.get(3))
                .courseUnit(units.get(3))
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

        ensureHomeworkItem(offering, "Unit 3 Speaking Retell - Conversations", ClassroomHomework.builder()
                .classSection(offering)
                .session(schedules.get(2))
                .courseUnit(units.get(2))
                .title("Unit 3 Speaking Retell - Conversations")
                .instruction("Tóm tắt bằng tiếng Anh nội dung một cuộc hội thoại về lịch hẹn hoặc dịch vụ khách hàng trong 60-90 giây.")
                .deadline(LocalDateTime.now().plusDays(6))
                .maxScore(BigDecimal.TEN)
                .allowResubmission(true)
                .activityType(HomeworkActivityType.TEXT_RESPONSE)
                .gradingMode(HomeworkGradingMode.TEACHER)
                .skill(AssessmentSkill.SPEAKING)
                .status(HomeworkStatus.OPEN)
                .createdBy(teacher)
                .build());

        ensureUnitProgressCheckHomework(offering, units.get(4), schedules.get(4), teacher, unitProgressCheck);

        ensureHomeworkItem(offering, "Unit 6 Text Completion - System Practice", ClassroomHomework.builder()
                .classSection(offering)
                .session(schedules.get(5))
                .courseUnit(units.get(5))
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
                .gradingMode(HomeworkGradingMode.AUTO)
                .skill(AssessmentSkill.READING)
                .status(HomeworkStatus.OPEN)
                .createdBy(teacher)
                .build());

        ensureHomeworkItem(offering, "Unit 7 Error Log - Reading", ClassroomHomework.builder()
                .classSection(offering)
                .session(schedules.get(6))
                .courseUnit(units.get(6))
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
                .classSection(offering)
                .session(schedules.get(7))
                .courseUnit(units.get(7))
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

        synchronizeObjectiveHomeworkGrading(offering);
    }

    private void ensureUnitProgressCheckHomework(
            ClassSection offering,
            CourseUnit unit,
            ClassSchedule session,
            User teacher,
            AssessmentBankItem assessment
    ) {
        String legacyTitle = "Unit 5 Module Test - Incomplete Sentences";
        String title = "Unit 5 Progress Check - Incomplete Sentences";
        ClassroomHomework homework = homeworkRepository.findByClassSectionIdOrderByCreatedAtDesc(offering.getId()).stream()
                .filter(item -> title.equalsIgnoreCase(item.getTitle()) || legacyTitle.equalsIgnoreCase(item.getTitle()))
                .findFirst()
                .orElseGet(ClassroomHomework::new);
        boolean isNew = homework.getId() == null;
        homework.setClassSection(offering);
        homework.setSession(session);
        homework.setCourseUnit(unit);
        homework.setTitle(title);
        homework.setInstruction("Bài kiểm tra tiến độ bắt buộc của Unit 5. Hoàn thành trực tiếp trên website theo giao diện Reading.");
        if (isNew || homework.getDeadline() == null) {
            homework.setDeadline(LocalDateTime.now().plusDays(7));
        }
        homework.setMaxScore(assessment.getMaxScore() == null ? BigDecimal.TEN : assessment.getMaxScore());
        homework.setAllowResubmission(true);
        homework.setActivityType(HomeworkActivityType.SKILL_PRACTICE);
        homework.setActivityConfigJson(assessment.getUiConfigJson());
        homework.setAssessmentBankItem(assessment);
        homework.setGradingMode(HomeworkGradingMode.AUTO);
        homework.setSkill(AssessmentSkill.READING);
        homework.setStatus(HomeworkStatus.OPEN);
        if (isNew) {
            homework.setCreatedBy(teacher);
        }
        homeworkRepository.save(homework);
    }

    private void synchronizeObjectiveHomeworkGrading(ClassSection offering) {
        homeworkRepository.findByClassSectionIdOrderByCreatedAtDesc(offering.getId()).stream()
                .filter(item -> item.getActivityType() == HomeworkActivityType.SKILL_PRACTICE)
                .filter(item -> StringUtils.hasText(item.getActivityConfigJson()) || item.getAssessmentBankItem() != null)
                .forEach(item -> {
                    item.setGradingMode(HomeworkGradingMode.AUTO);
                    item.setAiReviewEnabled(false);
                    homeworkRepository.save(item);
                });
    }

    private void ensureHomeworkSubmissions(
            ClassSection offering,
            User learnerTwo,
            User learnerThree,
            User learnerFour,
            User teacher
    ) {
        ClassroomHomework quiz = requireHomework(offering, "Unit 1 Quiz - Photographs");
        ensureSubmission(
                quiz,
                learnerTwo,
                "{\"responses\":{\"1\":\"B\",\"2\":\"A\",\"3\":\"C\"}}",
                null,
                submittedAt(quiz, false),
                HomeworkSubmissionStatus.GRADED,
                BigDecimal.TEN,
                "Hệ thống tự chấm: 3/3 câu đúng.",
                teacher
        );
        ensureSubmission(
                quiz,
                learnerThree,
                "{\"responses\":{\"1\":\"B\",\"2\":\"D\",\"3\":\"C\"}}",
                null,
                submittedAt(quiz, true),
                HomeworkSubmissionStatus.GRADED,
                BigDecimal.valueOf(6.67),
                "Hệ thống tự chấm: 2/3 câu đúng.",
                teacher
        );

        ClassroomHomework writing = requireHomework(offering, "Unit 7 Error Log - Reading");
        ensureSubmission(
                writing,
                learnerTwo,
                "My first recurring problem is reading every word before checking the question. This wastes time and makes me lose focus. I will scan names, dates and keywords first, then locate the relevant paragraph. My second problem is choosing an option because it repeats words from the passage. I will compare the meaning instead of matching vocabulary. Finally, I often leave difficult questions blank. I will make a temporary choice, flag it and return after completing the easier questions.",
                null,
                submittedAt(writing, false),
                HomeworkSubmissionStatus.SUBMITTED,
                null,
                null,
                null
        );
        ensureSubmission(
                writing,
                learnerThree,
                "I usually spend too much time on the first passage and then rush the final questions. I also confuse NOT GIVEN with FALSE when the passage does not mention enough information. In my next practice, I will use a strict time limit for each passage, underline evidence before answering, and keep an error log with the reason for every incorrect choice.",
                null,
                submittedAt(writing, true),
                HomeworkSubmissionStatus.SUBMITTED,
                null,
                null,
                null
        );
        ensureSubmission(
                writing,
                learnerFour,
                "My reading errors mainly come from overlooking reference words and reading options too quickly. I now circle pronouns, identify what they refer to, and eliminate choices that are only partly supported. I will review my error log twice a week and redo each incorrect question without looking at the answer.",
                null,
                submittedAt(writing, false),
                HomeworkSubmissionStatus.GRADED,
                BigDecimal.valueOf(8.5),
                "Phân tích lỗi rõ ràng và có kế hoạch cải thiện cụ thể. Em nên bổ sung một ví dụ thực tế từ bài Reading gần nhất.",
                teacher
        );

        ClassroomHomework speaking = requireHomework(offering, "Unit 3 Speaking Retell - Conversations");
        ensureSubmission(
                speaking,
                learnerTwo,
                "Học viên đã thu âm phần tóm tắt hội thoại và nộp bản ghi để giáo viên đánh giá.",
                MATERIAL_BASE_URL + "unit-3-speaking-submission.wav",
                submittedAt(speaking, false),
                HomeworkSubmissionStatus.SUBMITTED,
                null,
                null,
                null
        );
    }

    private ClassroomHomework requireHomework(ClassSection offering, String title) {
        return homeworkRepository.findByClassSectionIdOrderByCreatedAtDesc(offering.getId()).stream()
                .filter(item -> title.equalsIgnoreCase(item.getTitle()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy bài tập showcase: " + title));
    }

    private LocalDateTime submittedAt(ClassroomHomework homework, boolean late) {
        LocalDateTime deadline = homework.getDeadline() == null ? LocalDateTime.now() : homework.getDeadline();
        return late ? deadline.plusMinutes(45) : deadline.minusHours(4);
    }

    private void ensureSubmission(
            ClassroomHomework homework,
            User learner,
            String textAnswer,
            String attachmentUrl,
            LocalDateTime submittedAt,
            HomeworkSubmissionStatus status,
            BigDecimal score,
            String feedback,
            User gradedBy
    ) {
        homeworkSubmissionRepository.findByHomeworkIdAndStudentId(homework.getId(), learner.getId())
                .orElseGet(() -> homeworkSubmissionRepository.save(ClassroomHomeworkSubmission.builder()
                        .homework(homework)
                        .student(learner)
                        .textAnswer(textAnswer)
                        .attachmentUrl(attachmentUrl)
                        .submittedAt(submittedAt)
                        .status(status)
                        .score(score)
                        .teacherFeedback(feedback)
                        .gradedAt(status == HomeworkSubmissionStatus.GRADED ? submittedAt.plusHours(2) : null)
                        .gradedBy(status == HomeworkSubmissionStatus.GRADED ? gradedBy : null)
                        .build()));
    }

    private void ensureHomeworkItem(ClassSection offering, String title, ClassroomHomework item) {
        boolean exists = homeworkRepository.findByClassSectionIdOrderByCreatedAtDesc(offering.getId()).stream()
                .anyMatch(existing -> title.equalsIgnoreCase(existing.getTitle()));
        if (!exists) {
            homeworkRepository.save(item);
        }
    }

    private void ensureAnnouncement(ClassSection offering, User teacher) {
        boolean exists = announcementRepository.findByClassSectionIdOrderByCreatedAtDesc(offering.getId()).stream()
                .anyMatch(item -> "Chào mừng đến lớp TOEIC 650 Complete".equalsIgnoreCase(item.getTitle()));
        if (!exists) {
            announcementRepository.save(ClassroomAnnouncement.builder()
                    .classSection(offering)
                    .title("Chào mừng đến lớp TOEIC 650 Complete")
                    .content("Tài liệu, flashcard và bài tập của từng unit đã được mở. Quiz và bài file đều nằm trong mục Bài tập.")
                    .createdBy(teacher)
                    .build());
        }
    }

    private void ensureGradebook(ClassSection offering, User learner, User teacher) {
        gradebookRepository.findByClassSectionIdAndStudentId(offering.getId(), learner.getId())
                .orElseGet(() -> gradebookRepository.save(ClassroomGradebookEntry.builder()
                        .classSection(offering)
                        .student(learner)
                        .attendancePercent(BigDecimal.valueOf(100))
                        .participationScore(BigDecimal.valueOf(8.5))
                        .status(GradebookEntryStatus.PENDING)
                        .updatedBy(teacher)
                        .build()));
    }

    private User ensureUser(String email, String fullName, String roleCode) {
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User created = User.builder()
                    .email(email)
                    .fullName(fullName)
                    .password(passwordEncoder.encode("Password123!"))
                    .emailVerified(true)
                    .build();
            userRoleService.assignRole(created, roleCode);
            return userRepository.save(created);
        });
        userRoleService.ensureRole(user, roleCode);
        return RoleCodes.LEARNER.equals(roleCode) ? onboardingSupport.ensureReady(user) : user;
    }

    private record UnitSeed(String title, String description, String fileName, String tags) {
    }
}
