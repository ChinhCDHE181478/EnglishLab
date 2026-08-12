package fu.sep490.g23.backend.seed;
import fu.sep490.g23.backend.entity.classroom.ClassroomAnnouncement;
import fu.sep490.g23.backend.entity.curriculum.CurriculumFlashcardRef;
import fu.sep490.g23.backend.entity.curriculum.CurriculumMaterialRef;
import fu.sep490.g23.backend.entity.classroom.ClassroomPracticeAttempt;
import fu.sep490.g23.backend.entity.classroom.ClassroomSyllabusItem;
import fu.sep490.g23.backend.entity.classroom.enums.TuitionPaymentKind;
import fu.sep490.g23.backend.entity.curriculum.CurriculumExerciseRef;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomOfferingStatus;
import fu.sep490.g23.backend.entity.classroom.ClassroomPracticeAttemptHistory;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import fu.sep490.g23.backend.entity.classroom.ClassroomMaterial;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomTeacherRole;
import fu.sep490.g23.backend.entity.classroom.ClassroomTeacherAssignment;
import fu.sep490.g23.backend.entity.classroom.enums.GradebookEntryStatus;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomEnrollmentStatus;
import fu.sep490.g23.backend.entity.classroom.enums.LarkMeetingStatus;
import fu.sep490.g23.backend.entity.classroom.ClassroomTuitionPayment;
import fu.sep490.g23.backend.entity.classroom.ClassroomAttendance;
import fu.sep490.g23.backend.entity.classroom.ClassroomGradebookEntry;
import fu.sep490.g23.backend.entity.classroom.enums.HomeworkGradingMode;
import fu.sep490.g23.backend.entity.classroom.enums.TuitionSettlementType;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomRegistrationStatus;
import fu.sep490.g23.backend.entity.classroom.enums.ContentReviewStatus;
import fu.sep490.g23.backend.repository.classroom.CenterMaterialLibraryItemRepository;
import fu.sep490.g23.backend.entity.classroom.enums.HomeworkStatus;
import fu.sep490.g23.backend.entity.curriculum.CurriculumProgram;
import fu.sep490.g23.backend.repository.classroom.ClassroomHomeworkSubmissionRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomSessionRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomHomeworkRepository;
import fu.sep490.g23.backend.entity.classroom.ClassroomHomeworkSubmission;
import fu.sep490.g23.backend.entity.classroom.enums.HomeworkActivityType;
import fu.sep490.g23.backend.repository.classroom.ClassroomGradebookEntryRepository;
import fu.sep490.g23.backend.entity.curriculum.CurriculumUnit;
import fu.sep490.g23.backend.repository.classroom.ClassroomTuitionPaymentRepository;
import fu.sep490.g23.backend.entity.classroom.ClassroomOffering;
import fu.sep490.g23.backend.repository.classroom.ClassroomOfferingRepository;
import fu.sep490.g23.backend.entity.classroom.ClassroomHomework;
import fu.sep490.g23.backend.repository.classroom.ClassroomAnnouncementRepository;
import fu.sep490.g23.backend.entity.curriculum.FlashcardSet;
import fu.sep490.g23.backend.repository.classroom.ClassroomPracticeAttemptHistoryRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomAttendanceRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomPracticeAttemptRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomTeacherAssignmentRepository;
import fu.sep490.g23.backend.repository.classroom.TrainingProgramRepository;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomSessionStatus;
import fu.sep490.g23.backend.repository.curriculum.FlashcardSetRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomMaterialRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomSyllabusItemRepository;
import fu.sep490.g23.backend.entity.classroom.CenterMaterialLibraryItem;
import fu.sep490.g23.backend.repository.curriculum.CurriculumUnitRepository;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomAttendanceStatus;
import fu.sep490.g23.backend.repository.classroom.ClassroomEnrollmentRepository;
import fu.sep490.g23.backend.entity.classroom.ClassroomEnrollment;
import fu.sep490.g23.backend.entity.classroom.TrainingProgram;
import fu.sep490.g23.backend.repository.curriculum.CurriculumProgramRepository;
import fu.sep490.g23.backend.entity.classroom.enums.HomeworkSubmissionStatus;
import fu.sep490.g23.backend.entity.classroom.ClassroomSession;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.assessment.ExerciseBankItem;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sep490.g23.backend.entity.classroom.*;
import fu.sep490.g23.backend.entity.classroom.enums.*;
import fu.sep490.g23.backend.entity.classroom.*;
import fu.sep490.g23.backend.entity.course.LearningPackage;
import fu.sep490.g23.backend.entity.course.PackageType;
import fu.sep490.g23.backend.entity.course.enums.PackageStatus;
import fu.sep490.g23.backend.entity.course.enums.PackageTypeCode;
import fu.sep490.g23.backend.entity.curriculum.*;
import fu.sep490.g23.backend.entity.curriculum.*;
import fu.sep490.g23.backend.entity.enums.RoleEnum;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.assessment.ExerciseBankItemRepository;
import fu.sep490.g23.backend.repository.classroom.*;
import fu.sep490.g23.backend.repository.course.LearningPackageRepository;
import fu.sep490.g23.backend.repository.course.PackageTypeRepository;
import fu.sep490.g23.backend.repository.curriculum.*;
import fu.sep490.g23.backend.service.user.UserRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Seeder tạo dữ liệu test đầy đủ cho account chinhcdhe181478@fpt.edu.vn và các học viên cùng lớp.
 * Bao gồm 1 lớp học IELTS duy nhất với tất cả chức năng & dữ liệu:
 * - Giáo trình chuẩn (Curriculum): 8 Units đầy đủ nội dung, kết nối trực tiếp vào lớp
 * - Flashcards: 8 bộ flashcard từ vựng IELTS theo từng Unit
 * - Bài luyện tập (Practice): 8 bài luyện tập interactive theo từng Unit có câu hỏi, đáp án, giải thích
 * - Lịch sử làm bài luyện tập: Đã làm và có điểm cho Chinh (Unit 1: 100%, Unit 2: 66.7%)
 * - Buổi học: COMPLETED, IN_PROGRESS (hôm nay + join meeting), SCHEDULED
 * - Bài tập: QUÁ HẠN, hạn hôm nay, ngày mai, ngày kia, đã nộp, đã chấm điểm (nhiều đầu điểm)
 * - Học viên: Chinh + 3 học viên khác cùng tham gia lớp
 * - Điểm danh: có mặt, vắng, đến muộn
 * - Bảng điểm: đã publish với đầy đủ đầu điểm và điểm trung bình
 * - Tài liệu: PDF, slide, link
 * - Thông báo: nhiều loại
 * - Học phí: lịch sử thanh toán
 *
 * Bật seeder: APP_SEED_CHINH_TEST_ENABLED=true
 */
@Slf4j
@Component
@Order(220)
@RequiredArgsConstructor
public class ChinhTestClassroomSeeder implements CommandLineRunner {

    // ── Identities ──────────────────────────────────────────────────────────
    static final String LEARNER_EMAIL = "chinhcdhe181478@fpt.edu.vn";
    private static final String TEACHER_EMAIL = "classroom.teacher1@englishlab.vn";
    private static final String PACKAGE_SLUG = "ielts-intensive-chinh-test-v1";
    private static final String CLASS_TITLE = "IELTS Intensive 6.5+ – Lớp Test Chinh";
    private static final String CURRICULUM_SLUG = "ielts-650-complete-virtual-v1";
    private static final String TRAINING_PROGRAM_SLUG = "ielts-intensive-training-v1";
    private static final String MATERIAL_BASE_URL = "https://cdn.englishlab.vn/materials/ielts-650/";

    // ── Repositories ─────────────────────────────────────────────────────────
    private final UserRepository userRepository;
    private final UserRoleService userRoleService;
    private final PasswordEncoder passwordEncoder;
    private final DemoLearnerOnboardingSupport onboardingSupport;
    private final PackageTypeRepository packageTypeRepository;
    private final LearningPackageRepository learningPackageRepository;
    private final ClassroomOfferingRepository offeringRepository;
    private final ClassroomSessionRepository sessionRepository;
    private final ClassroomEnrollmentRepository enrollmentRepository;
    private final ClassroomTeacherAssignmentRepository teacherAssignmentRepository;
    private final ClassroomAttendanceRepository attendanceRepository;
    private final ClassroomHomeworkRepository homeworkRepository;
    private final ClassroomHomeworkSubmissionRepository submissionRepository;
    private final ClassroomGradebookEntryRepository gradebookRepository;
    private final ClassroomMaterialRepository materialRepository;
    private final ClassroomAnnouncementRepository announcementRepository;
    private final ClassroomSyllabusItemRepository syllabusRepository;
    private final ClassroomTuitionPaymentRepository tuitionPaymentRepository;
    private final CurriculumProgramRepository curriculumProgramRepository;
    private final CurriculumUnitRepository curriculumUnitRepository;
    private final CenterMaterialLibraryItemRepository centerMaterialRepository;
    private final ExerciseBankItemRepository exerciseRepository;
    private final FlashcardSetRepository flashcardSetRepository;
    private final TrainingProgramRepository trainingProgramRepository;
    private final ClassroomPracticeAttemptRepository practiceAttemptRepository;
    private final ClassroomPracticeAttemptHistoryRepository practiceAttemptHistoryRepository;

    @Value("${app.seed.test.enabled:false}")
    private boolean seedEnabled;

    record IeltsUnitSeed(
            String title,
            String description,
            String skill,
            String fileName,
            String tags
    ) {}

    private static final List<IeltsUnitSeed> UNIT_SEEDS = List.of(
            new IeltsUnitSeed(
                    "Listening Section 1–2 & Conversation Foundations",
                    "Nghe bắt từ khóa, điền tên riêng, số điện thoại, ngày tháng và form notes.",
                    "LISTENING",
                    "unit1-listening-s1s2.pdf",
                    "IELTS, Listening, Section 1, Section 2"
            ),
            new IeltsUnitSeed(
                    "Listening Section 3–4 & Academic Lecture Prediction",
                    "Nghe bài giảng học thuật, nhận diện từ nối và dự đoán thông tin.",
                    "LISTENING",
                    "unit2-listening-s3s4.pdf",
                    "IELTS, Listening, Section 3, Section 4"
            ),
            new IeltsUnitSeed(
                    "Reading Matching Headings & True/False/Not Given",
                    "Kỹ thuật Skimming, Scanning và phân biệt True vs Not Given trong đề đọc học thuật.",
                    "READING",
                    "unit3-reading-tfng.pdf",
                    "IELTS, Reading, Matching Headings, TFNG"
            ),
            new IeltsUnitSeed(
                    "Writing Task 1 – Trends, Charts & Diagrams",
                    "Cấu trúc 4 đoạn, từ vựng mô tả xu hướng tăng/giảm/dao động cho Bar chart, Line graph.",
                    "WRITING",
                    "unit4-writing-task1.pdf",
                    "IELTS, Writing, Task 1, Charts"
            ),
            new IeltsUnitSeed(
                    "Writing Task 2 – Opinion & Discussion Essays",
                    "Cấu trúc 5 đoạn, kỹ thuật paraphrase đề bài, lập luận logic và ví dụ minh chứng.",
                    "WRITING",
                    "unit5-writing-task2.pdf",
                    "IELTS, Writing, Task 2, Opinion Essay"
            ),
            new IeltsUnitSeed(
                    "Speaking Part 1 & 2 – Fluency & Cue Cards",
                    "Mở rộng câu trả lời theo công thức PREP; kỹ thuật xử lý cue card 2 phút.",
                    "SPEAKING",
                    "unit6-speaking-part12.pdf",
                    "IELTS, Speaking, Part 1, Part 2"
            ),
            new IeltsUnitSeed(
                    "Speaking Part 3 – In-depth Academic Discussion",
                    "Phát triển luận điểm xã hội, giáo dục, môi trường; dùng từ nối học thuật.",
                    "SPEAKING",
                    "unit7-speaking-part3.pdf",
                    "IELTS, Speaking, Part 3, Social Issues"
            ),
            new IeltsUnitSeed(
                    "Full IELTS Mock Test & Error Log Analysis",
                    "Thi thử toàn diện 4 kỹ năng; phân tích điểm yếu và tối ưu chiến lược phòng thi.",
                    "MIXED",
                    "unit8-mocktest-review.pdf",
                    "IELTS, Mock Test, Error Log, Final Review"
            )
    );

    @Override
    @Transactional
    public void run(String... args) {
        if (!seedEnabled) {
            return;
        }

        log.info("[ChinhTestSeeder] Bắt đầu seed / đồng bộ dữ liệu test cho {}...", LEARNER_EMAIL);

        PackageType classroomType = packageTypeRepository.findByCode(PackageTypeCode.CLASSROOM)
                .orElseThrow(() -> new IllegalStateException("CLASSROOM package type chưa tồn tại. Hãy chạy OnlineCourseDataSeeder trước."));

        User learner = ensureUser(LEARNER_EMAIL, "Chinh CDHE181478", RoleEnum.LEARNER);
        User teacher = ensureUser(TEACHER_EMAIL, "Nguyễn Văn Teacher", RoleEnum.TEACHER);

        // 1. Tạo hoặc đồng bộ Giáo trình chuẩn (Curriculum + Flashcards + Bài luyện tập)
        CurriculumProgram curriculum = ensureCurriculum(teacher);
        List<CurriculumUnit> units = curriculumUnitRepository.findByProgramIdOrderByDisplayOrderAscIdAsc(curriculum.getId());
        synchronizeCurriculumResources(units, teacher);
        units = curriculumUnitRepository.findByProgramIdOrderByDisplayOrderAscIdAsc(curriculum.getId());

        // 2. Tạo hoặc đồng bộ Training Program
        TrainingProgram trainingProgram = ensureTrainingProgram(curriculum);

        // 3. Tạo hoặc cập nhật Lớp học (ClassroomOffering)
        Optional<ClassroomOffering> existingOffering = offeringRepository.findByLearningPackageSlug(PACKAGE_SLUG);
        ClassroomOffering offering;
        if (existingOffering.isPresent()) {
            offering = existingOffering.get();
            offering.setCurriculumProgram(curriculum);
            offering.setTrainingProgram(trainingProgram);
            offering = offeringRepository.save(offering);
            log.info("[ChinhTestSeeder] Lớp học đã tồn tại (ID: {}), đã liên kết giáo trình ID: {}", offering.getId(), curriculum.getId());
        } else {
            offering = createOffering(classroomType, trainingProgram, curriculum, teacher);
        }

        ensureTeacherAssignment(offering, teacher);
        ClassroomEnrollment enrollment = ensureEnrollment(offering, learner, teacher);
        ensureTuitionPayments(enrollment, teacher);

        // 4. Tạo thêm 3 học sinh cùng tham gia lớp
        ensureAdditionalStudents(offering, teacher);

        // 5. Buổi học (Sessions)
        List<ClassroomSession> sessions;
        if (existingOffering.isPresent()) {
            sessions = sessionRepository.findByClassroomOfferingIdOrderBySessionDateAscStartTimeAsc(offering.getId());
            if (sessions.isEmpty()) {
                sessions = createSessions(offering, teacher);
                createSyllabus(offering, sessions);
                createAttendance(sessions, learner, teacher);
                createMaterials(offering, sessions, teacher);
                createAnnouncements(offering, teacher);
            }
        } else {
            sessions = createSessions(offering, teacher);
            createSyllabus(offering, sessions);
            createAttendance(sessions, learner, teacher);
            createMaterials(offering, sessions, teacher);
            createAnnouncements(offering, teacher);
        }

        // 6. Tạo/đồng bộ Bài tập & Bài nộp & Chấm điểm
        createHomework(offering, sessions, units, teacher, learner);

        // 7. Bảng điểm đã publish
        createGradebook(offering, learner, teacher);

        // 8. Tạo dữ liệu đã làm bài luyện tập (Practice attempts) cho học viên
        ensurePracticeAttempts(offering, units, learner);

        log.info("[ChinhTestSeeder] ✅ Seed/Cập nhật hoàn tất! Email: {} | Đã liên kết Giáo trình, Flashcards, Bài luyện tập!", LEARNER_EMAIL);
    }

    // ── Curriculum Program, Units, Flashcards, Practice, Materials ───────────

    private CurriculumProgram ensureCurriculum(User teacher) {
        CurriculumProgram program = curriculumProgramRepository.findBySlug(CURRICULUM_SLUG)
                .orElseGet(() -> curriculumProgramRepository.save(CurriculumProgram.builder()
                        .title("IELTS Intensive 6.5+ - Virtual Curriculum")
                        .code("EL-IELTS-650-V1")
                        .slug(CURRICULUM_SLUG)
                        .deliveryMode(ClassroomDeliveryMode.VIRTUAL)
                        .examCategory("IELTS")
                        .targetBand(BigDecimal.valueOf(6.5))
                        .entryLevel("IELTS 5.0+ hoặc CEFR B1")
                        .outcomes("Nắm vững cả 4 kỹ năng IELTS; đạt band 6.5+; thành thạo chiến thuật phòng thi.")
                        .teacherGuide("Mỗi unit gồm tài liệu trung tâm, luyện tập, bộ flashcard từ vựng và bài tập có deadline.")
                        .interactionActivities("Live practice, pair speaking, answer review, error log và mock test.")
                        .totalSessions(8)
                        .status("APPROVED")
                        .virtualPlatform("GOOGLE_MEET")
                        .recordingAllowed(true)
                        .recordingAvailableDays(30)
                        .materialsDownloadable(true)
                        .deviceCheckRequired(true)
                        .micRequired(true)
                        .speakerRequired(true)
                        .autoAttendanceEnabled(true)
                        .displayOrder(1)
                        .reviewedBy(teacher)
                        .reviewedAt(LocalDateTime.now())
                        .build()));

        if (curriculumUnitRepository.findByProgramIdOrderByDisplayOrderAscIdAsc(program.getId()).isEmpty()) {
            for (int index = 0; index < UNIT_SEEDS.size(); index++) {
                IeltsUnitSeed seed = UNIT_SEEDS.get(index);
                CenterMaterialLibraryItem material = ensureCenterMaterial(seed, index + 1, teacher);
                ExerciseBankItem exercise = ensureExercise(seed, index + 1, teacher);
                FlashcardSet flashcards = ensureFlashcards(seed, index + 1);

                CurriculumUnit unit = CurriculumUnit.builder()
                        .program(program)
                        .displayOrder(index + 1)
                        .title("Unit " + (index + 1) + " – " + seed.title())
                        .description(seed.description())
                        .sessionPlan("Warm-up 10 phút; chiến thuật 25 phút; guided practice 35 phút; review & Q&A 20 phút.")
                        .build();
                unit.getMaterialRefs().add(CurriculumMaterialRef.builder()
                        .unit(unit).material(material).displayOrder(1).note("Tài liệu bài học chuẩn của trung tâm").build());
                unit.getExerciseRefs().add(CurriculumExerciseRef.builder()
                        .unit(unit).exercise(exercise).displayOrder(1).note("Bài luyện tập củng cố kiến thức").build());
                unit.getFlashcardRefs().add(CurriculumFlashcardRef.builder()
                        .unit(unit).flashcardSet(flashcards).displayOrder(1).note("Từ vựng trọng tâm ôn trước và sau buổi học").build());
                curriculumUnitRepository.save(unit);
            }
        }
        return program;
    }

    private void synchronizeCurriculumResources(List<CurriculumUnit> units, User teacher) {
        for (int index = 0; index < Math.min(units.size(), UNIT_SEEDS.size()); index++) {
            CurriculumUnit unit = units.get(index);
            IeltsUnitSeed seed = UNIT_SEEDS.get(index);
            int unitNumber = index + 1;
            CenterMaterialLibraryItem material = ensureCenterMaterial(seed, unitNumber, teacher);
            ExerciseBankItem exercise = ensureExercise(seed, unitNumber, teacher);
            FlashcardSet flashcards = ensureFlashcards(seed, unitNumber);

            if (unit.getMaterialRefs().stream().noneMatch(ref -> ref.getMaterial().getId().equals(material.getId()))) {
                unit.getMaterialRefs().add(CurriculumMaterialRef.builder()
                        .unit(unit).material(material).displayOrder(1).note("Tài liệu bài học chuẩn của trung tâm").build());
            }
            if (unit.getExerciseRefs().stream().noneMatch(ref -> ref.getExercise().getId().equals(exercise.getId()))) {
                unit.getExerciseRefs().add(CurriculumExerciseRef.builder()
                        .unit(unit).exercise(exercise).displayOrder(1).note("Bài luyện tập củng cố kiến thức").build());
            }
            if (unit.getFlashcardRefs().stream().noneMatch(ref -> ref.getFlashcardSet().getId().equals(flashcards.getId()))) {
                unit.getFlashcardRefs().add(CurriculumFlashcardRef.builder()
                        .unit(unit).flashcardSet(flashcards).displayOrder(1).note("Từ vựng trọng tâm ôn trước và sau buổi học").build());
            }
            curriculumUnitRepository.save(unit);
        }
    }

    private CenterMaterialLibraryItem ensureCenterMaterial(IeltsUnitSeed seed, int unitNumber, User teacher) {
        String title = "IELTS 6.5+ Unit " + unitNumber + " – " + seed.title();
        return centerMaterialRepository.findAllByOrderByUpdatedAtDescIdDesc().stream()
                .filter(item -> title.equalsIgnoreCase(item.getTitle()))
                .findFirst()
                .orElseGet(() -> centerMaterialRepository.save(CenterMaterialLibraryItem.builder()
                        .title(title)
                        .description(seed.description())
                        .fileUrl(MATERIAL_BASE_URL + seed.fileName())
                        .fileType("PDF")
                        .materialType("LESSON_NOTE")
                        .provider("EnglishLab")
                        .examCategory("IELTS")
                        .ieltsBandMin(BigDecimal.valueOf(5.0))
                        .ieltsBandMax(BigDecimal.valueOf(7.5))
                        .skill(seed.skill())
                        .tags(seed.tags())
                        .status("PUBLISHED")
                        .createdBy(teacher)
                        .updatedBy(teacher)
                        .build()));
    }

    private ExerciseBankItem ensureExercise(IeltsUnitSeed seed, int unitNumber, User teacher) {
        String title = "IELTS 6.5+ Unit " + unitNumber + " Practice";
        ExerciseBankItem exercise = exerciseRepository.findAllByOrderByUpdatedAtDesc().stream()
                .filter(item -> title.equalsIgnoreCase(item.getTitle()))
                .findFirst()
                .orElseGet(() -> ExerciseBankItem.builder()
                        .title(title)
                        .skill(seed.skill())
                        .level("IELTS 5.0-6.5+")
                        .prompt("Hoàn thành bài luyện tập " + seed.title() + " và ghi lại nguyên nhân các câu sai.")
                        .answerKey("{\"1\":\"B\",\"2\":\"A\",\"3\":\"C\"}")
                        .explanation("Đối chiếu đáp án, xác định nguyên nhân câu chưa đúng và ôn lại lý thuyết trọng tâm.")
                        .tags(seed.tags())
                        .active(true)
                        .createdBy(teacher)
                        .build());
        exercise.setExerciseType("PRACTICE");
        if (exercise.getPrompt() == null || !exercise.getPrompt().trim().startsWith("{")) {
            exercise.setPrompt(buildSystemPracticeConfig(seed, unitNumber));
            exercise.setAnswerKey("{\"1\":\"B\",\"2\":\"A\",\"3\":\"C\"}");
            exercise.setExplanation("Xem lại đáp án, phân loại lỗi sai vào Error Log và thực hiện lại lượt mới để cải thiện kết quả.");
        }
        return exerciseRepository.save(exercise);
    }

    private String buildSystemPracticeConfig(IeltsUnitSeed seed, int unitNumber) {
        String skill = seed.skill();
        String type = switch (skill) {
            case "LISTENING" -> "ielts_listening_exam";
            case "READING" -> "ielts_reading_exam";
            case "WRITING" -> "ielts_writing_exam";
            case "SPEAKING" -> "ielts_speaking_exam";
            default -> "ielts_reading_exam";
        };
        return """
                {
                  "type":"%s",
                  "key":"ielts-650-unit-%d-practice",
                  "title":"IELTS 6.5+ Unit %d Practice - %s",
                  "durationMinutes":15,
                  "rules":["Mỗi câu chỉ chọn một đáp án đúng nhất","Có thể luyện lại nhiều lần để nắm vững kiến thức","Kết quả ghi nhận vào lịch sử luyện tập cá nhân"],
                  "parts":[{
                    "key":"part_1",
                    "partNumber":%d,
                    "title":"%s Practice - %s",
                    "questionRange":"Questions 1-3",
                    "passage":{"title":"Hướng dẫn & Ngữ cảnh bài luyện tập","paragraphs":[{"label":"Target Skill: %s","text":"%s"}]},
                    "questionGroups":[{
                      "type":"single_choice",
                      "title":"Chọn câu trả lời chính xác nhất",
                      "instructions":"Đọc kỹ yêu cầu và chọn 1 đáp án A, B, C hoặc D.",
                      "questions":[
                        {"number":1,"prompt":"Chiến thuật quan trọng nhất khi xử lý phần này là gì?","options":[{"value":"A","label":"Bỏ qua ngữ cảnh và chọn ngẫu nhiên"},{"value":"B","label":"Xác định từ khóa, ngữ cảnh và cấu trúc câu trước khi trả lời"},{"value":"C","label":"Chỉ chọn câu có từ dài nhất"},{"value":"D","label":"Không cần đọc lại bài sau khi làm"}]},
                        {"number":2,"prompt":"Bước kiểm tra cần thiết trước khi xác nhận đáp án là gì?","options":[{"value":"A","label":"Đối chiếu lại bằng chứng trong bài hoặc đề bài"},{"value":"B","label":"Đổi đáp án mà không có cơ sở"},{"value":"C","label":"Chỉ nhìn vào 1 từ đơn lẻ"},{"value":"D","label":"Để trống câu trả lời"}]},
                        {"number":3,"prompt":"Phương pháp ôn tập hiệu quả nhất sau khi hoàn thành bài luyện tập là gì?","options":[{"value":"A","label":"Bỏ qua các câu làm sai"},{"value":"B","label":"Làm lại ngay lập tức mà không xem giải thích"},{"value":"C","label":"Phân loại lỗi sai vào Error Log và đọc kỹ lời giải"},{"value":"D","label":"Chỉ học thuộc thứ tự chữ cái A/B/C/D"}]}
                      ]
                    }]
                  }]
                }
                """.formatted(type, unitNumber, unitNumber, seed.title(), unitNumber, skill, seed.title(), skill, seed.description());
    }

    private FlashcardSet ensureFlashcards(IeltsUnitSeed seed, int unitNumber) {
        String title = "IELTS 6.5+ Unit " + unitNumber + " Flashcards";
        FlashcardSet set = flashcardSetRepository.findByTitleIgnoreCase(title)
                .orElseGet(() -> FlashcardSet.builder().title(title).build());
        set.setDescription("Từ vựng trọng tâm cho " + seed.title());
        set.setExamCategory("IELTS");
        set.setSkill(seed.skill());
        set.setTags(seed.tags());
        set.setCardsJson(flashcardsJson(unitNumber));
        set.setStatus("PUBLISHED");
        set.setDisplayOrder(unitNumber);
        return flashcardSetRepository.save(set);
    }

    private String flashcardsJson(int unitNumber) {
        return switch (unitNumber) {
            case 1 -> """
                    [
                      {"front":"accommodation","back":"chỗ ở, nơi lưu trú","example":"The university provides affordable student accommodation."},
                      {"front":"reservation","back":"sự đặt chỗ trước","example":"I would like to confirm my table reservation for tonight."},
                      {"front":"departure","back":"sự khởi hành, xuất phát","example":"Our departure time was delayed by 30 minutes due to weather."},
                      {"front":"itinerary","back":"lịch trình chuyến đi","example":"Please check the travel itinerary before leaving for the airport."},
                      {"front":"confirmation","back":"xác nhận, thư chứng nhận","example":"You will receive a booking confirmation email shortly."},
                      {"front":"excursion","back":"chuyến du ngoạn ngắn ngày","example":"We booked a full-day excursion to the national park."}
                    ]
                    """;
            case 2 -> """
                    [
                      {"front":"methodology","back":"phương pháp luận nghiên cứu","example":"The research methodology relies on quantitative surveys."},
                      {"front":"hypothesis","back":"giả thuyết khoa học","example":"The experimental data fully supported our initial hypothesis."},
                      {"front":"empirical","back":"thực nghiệm, dựa trên quan sát","example":"We need empirical evidence to substantiate this claim."},
                      {"front":"bibliography","back":"thư mục tham khảo","example":"Always format your bibliography using standard APA style."},
                      {"front":"qualitative","back":"định tính (dữ liệu/nghiên cứu)","example":"Qualitative interviews provided rich in-depth insights."},
                      {"front":"quantitative","back":"định lượng (dữ liệu số liệu)","example":"Quantitative analysis showed a 25% increase in efficiency."}
                    ]
                    """;
            case 3 -> """
                    [
                      {"front":"contradiction","back":"sự mâu thuẫn, trái ngược","example":"There is a clear contradiction in the author's statement."},
                      {"front":"substantiate","back":"chứng minh, xác thực","example":"More clinical trials are needed to substantiate this result."},
                      {"front":"comprehensive","back":"toàn diện, bao quát","example":"The report provides a comprehensive analysis of climate trends."},
                      {"front":"subsequent","back":"xảy ra sau đó, tiếp theo","example":"The initial trial failed, but subsequent tests succeeded."},
                      {"front":"obsolete","back":"lỗi thời, không còn dùng","example":"Outdated machinery has become completely obsolete."},
                      {"front":"predominantly","back":"chủ yếu, phần lớn","example":"The region's economy is predominantly agricultural."}
                    ]
                    """;
            case 4 -> """
                    [
                      {"front":"fluctuate","back":"dao động, biến động liên tục","example":"Oil prices fluctuated widely during the first quarter."},
                      {"front":"skyrocket","back":"tăng vọt, tăng đột biến","example":"Sales skyrocketed after the product launch in May."},
                      {"front":"plummet","back":"giảm mạnh, rơi thẳng đứng","example":"Tourism revenues plummeted during the pandemic."},
                      {"front":"plateau","back":"chạm ngưỡng ổn định, đi ngang","example":"After two years of growth, user numbers began to plateau."},
                      {"front":"respectively","back":"lần lượt theo thứ tự","example":"Class A and B scored 85 and 92, respectively."},
                      {"front":"substantial","back":"đáng kể, có giá trị lớn","example":"There was a substantial rise in renewable energy investment."}
                    ]
                    """;
            case 5 -> """
                    [
                      {"front":"detrimental","back":"có hại, gây bất lợi","example":"Excessive screen time has a detrimental impact on health."},
                      {"front":"paramount","back":"tối quan trọng, hàng đầu","example":"Safety remains of paramount importance in construction."},
                      {"front":"inevitable","back":"không thể tránh khỏi","example":"Automation is an inevitable trend in modern manufacturing."},
                      {"front":"exacerbate","back":"làm trầm trọng thêm","example":"Traffic congestion exacerbates air pollution in urban areas."},
                      {"front":"unprecedented","back":"chưa từng có tiền lệ","example":"The city witnessed unprecedented growth over the past decade."},
                      {"front":"foster","back":"thúc đẩy, nuôi dưỡng","example":"Good educators foster critical thinking in their students."}
                    ]
                    """;
            case 6 -> """
                    [
                      {"front":"over the moon","back":"vô cùng hạnh phúc, vui sướng","example":"I was over the moon when I achieved my target IELTS band."},
                      {"front":"once in a blue moon","back":"rất hiếm khi xảy ra","example":"I only eat fast food once in a blue moon."},
                      {"front":"hit the books","back":"bắt tay vào học tập chăm chỉ","example":"It is time to hit the books for the upcoming exams."},
                      {"front":"take with a pinch of salt","back":"tiếp nhận có chọn lọc, hoài nghi","example":"You should take online rumors with a pinch of salt."},
                      {"front":"cost an arm and a leg","back":"rất đắt đỏ, tốn kém","example":"Studying abroad at top universities can cost an arm and a leg."},
                      {"front":"touch base","back":"liên lạc nhanh, trao đổi ngắn","example":"Let us touch base next Monday before the group presentation."}
                    ]
                    """;
            case 7 -> """
                    [
                      {"front":"sustainable development","back":"phát triển bền vững","example":"Clean energy is essential for sustainable development."},
                      {"front":"technological breakthrough","back":"đột phá công nghệ","example":"Artificial intelligence represents a major technological breakthrough."},
                      {"front":"cultural heritage","back":"di sản văn hóa","example":"Preserving cultural heritage is a national priority."},
                      {"front":"socioeconomic gap","back":"khoảng cách kinh tế - xã hội","example":"Free education helps narrow the socioeconomic gap."},
                      {"front":"moral obligation","back":"nghĩa vụ đạo đức","example":"Every citizen has a moral obligation to protect nature."},
                      {"front":"profound impact","back":"tác động sâu sắc","example":"Digital connectivity has had a profound impact on society."}
                    ]
                    """;
            default -> """
                    [
                      {"front":"time management","back":"quản lý thời gian","example":"Effective time management is crucial in the reading test."},
                      {"front":"distraction trap","back":"bẫy đánh lạc hướng","example":"Examiners often use distraction traps in listening sections."},
                      {"front":"cohesion and coherence","back":"sự liên kết và mạch lạc","example":"Use linking devices to improve cohesion and coherence."},
                      {"front":"lexical resource","back":"vốn từ vựng, độ phong phú từ","example":"Demonstrating a wide lexical resource raises your score."},
                      {"front":"grammatical range","back":"sự đa dạng cấu trúc ngữ pháp","example":"Combine simple and complex sentences for grammatical range."},
                      {"front":"elimination strategy","back":"phương pháp loại trừ đáp án","example":"Use the elimination strategy when unsure of the answer."}
                    ]
                    """;
        };
    }

    private TrainingProgram ensureTrainingProgram(CurriculumProgram curriculum) {
        return trainingProgramRepository.findBySlug(TRAINING_PROGRAM_SLUG)
                .orElseGet(() -> trainingProgramRepository.save(TrainingProgram.builder()
                        .title("IELTS Intensive 6.5+ Program")
                        .code("TR-IELTS-650-V1")
                        .slug(TRAINING_PROGRAM_SLUG)
                        .deliveryMode(ClassroomDeliveryMode.VIRTUAL)
                        .curriculumProgram(curriculum)
                        .shortDescription("Chương trình đào tạo IELTS 6.5+ chuyên sâu 8 tuần.")
                        .description("Chương trình bám sát 4 kỹ năng IELTS chuẩn quốc tế, tích hợp bài giảng, bài tập, flashcard và bài thi thử.")
                        .price(BigDecimal.valueOf(5_200_000))
                        .salePrice(BigDecimal.valueOf(4_690_000))
                        .status(PackageStatus.PUBLISHED)
                        .displayOrder(1)
                        .build()));
    }

    // ── Practice Attempts ────────────────────────────────────────────────────

    private void ensurePracticeAttempts(ClassroomOffering offering, List<CurriculumUnit> units, User learner) {
        if (units.isEmpty()) return;

        // Attempt 1: Unit 1 Practice (100% score)
        if (units.size() > 0 && !units.get(0).getExerciseRefs().isEmpty()) {
            ExerciseBankItem ex1 = units.get(0).getExerciseRefs().get(0).getExercise();
            if (practiceAttemptRepository.findByClassroomOfferingIdAndStudentIdAndExerciseId(offering.getId(), learner.getId(), ex1.getId()).isEmpty()) {
                practiceAttemptRepository.save(ClassroomPracticeAttempt.builder()
                        .classroomOffering(offering)
                        .student(learner)
                        .exercise(ex1)
                        .responseText("{\"1\":\"B\",\"2\":\"A\",\"3\":\"C\"}")
                        .completedAt(LocalDateTime.now().minusWeeks(4))
                        .build());
                practiceAttemptHistoryRepository.save(ClassroomPracticeAttemptHistory.builder()
                        .classroomOffering(offering)
                        .student(learner)
                        .exercise(ex1)
                        .attemptNumber(1)
                        .responseText("{\"1\":\"B\",\"2\":\"A\",\"3\":\"C\"}")
                        .answersJson("{\"1\":\"B\",\"2\":\"A\",\"3\":\"C\"}")
                        .correctAnswers(3)
                        .totalQuestions(3)
                        .scorePercent(100.0)
                        .durationSeconds(420)
                        .startedAt(LocalDateTime.now().minusWeeks(4).minusMinutes(7))
                        .completedAt(LocalDateTime.now().minusWeeks(4))
                        .build());
            }
        }

        // Attempt 2: Unit 2 Practice (66.7% score)
        if (units.size() > 1 && !units.get(1).getExerciseRefs().isEmpty()) {
            ExerciseBankItem ex2 = units.get(1).getExerciseRefs().get(0).getExercise();
            if (practiceAttemptRepository.findByClassroomOfferingIdAndStudentIdAndExerciseId(offering.getId(), learner.getId(), ex2.getId()).isEmpty()) {
                practiceAttemptRepository.save(ClassroomPracticeAttempt.builder()
                        .classroomOffering(offering)
                        .student(learner)
                        .exercise(ex2)
                        .responseText("{\"1\":\"B\",\"2\":\"A\",\"3\":\"A\"}")
                        .completedAt(LocalDateTime.now().minusWeeks(3))
                        .build());
                practiceAttemptHistoryRepository.save(ClassroomPracticeAttemptHistory.builder()
                        .classroomOffering(offering)
                        .student(learner)
                        .exercise(ex2)
                        .attemptNumber(1)
                        .responseText("{\"1\":\"B\",\"2\":\"A\",\"3\":\"A\"}")
                        .answersJson("{\"1\":\"B\",\"2\":\"A\",\"3\":\"A\"}")
                        .correctAnswers(2)
                        .totalQuestions(3)
                        .scorePercent(66.7)
                        .durationSeconds(510)
                        .startedAt(LocalDateTime.now().minusWeeks(3).minusMinutes(9))
                        .completedAt(LocalDateTime.now().minusWeeks(3))
                        .build());
            }
        }
    }

    // ── Offering ──────────────────────────────────────────────────────────────

    private ClassroomOffering createOffering(PackageType classroomType, TrainingProgram trainingProgram,
                                             CurriculumProgram curriculum, User teacher) {
        LearningPackage pkg = learningPackageRepository.save(LearningPackage.builder()
                .packageType(classroomType)
                .title(CLASS_TITLE)
                .slug(PACKAGE_SLUG)
                .shortDescription("Lớp học IELTS 6.5+ dành để test đầy đủ tính năng.")
                .description("Lớp test cho chinh: có bài tập quá hạn, bài mới, buổi học đang diễn ra và sắp tới. Bao gồm giáo trình, flashcard, bài luyện tập, điểm danh, bảng điểm, học phí, tài liệu, thông báo.")
                .targetScore("IELTS 6.5+")
                .duration("8 tuần")
                .studyMode("Virtual · Google Meet")
                .price(BigDecimal.valueOf(5_200_000))
                .salePrice(BigDecimal.valueOf(4_690_000))
                .status(PackageStatus.PUBLISHED)
                .featured(true)
                .createdBy(teacher)
                .build());

        return offeringRepository.save(ClassroomOffering.builder()
                .learningPackage(pkg)
                .deliveryMode(ClassroomDeliveryMode.VIRTUAL)
                .trainingProgram(trainingProgram)
                .curriculumProgram(curriculum)
                .status(ClassroomOfferingStatus.ACTIVE)
                .entryLevel("IELTS 5.0+ hoặc CEFR B1")
                .targetOutcome("Đạt IELTS 6.5+; thành thạo cả 4 kỹ năng; có chiến lược thi thực tế.")
                .maxCapacity(20)
                .startDate(LocalDate.now().minusWeeks(5))
                .endDate(LocalDate.now().plusWeeks(3))
                .primaryTeacher(teacher)
                .larkMeetingStatus(LarkMeetingStatus.NOT_CREATED)
                .recordingVisible(false)
                .syllabusSummary("8 buổi bám sát 4 kỹ năng IELTS: Listening, Reading, Writing, Speaking. Mỗi buổi gồm lý thuyết + luyện tập + flashcards + feedback cá nhân.")
                .programOutcomes("Đạt band 6.5 IELTS tổng. Viết task 1 và task 2 đạt band 6.0+. Nói liên tục 2 phút không dừng.")
                .teacherGuide("Mỗi buổi: review 10 phút + dạy chiến lược 30 phút + luyện tập có hướng dẫn 40 phút + Q&A 10 phút.")
                .interactionActivities("Mock test, pair speaking, error log review, timed writing, peer feedback.")
                .build());
    }

    // ── Teacher assignment ────────────────────────────────────────────────────

    private void ensureTeacherAssignment(ClassroomOffering offering, User teacher) {
        if (teacherAssignmentRepository.findAllByClassroomOfferingIdAndTeacherId(offering.getId(), teacher.getId()).isEmpty()) {
            teacherAssignmentRepository.save(ClassroomTeacherAssignment.builder()
                        .classroomOffering(offering)
                        .teacher(teacher)
                        .role(ClassroomTeacherRole.PRIMARY)
                        .effectiveFrom(offering.getStartDate())
                        .reason("Giáo viên phụ trách lớp IELTS test Chinh")
                        .build());
        }
    }

    // ── Enrollment & tuition ──────────────────────────────────────────────────

    private ClassroomEnrollment ensureEnrollment(ClassroomOffering offering, User learner, User teacher) {
        return enrollmentRepository.findByStudentIdAndClassroomOfferingId(learner.getId(), offering.getId())
                .orElseGet(() -> enrollmentRepository.save(ClassroomEnrollment.builder()
                        .student(learner)
                        .classroomOffering(offering)
                        .status(ClassroomEnrollmentStatus.ENROLLED)
                        .registrationStatus(ClassroomRegistrationStatus.ASSIGNED)
                        .tuitionAmountDue(BigDecimal.valueOf(4_690_000))
                        .tuitionAmountPaid(BigDecimal.valueOf(4_690_000))
                        .tuitionDepositPaid(BigDecimal.valueOf(1_000_000))
                        .tuitionSettlementType(TuitionSettlementType.NONE)
                        .enrolledAt(LocalDateTime.now().minusWeeks(5))
                        .assignedAt(LocalDateTime.now().minusWeeks(5))
                        .assignedBy(teacher)
                        .confirmedAt(LocalDateTime.now().minusWeeks(5))
                        .confirmedBy(teacher)
                        .assignmentNote("Học viên tham gia lớp: " + learner.getEmail())
                        .build()));
    }

    private void ensureAdditionalStudents(ClassroomOffering offering, User teacher) {
        String[][] studentsData = {
                {"student.test1@englishlab.vn", "Nguyễn Hoàng Nam"},
                {"student.test2@englishlab.vn", "Trần Thị Mai"},
                {"student.test3@englishlab.vn", "Lê Minh Khang"}
        };
        for (String[] data : studentsData) {
            User student = ensureUser(data[0], data[1], RoleEnum.LEARNER);
            ensureEnrollment(offering, student, teacher);
        }
    }

    private void ensureTuitionPayments(ClassroomEnrollment enrollment, User teacher) {
        if (!tuitionPaymentRepository.findByEnrollmentIdOrderByCreatedAtDesc(enrollment.getId()).isEmpty()) {
            return;
        }
        // Đặt cọc lần 1 (5 tuần trước)
        tuitionPaymentRepository.save(ClassroomTuitionPayment.builder()
                .enrollment(enrollment)
                .amount(BigDecimal.valueOf(1_000_000))
                .paymentKind(TuitionPaymentKind.DEPOSIT)
                .note("Đặt cọc giữ chỗ – chuyển khoản Vietcombank")
                .recordedBy(teacher)
                .build());
        // Thanh toán phần còn lại (3 tuần trước)
        tuitionPaymentRepository.save(ClassroomTuitionPayment.builder()
                .enrollment(enrollment)
                .amount(BigDecimal.valueOf(3_690_000))
                .paymentKind(TuitionPaymentKind.PARTIAL)
                .note("Thanh toán phần còn lại – MoMo QR")
                .recordedBy(teacher)
                .build());
    }

    // ── Sessions ──────────────────────────────────────────────────────────────

    private List<ClassroomSession> createSessions(ClassroomOffering offering, User teacher) {
        LocalDate today = LocalDate.now();
        ClassroomSession s1 = saveSession(offering, teacher, today.minusWeeks(5), 19, 21, ClassroomSessionStatus.COMPLETED,
                "Buổi 1: Tổng quan IELTS & Listening Section 1-2");
        ClassroomSession s2 = saveSession(offering, teacher, today.minusWeeks(4), 19, 21, ClassroomSessionStatus.COMPLETED,
                "Buổi 2: Listening Section 3-4 & chiến thuật dự đoán đáp án");
        ClassroomSession s3 = saveSession(offering, teacher, today.minusWeeks(3), 19, 21, ClassroomSessionStatus.COMPLETED,
                "Buổi 3: Reading – Matching Headings & True/False/Not Given");
        ClassroomSession s4 = saveSession(offering, teacher, today.minusWeeks(2), 19, 21, ClassroomSessionStatus.COMPLETED,
                "Buổi 4: Writing Task 1 – Bar chart & Line graph");
        ClassroomSession s5 = saveSession(offering, teacher, today, 19, 21, ClassroomSessionStatus.OPEN,
                "Buổi 5: Writing Task 2 – Opinion essay (Đang diễn ra – có thể Join)");
        ClassroomSession s6 = saveSession(offering, teacher, today.plusDays(1), 19, 21, ClassroomSessionStatus.SCHEDULED,
                "Buổi 6: Speaking Part 1 & 2 – chiến thuật mở rộng ý");
        ClassroomSession s7 = saveSession(offering, teacher, today.plusDays(3), 19, 21, ClassroomSessionStatus.SCHEDULED,
                "Buổi 7: Speaking Part 3 – thảo luận chủ đề xã hội");
        ClassroomSession s8 = saveSession(offering, teacher, today.plusWeeks(1), 19, 21, ClassroomSessionStatus.SCHEDULED,
                "Buổi 8: Mock test toàn phần & tổng kết khóa học");

        return List.of(s1, s2, s3, s4, s5, s6, s7, s8);
    }

    private ClassroomSession saveSession(ClassroomOffering offering, User teacher,
                                          LocalDate date, int startHour, int endHour,
                                          ClassroomSessionStatus status, String content) {
        return sessionRepository.save(ClassroomSession.builder()
                .classroomOffering(offering)
                .sessionDate(date)
                .startTime(LocalTime.of(startHour, 30))
                .endTime(LocalTime.of(endHour, 0))
                .teacher(teacher)
                .status(status)
                .deliveryMode(ClassroomDeliveryMode.VIRTUAL)
                .larkMeetingStatus(LarkMeetingStatus.NOT_CREATED)
                .larkSyncStatus("PENDING")
                .recordingVisible(false)
                .sessionContent(content)
                .note("Lớp test chinhcdhe181478 – buổi " + date)
                .build());
    }

    // ── Syllabus ──────────────────────────────────────────────────────────────

    private void createSyllabus(ClassroomOffering offering, List<ClassroomSession> sessions) {
        String[][] syllabusData = {
                {"Buổi 1 – Listening Section 1–2", "Nghe hội thoại ngắn, điền form và ghi chú.", "Warm-up nghe thụ động; chiến thuật đọc câu hỏi trước; guided practice 2 đề; sửa lỗi.", "Luyện thêm 1 đề Section 1-2 tại nhà."},
                {"Buổi 2 – Listening Section 3–4", "Nghe thảo luận học thuật và bài giảng.", "Phân tích cấu trúc Section 3-4; chiến thuật dự đoán; luyện 2 đề.", "Tóm tắt 5 lỗi phổ biến nhất của cá nhân."},
                {"Buổi 3 – Reading Matching & T/F/NG", "Chiến thuật làm nhanh dạng Matching Headings và True/False/Not Given.", "Scanning kỹ thuật; không đọc toàn văn; luyện 1 passage dài.", "Hoàn thành bài Reading Section 1 nộp file."},
                {"Buổi 4 – Writing Task 1", "Viết mô tả biểu đồ Bar chart và Line graph.", "Cấu trúc 4 đoạn; từ vựng mô tả xu hướng; luyện viết tại lớp 20 phút.", "Viết 1 bài Task 1 hoàn chỉnh (150 từ) nộp online."},
                {"Buổi 5 – Writing Task 2 (Hôm nay)", "Opinion essay – lập luận và ví dụ minh chứng.", "Cấu trúc 5 đoạn; cách mở bài paraphrase; luyện outline 15 phút.", "Viết bài Task 2 hoàn chỉnh (250 từ) – deadline ngày mai."},
                {"Buổi 6 – Speaking Part 1 & 2", "Mở rộng câu trả lời Part 1; kỹ thuật cue card Part 2.", "Role-play hỏi đáp; chiến thuật PREP; luyện cue card 4 chủ đề.", "Ghi âm 1 cue card và nộp link."},
                {"Buổi 7 – Speaking Part 3", "Thảo luận chủ đề xã hội – giáo dục, công nghệ, môi trường.", "Cách đưa ra quan điểm có căn cứ; từ nối học thuật; luyện pair speaking.", "Ghi âm trả lời 3 câu Part 3 theo chủ đề môi trường."},
                {"Buổi 8 – Mock Test & Tổng kết", "Làm toàn bộ 4 phần IELTS trong điều kiện thi thực tế.", "Full test 2h45m; chữa bài; phân tích điểm yếu từng kỹ năng.", "Hoàn thiện error log cá nhân và nộp trước tốt nghiệp."}
        };

        for (int i = 0; i < syllabusData.length; i++) {
            String[] row = syllabusData[i];
            ClassroomSession session = i < sessions.size() ? sessions.get(i) : null;
            syllabusRepository.save(ClassroomSyllabusItem.builder()
                    .classroomOffering(offering)
                    .title(row[0])
                    .description(row[1])
                    .displayOrder(i + 1)
                    .sessionNumber(i + 1)
                    .linkedSessionId(session != null ? session.getId() : null)
                    .sessionPlan(row[2])
                    .homeworkNotes(row[3])
                    .quizNotes("Quiz ôn bài nằm trong mục Bài tập của buổi học.")
                    .teacherNotes("Review error log đầu buổi kế tiếp; gọi 2-3 học viên trình bày.")
                    .reviewStatus(ContentReviewStatus.APPROVED)
                    .status("PUBLISHED")
                    .build());
        }
    }

    // ── Attendance ────────────────────────────────────────────────────────────

    private void createAttendance(List<ClassroomSession> sessions, User learner, User teacher) {
        if (sessions.isEmpty()) return;
        LocalDate today = LocalDate.now();

        if (sessions.size() > 0) {
            saveAttendance(sessions.get(0), learner, teacher, ClassroomAttendanceStatus.PRESENT,
                    sessions.get(0).getSessionDate().atTime(19, 32), sessions.get(0).getSessionDate().atTime(21, 0), 88, null);
        }
        if (sessions.size() > 1) {
            saveAttendance(sessions.get(1), learner, teacher, ClassroomAttendanceStatus.PRESENT,
                    sessions.get(1).getSessionDate().atTime(19, 28), sessions.get(1).getSessionDate().atTime(21, 0), 92, null);
        }
        if (sessions.size() > 2) {
            saveAttendance(sessions.get(2), learner, teacher, ClassroomAttendanceStatus.LATE,
                    sessions.get(2).getSessionDate().atTime(19, 45), sessions.get(2).getSessionDate().atTime(21, 0), 75, "Đến trễ 15 phút do kẹt xe");
        }
        if (sessions.size() > 3) {
            saveAttendance(sessions.get(3), learner, teacher, ClassroomAttendanceStatus.ABSENT,
                    null, null, null, "Vắng có phép – báo giáo viên trước qua Zalo");
        }
        if (sessions.size() > 4) {
            saveAttendance(sessions.get(4), learner, teacher, ClassroomAttendanceStatus.PRESENT,
                    today.atTime(19, 33), null, null, "Tham gia buổi hôm nay qua Google Meet");
        }
    }

    private void saveAttendance(ClassroomSession session, User learner, User teacher,
                                 ClassroomAttendanceStatus status,
                                 LocalDateTime joinTime, LocalDateTime leaveTime,
                                 Integer durationMinutes, String note) {
        if (attendanceRepository.findBySessionIdAndStudentId(session.getId(), learner.getId()).isPresent()) {
            return;
        }
        ClassroomAttendance.ClassroomAttendanceBuilder builder = ClassroomAttendance.builder()
                .session(session)
                .student(learner)
                .status(status)
                .teacherConfirmed(true)
                .markedBy(teacher)
                .note(note);
        if (joinTime != null) builder.joinTime(joinTime);
        if (leaveTime != null) builder.leaveTime(leaveTime);
        if (durationMinutes != null) builder.durationMinutes(durationMinutes);
        attendanceRepository.save(builder.build());
    }

    // ── Materials ─────────────────────────────────────────────────────────────

    private void createMaterials(ClassroomOffering offering, List<ClassroomSession> sessions, User teacher) {
        saveMaterial(offering, null, teacher, "Tài liệu tổng quan IELTS 2024",
                "https://cdn.englishlab.vn/test/ielts-overview-2024.pdf", "PDF",
                "Hướng dẫn tổng quan cấu trúc đề thi IELTS Academic 2024", "DOCUMENT");
        saveMaterial(offering, null, teacher, "Bảng từ vựng IELTS theo chủ đề",
                "https://cdn.englishlab.vn/test/ielts-vocab-topics.pdf", "PDF",
                "500 từ vựng IELTS chia theo 10 chủ đề: environment, technology, education, health...", "DOCUMENT");
        saveMaterial(offering, null, teacher, "Template lập Error Log cá nhân",
                "https://cdn.englishlab.vn/test/error-log-template.xlsx", "XLSX",
                "File Excel để ghi lại lỗi sai theo từng kỹ năng và buổi học", "TEMPLATE");

        if (sessions.size() > 0) {
            saveMaterial(offering, sessions.get(0), teacher, "Slide Buổi 1 – Listening Overview",
                    "https://cdn.englishlab.vn/test/session1-listening.pdf", "PDF",
                    "Slide giảng dạy buổi 1 – Listening Part 1 & 2", "SLIDE");
        }
        if (sessions.size() > 1) {
            saveMaterial(offering, sessions.get(1), teacher, "Đề luyện Listening Section 3-4",
                    "https://cdn.englishlab.vn/test/listening-practice-s3s4.pdf", "PDF",
                    "2 đề luyện tập Section 3-4 với đáp án chi tiết", "EXERCISE");
        }
        if (sessions.size() > 2) {
            saveMaterial(offering, sessions.get(2), teacher, "Slide Buổi 3 – Reading Strategies",
                    "https://cdn.englishlab.vn/test/session3-reading.pdf", "PDF",
                    "Chiến thuật Matching Headings và True/False/Not Given", "SLIDE");
        }
        if (sessions.size() > 3) {
            saveMaterial(offering, sessions.get(3), teacher, "Writing Task 1 – Mẫu bài band 7.0",
                    "https://cdn.englishlab.vn/test/writing-task1-sample-band7.pdf", "PDF",
                    "3 bài mẫu band 7.0 với annotations chi tiết từ giáo viên", "SAMPLE");
        }
        if (sessions.size() > 4) {
            saveMaterial(offering, sessions.get(4), teacher, "Writing Task 2 – Outline Framework",
                    "https://cdn.englishlab.vn/test/writing-task2-framework.pdf", "PDF",
                    "Khung dàn bài 5 đoạn cho Opinion, Discussion, Problem-Solution essays", "SLIDE");
        }
    }

    private void saveMaterial(ClassroomOffering offering, ClassroomSession session, User teacher,
                               String title, String url, String fileType, String description, String materialType) {
        materialRepository.save(ClassroomMaterial.builder()
                .classroomOffering(offering)
                .session(session)
                .title(title)
                .fileUrl(url)
                .fileType(fileType)
                .description(description)
                .materialType(materialType)
                .visibility("LEARNERS_IN_CLASS")
                .sourceType("CLASSROOM_UPLOAD")
                .uploadedBy(teacher)
                .reviewStatus(ContentReviewStatus.APPROVED)
                .build());
    }

    // ── Announcements ─────────────────────────────────────────────────────────

    private void createAnnouncements(ClassroomOffering offering, User teacher) {
        saveAnnouncement(offering, teacher,
                "📚 Chào mừng đến lớp IELTS Intensive 6.5+",
                "Chào các bạn! Lớp học bắt đầu từ tuần này. Giáo trình, bài tập, flashcard và bài luyện tập của từng buổi đã được kích hoạt đầy đủ. Chúc các bạn học tốt!");
        saveAnnouncement(offering, teacher,
                "📝 Nhắc nhở: Nộp bài Writing Task 1 trước hôm nay 23:59",
                "Bài Writing Task 1 (Buổi 4) đến hạn hôm nay lúc 23:59. Các bạn chưa nộp vui lòng hoàn thành gấp trong mục Bài tập.");
        saveAnnouncement(offering, teacher,
                "🎯 Buổi 5 tối nay – Writing Task 2 Opinion Essay",
                "Tối nay 19:30 chúng ta học Writing Task 2. Các bạn chuẩn bị sẵn 1 chủ đề yêu thích để luyện outline ngay tại lớp. Link Google Meet sẽ mở lúc 19:25.");
        saveAnnouncement(offering, teacher,
                "🔔 Lịch thi thử Mock Test – Buổi 8",
                "Buổi 8 sẽ là mock test toàn phần 2h45m theo format thi thật. Các bạn cần chuẩn bị tai nghe và đường truyền mạng ổn định.");
    }

    private void saveAnnouncement(ClassroomOffering offering, User teacher, String title, String content) {
        announcementRepository.save(ClassroomAnnouncement.builder()
                .classroomOffering(offering)
                .title(title)
                .content(content)
                .createdBy(teacher)
                .build());
    }

    // ── Homework ──────────────────────────────────────────────────────────────

    private void createHomework(ClassroomOffering offering, List<ClassroomSession> sessions,
                                 List<CurriculumUnit> units, User teacher, User learner) {
        LocalDate today = LocalDate.now();

        ClassroomSession s1 = sessions.size() > 0 ? sessions.get(0) : null;
        ClassroomSession s2 = sessions.size() > 1 ? sessions.get(1) : null;
        ClassroomSession s3 = sessions.size() > 2 ? sessions.get(2) : null;
        ClassroomSession s4 = sessions.size() > 3 ? sessions.get(3) : null;
        ClassroomSession s5 = sessions.size() > 4 ? sessions.get(4) : null;
        ClassroomSession s6 = sessions.size() > 5 ? sessions.get(5) : null;
        ClassroomSession s7 = sessions.size() > 6 ? sessions.get(6) : null;
        ClassroomSession s8 = sessions.size() > 7 ? sessions.get(7) : null;

        CurriculumUnit u1 = units.size() > 0 ? units.get(0) : null;
        CurriculumUnit u2 = units.size() > 1 ? units.get(1) : null;
        CurriculumUnit u3 = units.size() > 2 ? units.get(2) : null;
        CurriculumUnit u4 = units.size() > 3 ? units.get(3) : null;
        CurriculumUnit u5 = units.size() > 4 ? units.get(4) : null;
        CurriculumUnit u6 = units.size() > 5 ? units.get(5) : null;
        CurriculumUnit u7 = units.size() > 6 ? units.get(6) : null;
        CurriculumUnit u8 = units.size() > 7 ? units.get(7) : null;

        // ── HW1: Quá hạn – chưa nộp ─────────────────────────────────────────
        saveHomework(offering, s1, u1, teacher,
                "HW1 – Nghe Section 1-2 và điền bảng (QUÁ HẠN)",
                "Nghe audio Section 1-2, điền form và trả lời 10 câu. Tải file đề tại Tài liệu Buổi 1.",
                today.minusWeeks(4).atTime(23, 59),
                HomeworkActivityType.FILE_RESPONSE, HomeworkStatus.OPEN,
                AssessmentSkill.LISTENING, null, null, false);

        // ── HW2: Quá hạn – đã nộp & đã chấm (8.5 / 10) ─────────────────────
        ClassroomHomework hw2 = saveHomework(offering, s2, u2, teacher,
                "HW2 – Tóm tắt lỗi Listening (QUÁ HẠN – đã chấm)",
                "Viết 150-200 từ tóm tắt 5 lỗi Listening hay mắc nhất của bản thân và cách khắc phục.",
                today.minusWeeks(3).atTime(23, 59),
                HomeworkActivityType.TEXT_RESPONSE, HomeworkStatus.OPEN,
                AssessmentSkill.LISTENING, null, null, true);
        saveSubmission(hw2, learner,
                "1. Nghe nhầm số (số nhiều/đơn). 2. Bỏ sót thông tin khi chuyển section. 3. Không đọc câu hỏi trước. 4. Viết sai chính tả. 5. Không kiểm tra lại đáp án.",
                null, HomeworkSubmissionStatus.GRADED,
                BigDecimal.valueOf(8.5), "Bài làm tốt, phân tích sâu. Cần bổ sung cách khắc phục cụ thể hơn cho lỗi số 3.", teacher);

        // ── HW3: Quá hạn – đã nộp & đã chấm (7.0 / 10) ─────────────────────
        ClassroomHomework hw3 = saveHomework(offering, s3, u3, teacher,
                "HW3 – Nộp bài Reading Section 1 (QUÁ HẠN – đã chấm)",
                "Tải đề Reading Section 1 ở Tài liệu Buổi 3, làm và scan nộp lại file PDF/ảnh.",
                today.minusWeeks(2).atTime(23, 59),
                HomeworkActivityType.FILE_RESPONSE, HomeworkStatus.OPEN,
                AssessmentSkill.READING,
                "https://cdn.englishlab.vn/test/reading-section1-exercise.pdf", null, true);
        saveSubmission(hw3, learner, "Em đã hoàn thành 10 câu Reading Section 1 theo thời gian quy định.",
                "https://cdn.englishlab.vn/test/submissions/chinh-hw3-reading.pdf",
                HomeworkSubmissionStatus.GRADED,
                BigDecimal.valueOf(7.0), "Làm đúng 7/10 câu Reading Section 1. Chú ý bẫy True/False/Not Given ở câu 5 và 8.", teacher);

        // ── HW4: Hạn HÔM NAY – quiz trắc nghiệm đã làm (9.0 / 10) ───────────
        String quizConfigWritingTask1 = """
                {"questions":[
                  {"number":1,"prompt":"Which of the following BEST describes the trend in the bar chart?","options":[{"value":"A","label":"The data shows a steady decline over 5 years."},{"value":"B","label":"There was a sharp increase followed by a slight decrease."},{"value":"C","label":"All categories remained stable throughout the period."},{"value":"D","label":"The figures fluctuated with no clear trend."}]},
                  {"number":2,"prompt":"In Writing Task 1, the introduction should _____.","options":[{"value":"A","label":"copy the question directly"},{"value":"B","label":"paraphrase the question using different words"},{"value":"C","label":"include your opinion about the data"},{"value":"D","label":"describe all the data in detail"}]},
                  {"number":3,"prompt":"The phrase 'rose sharply' means the data _____.","options":[{"value":"A","label":"decreased significantly"},{"value":"B","label":"remained unchanged"},{"value":"C","label":"increased dramatically"},{"value":"D","label":"fluctuated slightly"}]},
                  {"number":4,"prompt":"Which sentence correctly describes an overview?","options":[{"value":"A","label":"In 2010, the value was 45 million."},{"value":"B","label":"Overall, the most notable feature is the upward trend in Category A."},{"value":"C","label":"Furthermore, the data shows various figures."},{"value":"D","label":"The chart is about population growth."}]}
                ],"answerKey":{"1":"B","2":"B","3":"C","4":"B"}}
                """;
        ClassroomHomework hw4 = saveHomework(offering, s4, u4, teacher,
                "HW4 – Quiz Writing Task 1 (Hạn HÔM NAY 23:59)",
                "Trả lời 4 câu trắc nghiệm về Writing Task 1 – Bar chart. Hoàn thành trực tiếp trên website.",
                today.atTime(23, 59),
                HomeworkActivityType.SKILL_PRACTICE, HomeworkStatus.OPEN,
                AssessmentSkill.WRITING, null, quizConfigWritingTask1, false);
        saveSubmission(hw4, learner, "{\"1\":\"B\",\"2\":\"B\",\"3\":\"C\",\"4\":\"B\"}",
                null, HomeworkSubmissionStatus.GRADED,
                BigDecimal.valueOf(9.0), "Trả lời đúng 4/4 câu quiz trắc nghiệm Writing Task 1. Nắm rất vững cấu trúc và từ vựng mô tả biểu đồ.", teacher);

        // ── HW5: Hạn NGÀY MAI – bài viết tự do (chưa nộp) ──────────────────
        saveHomework(offering, s5, u5, teacher,
                "HW5 – Viết Writing Task 2 hoàn chỉnh (Hạn NGÀY MAI)",
                "Viết bài Opinion Essay 250+ từ theo đề: 'Some people think that technology has made our lives more complex. To what extent do you agree or disagree?' Nộp trực tiếp vào ô text phía dưới.",
                today.plusDays(1).atTime(23, 59),
                HomeworkActivityType.TEXT_RESPONSE, HomeworkStatus.OPEN,
                AssessmentSkill.WRITING, null, null, true);

        // ── HW6: Hạn NGÀY MAI – quiz kỹ năng (chưa nộp) ────────────────────
        String quizConfigWritingTask2 = """
                {"durationMinutes":12,"questions":[
                  {"number":1,"prompt":"In an Opinion Essay, the thesis statement should appear in the _____.","options":[{"value":"A","label":"body paragraph 1"},{"value":"B","label":"introduction"},{"value":"C","label":"conclusion"},{"value":"D","label":"body paragraph 2"}]},
                  {"number":2,"prompt":"Which linking word best introduces a contrasting idea?","options":[{"value":"A","label":"Furthermore"},{"value":"B","label":"Therefore"},{"value":"C","label":"However"},{"value":"D","label":"Additionally"}]},
                  {"number":3,"prompt":"A well-structured IELTS Task 2 essay should have how many paragraphs?","options":[{"value":"A","label":"2"},{"value":"B","label":"3"},{"value":"C","label":"4 to 5"},{"value":"D","label":"6 or more"}]},
                  {"number":4,"prompt":"'The government should invest in renewable energy.' This is an example of _____.","options":[{"value":"A","label":"a fact"},{"value":"B","label":"an opinion"},{"value":"C","label":"a statistic"},{"value":"D","label":"an example"}]},
                  {"number":5,"prompt":"Which sentence is the best paraphrase of the essay question?","options":[{"value":"A","label":"Technology makes life harder."},{"value":"B","label":"Some argue that technology has increased the complexity of modern life."},{"value":"C","label":"I agree that technology is bad."},{"value":"D","label":"Technology is a complex topic."}]}
                ],"answerKey":{"1":"B","2":"C","3":"C","4":"B","5":"B"}}
                """;
        saveHomework(offering, s5, u5, teacher,
                "HW6 – Quiz chiến thuật Task 2 (Hạn NGÀY MAI)",
                "5 câu trắc nghiệm về cấu trúc và chiến thuật Writing Task 2. Hoàn thành trong 12 phút.",
                today.plusDays(1).atTime(23, 59),
                HomeworkActivityType.SKILL_PRACTICE, HomeworkStatus.OPEN,
                AssessmentSkill.WRITING, null, quizConfigWritingTask2, false);

        // ── HW7: Hạn NGÀY KIA – ghi âm Speaking (chưa nộp) ─────────────────
        saveHomework(offering, s6, u6, teacher,
                "HW7 – Ghi âm Speaking Part 1 (Hạn NGÀY KIA)",
                "Ghi âm trả lời 3 câu hỏi Speaking Part 1 theo chủ đề Hometown (1-2 phút mỗi câu). Nộp link Google Drive hoặc file âm thanh vào ô text bên dưới.",
                today.plusDays(2).atTime(23, 59),
                HomeworkActivityType.TEXT_RESPONSE, HomeworkStatus.OPEN,
                AssessmentSkill.SPEAKING, null, null, true);

        // ── HW8: Hạn 5 ngày nữa – ghi âm Speaking Part 3 (chưa nộp) ────────
        saveHomework(offering, s7, u7, teacher,
                "HW8 – Ghi âm Speaking Part 3 – Environment (Hạn 5 ngày)",
                "Ghi âm trả lời 2 câu Speaking Part 3 về chủ đề Environment (tối thiểu 2 phút mỗi câu).",
                today.plusDays(5).atTime(23, 59),
                HomeworkActivityType.TEXT_RESPONSE, HomeworkStatus.OPEN,
                AssessmentSkill.SPEAKING, null, null, true);

        // ── HW9: Hạn 1 tuần nữa – nộp file Error Log (chưa nộp) ────────────
        saveHomework(offering, s8, u8, teacher,
                "HW9 – Nộp Error Log cá nhân (Hạn 1 tuần nữa)",
                "Hoàn chỉnh bảng Error Log theo template ở Tài liệu. Ghi đầy đủ lỗi của từng kỹ năng qua 8 buổi học.",
                today.plusWeeks(1).atTime(23, 59),
                HomeworkActivityType.FILE_RESPONSE, HomeworkStatus.OPEN,
                AssessmentSkill.MIXED,
                "https://cdn.englishlab.vn/test/error-log-template.xlsx", null, true);
    }

    private ClassroomHomework saveHomework(ClassroomOffering offering, ClassroomSession session,
                                            CurriculumUnit unit,
                                            User teacher, String title, String instruction,
                                            LocalDateTime deadline,
                                            HomeworkActivityType activityType,
                                            HomeworkStatus status,
                                            AssessmentSkill skill,
                                            String attachmentUrl, String activityConfigJson,
                                            boolean aiReviewEnabled) {
        Optional<ClassroomHomework> existing = homeworkRepository.findByClassroomOfferingIdOrderByCreatedAtDesc(offering.getId())
                .stream().filter(h -> title.equalsIgnoreCase(h.getTitle())).findFirst();
        if (existing.isPresent()) {
            ClassroomHomework hw = existing.get();
            if (unit != null && hw.getCurriculumUnit() == null) {
                hw.setCurriculumUnit(unit);
                return homeworkRepository.save(hw);
            }
            return hw;
        }

        ClassroomHomework.ClassroomHomeworkBuilder builder = ClassroomHomework.builder()
                .classroomOffering(offering)
                .session(session)
                .curriculumUnit(unit)
                .title(title)
                .instruction(instruction)
                .deadline(deadline)
                .maxScore(BigDecimal.TEN)
                .activityType(activityType)
                .gradingMode(HomeworkGradingMode.TEACHER)
                .skill(skill)
                .status(status)
                .allowResubmission(true)
                .aiReviewEnabled(aiReviewEnabled)
                .createdBy(teacher);
        if (attachmentUrl != null) builder.attachmentUrl(attachmentUrl);
        if (activityConfigJson != null) builder.activityConfigJson(activityConfigJson);
        return homeworkRepository.save(builder.build());
    }

    private ClassroomHomeworkSubmission saveSubmission(ClassroomHomework homework, User learner,
                                                        String textAnswer, String attachmentUrl,
                                                        HomeworkSubmissionStatus status,
                                                        BigDecimal score, String feedback,
                                                        User gradedBy) {
        Optional<ClassroomHomeworkSubmission> existingOpt = submissionRepository.findByHomeworkIdAndStudentId(homework.getId(), learner.getId());
        if (existingOpt.isPresent()) {
            ClassroomHomeworkSubmission sub = existingOpt.get();
            if (score != null) {
                sub.setScore(score);
                sub.setStatus(status);
                sub.setTeacherFeedback(feedback);
                sub.setGradedBy(gradedBy);
                sub.setGradedAt(LocalDateTime.now());
                return submissionRepository.save(sub);
            }
            return sub;
        }

        ClassroomHomeworkSubmission.ClassroomHomeworkSubmissionBuilder builder = ClassroomHomeworkSubmission.builder()
                .homework(homework)
                .student(learner)
                .status(status)
                .submittedAt(homework.getDeadline() != null
                        ? homework.getDeadline().minusDays(1)
                        : LocalDateTime.now().minusDays(1));
        if (textAnswer != null) builder.textAnswer(textAnswer);
        if (attachmentUrl != null) builder.attachmentUrl(attachmentUrl);
        if (score != null) {
            builder.score(score);
            builder.gradedAt(homework.getDeadline() != null
                    ? homework.getDeadline().plusDays(1)
                    : LocalDateTime.now());
        }
        if (feedback != null) builder.teacherFeedback(feedback);
        if (gradedBy != null) builder.gradedBy(gradedBy);
        return submissionRepository.save(builder.build());
    }

    // ── Gradebook ─────────────────────────────────────────────────────────────

    private void createGradebook(ClassroomOffering offering, User learner, User teacher) {
        Optional<ClassroomGradebookEntry> existingOpt = gradebookRepository.findByClassroomOfferingIdAndStudentId(offering.getId(), learner.getId());
        if (existingOpt.isPresent()) {
            ClassroomGradebookEntry entry = existingOpt.get();
            entry.setHomeworkScore(BigDecimal.valueOf(8.2));
            entry.setQuizScore(BigDecimal.valueOf(9.0));
            entry.setAttendancePercent(BigDecimal.valueOf(80.0));
            entry.setParticipationScore(BigDecimal.valueOf(8.5));
            entry.setFinalResult(BigDecimal.valueOf(8.4));
            entry.setTeacherComment("Học viên học nghiêm túc, tham gia tốt. Đã hoàn thành 3 bài tập và đạt điểm số ấn tượng (TB: 8.2). Cần tiếp tục duy trì phong độ cho các bài tập sắp tới!");
            entry.setStatus(GradebookEntryStatus.PUBLISHED);
            entry.setUpdatedBy(teacher);
            gradebookRepository.save(entry);
            return;
        }

        gradebookRepository.save(ClassroomGradebookEntry.builder()
                .classroomOffering(offering)
                .student(learner)
                .homeworkScore(BigDecimal.valueOf(8.2))
                .quizScore(BigDecimal.valueOf(9.0))
                .attendancePercent(BigDecimal.valueOf(80.0))
                .participationScore(BigDecimal.valueOf(8.5))
                .finalResult(BigDecimal.valueOf(8.4))
                .teacherComment("Học viên học nghiêm túc, tham gia tốt. Đã hoàn thành 3 bài tập và đạt điểm số ấn tượng (TB: 8.2). Cần tiếp tục duy trì phong độ cho các bài tập sắp tới!")
                .status(GradebookEntryStatus.PUBLISHED)
                .updatedBy(teacher)
                .build());
    }

    // ── User helper ───────────────────────────────────────────────────────────

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
}
