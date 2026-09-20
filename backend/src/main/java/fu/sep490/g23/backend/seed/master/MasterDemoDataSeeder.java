package fu.sep490.g23.backend.seed.master;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.admin.AdminBroadcast;
import fu.sep490.g23.backend.entity.admin.enums.BroadcastStatus;
import fu.sep490.g23.backend.entity.classroom.CenterMaterialLibraryItem;
import fu.sep490.g23.backend.entity.classroom.ClassEnrollment;
import fu.sep490.g23.backend.entity.classroom.ClassroomAnnouncement;
import fu.sep490.g23.backend.entity.classroom.ClassroomChangeRequest;
import fu.sep490.g23.backend.entity.classroom.ClassroomPracticeAttemptHistory;
import fu.sep490.g23.backend.entity.classroom.ClassroomTuitionPayment;
import fu.sep490.g23.backend.entity.classroom.ClassroomTuitionPaymentProof;
import fu.sep490.g23.backend.entity.classroom.ClassSchedule;
import fu.sep490.g23.backend.entity.classroom.ClassSection;
import fu.sep490.g23.backend.entity.classroom.ClassroomAttendance;
import fu.sep490.g23.backend.entity.classroom.ClassroomGradebookEntry;
import fu.sep490.g23.backend.entity.classroom.ClassroomHomework;
import fu.sep490.g23.backend.entity.classroom.ClassroomHomeworkSubmission;
import fu.sep490.g23.backend.entity.classroom.ClassroomMaterial;
import fu.sep490.g23.backend.entity.classroom.ClassroomProposal;
import fu.sep490.g23.backend.entity.classroom.CourseRegistrationRequest;
import fu.sep490.g23.backend.entity.classroom.Room;
import fu.sep490.g23.backend.entity.classroom.enums.AttendanceDisputeStatus;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomApprovalStatus;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomAttendanceStatus;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomChangeRequestStatus;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomChangeRequestType;
import fu.sep490.g23.backend.entity.classroom.enums.TuitionPaymentKind;
import fu.sep490.g23.backend.entity.classroom.enums.TuitionProofStatus;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomOfferingStatus;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomRegistrationStatus;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomSessionStatus;
import fu.sep490.g23.backend.entity.classroom.enums.EnrollmentRequestSource;
import fu.sep490.g23.backend.entity.classroom.enums.EnrollmentRequestStatus;
import fu.sep490.g23.backend.entity.classroom.enums.GradebookEntryStatus;
import fu.sep490.g23.backend.entity.classroom.enums.HomeworkStatus;
import fu.sep490.g23.backend.entity.classroom.enums.HomeworkSubmissionStatus;
import fu.sep490.g23.backend.entity.commerce.CourseListItem;
import fu.sep490.g23.backend.entity.commerce.enums.CourseListType;
import fu.sep490.g23.backend.entity.course.CourseCategory;
import fu.sep490.g23.backend.entity.course.CourseDiscussionPost;
import fu.sep490.g23.backend.entity.course.CourseDiscussionReaction;
import fu.sep490.g23.backend.entity.course.CourseDiscussionReport;
import fu.sep490.g23.backend.entity.course.CourseLesson;
import fu.sep490.g23.backend.entity.course.CourseUnit;
import fu.sep490.g23.backend.entity.course.InstructorLedCourse;
import fu.sep490.g23.backend.entity.course.LearnerLessonNote;
import fu.sep490.g23.backend.entity.course.LessonProgress;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.entity.course.OnlineCourseEnrollment;
import fu.sep490.g23.backend.entity.course.OnlineCourseModule;
import fu.sep490.g23.backend.entity.course.OnlineCourseVersion;
import fu.sep490.g23.backend.entity.course.OnlineLesson;
import fu.sep490.g23.backend.entity.course.enums.CourseDiscussionPostType;
import fu.sep490.g23.backend.entity.course.enums.CourseDiscussionReactionType;
import fu.sep490.g23.backend.entity.course.enums.CourseDiscussionReportReasonCategory;
import fu.sep490.g23.backend.entity.course.enums.CourseDiscussionReportStatus;
import fu.sep490.g23.backend.entity.course.enums.CourseDiscussionStatus;
import fu.sep490.g23.backend.entity.course.enums.CourseVersionStatus;
import fu.sep490.g23.backend.entity.course.enums.EnrollmentStatus;
import fu.sep490.g23.backend.entity.course.enums.LessonProgressStatus;
import fu.sep490.g23.backend.entity.course.enums.PackageStatus;
import fu.sep490.g23.backend.entity.course.enums.CourseLevel;
import fu.sep490.g23.backend.entity.assessment.ExerciseBankItem;
import fu.sep490.g23.backend.entity.curriculum.ContentBankItem;
import fu.sep490.g23.backend.entity.curriculum.enums.ContentBankType;
import fu.sep490.g23.backend.entity.enums.RoleCodes;
import fu.sep490.g23.backend.entity.notification.AppNotification;
import fu.sep490.g23.backend.entity.payment.DiscountCode;
import fu.sep490.g23.backend.entity.payment.PaymentOrder;
import fu.sep490.g23.backend.entity.payment.PaymentOrderItem;
import fu.sep490.g23.backend.entity.payment.enums.DiscountType;
import fu.sep490.g23.backend.entity.payment.enums.PaymentOrderItemType;
import fu.sep490.g23.backend.entity.payment.enums.PaymentOrderStatus;
import fu.sep490.g23.backend.entity.support.SupportTicket;
import fu.sep490.g23.backend.entity.support.SupportTicketMessage;
import fu.sep490.g23.backend.entity.teacher.TeacherCourseFeedback;
import fu.sep490.g23.backend.entity.teacher.TeacherCredential;
import fu.sep490.g23.backend.entity.teacher.enums.CredentialVerificationStatus;
import fu.sep490.g23.backend.entity.teacher.enums.TeacherFeedbackPace;
import fu.sep490.g23.backend.entity.support.enums.SupportTicketCategory;
import fu.sep490.g23.backend.entity.support.enums.SupportTicketPriority;
import fu.sep490.g23.backend.entity.support.enums.SupportTicketStatus;
import fu.sep490.g23.backend.entity.teacher.TeacherPerformanceEvaluation;
import fu.sep490.g23.backend.entity.teacher.enums.TeacherEvaluationStatus;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.admin.AdminBroadcastRepository;
import fu.sep490.g23.backend.repository.classroom.CenterMaterialLibraryItemRepository;
import fu.sep490.g23.backend.repository.classroom.ClassEnrollmentRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomAnnouncementRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomChangeRequestRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomPracticeAttemptHistoryRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomTuitionPaymentProofRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomTuitionPaymentRepository;
import fu.sep490.g23.backend.repository.classroom.ClassScheduleRepository;
import fu.sep490.g23.backend.repository.classroom.ClassSectionRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomAttendanceRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomGradebookEntryRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomHomeworkRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomHomeworkSubmissionRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomMaterialRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomProposalRepository;
import fu.sep490.g23.backend.repository.classroom.CourseRegistrationRequestRepository;
import fu.sep490.g23.backend.repository.classroom.RoomRepository;
import fu.sep490.g23.backend.repository.commerce.CourseListItemRepository;
import fu.sep490.g23.backend.repository.course.CourseCategoryRepository;
import fu.sep490.g23.backend.repository.course.CourseDiscussionPostRepository;
import fu.sep490.g23.backend.repository.course.CourseDiscussionReactionRepository;
import fu.sep490.g23.backend.repository.course.CourseDiscussionReportRepository;
import fu.sep490.g23.backend.repository.course.CourseLessonRepository;
import fu.sep490.g23.backend.repository.course.LearnerLessonNoteRepository;
import fu.sep490.g23.backend.repository.course.CourseUnitRepository;
import fu.sep490.g23.backend.repository.course.InstructorLedCourseRepository;
import fu.sep490.g23.backend.repository.course.LessonProgressRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseEnrollmentRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseVersionRepository;
import fu.sep490.g23.backend.repository.notification.AppNotificationRepository;
import fu.sep490.g23.backend.repository.payment.DiscountCodeRepository;
import fu.sep490.g23.backend.repository.payment.PaymentOrderItemRepository;
import fu.sep490.g23.backend.repository.payment.PaymentOrderRepository;
import fu.sep490.g23.backend.repository.assessment.ExerciseBankItemRepository;
import fu.sep490.g23.backend.repository.curriculum.ContentBankItemRepository;
import fu.sep490.g23.backend.repository.support.SupportTicketMessageRepository;
import fu.sep490.g23.backend.repository.support.SupportTicketRepository;
import fu.sep490.g23.backend.repository.teacher.TeacherCourseFeedbackRepository;
import fu.sep490.g23.backend.repository.teacher.TeacherCredentialRepository;
import fu.sep490.g23.backend.repository.teacher.TeacherPerformanceEvaluationRepository;
import fu.sep490.g23.backend.service.course.OnlineCourseVersionService;
import fu.sep490.g23.backend.service.user.UserRoleService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Imports the deterministic MASTER demo dataset from classpath JSON when {@code app.seed.master.enabled=true}.
 */
@Component
@Order(500)
@RequiredArgsConstructor
@Slf4j
public class MasterDemoDataSeeder implements CommandLineRunner {

    private static final String MASTER_PASSWORD = "Password123!";
    private static final String MASTER_REF_PREFIX = MasterDemoMarkers.MASTER_REF_PREFIX;
    private static final String NATURAL_KEY_PREFIX = "master-ops:nk:";

    private final MasterDemoProperties properties;
    private final MasterDemoCleanupService cleanupService;
    private final MasterDemoSemanticValidator semanticValidator;
    /** Local mapper — project does not expose an ObjectMapper Spring bean. */
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PasswordEncoder passwordEncoder;
    private final UserRoleService userRoleService;

    private final UserRepository userRepository;
    private final CourseCategoryRepository courseCategoryRepository;
    private final InstructorLedCourseRepository instructorLedCourseRepository;
    private final CourseUnitRepository courseUnitRepository;
    private final CourseLessonRepository courseLessonRepository;
    private final OnlineCourseRepository onlineCourseRepository;
    private final OnlineCourseVersionRepository onlineCourseVersionRepository;
    private final OnlineCourseVersionService onlineCourseVersionService;
    private final OnlineCourseEnrollmentRepository onlineCourseEnrollmentRepository;
    private final RoomRepository roomRepository;
    private final ClassSectionRepository classSectionRepository;
    private final ClassScheduleRepository classScheduleRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;
    private final ClassroomAttendanceRepository attendanceRepository;
    private final ClassroomHomeworkRepository homeworkRepository;
    private final ClassroomHomeworkSubmissionRepository homeworkSubmissionRepository;
    private final ClassroomGradebookEntryRepository gradebookEntryRepository;
    private final ClassroomMaterialRepository materialRepository;
    private final ClassroomProposalRepository classroomProposalRepository;
    private final CourseRegistrationRequestRepository courseRegistrationRequestRepository;
    private final PaymentOrderRepository paymentOrderRepository;
    private final PaymentOrderItemRepository paymentOrderItemRepository;
    private final DiscountCodeRepository discountCodeRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final AppNotificationRepository appNotificationRepository;
    private final TeacherPerformanceEvaluationRepository teacherEvaluationRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final CourseDiscussionPostRepository discussionPostRepository;
    private final CourseDiscussionReactionRepository discussionReactionRepository;
    private final CourseDiscussionReportRepository discussionReportRepository;
    private final ClassroomAnnouncementRepository classroomAnnouncementRepository;
    private final AdminBroadcastRepository adminBroadcastRepository;
    private final TeacherCourseFeedbackRepository teacherCourseFeedbackRepository;
    private final ClassroomChangeRequestRepository classroomChangeRequestRepository;
    private final CourseListItemRepository courseListItemRepository;
    private final ClassroomTuitionPaymentRepository classroomTuitionPaymentRepository;
    private final ClassroomTuitionPaymentProofRepository classroomTuitionPaymentProofRepository;
    private final TeacherCredentialRepository teacherCredentialRepository;
    private final SupportTicketMessageRepository supportTicketMessageRepository;
    private final ClassroomPracticeAttemptHistoryRepository practiceAttemptHistoryRepository;
    private final ExerciseBankItemRepository exerciseBankItemRepository;
    private final ContentBankItemRepository contentBankItemRepository;
    private final CenterMaterialLibraryItemRepository centerMaterialLibraryItemRepository;
    private final LearnerLessonNoteRepository learnerLessonNoteRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public void run(String... args) {
        if (!properties.isEnabled()) {
            return;
        }
        log.info("[MasterDemo] Import enabled — loading {}", properties.getDatasetClasspath());
        try {
            JsonNode root = loadDataset();
            semanticValidator.validateDatasetOrThrow(root);
            if (properties.isCleanupBeforeImport()) {
                cleanupService.cleanupMasterOnly();
            }
            importDataset(root);
            log.info("[MasterDemo] Import finished");
        } catch (Exception ex) {
            log.error("[MasterDemo] Import failed", ex);
            throw ex instanceof RuntimeException runtime ? runtime : new IllegalStateException(ex);
        }
    }

    private JsonNode loadDataset() throws IOException {
        ClassPathResource resource = new ClassPathResource(properties.getDatasetClasspath());
        try (InputStream input = resource.getInputStream()) {
            return objectMapper.readTree(input);
        }
    }

    private void importDataset(JsonNode root) {
        Map<String, User> usersByEmail = seedAccounts(root.path("accounts"));
        Map<String, CourseCategory> categoriesByCode = seedCategories(root.path("catalog").path("categories"));
        Map<String, InstructorLedCourse> programsByCode = seedInstructorLedPrograms(
                root.path("catalog").path("instructorLed"),
                usersByEmail
        );
        seedCourseSyllabus(root.path("world").path("courseUnits"), programsByCode);
        Map<String, Room> roomsByCode = seedRooms(root.path("rooms"));
        Map<String, OnlineCourse> onlineCoursesBySlug = seedOnlineCourses(
                root.path("world").path("onlineCourses"),
                categoriesByCode,
                usersByEmail
        );

        JsonNode world = root.path("world");
        Map<String, ClassSection> sectionsByCode = seedClassSections(
                world.path("classes"),
                programsByCode,
                roomsByCode,
                usersByEmail
        );
        Map<String, ClassSchedule> sessionsByNaturalKey = seedSessions(
                world.path("sessions"),
                sectionsByCode,
                roomsByCode,
                usersByEmail
        );
        seedClassEnrollments(world.path("enrollments"), sectionsByCode, usersByEmail);
        seedAttendance(world.path("attendance"), sectionsByCode, sessionsByNaturalKey, usersByEmail);
        Map<String, ClassroomHomework> homeworkByKey = seedHomework(
                world.path("homework"),
                sectionsByCode,
                usersByEmail
        );
        seedHomeworkSubmissions(world.path("submissions"), homeworkByKey, usersByEmail);
        seedGradebook(world.path("gradebook"), sectionsByCode, usersByEmail);
        seedOnlineEnrollments(world.path("onlineEnrollments"), onlineCoursesBySlug, usersByEmail);
        seedLessonProgress(world.path("lessonProgressPlan"), onlineCoursesBySlug, usersByEmail);
        seedDiscountCodes(world.path("discountCodes"), usersByEmail);
        seedPayments(world.path("payments"), onlineCoursesBySlug, usersByEmail);
        Map<String, SupportTicket> ticketsByNaturalKey = seedSupportTickets(world.path("tickets"), usersByEmail);
        seedProposals(world.path("proposals"), programsByCode, usersByEmail);
        seedRegistrationRequests(world.path("registrations"), programsByCode, usersByEmail);
        seedNotifications(world.path("notifications"), usersByEmail);
        seedTeacherEvaluations(world.path("evaluations"), usersByEmail);
        seedMaterials(world.path("materials"), sectionsByCode, usersByEmail);

        Map<String, CourseDiscussionPost> discussionPostsByKey = seedDiscussions(
                world.path("discussions"),
                onlineCoursesBySlug,
                usersByEmail
        );
        seedDiscussionReplies(world.path("discussionReplies"), discussionPostsByKey, usersByEmail);
        seedDiscussionReactions(world.path("discussionReactions"), discussionPostsByKey, usersByEmail);
        seedDiscussionReports(world.path("discussionReports"), discussionPostsByKey, usersByEmail);
        seedTeacherFeedback(world.path("teacherFeedback"), sectionsByCode, usersByEmail);
        seedAnnouncements(world.path("announcements"), sectionsByCode, usersByEmail);
        seedBroadcasts(world.path("broadcasts"), usersByEmail);
        seedTicketMessages(world.path("ticketMessages"), ticketsByNaturalKey, usersByEmail);
        seedChangeRequests(
                world.path("changeRequests"),
                sectionsByCode,
                sessionsByNaturalKey,
                usersByEmail
        );
        seedAttendanceDisputes(
                world.path("attendanceDisputes"),
                sectionsByCode,
                sessionsByNaturalKey,
                usersByEmail
        );
        seedCourseListItems(world.path("courseListItems"), onlineCoursesBySlug, usersByEmail);
        Map<String, ClassroomTuitionPayment> tuitionByNaturalKey = seedTuitionPayments(
                world.path("tuitionPayments"),
                sectionsByCode,
                usersByEmail
        );
        seedTuitionProofs(world.path("tuitionProofs"), tuitionByNaturalKey, sectionsByCode, usersByEmail);
        seedTeacherCredentials(world.path("teacherCredentials"), usersByEmail);
        seedPracticeAttempts(world.path("practiceAttempts"), sectionsByCode, usersByEmail);
        seedFlashcardSets(world.path("flashcardSets"), usersByEmail);
        seedCenterLibrary(world.path("centerLibrary"), usersByEmail);
        seedLessonNotes(world.path("lessonNotes"), onlineCoursesBySlug, usersByEmail);
    }

    private Map<String, User> seedAccounts(JsonNode accountsNode) {
        Map<String, User> byEmail = new HashMap<>();
        for (JsonNode account : accountsNode) {
            String email = account.path("email").asText("").trim().toLowerCase();
            if (email.isBlank()) {
                continue;
            }
            if (account.path("preserved").asBoolean(false) || MasterDemoMarkers.isPreservedEmail(email)) {
                userRepository.findByEmail(email).ifPresent(user -> {
                    if (account.path("renameFullNameOnly").asBoolean(false) && account.hasNonNull("fullName")) {
                        String fullName = account.path("fullName").asText();
                        if (!fullName.isBlank() && !fullName.startsWith("(")) {
                            semanticValidator.assertValidVietnameseName(email, fullName);
                            user.setFullName(fullName);
                            userRepository.save(user);
                        }
                    }
                    byEmail.put(email, user);
                });
                continue;
            }
            if (!MasterDemoMarkers.isGeneratedMasterEmail(email)) {
                continue;
            }
            String fullName = account.path("fullName").asText();
            semanticValidator.assertValidVietnameseName(email, fullName);
            String role = account.path("role").asText(RoleCodes.LEARNER);
            User user = userRepository.findByEmail(email).map(existing -> {
                userRoleService.ensureRole(existing, role);
                existing.setFullName(fullName);
                existing.setEmailVerified(true);
                existing.setProfileCompleted(true);
                applyOptionalProfile(existing, account);
                return userRepository.save(existing);
            }).orElseGet(() -> {
                User created = User.builder()
                        .email(email)
                        .fullName(fullName)
                        .password(passwordEncoder.encode(MASTER_PASSWORD))
                        .emailVerified(true)
                        .profileCompleted(true)
                        .passwordSet(true)
                        .build();
                applyOptionalProfile(created, account);
                if (account.hasNonNull("createdAt")) {
                    LocalDateTime createdAt = parseDateTime(account.path("createdAt").asText(null));
                    if (createdAt != null) {
                        created.setCreatedAt(createdAt);
                    }
                }
                userRoleService.assignRole(created, role);
                return userRepository.save(created);
            });
            // created_at is @CreatedDate updatable=false — force cohort timestamp via native update
            if (account.hasNonNull("createdAt") && user.getId() != null) {
                LocalDateTime createdAt = parseDateTime(account.path("createdAt").asText(null));
                if (createdAt != null) {
                    entityManager.createNativeQuery(
                                    "UPDATE users SET created_at = :createdAt WHERE id = :id")
                            .setParameter("createdAt", createdAt)
                            .setParameter("id", user.getId())
                            .executeUpdate();
                    user.setCreatedAt(createdAt);
                }
            }
            byEmail.put(email, user);
        }
        return byEmail;
    }

    private void applyOptionalProfile(User user, JsonNode account) {
        if (account.hasNonNull("phone")) {
            user.setPhoneNumber(account.path("phone").asText());
        }
        if (account.hasNonNull("targetExam")) {
            user.setTargetExam(account.path("targetExam").asText());
        }
        if (account.hasNonNull("targetScore")) {
            user.setTargetScore(account.path("targetScore").asText());
        }
        if (account.hasNonNull("teacherHeadline")) {
            user.setTeacherHeadline(account.path("teacherHeadline").asText());
            user.setTeacherPublicProfile(true);
        }
        if (account.hasNonNull("yearsOfExperience")) {
            user.setTeacherYearsOfExperience(account.path("yearsOfExperience").asInt());
        }
    }

    private Map<String, CourseCategory> seedCategories(JsonNode categoriesNode) {
        Map<String, CourseCategory> byCode = new HashMap<>();
        for (JsonNode node : categoriesNode) {
            String code = node.path("code").asText();
            CourseCategory category = courseCategoryRepository.findByCode(code).orElseGet(() -> CourseCategory.builder()
                    .code(code)
                    .build());
            category.setName(node.path("name").asText(code));
            category.setDescription(node.path("description").asText(null));
            category.setActive(true);
            category = courseCategoryRepository.save(category);
            byCode.put(code, category);
        }
        return byCode;
    }

    private Map<String, InstructorLedCourse> seedInstructorLedPrograms(
            JsonNode programsNode,
            Map<String, User> usersByEmail
    ) {
        Map<String, InstructorLedCourse> byCode = new HashMap<>();
        User creator = usersByEmail.values().stream().findFirst().orElse(null);
        for (JsonNode node : programsNode) {
            String code = node.path("code").asText();
            InstructorLedCourse program = instructorLedCourseRepository.findByCodeIgnoreCase(code)
                    .orElseGet(() -> InstructorLedCourse.builder().code(code).build());
            program.setTitle(node.path("title").asText());
            String categoryCode = node.path("category").asText("IELTS");
            program.setExamType(categoryCode);
            program.setBaseTuitionFeeVnd(BigDecimal.valueOf(node.path("tuition").asLong(0)));
            program.setPublicationStatus(PackageStatus.PUBLISHED);
            program.setShortDescription(node.path("title").asText() + " — chương trình instructor-led tại trung tâm.");
            if (program.getCreatedBy() == null && creator != null) {
                program.setCreatedBy(creator);
            }
            program = instructorLedCourseRepository.save(program);
            byCode.put(code, program);
        }
        return byCode;
    }

    private void seedCourseSyllabus(
            JsonNode unitsNode,
            Map<String, InstructorLedCourse> programsByCode
    ) {
        if (unitsNode == null || !unitsNode.isArray() || unitsNode.isEmpty()) {
            // Fallback: build a default syllabus when JSON omits courseUnits
            for (Map.Entry<String, InstructorLedCourse> entry : programsByCode.entrySet()) {
                ensureDefaultSyllabus(entry.getValue());
            }
            return;
        }
        Map<String, List<JsonNode>> byProgram = new HashMap<>();
        for (JsonNode node : unitsNode) {
            String programCode = node.path("programCode").asText("");
            if (programCode.isBlank()) {
                continue;
            }
            byProgram.computeIfAbsent(programCode, key -> new java.util.ArrayList<>()).add(node);
        }
        for (Map.Entry<String, List<JsonNode>> entry : byProgram.entrySet()) {
            InstructorLedCourse program = programsByCode.get(entry.getKey());
            if (program == null) {
                continue;
            }
            List<JsonNode> units = entry.getValue();
            units.sort((a, b) -> Integer.compare(
                    a.path("sequenceNumber").asInt(0),
                    b.path("sequenceNumber").asInt(0)
            ));
            if (courseUnitRepository.countByInstructorLedCourseId(program.getId()) > 0) {
                syncExistingSyllabusTitles(program, units);
                continue;
            }
            for (JsonNode unitNode : units) {
                CourseUnit unit = CourseUnit.builder()
                        .instructorLedCourse(program)
                        .sequenceNumber(unitNode.path("sequenceNumber").asInt(1))
                        .title(unitNode.path("title").asText())
                        .description(unitNode.path("description").asText(null))
                        .learningObjectives(unitNode.path("learningObjectives").asText(null))
                        .build();
                JsonNode lessonsNode = unitNode.path("lessons");
                if (lessonsNode.isArray()) {
                    for (JsonNode lessonNode : lessonsNode) {
                        unit.addLesson(CourseLesson.builder()
                                .sequenceNumber(lessonNode.path("sequenceNumber").asInt(1))
                                .title(lessonNode.path("title").asText())
                                .description(lessonNode.path("description").asText(null))
                                .learningObjectives(lessonNode.path("learningObjectives").asText(null))
                                .plannedSessionCount(lessonNode.path("plannedSessionCount").asInt(1))
                                .build());
                    }
                }
                courseUnitRepository.save(unit);
            }
        }
        for (InstructorLedCourse program : programsByCode.values()) {
            if (courseUnitRepository.countByInstructorLedCourseId(program.getId()) == 0) {
                ensureDefaultSyllabus(program);
            }
        }
        normalizeBuoiPrefixedLessonTitles();
    }

    /**
     * Re-align existing unit/lesson titles and per-unit sequence numbers with MASTER JSON.
     * Matches lessons by order within the unit (not global sequence), so old 7/8/9 become 1/2/3.
     */
    private void syncExistingSyllabusTitles(InstructorLedCourse program, List<JsonNode> unitNodes) {
        List<CourseUnit> existingUnits = courseUnitRepository
                .findByInstructorLedCourseIdOrderBySequenceNumberAscIdAsc(program.getId());
        Map<Integer, CourseUnit> unitsBySeq = new HashMap<>();
        for (CourseUnit unit : existingUnits) {
            unitsBySeq.put(unit.getSequenceNumber(), unit);
        }
        for (JsonNode unitNode : unitNodes) {
            CourseUnit unit = unitsBySeq.get(unitNode.path("sequenceNumber").asInt(0));
            if (unit == null) {
                continue;
            }
            String unitTitle = unitNode.path("title").asText(null);
            if (unitTitle != null && !unitTitle.isBlank() && !unitTitle.equals(unit.getTitle())) {
                unit.setTitle(unitTitle);
            }
            if (unitNode.path("description").isTextual()) {
                unit.setDescription(unitNode.path("description").asText(null));
            }
            if (unitNode.path("learningObjectives").isTextual()) {
                unit.setLearningObjectives(unitNode.path("learningObjectives").asText(null));
            }
            JsonNode lessonsNode = unitNode.path("lessons");
            if (!lessonsNode.isArray()) {
                courseUnitRepository.save(unit);
                continue;
            }
            List<CourseLesson> existingLessons = new java.util.ArrayList<>(unit.getLessons());
            existingLessons.sort((a, b) -> {
                int cmp = Integer.compare(
                        a.getSequenceNumber() == null ? 0 : a.getSequenceNumber(),
                        b.getSequenceNumber() == null ? 0 : b.getSequenceNumber()
                );
                if (cmp != 0) {
                    return cmp;
                }
                long idA = a.getId() == null ? Long.MAX_VALUE : a.getId();
                long idB = b.getId() == null ? Long.MAX_VALUE : b.getId();
                return Long.compare(idA, idB);
            });
            List<JsonNode> lessonNodes = new java.util.ArrayList<>();
            lessonsNode.forEach(lessonNodes::add);
            lessonNodes.sort((a, b) -> Integer.compare(
                    a.path("sequenceNumber").asInt(0),
                    b.path("sequenceNumber").asInt(0)
            ));
            int matched = Math.min(existingLessons.size(), lessonNodes.size());
            for (int i = 0; i < matched; i++) {
                CourseLesson lesson = existingLessons.get(i);
                JsonNode lessonNode = lessonNodes.get(i);
                lesson.setSequenceNumber(lessonNode.path("sequenceNumber").asInt(i + 1));
                String lessonTitle = lessonNode.path("title").asText(null);
                if (lessonTitle != null && !lessonTitle.isBlank()) {
                    lesson.setTitle(lessonTitle);
                }
                if (lessonNode.path("description").isTextual()) {
                    lesson.setDescription(lessonNode.path("description").asText(null));
                }
                if (lessonNode.path("learningObjectives").isTextual()) {
                    lesson.setLearningObjectives(lessonNode.path("learningObjectives").asText(null));
                }
                if (lessonNode.has("plannedSessionCount")) {
                    lesson.setPlannedSessionCount(lessonNode.path("plannedSessionCount").asInt(1));
                }
            }
            for (int i = matched; i < existingLessons.size(); i++) {
                existingLessons.get(i).setSequenceNumber(i + 1);
            }
            courseUnitRepository.save(unit);
        }
    }

    /** Strip leftover "Buổi N ·" prefixes on any course lesson (MASTER + legacy seeders). */
    private void normalizeBuoiPrefixedLessonTitles() {
        java.util.regex.Pattern prefix = java.util.regex.Pattern.compile(
                "^Buổi\\s+\\d+\\s*[·•–—:\\-]\\s*"
        );
        String[] focusSuffixes = {
                " · Warm-up & input",
                " · Guided practice",
                " · Review & homework",
                " · Skill drill"
        };
        List<CourseLesson> all = courseLessonRepository.findAll();
        Map<Long, List<CourseLesson>> byUnit = new HashMap<>();
        int stripped = 0;
        for (CourseLesson lesson : all) {
            String title = lesson.getTitle();
            if (title == null) {
                continue;
            }
            String cleaned = stripBuoiPrefix(title);
            if (!cleaned.equals(title)) {
                lesson.setTitle(cleaned);
                stripped++;
            }
            Long unitId = lesson.getCourseUnit() != null ? lesson.getCourseUnit().getId() : null;
            if (unitId != null) {
                byUnit.computeIfAbsent(unitId, key -> new java.util.ArrayList<>()).add(lesson);
            }
        }
        int disambiguated = 0;
        int renumbered = 0;
        for (List<CourseLesson> lessons : byUnit.values()) {
            Map<String, List<CourseLesson>> byTitle = new HashMap<>();
            for (CourseLesson lesson : lessons) {
                byTitle.computeIfAbsent(lesson.getTitle(), key -> new java.util.ArrayList<>()).add(lesson);
            }
            for (Map.Entry<String, List<CourseLesson>> entry : byTitle.entrySet()) {
                List<CourseLesson> group = entry.getValue();
                if (group.size() < 2) {
                    continue;
                }
                group.sort((a, b) -> {
                    int cmp = Integer.compare(
                            a.getSequenceNumber() == null ? 0 : a.getSequenceNumber(),
                            b.getSequenceNumber() == null ? 0 : b.getSequenceNumber()
                    );
                    if (cmp != 0) {
                        return cmp;
                    }
                    long idA = a.getId() == null ? Long.MAX_VALUE : a.getId();
                    long idB = b.getId() == null ? Long.MAX_VALUE : b.getId();
                    return Long.compare(idA, idB);
                });
                for (int i = 0; i < group.size(); i++) {
                    String base = entry.getKey();
                    String suffix = i < focusSuffixes.length
                            ? focusSuffixes[i]
                            : (" · Phần " + (i + 1));
                    if (base != null && !base.contains(" · ")) {
                        group.get(i).setTitle(base + suffix);
                        disambiguated++;
                    }
                }
            }
            lessons.sort((a, b) -> {
                int cmp = Integer.compare(
                        a.getSequenceNumber() == null ? 0 : a.getSequenceNumber(),
                        b.getSequenceNumber() == null ? 0 : b.getSequenceNumber()
                );
                if (cmp != 0) {
                    return cmp;
                }
                long idA = a.getId() == null ? Long.MAX_VALUE : a.getId();
                long idB = b.getId() == null ? Long.MAX_VALUE : b.getId();
                return Long.compare(idA, idB);
            });
            for (int i = 0; i < lessons.size(); i++) {
                int expected = i + 1;
                Integer current = lessons.get(i).getSequenceNumber();
                if (current == null || current != expected) {
                    lessons.get(i).setSequenceNumber(expected);
                    renumbered++;
                }
            }
        }
        if (stripped > 0 || disambiguated > 0 || renumbered > 0) {
            courseLessonRepository.saveAll(all);
            log.info("[MasterDemo] Normalized course lessons (stripped={}, disambiguated={}, renumbered={})",
                    stripped, disambiguated, renumbered);
        }
    }

    private static String stripBuoiPrefix(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        return value.replaceFirst("^Buổi\\s+\\d+\\s*[·•–—:\\-]\\s*", "").trim();
    }

    private void ensureDefaultSyllabus(InstructorLedCourse program) {
        if (program == null || program.getId() == null) {
            return;
        }
        if (courseUnitRepository.countByInstructorLedCourseId(program.getId()) > 0) {
            return;
        }
        String exam = program.getExamType() == null ? "IELTS" : program.getExamType().toUpperCase();
        String[][] catalog;
        if (exam.contains("TOEIC")) {
            catalog = new String[][]{
                    {"Unit 1 · Photographs & Q-R", "Part 1-2 Listening fundamentals.", "Loại đáp án nhiễu về thì và từ đồng âm."},
                    {"Unit 2 · Conversations & Talks", "Part 3-4 Listening strategies.", "Nghe mục đích và implied meaning."},
                    {"Unit 3 · Grammar & Text completion", "Part 5-6 Reading grammar.", "Chọn từ loại đúng theo ngữ cảnh."},
                    {"Unit 4 · Reading passages", "Part 7 single and double passages.", "Inference và vocabulary in context."},
                    {"Unit 5 · Timed practice", "Mini mock Listening + Reading.", "Quản lý thời gian và review lỗi."},
                    {"Unit 6 · Intensive review", "Review theo điểm yếu.", "Tổng hợp chiến thuật đạt mục tiêu điểm."},
            };
        } else if (exam.contains("COMM") || exam.contains("BUSINESS")) {
            catalog = new String[][]{
                    {"Unit 1 · Workplace introductions", "Small talk và giới thiệu bản thân.", "Mở đầu cuộc trò chuyện chuyên nghiệp."},
                    {"Unit 2 · Meetings", "Agenda, turn-taking, summarizing.", "Tóm tắt quyết định sau họp."},
                    {"Unit 3 · Email writing", "Tone, request, follow-up.", "Viết email action-oriented."},
                    {"Unit 4 · Presentations", "Structure và Q&A.", "Trình bày 3 điểm chính rõ ràng."},
                    {"Unit 5 · Negotiation soft skills", "Propose, concede, close.", "Đưa đề xuất lịch sự."},
                    {"Unit 6 · Capstone role-play", "Tình huống công sở tổng hợp.", "Áp dụng kỹ năng đã học."},
            };
        } else {
            catalog = new String[][]{
                    {"Unit 1 · Listening foundations", "Section 1-2 strategies.", "Nghe lấy thông tin cụ thể."},
                    {"Unit 2 · Listening academic", "Section 3-4 lecture notes.", "Ghi chú paraphrase trong lecture."},
                    {"Unit 3 · Reading skills", "TFNG và Matching Headings.", "Skimming/scanning hiệu quả."},
                    {"Unit 4 · Writing Task 1", "Charts và overview.", "Chọn số liệu then chốt."},
                    {"Unit 5 · Writing Task 2", "Essay structure.", "Lập dàn ý 4 đoạn."},
                    {"Unit 6 · Speaking Part 1-2", "Fluency và cue card.", "Mở rộng câu trả lời."},
                    {"Unit 7 · Speaking Part 3", "Abstract discussion.", "So sánh và đưa quan điểm."},
                    {"Unit 8 · Full mock", "Mock 4 kỹ năng.", "Tự đánh giá theo band."},
            };
        }
        String[] focuses = {"Warm-up & input", "Guided practice", "Review & homework"};
        for (int i = 0; i < catalog.length; i++) {
            CourseUnit unit = CourseUnit.builder()
                    .instructorLedCourse(program)
                    .sequenceNumber(i + 1)
                    .title(catalog[i][0])
                    .description(catalog[i][1])
                    .learningObjectives(catalog[i][2])
                    .build();
            String baseSkill = catalog[i][0].replaceFirst("^Unit \\d+ · ", "");
            for (int s = 0; s < focuses.length; s++) {
                unit.addLesson(CourseLesson.builder()
                        .sequenceNumber(s + 1)
                        .title(baseSkill + " · " + focuses[s])
                        .description(catalog[i][1])
                        .learningObjectives(catalog[i][2])
                        .plannedSessionCount(1)
                        .build());
            }
            courseUnitRepository.save(unit);
        }
    }

    private Map<String, Room> seedRooms(JsonNode roomsNode) {
        Map<String, Room> byCode = new HashMap<>();
        List<Room> existing = roomRepository.findByActiveTrueOrderByNameAsc();
        for (JsonNode node : roomsNode) {
            String code = node.path("code").asText();
            String name = node.path("name").asText();
            Room room = existing.stream()
                    .filter(candidate -> name.equals(candidate.getName()))
                    .findFirst()
                    .orElseGet(() -> Room.builder().name(name).build());
            room.setName(name);
            room.setLocationName(node.path("locationName").asText(null));
            room.setLocationAddress(node.path("locationAddress").asText(null));
            room.setCapacity(node.path("capacity").asInt(12));
            room.setActive(node.path("active").asBoolean(true));
            room = roomRepository.save(room);
            byCode.put(code, room);
        }
        return byCode;
    }

    private Map<String, OnlineCourse> seedOnlineCourses(
            JsonNode coursesNode,
            Map<String, CourseCategory> categoriesByCode,
            Map<String, User> usersByEmail
    ) {
        Map<String, OnlineCourse> bySlug = new HashMap<>();
        for (JsonNode node : coursesNode) {
            String slug = node.path("slug").asText();
            if (MasterDemoMarkers.isProtectedCourseSlug(slug)) {
                onlineCourseRepository.findBySlug(slug).ifPresent(course -> bySlug.put(slug, course));
                continue;
            }
            OnlineCourse course = onlineCourseRepository.findBySlug(slug)
                    .orElseGet(() -> OnlineCourse.builder().slug(slug).build());
            course.setTitle(node.path("title").asText());
            course.setShortDescription(node.path("shortDescription").asText(null));
            course.setDescription(node.path("description").asText(null));
            course.setDuration(node.path("duration").asText(null));
            course.setPrice(BigDecimal.valueOf(node.path("price").asLong(0)));
            course.setLevel(mapCourseLevel(node.path("level").asText("INTERMEDIATE")));
            course.setStatus(PackageStatus.valueOf(node.path("status").asText("DRAFT")));
            course.setFeatured(node.path("featured").asBoolean(false));
            if (node.hasNonNull("thumbnailUrl") && !node.path("thumbnailUrl").asText("").isBlank()) {
                course.setThumbnailUrl(node.path("thumbnailUrl").asText());
            }
            String categoryCode = node.path("category").asText();
            course.setCategory(categoriesByCode.get(categoryCode));
            String createdByEmail = node.path("createdByEmail").asText(null);
            if (createdByEmail != null) {
                course.setCreatedBy(usersByEmail.get(createdByEmail.toLowerCase()));
            }
            course = onlineCourseRepository.save(course);
            ensureMinimalPublishedStructure(course);
            bySlug.put(slug, course);
        }
        return bySlug;
    }

    private void ensureMinimalPublishedStructure(OnlineCourse course) {
        OnlineCourseVersion version = onlineCourseVersionRepository
                .findFirstByOnlineCourseAndStatusOrderByVersionNumberDesc(course, CourseVersionStatus.DRAFT)
                .or(() -> onlineCourseVersionRepository.findFirstByOnlineCourseOrderByVersionNumberDesc(course))
                .orElseGet(() -> onlineCourseVersionRepository.save(OnlineCourseVersion.builder()
                        .onlineCourse(course)
                        .versionNumber(1)
                        .status(CourseVersionStatus.DRAFT)
                        .totalRequiredLessons(0)
                        .totalRequiredAssessments(0)
                        .build()));
        if (version.getModules() == null || version.getModules().isEmpty()) {
            String[][] curriculum = resolveStarterCurriculum(course);
            for (int m = 0; m < curriculum.length; m++) {
                String moduleTitle = curriculum[m][0];
                String moduleDesc = curriculum[m][1];
                OnlineCourseModule module = OnlineCourseModule.builder()
                        .title(moduleTitle)
                        .description(moduleDesc)
                        .sequenceNumber(m + 1)
                        .build();
                for (int l = 1; l <= 3; l++) {
                    String lessonTitle = curriculum[m][1 + l];
                    String lessonBody = curriculum[m][4 + l];
                    String contentText = "### " + lessonTitle + "\n\n" + lessonBody
                            + "\n\n**Việc cần làm**\n- Đọc mục tiêu bài và ghi chú 3 ý chính.\n"
                            + "- Hoàn thành phần luyện trong bài.\n- Đánh dấu hoàn thành khi đã tự kiểm tra.";
                    module.addLesson(OnlineLesson.builder()
                            .stableLessonKey(course.getSlug() + "-m" + (m + 1) + "-l" + l)
                            .title(lessonTitle)
                            .description(lessonBody.length() > 480 ? lessonBody.substring(0, 480) : lessonBody)
                            .contentType("ARTICLE")
                            .contentText(contentText)
                            .durationMinutes(20 + (l * 5))
                            .sequenceNumber(l)
                            .preview(m == 0 && l == 1)
                            .build());
                }
                version.addModule(module);
            }
            onlineCourseVersionRepository.save(version);
        }
        int lessonCount = version.getModules().stream()
                .mapToInt(module -> module.getLessons() == null ? 0 : module.getLessons().size())
                .sum();
        course.setTotalLessons(Math.max(1, lessonCount));
        course.setTotalHours(Math.max(1, lessonCount / 2));
        onlineCourseRepository.save(course);
        if (course.getStatus() == PackageStatus.PUBLISHED) {
            onlineCourseVersionService.refreshPublishedSnapshot(course);
        }
    }

    /**
     * Starter curriculum for MASTER online extras (no protected E2/Vocab).
     * Each module row: title, description, 3 lesson titles, 3 lesson bodies.
     */
    private String[][] resolveStarterCurriculum(OnlineCourse course) {
        String slug = course.getSlug() == null ? "" : course.getSlug();
        String category = course.getCategory() == null || course.getCategory().getCode() == null
                ? ""
                : course.getCategory().getCode().toUpperCase();
        if (slug.contains("writing") || (category.contains("IELTS") && slug.contains("writing"))) {
            return new String[][]{
                    {"Task 1 — Overview & số liệu", "Biểu đồ và overview.", "Cấu trúc Task 1 4 đoạn", "Overview không nhồi số", "Chọn số liệu then chốt", "Nắm intro-overview-body1-body2.", "Viết overview tối đa 2 câu.", "So sánh highest/lowest trong 1 body."},
                    {"Task 1 — Process & so sánh", "Quy trình và ngôn ngữ so sánh.", "Mô tả process", "While/whereas/compared with", "Mini Task 1 timed", "Dùng first/then và bị động.", "Viết 10 câu so sánh số liệu.", "Làm 1 đề Task 1 trong 20 phút."},
                    {"Task 2 — Dàn ý & lập luận", "Opinion/discussion.", "Paraphrase & thesis", "Body TEE", "Counter-argument ngắn", "Paraphrase 4 đề và viết thesis.", "Topic-explain-example cho 2 body.", "Thêm 1 câu thừa nhận rồi bác bỏ lịch sự."},
                    {"Tự sửa & full Writing", "Checklist band 6+.", "Collocation Academic", "Câu phức an toàn", "Full mock 60 phút", "Học 15 collocation và viết 8 câu.", "Thêm although/which/if không lỗi.", "Làm Task 1+2 và tự chấm 4 tiêu chí."}
            };
        }
        if (slug.contains("speaking")) {
            return new String[][]{
                    {"Fluency & Part 1", "Nói liền mạch.", "Fillers tự nhiên", "Part 1 topics", "Why + Example", "Trả lời ≥25 giây/câu.", "Ghi âm hometown/study/free time.", "Công thức Answer-Reason-Example."},
                    {"Pronunciation", "Stress và linking.", "Word stress", "Linking & weak forms", "Âm /θ/ /ð/", "Luyện 15 từ Academic.", "Đọc 10 câu có linking.", "Minimal pairs thin/this/that."},
                    {"Part 2 cue card", "Outline 1 phút — nói 2 phút.", "Khung what/when/who/why", "Kéo dài ý phút 1:20", "Timed Part 2 set", "3 cue card bullet rồi nói.", "Thêm feeling/result/contrast.", "2 cue card liên tiếp."},
                    {"Part 3 & mock", "Câu trừu tượng.", "Compare/speculate", "Lý do 2 tầng", "Full mock Speaking", "would/might/tend to.", "Opinion-reason-example.", "Mock 11–14 phút và tự chấm."}
            };
        }
        if (slug.contains("listening")) {
            return new String[][]{
                    {"Trước khi nghe", "Predict keywords.", "Đọc câu hỏi 30 giây", "Paraphrase Listening", "Distractors", "Gạch keyword và đoán loại thông tin.", "Bảng synonym thường gặp.", "Nhận biết speaker sửa đáp án."},
                    {"Section 1–2", "Form và map.", "Spelling & số liệu", "Map labelling", "MCQ Section 2", "Checklist lỗi spelling.", "Ôn hướng và landmark.", "Loại đáp án nhiễu."},
                    {"Section 3", "Academic dialogue.", "Ai nói gì", "Matching", "Khi bị lạc từ khóa", "Theo dõi speaker A/B.", "Loại dần matching.", "Skip & rejoin câu tiếp."},
                    {"Section 4 & mock", "Lecture notes.", "Note-taking", "Summary completion", "Full Listening mock", "Ghi chú theo outline.", "Đọc trước chỗ trống.", "Đề full + error log."}
            };
        }
        if (category.contains("TOEIC") || slug.contains("toeic")) {
            return new String[][]{
                    {"Listening Part 1–2", "Photos & Q-R.", "Part 1 ảnh", "Part 2 loại câu hỏi", "Đồng âm & paraphrase", "Loại đáp án sai thì/vị trí.", "Wh-/Yes-No patterns.", "Bẫy từ nghe giống."},
                    {"Listening Part 3–4", "Conversations & talks.", "Part 3 mục đích", "Part 3 graphic", "Part 4 announcement", "Đọc 3 câu trước audio.", "Đối chiếu schedule/menu.", "Bắt topic sentence."},
                    {"Reading Part 5–6", "Grammar speed.", "Word form & thì", "Giới từ & liên từ", "Part 6 ngữ cảnh", "Checklist N/V/Adj/Adv.", "20 câu preposition.", "Email 4 chỗ trống."},
                    {"Part 7 & mock", "Single/double passages.", "Email & notice", "Double passages", "Mini mock L&R", "Câu inference nhanh.", "Nối thông tin 2 văn bản.", "Đề rút gọn + review lỗi."}
            };
        }
        if (category.contains("COMM") || category.contains("BUSINESS") || slug.contains("business") || slug.contains("communication")) {
            return new String[][]{
                    {"Small talk & introductions", "Chào hỏi công sở.", "Self-intro 45 giây", "Small talk an toàn", "Kết thúc hội thoại", "Tên-vị trí-việc đang làm.", "8 cặp hỏi–đáp weekend/project.", "Cụm wrap-up lịch sự."},
                    {"Meetings", "Agenda và turn-taking.", "Mở họp & lấy lượt", "Tóm tắt quyết định", "Disagree lịch sự", "Shall we start / Can I add.", "5 bullet decision+owner.", "I see your point, however…"},
                    {"Email & calls", "Tone và action.", "Email request", "Clarify trên điện thoại", "Follow-up email", "Subject-context-ask-thanks.", "Could you repeat / confirm.", "Action items 6–8 dòng."},
                    {"Presenting ideas", "3 điểm trong 3 phút.", "Signposting", "Q&A ngắn", "Mock presentation", "First/Next/Finally.", "Buy time rồi trả lời.", "Thuyết trình 3 phút + checklist."}
            };
        }
        // Foundation / academic default
        return new String[][]{
                {"Câu & đoạn học thuật", "Từ câu đơn đến phức.", "Compound/complex", "Topic sentence", "Supporting details", "Viết 12 câu biến đổi cấu trúc.", "5 topic sentence rõ.", "2 đoạn explain+example."},
                {"Academic vocabulary", "Education/environment/health.", "Collocation Education", "Paraphrase câu", "Word families", "20 collocation + 8 câu.", "Paraphrase 10 câu giữ nghĩa.", "15 chỗ trống N/V/Adj/Adv."},
                {"Đọc đoạn ngắn", "Main idea & reference.", "Main idea", "Reference words", "Inference cơ bản", "3 đoạn chọn main idea.", "10 câu it/this/these.", "6 câu inference có evidence."},
                {"Viết paragraph & mini essay", "Chuẩn bị IELTS Writing.", "Paragraph 120 từ", "Mini essay 220 từ", "Self-edit checklist", "Online learning + linking words.", "Opinion 4 đoạn rút gọn.", "Sửa grammar/vocab/coherence."}
        };
    }

    private Map<String, ClassSection> seedClassSections(
            JsonNode classesNode,
            Map<String, InstructorLedCourse> programsByCode,
            Map<String, Room> roomsByCode,
            Map<String, User> usersByEmail
    ) {
        Map<String, ClassSection> byCode = new HashMap<>();
        for (JsonNode node : classesNode) {
            String code = node.path("code").asText();
            ClassSection section = classSectionRepository.findByCode(code)
                    .orElseGet(() -> ClassSection.builder().code(code).build());
            section.setName(node.path("name").asText());
            section.setDeliveryMode(ClassroomDeliveryMode.valueOf(node.path("deliveryMode").asText("OFFLINE")));
            section.setStatus(ClassroomOfferingStatus.valueOf(node.path("status").asText("DRAFT")));
            section.setStartDate(LocalDate.parse(node.path("startDate").asText()));
            section.setPlannedEndDate(LocalDate.parse(node.path("plannedEndDate").asText()));
            section.setTuitionFeeVnd(BigDecimal.valueOf(node.path("tuitionFeeVnd").asLong(0)));
            section.setCapacity(node.path("capacity").asInt(12));
            section.setInstructorLedCourse(programsByCode.get(node.path("programCode").asText()));
            String teacherEmail = node.path("primaryTeacherEmail").asText(null);
            if (teacherEmail != null) {
                section.setPrimaryTeacher(usersByEmail.get(teacherEmail.toLowerCase()));
            }
            String roomCode = node.path("roomCode").asText(null);
            if (roomCode != null && !roomCode.isBlank()) {
                section.setRoom(roomsByCode.get(roomCode));
            } else {
                section.setRoom(null);
            }
            section = classSectionRepository.save(section);
            byCode.put(code, section);
        }
        return byCode;
    }

    private Map<String, ClassSchedule> seedSessions(
            JsonNode sessionsNode,
            Map<String, ClassSection> sectionsByCode,
            Map<String, Room> roomsByCode,
            Map<String, User> usersByEmail
    ) {
        Map<String, ClassSchedule> byKey = new HashMap<>();
        for (JsonNode node : sessionsNode) {
            String naturalKey = node.path("naturalKey").asText();
            String classCode = node.path("classCode").asText();
            ClassSection section = sectionsByCode.get(classCode);
            if (section == null) {
                continue;
            }
            LocalDate sessionDate = LocalDate.parse(node.path("sessionDate").asText());
            LocalTime startTime = LocalTime.parse(node.path("startTime").asText());
            LocalTime endTime = LocalTime.parse(node.path("endTime").asText());
            ClassSchedule session = classScheduleRepository
                    .findByClassSectionIdOrderBySessionDateAscStartTimeAsc(section.getId())
                    .stream()
                    .filter(row -> row.getSessionDate().equals(sessionDate) && row.getStartTime().equals(startTime))
                    .findFirst()
                    .orElseGet(() -> ClassSchedule.builder().classSection(section).build());
            session.setSessionDate(sessionDate);
            session.setStartTime(startTime);
            session.setEndTime(endTime);
            session.setStatus(ClassroomSessionStatus.valueOf(node.path("status").asText("SCHEDULED")));
            String title = node.path("title").asText(null);
            session.setNote(title);
            // Prefer linked lesson title semantics: session_content holds topic name only (no "Buổi N").
            if (title != null && !title.isBlank()) {
                session.setSessionContent(stripBuoiPrefix(title));
            } else if (session.getSessionContent() == null || session.getSessionContent().isBlank()) {
                session.setSessionContent("Nội dung buổi học " + sessionDate + " " + startTime);
            } else {
                session.setSessionContent(stripBuoiPrefix(session.getSessionContent()));
            }
            String teacherEmail = node.path("teacherEmail").asText(null);
            if (teacherEmail != null) {
                session.setTeacher(usersByEmail.get(teacherEmail.toLowerCase()));
            }
            String roomCode = node.path("roomCode").asText(null);
            if (roomCode != null && !roomCode.isBlank()) {
                session.setRoom(roomsByCode.get(roomCode));
            }
            session = classScheduleRepository.save(session);
            byKey.put(naturalKey, session);
        }
        return byKey;
    }

    private void seedClassEnrollments(
            JsonNode enrollmentsNode,
            Map<String, ClassSection> sectionsByCode,
            Map<String, User> usersByEmail
    ) {
        for (JsonNode node : enrollmentsNode) {
            ClassSection section = sectionsByCode.get(node.path("classCode").asText());
            User learner = usersByEmail.get(node.path("learnerEmail").asText("").toLowerCase());
            if (section == null || learner == null) {
                continue;
            }
            ClassEnrollment enrollment = classEnrollmentRepository
                    .findByStudentIdAndClassSectionId(learner.getId(), section.getId())
                    .orElseGet(() -> ClassEnrollment.builder()
                            .classSection(section)
                            .student(learner)
                            .build());
            enrollment.setRegistrationStatus(
                    ClassroomRegistrationStatus.valueOf(node.path("registrationStatus").asText("FULLY_PAID"))
            );
            enrollment.setAgreedTuitionFeeVnd(BigDecimal.valueOf(node.path("agreedTuitionFeeVnd").asLong(0)));
            enrollment.setTuitionAmountDue(BigDecimal.valueOf(node.path("tuitionAmountDue").asLong(0)));
            enrollment.setTuitionAmountPaid(BigDecimal.valueOf(node.path("tuitionAmountPaid").asLong(0)));
            enrollment.setEnrolledAt(parseDateTime(node.path("enrolledAt").asText(null)));
            classEnrollmentRepository.save(enrollment);
        }
    }

    private void seedAttendance(
            JsonNode attendanceNode,
            Map<String, ClassSection> sectionsByCode,
            Map<String, ClassSchedule> sessionsByNaturalKey,
            Map<String, User> usersByEmail
    ) {
        for (JsonNode node : attendanceNode) {
            String classCode = node.path("classCode").asText();
            ClassSection section = sectionsByCode.get(classCode);
            User learner = usersByEmail.get(node.path("learnerEmail").asText("").toLowerCase());
            if (section == null || learner == null) {
                continue;
            }
            String sessionDateText = node.path("sessionDate").asText();
            String startTimeText = node.path("startTime").asText();
            LocalDate sessionDate = LocalDate.parse(sessionDateText);
            LocalTime startTime = LocalTime.parse(startTimeText);
            String sessionKey = classCode + "|" + sessionDateText + "|" + startTimeText;
            ClassSchedule session = sessionsByNaturalKey.get(sessionKey);
            if (session == null) {
                session = classScheduleRepository
                        .findByClassSectionIdOrderBySessionDateAscStartTimeAsc(section.getId())
                        .stream()
                        .filter(row -> row.getSessionDate().equals(sessionDate) && row.getStartTime().equals(startTime))
                        .findFirst()
                        .orElse(null);
            }
            if (session == null) {
                continue;
            }
            Long sessionId = session.getId();
            Long studentId = learner.getId();
            ClassSchedule attendanceSession = session;
            User attendanceStudent = learner;
            ClassroomAttendance row = attendanceRepository.findBySessionIdAndStudentId(sessionId, studentId)
                    .orElseGet(() -> ClassroomAttendance.builder()
                            .session(attendanceSession)
                            .student(attendanceStudent)
                            .build());
            row.setStatus(ClassroomAttendanceStatus.valueOf(node.path("status").asText("PRESENT")));
            row.setTeacherConfirmed(true);
            attendanceRepository.save(row);
        }
    }

    private Map<String, ClassroomHomework> seedHomework(
            JsonNode homeworkNode,
            Map<String, ClassSection> sectionsByCode,
            Map<String, User> usersByEmail
    ) {
        Map<String, ClassroomHomework> byKey = new HashMap<>();
        for (JsonNode node : homeworkNode) {
            String naturalKey = node.path("naturalKey").asText();
            ClassSection section = sectionsByCode.get(node.path("classCode").asText());
            if (section == null) {
                continue;
            }
            String markerUrl = MASTER_REF_PREFIX + naturalKey;
            ClassroomHomework homework = homeworkRepository.findFirstByAttachmentUrlEndingWith(naturalKey)
                    .orElseGet(() -> ClassroomHomework.builder()
                            .classSection(section)
                            .attachmentUrl(markerUrl)
                            .build());
            homework.setClassSection(section);
            homework.setTitle(node.path("title").asText());
            homework.setDeadline(parseDateTime(node.path("dueAt").asText(null)));
            homework.setMaxScore(BigDecimal.valueOf(node.path("maxScore").asDouble(10)));
            homework.setStatus(HomeworkStatus.OPEN);
            homework.setCreatedAt(parseDateTime(node.path("publishedAt").asText(null)));
            String teacherEmail = section.getPrimaryTeacher() != null
                    ? section.getPrimaryTeacher().getEmail()
                    : null;
            if (teacherEmail != null) {
                homework.setCreatedBy(usersByEmail.get(teacherEmail.toLowerCase()));
            }
            homework = homeworkRepository.save(homework);
            byKey.put(naturalKey, homework);
        }
        return byKey;
    }

    private void seedHomeworkSubmissions(
            JsonNode submissionsNode,
            Map<String, ClassroomHomework> homeworkByKey,
            Map<String, User> usersByEmail
    ) {
        for (JsonNode node : submissionsNode) {
            ClassroomHomework homework = homeworkByKey.get(node.path("homeworkKey").asText());
            User learner = usersByEmail.get(node.path("learnerEmail").asText("").toLowerCase());
            if (homework == null || learner == null) {
                continue;
            }
            ClassroomHomeworkSubmission submission = homeworkSubmissionRepository
                    .findByHomeworkIdAndStudentId(homework.getId(), learner.getId())
                    .orElseGet(() -> ClassroomHomeworkSubmission.builder()
                            .homework(homework)
                            .student(learner)
                            .build());
            submission.setStatus(HomeworkSubmissionStatus.valueOf(node.path("status").asText("GRADED")));
            submission.setScore(BigDecimal.valueOf(node.path("score").asDouble(0)));
            submission.setSubmittedAt(parseDateTime(node.path("submittedAt").asText(null)));
            submission.setGradedAt(parseDateTime(node.path("gradedAt").asText(null)));
            submission.setTeacherFeedback(node.path("feedback").asText(null));
            homeworkSubmissionRepository.save(submission);
        }
    }

    private void seedGradebook(
            JsonNode gradebookNode,
            Map<String, ClassSection> sectionsByCode,
            Map<String, User> usersByEmail
    ) {
        for (JsonNode node : gradebookNode) {
            ClassSection section = sectionsByCode.get(node.path("classCode").asText());
            User learner = usersByEmail.get(node.path("learnerEmail").asText("").toLowerCase());
            if (section == null || learner == null) {
                continue;
            }
            Optional<ClassroomGradebookEntry> existing = gradebookEntryRepository
                    .findByClassSectionIdAndStudentId(section.getId(), learner.getId());
            ClassroomGradebookEntry entry = existing.orElseGet(() -> ClassroomGradebookEntry.builder()
                    .classSection(section)
                    .student(learner)
                    .build());
            if (!node.path("homeworkScore").isNull()) {
                entry.setHomeworkScore(BigDecimal.valueOf(node.path("homeworkScore").asDouble()));
            }
            if (!node.path("attendancePercent").isNull()) {
                entry.setAttendancePercent(BigDecimal.valueOf(node.path("attendancePercent").asDouble()));
            }
            if (!node.path("finalResult").isNull()) {
                entry.setFinalResult(BigDecimal.valueOf(node.path("finalResult").asDouble()));
            }
            entry.setStatus(mapGradebookStatus(node.path("status").asText("PENDING")));
            gradebookEntryRepository.save(entry);
        }
    }

    private GradebookEntryStatus mapGradebookStatus(String raw) {
        return switch (raw == null ? "" : raw.trim().toUpperCase()) {
            case "DRAFT", "PENDING" -> GradebookEntryStatus.PENDING;
            case "GRADED" -> GradebookEntryStatus.GRADED;
            case "LOCKED" -> GradebookEntryStatus.LOCKED;
            case "PUBLISHED" -> GradebookEntryStatus.PUBLISHED;
            default -> GradebookEntryStatus.PENDING;
        };
    }

    private void seedOnlineEnrollments(
            JsonNode enrollmentsNode,
            Map<String, OnlineCourse> coursesBySlug,
            Map<String, User> usersByEmail
    ) {
        for (JsonNode node : enrollmentsNode) {
            OnlineCourse course = coursesBySlug.get(node.path("courseSlug").asText());
            User learner = usersByEmail.get(node.path("learnerEmail").asText("").toLowerCase());
            if (course == null || learner == null) {
                continue;
            }
            if (MasterDemoMarkers.isPreservedEmail(learner.getEmail())) {
                continue;
            }
            OnlineCourseEnrollment enrollment = onlineCourseEnrollmentRepository
                    .findByStudentAndOnlineCourse(learner, course)
                    .orElseGet(() -> OnlineCourseEnrollment.builder()
                            .student(learner)
                            .onlineCourse(course)
                            .build());
            enrollment.setStatus(EnrollmentStatus.valueOf(node.path("status").asText("ACTIVE")));
            enrollment.setProgressPercent(node.path("progressPercent").asInt(0));
            enrollment.setRegisteredAt(parseDateTime(node.path("enrolledAt").asText(null)));
            if (node.hasNonNull("reviewRating")) {
                enrollment.setReviewRating(node.path("reviewRating").asInt());
            }
            if (node.hasNonNull("reviewComment")) {
                enrollment.setReviewComment(node.path("reviewComment").asText());
            }
            if (node.hasNonNull("reviewedAt")) {
                enrollment.setReviewedAt(parseDateTime(node.path("reviewedAt").asText()));
            }
            onlineCourseVersionRepository.findFirstByOnlineCourseAndStatusOrderByVersionNumberDesc(
                    course,
                    CourseVersionStatus.PUBLISHED
            ).or(() -> onlineCourseVersionRepository.findFirstByOnlineCourseOrderByVersionNumberDesc(course))
                    .ifPresent(enrollment::setCourseVersion);
            onlineCourseEnrollmentRepository.save(enrollment);
        }
    }

    private void seedLessonProgress(
            JsonNode plansNode,
            Map<String, OnlineCourse> coursesBySlug,
            Map<String, User> usersByEmail
    ) {
        for (JsonNode node : plansNode) {
            OnlineCourse course = coursesBySlug.get(node.path("courseSlug").asText());
            User learner = usersByEmail.get(node.path("learnerEmail").asText("").toLowerCase());
            if (course == null || learner == null || MasterDemoMarkers.isPreservedEmail(learner.getEmail())) {
                continue;
            }
            OnlineCourseEnrollment enrollment = onlineCourseEnrollmentRepository
                    .findByStudentAndOnlineCourse(learner, course)
                    .orElse(null);
            if (enrollment == null) {
                continue;
            }
            List<OnlineLesson> lessons = course.getPublishedModules().stream()
                    .flatMap(module -> module.getLessons().stream())
                    .limit(Math.max(1, node.path("completedLessonCount").asInt(1)))
                    .toList();
            for (OnlineLesson lesson : lessons) {
                if (lessonProgressRepository.findByEnrollmentAndLesson(enrollment, lesson).isPresent()) {
                    continue;
                }
                lessonProgressRepository.save(LessonProgress.builder()
                        .enrollment(enrollment)
                        .lesson(lesson)
                        .status(LessonProgressStatus.COMPLETED)
                        .completedAt(LocalDateTime.now())
                        .build());
            }
        }
    }

    private void seedDiscountCodes(JsonNode codesNode, Map<String, User> usersByEmail) {
        User creator = usersByEmail.values().stream().findFirst().orElse(null);
        for (JsonNode node : codesNode) {
            String code = node.path("code").asText();
            DiscountCode discount = discountCodeRepository.findByCodeIgnoreCase(code)
                    .orElseGet(() -> DiscountCode.builder().code(code).build());
            discount.setName(code);
            discount.setType(DiscountType.PERCENTAGE);
            discount.setValue(BigDecimal.valueOf(node.path("percentOff").asInt(0)));
            discount.setUsageLimit(500);
            discount.setActive(node.path("active").asBoolean(true));
            discount.setStartsAt(parseDateTime(node.path("startsOn").asText(null) + "T00:00:00"));
            discount.setExpiresAt(parseDateTime(node.path("endsOn").asText(null) + "T23:59:59"));
            if (discount.getCreatedBy() == null && creator != null) {
                discount.setCreatedBy(creator);
            }
            discountCodeRepository.save(discount);
        }
    }

    private void seedPayments(
            JsonNode paymentsNode,
            Map<String, OnlineCourse> coursesBySlug,
            Map<String, User> usersByEmail
    ) {
        for (JsonNode node : paymentsNode) {
            long orderCode = node.path("orderCode").asLong();
            if (paymentOrderRepository.findByOrderCode(orderCode).isPresent()) {
                continue;
            }
            User learner = usersByEmail.get(node.path("learnerEmail").asText("").toLowerCase());
            OnlineCourse course = coursesBySlug.get(node.path("courseSlug").asText());
            if (learner == null || course == null || MasterDemoMarkers.isPreservedEmail(learner.getEmail())) {
                continue;
            }
            long finalAmount = node.path("finalAmount").asLong();
            long originalAmount = node.path("originalAmount").asLong();
            long couponDiscount = node.path("couponDiscountAmount").asLong(0);
            LocalDateTime createdAt = parseDateTime(node.path("createdAt").asText(null));
            LocalDateTime paidAt = parseDateTime(node.path("paidAt").asText(null));
            PaymentOrder order = PaymentOrder.builder()
                    .orderCode(orderCode)
                    .student(learner)
                    .originalAmount(originalAmount)
                    .amount(finalAmount)
                    .couponDiscountAmount(couponDiscount)
                    .systemDiscountAmount(0L)
                    .learningPathDiscountAmount(0L)
                    .couponReservationReleased(true)
                    .description("MASTER demo order " + orderCode)
                    .status(PaymentOrderStatus.valueOf(node.path("status").asText("PAID")))
                    .providerReference("MASTER-DEMO")
                    .paidAt(paidAt)
                    .webhookConfirmedAt(paidAt)
                    .build();
            order.setCreatedAt(createdAt);
            PaymentOrder saved = paymentOrderRepository.save(order);
            paymentOrderItemRepository.save(PaymentOrderItem.builder()
                    .paymentOrder(saved)
                    .itemType(PaymentOrderItemType.ONLINE_COURSE)
                    .onlineCourse(course)
                    .titleSnapshot(course.getTitle())
                    .unitPriceVnd(originalAmount)
                    .discountAmountVnd(Math.max(0L, originalAmount - finalAmount))
                    .finalAmountVnd(finalAmount)
                    .quantity(1)
                    .build());
        }
    }

    private Map<String, SupportTicket> seedSupportTickets(JsonNode ticketsNode, Map<String, User> usersByEmail) {
        Map<String, SupportTicket> byNaturalKey = new HashMap<>();
        for (JsonNode node : ticketsNode) {
            User learner = usersByEmail.get(node.path("learnerEmail").asText("").toLowerCase());
            User assignee = usersByEmail.get(node.path("assigneeEmail").asText("").toLowerCase());
            if (learner == null) {
                continue;
            }
            String subject = node.path("subject").asText();
            String naturalKey = node.path("naturalKey").asText(null);
            Optional<SupportTicket> existing = supportTicketRepository
                    .findByRequesterIdOrderByUpdatedAtDesc(learner.getId())
                    .stream()
                    .filter(ticket -> subject.equals(ticket.getSubject()))
                    .findFirst();
            if (existing.isPresent()) {
                if (naturalKey != null && !naturalKey.isBlank()) {
                    byNaturalKey.put(naturalKey, existing.get());
                }
                continue;
            }
            SupportTicketStatus status = SupportTicketStatus.valueOf(
                    "RESOLVED".equals(node.path("status").asText()) ? "RESOLVED" : node.path("status").asText("OPEN")
            );
            SupportTicket ticket = SupportTicket.builder()
                    .requester(learner)
                    .assignee(assignee)
                    .subject(subject)
                    .category(SupportTicketCategory.OTHER)
                    .priority(SupportTicketPriority.NORMAL)
                    .status(status)
                    .build();
            ticket.setCreatedAt(parseDateTime(node.path("createdAt").asText(null)));
            if (status == SupportTicketStatus.RESOLVED && node.hasNonNull("closedAt")) {
                ticket.setResolvedAt(parseDateTime(node.path("closedAt").asText()));
                ticket.setResolvedBy(assignee);
            }
            SupportTicket saved = supportTicketRepository.save(ticket);
            if (naturalKey != null && !naturalKey.isBlank()) {
                byNaturalKey.put(naturalKey, saved);
            }
        }
        return byNaturalKey;
    }

    private void seedProposals(
            JsonNode proposalsNode,
            Map<String, InstructorLedCourse> programsByCode,
            Map<String, User> usersByEmail
    ) {
        Set<String> existingCodes = classroomProposalRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(ClassroomProposal::getProposalCode)
                .collect(Collectors.toCollection(HashSet::new));
        for (JsonNode node : proposalsNode) {
            String proposalCode = node.path("naturalKey").asText();
            if (existingCodes.contains(proposalCode)) {
                continue;
            }
            InstructorLedCourse program = programsByCode.get(node.path("programCode").asText());
            User createdBy = usersByEmail.get(node.path("createdByEmail").asText("").toLowerCase());
            User reviewedBy = usersByEmail.get(node.path("reviewedByEmail").asText("").toLowerCase());
            if (program == null || createdBy == null) {
                continue;
            }
            LocalDateTime createdAt = parseDateTime(node.path("createdAt").asText(null));
            LocalDate start = createdAt == null ? LocalDate.parse("2026-06-01") : createdAt.toLocalDate().plusDays(14);
            ClassroomProposal proposal = ClassroomProposal.builder()
                    .proposalCode(proposalCode)
                    .title(node.path("title").asText())
                    .courseOffering(program)
                    .deliveryType(ClassroomDeliveryMode.OFFLINE)
                    .capacity(12)
                    .plannedStartDate(start)
                    .plannedEndDate(start.plusWeeks(10))
                    .scheduleWeekdays("1,3,5")
                    .sessionStartTime(LocalTime.of(18, 0))
                    .sessionEndTime(LocalTime.of(19, 30))
                    .createdBy(createdBy)
                    .reviewedBy(reviewedBy)
                    .approvalStatus(mapProposalStatus(node.path("status").asText("DRAFT")))
                    .build();
            proposal.setCreatedAt(createdAt);
            if (node.hasNonNull("reviewedAt")) {
                proposal.setReviewedAt(parseDateTime(node.path("reviewedAt").asText()));
            }
            classroomProposalRepository.save(proposal);
            existingCodes.add(proposalCode);
        }
    }

    private void seedRegistrationRequests(
            JsonNode registrationsNode,
            Map<String, InstructorLedCourse> programsByCode,
            Map<String, User> usersByEmail
    ) {
        for (JsonNode node : registrationsNode) {
            String naturalKey = node.path("naturalKey").asText();
            User learner = usersByEmail.get(node.path("learnerEmail").asText("").toLowerCase());
            InstructorLedCourse program = programsByCode.get(node.path("programCode").asText());
            if (learner == null || program == null) {
                continue;
            }
            String marker = NATURAL_KEY_PREFIX + naturalKey;
            boolean exists = courseRegistrationRequestRepository.findByLearnerOrderByCreatedAtDesc(learner).stream()
                    .anyMatch(row -> marker.equals(row.getConsultationTrack()));
            if (exists) {
                continue;
            }
            CourseRegistrationRequest request = CourseRegistrationRequest.builder()
                    .learner(learner)
                    .courseOffering(program)
                    .status(mapRegistrationStatus(node.path("status").asText("SUBMITTED")))
                    .requestSource(EnrollmentRequestSource.ONLINE)
                    .consultationTrack(marker)
                    .learnerNote(node.path("note").asText(null))
                    .build();
            request.setCreatedAt(parseDateTime(node.path("createdAt").asText(null)));
            courseRegistrationRequestRepository.save(request);
        }
    }

    private void seedNotifications(JsonNode notificationsNode, Map<String, User> usersByEmail) {
        for (JsonNode node : notificationsNode) {
            String naturalKey = node.path("naturalKey").asText();
            User recipient = usersByEmail.get(node.path("recipientEmail").asText("").toLowerCase());
            if (recipient == null) {
                continue;
            }
            if (appNotificationRepository.existsByUserIdAndDeduplicationKey(recipient.getId(), naturalKey)) {
                continue;
            }
            AppNotification notification = AppNotification.builder()
                    .user(recipient)
                    .type(MasterDemoMarkers.MARKER)
                    .title(node.path("title").asText())
                    .body(node.path("body").asText())
                    .deduplicationKey(naturalKey)
                    .actionPath(node.path("actionPath").asText(null))
                    .read(node.path("read").asBoolean(false))
                    .build();
            notification.setCreatedAt(parseDateTime(node.path("createdAt").asText(null)));
            if (notification.isRead()) {
                notification.setReadAt(notification.getCreatedAt());
            }
            appNotificationRepository.save(notification);
        }
    }

    private void seedTeacherEvaluations(JsonNode evaluationsNode, Map<String, User> usersByEmail) {
        for (JsonNode node : evaluationsNode) {
            User teacher = usersByEmail.get(node.path("teacherEmail").asText("").toLowerCase());
            User evaluator = usersByEmail.get(node.path("evaluatorEmail").asText("").toLowerCase());
            if (teacher == null || evaluator == null) {
                continue;
            }
            LocalDate periodEnd = LocalDate.parse(node.path("periodTo").asText());
            boolean exists = teacherEvaluationRepository.findByTeacherIdOrderByPeriodEndDescIdDesc(teacher.getId())
                    .stream()
                    .anyMatch(row -> periodEnd.equals(row.getPeriodEnd()));
            if (exists) {
                continue;
            }
            BigDecimal overall = BigDecimal.valueOf(node.path("score").asDouble(7))
                    .min(BigDecimal.valueOf(9.99))
                    .max(BigDecimal.valueOf(1))
                    .setScale(2, RoundingMode.HALF_UP);
            TeacherPerformanceEvaluation evaluation = TeacherPerformanceEvaluation.builder()
                    .teacher(teacher)
                    .evaluator(evaluator)
                    .periodStart(LocalDate.parse(node.path("periodFrom").asText()))
                    .periodEnd(periodEnd)
                    .lessonDeliveryScore(overall)
                    .learnerSupportScore(overall)
                    .gradingTimelinessScore(overall)
                    .professionalismScore(overall)
                    .overallScore(overall)
                    .strengths(node.path("comment").asText(null))
                    .status(TeacherEvaluationStatus.PUBLISHED)
                    .publishedAt(parseDateTime(node.path("periodTo").asText() + "T10:00:00"))
                    .build();
            teacherEvaluationRepository.save(evaluation);
        }
    }

    private void seedMaterials(
            JsonNode materialsNode,
            Map<String, ClassSection> sectionsByCode,
            Map<String, User> usersByEmail
    ) {
        for (JsonNode node : materialsNode) {
            String naturalKey = node.path("naturalKey").asText();
            ClassSection section = sectionsByCode.get(node.path("classCode").asText());
            if (section == null) {
                continue;
            }
            if (materialRepository.existsByFileUrlEndingWith(naturalKey)) {
                continue;
            }
            User uploader = usersByEmail.get(node.path("uploadedByEmail").asText("").toLowerCase());
            ClassroomMaterial material = ClassroomMaterial.builder()
                    .classSection(section)
                    .title(node.path("title").asText())
                    .fileUrl(MASTER_REF_PREFIX + naturalKey)
                    .fileType("application/pdf")
                    .uploadedBy(uploader)
                    .build();
            material.setCreatedAt(parseDateTime(node.path("uploadedAt").asText(null)));
            materialRepository.save(material);
        }
    }

    private Map<String, CourseDiscussionPost> seedDiscussions(
            JsonNode discussionsNode,
            Map<String, OnlineCourse> coursesBySlug,
            Map<String, User> usersByEmail
    ) {
        Map<String, CourseDiscussionPost> byNaturalKey = new HashMap<>();
        if (discussionsNode == null || !discussionsNode.isArray()) {
            return byNaturalKey;
        }
        for (JsonNode node : discussionsNode) {
            String postTypeRaw = node.path("postType").asText("THREAD");
            if (!"THREAD".equalsIgnoreCase(postTypeRaw)) {
                continue;
            }
            CourseDiscussionPost thread = upsertDiscussionPost(
                    node,
                    null,
                    coursesBySlug,
                    usersByEmail,
                    byNaturalKey,
                    CourseDiscussionPostType.THREAD
            );
            if (thread == null) {
                continue;
            }
            String naturalKey = node.path("naturalKey").asText(null);
            if (naturalKey != null && !naturalKey.isBlank()) {
                byNaturalKey.put(naturalKey, thread);
            }
            JsonNode nestedReplies = node.path("replies");
            if (nestedReplies.isArray()) {
                for (JsonNode replyNode : nestedReplies) {
                    CourseDiscussionPost reply = upsertDiscussionPost(
                            replyNode,
                            thread,
                            coursesBySlug,
                            usersByEmail,
                            byNaturalKey,
                            CourseDiscussionPostType.REPLY
                    );
                    if (reply == null) {
                        continue;
                    }
                    String replyKey = replyNode.path("naturalKey").asText(null);
                    if (replyKey != null && !replyKey.isBlank()) {
                        byNaturalKey.put(replyKey, reply);
                    }
                }
            }
        }
        return byNaturalKey;
    }

    private void seedDiscussionReplies(
            JsonNode repliesNode,
            Map<String, CourseDiscussionPost> postsByNaturalKey,
            Map<String, User> usersByEmail
    ) {
        if (repliesNode == null || !repliesNode.isArray()) {
            return;
        }
        for (JsonNode node : repliesNode) {
            String parentKey = node.path("parentNaturalKey").asText(null);
            if (parentKey == null || parentKey.isBlank()) {
                continue;
            }
            CourseDiscussionPost parent = postsByNaturalKey.get(parentKey);
            if (parent == null) {
                continue;
            }
            CourseDiscussionPost reply = upsertDiscussionPost(
                    node,
                    parent,
                    Map.of(),
                    usersByEmail,
                    postsByNaturalKey,
                    CourseDiscussionPostType.REPLY
            );
            if (reply == null) {
                continue;
            }
            String naturalKey = node.path("naturalKey").asText(null);
            if (naturalKey != null && !naturalKey.isBlank()) {
                postsByNaturalKey.put(naturalKey, reply);
            }
        }
    }

    private CourseDiscussionPost upsertDiscussionPost(
            JsonNode node,
            CourseDiscussionPost parentThread,
            Map<String, OnlineCourse> coursesBySlug,
            Map<String, User> usersByEmail,
            Map<String, CourseDiscussionPost> postsByNaturalKey,
            CourseDiscussionPostType postType
    ) {
        String naturalKey = node.path("naturalKey").asText(null);
        if (naturalKey != null && postsByNaturalKey.containsKey(naturalKey)) {
            return postsByNaturalKey.get(naturalKey);
        }
        User author = usersByEmail.get(node.path("authorEmail").asText("").toLowerCase());
        if (author == null || MasterDemoMarkers.isPreservedEmail(author.getEmail())) {
            return null;
        }
        OnlineCourse course;
        if (parentThread != null) {
            course = parentThread.getCourse();
        } else {
            course = coursesBySlug.get(node.path("courseSlug").asText());
        }
        if (course == null || MasterDemoMarkers.isProtectedCourseSlug(course.getSlug())) {
            return null;
        }
        String visibleContent = node.path("content").asText("");
        String storedContent = naturalKey == null ? visibleContent : markedContent(naturalKey, visibleContent);
        CourseDiscussionPost post;
        if (postType == CourseDiscussionPostType.THREAD) {
            String title = node.path("title").asText();
            post = discussionPostRepository
                    .findFirstByCourseAndPostTypeAndTitle(course, CourseDiscussionPostType.THREAD, title)
                    .orElseGet(() -> CourseDiscussionPost.builder()
                            .course(course)
                            .postType(CourseDiscussionPostType.THREAD)
                            .title(title)
                            .build());
            post.setLesson(resolveLesson(course, node.path("lessonStableKey").asText(null)));
        } else {
            if (parentThread == null) {
                return null;
            }
            // After cleanup, owned replies are gone — create fresh (avoid findAll PC pollution).
            post = CourseDiscussionPost.builder()
                    .course(course)
                    .parentPost(parentThread)
                    .postType(CourseDiscussionPostType.REPLY)
                    .build();
        }
        post.setAuthor(userRepository.getReferenceById(author.getId()));
        post.setContent(storedContent);
        post.setAccepted(node.path("accepted").asBoolean(false));
        post.setStatus(CourseDiscussionStatus.valueOf(node.path("status").asText("OPEN")));
        if (node.hasNonNull("createdAt")) {
            post.setCreatedAt(parseDateTime(node.path("createdAt").asText()));
        }
        return discussionPostRepository.save(post);
    }

    private void seedDiscussionReactions(
            JsonNode reactionsNode,
            Map<String, CourseDiscussionPost> postsByNaturalKey,
            Map<String, User> usersByEmail
    ) {
        if (reactionsNode == null || !reactionsNode.isArray()) {
            return;
        }
        for (JsonNode node : reactionsNode) {
            CourseDiscussionPost post = postsByNaturalKey.get(node.path("postNaturalKey").asText());
            User user = usersByEmail.get(node.path("userEmail").asText("").toLowerCase());
            if (post == null || user == null || MasterDemoMarkers.isPreservedEmail(user.getEmail())) {
                continue;
            }
            CourseDiscussionReaction reaction = discussionReactionRepository.findByPostAndUser(post, user)
                    .orElseGet(() -> CourseDiscussionReaction.builder().post(post).user(user).build());
            reaction.setReactionType(
                    CourseDiscussionReactionType.valueOf(node.path("reactionType").asText("LIKE"))
            );
            reaction.setHelpful(node.path("helpful").asBoolean(false));
            discussionReactionRepository.save(reaction);
        }
    }

    private void seedDiscussionReports(
            JsonNode reportsNode,
            Map<String, CourseDiscussionPost> postsByNaturalKey,
            Map<String, User> usersByEmail
    ) {
        if (reportsNode == null || !reportsNode.isArray()) {
            return;
        }
        for (JsonNode node : reportsNode) {
            CourseDiscussionPost post = postsByNaturalKey.get(node.path("postNaturalKey").asText());
            User reporter = usersByEmail.get(node.path("reporterEmail").asText("").toLowerCase());
            if (post == null || reporter == null) {
                continue;
            }
            CourseDiscussionReport report = discussionReportRepository.findByPostAndReporter(post, reporter)
                    .orElseGet(() -> CourseDiscussionReport.builder().post(post).reporter(reporter).build());
            report.setReason(node.path("reason").asText(null));
            report.setReasonCategory(CourseDiscussionReportReasonCategory.valueOf(
                    node.path("reasonCategory").asText("OTHER")
            ));
            report.setStatus(CourseDiscussionReportStatus.valueOf(node.path("status").asText("PENDING")));
            String reviewerEmail = node.path("reviewedByEmail").asText(null);
            if (reviewerEmail != null) {
                report.setReviewedBy(usersByEmail.get(reviewerEmail.toLowerCase()));
            }
            if (node.hasNonNull("reviewedAt")) {
                report.setReviewedAt(parseDateTime(node.path("reviewedAt").asText()));
            }
            report.setActionNote(node.path("actionNote").asText(null));
            discussionReportRepository.save(report);
        }
    }

    private void seedTeacherFeedback(
            JsonNode feedbackNode,
            Map<String, ClassSection> sectionsByCode,
            Map<String, User> usersByEmail
    ) {
        if (feedbackNode == null || !feedbackNode.isArray()) {
            return;
        }
        for (JsonNode node : feedbackNode) {
            ClassSection section = sectionsByCode.get(node.path("classCode").asText());
            User learner = usersByEmail.get(node.path("learnerEmail").asText("").toLowerCase());
            User teacher = usersByEmail.get(node.path("teacherEmail").asText("").toLowerCase());
            if (section == null || learner == null || teacher == null
                    || MasterDemoMarkers.isPreservedEmail(learner.getEmail())
                    || MasterDemoMarkers.isPreservedEmail(teacher.getEmail())) {
                continue;
            }
            ClassEnrollment enrollment = classEnrollmentRepository
                    .findByStudentIdAndClassSectionId(learner.getId(), section.getId())
                    .orElse(null);
            if (enrollment == null) {
                continue;
            }
            TeacherCourseFeedback feedback = teacherCourseFeedbackRepository
                    .findByEnrollmentIdAndTeacherId(enrollment.getId(), teacher.getId())
                    .orElseGet(() -> TeacherCourseFeedback.builder()
                            .enrollment(enrollment)
                            .classSection(section)
                            .teacher(teacher)
                            .build());
            feedback.setClarityScore(node.path("clarityScore").asInt(4));
            feedback.setEngagementScore(node.path("engagementScore").asInt(4));
            feedback.setLearnerSupportScore(node.path("learnerSupportScore").asInt(4));
            feedback.setFeedbackTimelinessScore(node.path("feedbackTimelinessScore").asInt(4));
            feedback.setProfessionalismScore(node.path("professionalismScore").asInt(4));
            feedback.setPace(TeacherFeedbackPace.valueOf(node.path("pace").asText("JUST_RIGHT")));
            feedback.setWouldRecommend(node.path("wouldRecommend").asBoolean(true));
            feedback.setStrengths(node.path("strengths").asText(""));
            feedback.setImprovementSuggestions(node.path("improvementSuggestions").asText(""));
            feedback.setAdditionalComment(node.path("additionalComment").asText(null));
            feedback.setSubmittedAt(parseDateTime(node.path("submittedAt").asText(null)));
            teacherCourseFeedbackRepository.save(feedback);
        }
    }

    private void seedAnnouncements(
            JsonNode announcementsNode,
            Map<String, ClassSection> sectionsByCode,
            Map<String, User> usersByEmail
    ) {
        if (announcementsNode == null || !announcementsNode.isArray()) {
            return;
        }
        for (JsonNode node : announcementsNode) {
            ClassSection section = sectionsByCode.get(node.path("classCode").asText());
            if (section == null) {
                continue;
            }
            String title = node.path("title").asText();
            boolean exists = classroomAnnouncementRepository.findByClassSectionIdOrderByCreatedAtDesc(section.getId())
                    .stream()
                    .anyMatch(row -> title.equals(row.getTitle()));
            if (exists) {
                continue;
            }
            User createdBy = usersByEmail.get(node.path("createdByEmail").asText("").toLowerCase());
            ClassroomAnnouncement announcement = ClassroomAnnouncement.builder()
                    .classSection(section)
                    .title(title)
                    .content(node.path("content").asText())
                    .createdBy(createdBy)
                    .build();
            if (node.hasNonNull("createdAt")) {
                announcement.setCreatedAt(parseDateTime(node.path("createdAt").asText()));
            }
            classroomAnnouncementRepository.save(announcement);
        }
    }

    private void seedBroadcasts(JsonNode broadcastsNode, Map<String, User> usersByEmail) {
        if (broadcastsNode == null || !broadcastsNode.isArray()) {
            return;
        }
        for (JsonNode node : broadcastsNode) {
            String naturalKey = node.path("naturalKey").asText(null);
            String actionPath = node.path("actionPath").asText(null);
            if (actionPath == null && naturalKey != null) {
                actionPath = MASTER_REF_PREFIX + "broadcast/" + naturalKey;
            }
            String finalActionPath = actionPath;
            boolean exists = adminBroadcastRepository.findAll().stream()
                    .anyMatch(row -> finalActionPath != null && finalActionPath.equals(row.getActionPath()));
            if (exists) {
                continue;
            }
            User createdBy = usersByEmail.get(node.path("createdByEmail").asText("").toLowerCase());
            if (createdBy == null) {
                continue;
            }
            AdminBroadcast broadcast = AdminBroadcast.builder()
                    .title(node.path("title").asText())
                    .message(node.path("message").asText())
                    .targetRole(node.path("targetRole").asText(null))
                    .actionPath(actionPath)
                    .sendInApp(node.path("sendInApp").asBoolean(true))
                    .sendEmail(node.path("sendEmail").asBoolean(false))
                    .status(BroadcastStatus.valueOf(node.path("status").asText("DRAFT")))
                    .scheduledAt(parseDateTime(node.path("scheduledAt").asText(null)))
                    .sentAt(parseDateTime(node.path("sentAt").asText(null)))
                    .recipientCount(node.path("recipientCount").asInt(0))
                    .inAppSuccessCount(node.path("inAppSuccessCount").asInt(0))
                    .emailQueuedCount(node.path("emailQueuedCount").asInt(0))
                    .createdBy(createdBy)
                    .build();
            adminBroadcastRepository.save(broadcast);
        }
    }

    private void seedTicketMessages(
            JsonNode messagesNode,
            Map<String, SupportTicket> ticketsByNaturalKey,
            Map<String, User> usersByEmail
    ) {
        if (messagesNode == null || !messagesNode.isArray()) {
            return;
        }
        for (JsonNode node : messagesNode) {
            SupportTicket ticket = ticketsByNaturalKey.get(node.path("ticketNaturalKey").asText());
            if (ticket == null) {
                continue;
            }
            User author = usersByEmail.get(node.path("authorEmail").asText("").toLowerCase());
            if (author == null) {
                continue;
            }
            String naturalKey = node.path("naturalKey").asText(null);
            String body = node.path("body").asText();
            String storedBody = naturalKey == null ? body : markedContent(naturalKey, body);
            boolean exists = supportTicketMessageRepository.findByTicketIdOrderByCreatedAtAsc(ticket.getId()).stream()
                    .anyMatch(row -> storedBody.equals(row.getBody()));
            if (exists) {
                continue;
            }
            SupportTicketMessage message = SupportTicketMessage.builder()
                    .ticket(ticket)
                    .author(author)
                    .body(storedBody)
                    .build();
            if (node.hasNonNull("createdAt")) {
                message.setCreatedAt(parseDateTime(node.path("createdAt").asText()));
            }
            supportTicketMessageRepository.save(message);
        }
    }

    private void seedChangeRequests(
            JsonNode requestsNode,
            Map<String, ClassSection> sectionsByCode,
            Map<String, ClassSchedule> sessionsByNaturalKey,
            Map<String, User> usersByEmail
    ) {
        if (requestsNode == null || !requestsNode.isArray()) {
            return;
        }
        for (JsonNode node : requestsNode) {
            String naturalKey = node.path("naturalKey").asText(null);
            User requester = usersByEmail.get(node.path("requesterEmail").asText("").toLowerCase());
            ClassSection section = sectionsByCode.get(node.path("classCode").asText());
            if (requester == null || section == null || MasterDemoMarkers.isPreservedEmail(requester.getEmail())) {
                continue;
            }
            if (naturalKey != null && changeRequestExists(requester, naturalKey)) {
                continue;
            }
            ClassSchedule targetSession = null;
            String sessionKey = node.path("sessionNaturalKey").asText(null);
            if (sessionKey != null) {
                targetSession = sessionsByNaturalKey.get(sessionKey);
            }
            User reviewer = usersByEmail.get(node.path("reviewerEmail").asText("").toLowerCase());
            ClassroomChangeRequest request = ClassroomChangeRequest.builder()
                    .requestType(ClassroomChangeRequestType.valueOf(node.path("requestType").asText()))
                    .requester(requester)
                    .requesterRole(node.path("requesterRole").asText("LEARNER"))
                    .classSection(section)
                    .targetClassSchedule(targetSession)
                    .oldValuesJson(wrapChangeRequestMarker(naturalKey, node.path("oldValuesJson").asText(null)))
                    .newValuesJson(node.path("newValuesJson").asText(null))
                    .reason(node.path("reason").asText())
                    .status(ClassroomChangeRequestStatus.valueOf(node.path("status").asText("PENDING")))
                    .reviewer(reviewer)
                    .reviewedAt(parseDateTime(node.path("reviewedAt").asText(null)))
                    .reviewNote(node.path("reviewNote").asText(null))
                    .build();
            if (node.hasNonNull("createdAt")) {
                request.setCreatedAt(parseDateTime(node.path("createdAt").asText()));
            }
            classroomChangeRequestRepository.save(request);
        }
    }

    private void seedAttendanceDisputes(
            JsonNode disputesNode,
            Map<String, ClassSection> sectionsByCode,
            Map<String, ClassSchedule> sessionsByNaturalKey,
            Map<String, User> usersByEmail
    ) {
        if (disputesNode == null || !disputesNode.isArray()) {
            return;
        }
        for (JsonNode node : disputesNode) {
            String classCode = node.path("classCode").asText("");
            String sessionDateText = node.path("sessionDate").asText("");
            String startTimeText = node.path("startTime").asText("");
            String learnerEmail = node.path("learnerEmail").asText("").toLowerCase();
            String attendanceKey = node.path("attendanceNaturalKey").asText("");
            if (!attendanceKey.isBlank()) {
                // Format: classCode|sessionDate|startTime|learnerEmail
                String[] parts = attendanceKey.split("\\|", -1);
                if (parts.length >= 4) {
                    if (classCode.isBlank()) {
                        classCode = parts[0];
                    }
                    if (sessionDateText.isBlank()) {
                        sessionDateText = parts[1];
                    }
                    if (startTimeText.isBlank()) {
                        startTimeText = parts[2];
                    }
                    if (learnerEmail.isBlank()) {
                        learnerEmail = parts[3].toLowerCase();
                    }
                }
            }
            ClassSection section = sectionsByCode.get(classCode);
            User learner = usersByEmail.get(learnerEmail);
            if (section == null || learner == null) {
                continue;
            }
            String sessionKey = node.path("sessionNaturalKey").asText(
                    classCode + "|" + sessionDateText + "|" + startTimeText
            );
            ClassSchedule session = sessionsByNaturalKey.get(sessionKey);
            if (session == null) {
                continue;
            }
            ClassroomAttendance row = attendanceRepository
                    .findBySessionIdAndStudentId(session.getId(), learner.getId())
                    .orElse(null);
            if (row == null) {
                continue;
            }
            row.setDisputeReason(node.path("disputeReason").asText(null));
            row.setDisputeStatus(AttendanceDisputeStatus.valueOf(node.path("disputeStatus").asText("PENDING")));
            row.setDisputeReviewNote(node.path("disputeReviewNote").asText(null));
            String reviewerEmail = node.path("disputeReviewedByEmail").asText(null);
            if (reviewerEmail != null) {
                row.setDisputeReviewedBy(usersByEmail.get(reviewerEmail.toLowerCase()));
            }
            if (node.hasNonNull("disputeReviewedAt")) {
                row.setDisputeReviewedAt(parseDateTime(node.path("disputeReviewedAt").asText()));
            }
            attendanceRepository.save(row);
        }
    }

    private void seedCourseListItems(
            JsonNode itemsNode,
            Map<String, OnlineCourse> coursesBySlug,
            Map<String, User> usersByEmail
    ) {
        if (itemsNode == null || !itemsNode.isArray()) {
            return;
        }
        for (JsonNode node : itemsNode) {
            User student = usersByEmail.get(node.path("learnerEmail").asText("").toLowerCase());
            OnlineCourse course = coursesBySlug.get(node.path("courseSlug").asText());
            if (student == null || course == null || MasterDemoMarkers.isPreservedEmail(student.getEmail())
                    || MasterDemoMarkers.isProtectedCourseSlug(course.getSlug())) {
                continue;
            }
            CourseListType listType = CourseListType.valueOf(node.path("listType").asText("WISHLIST"));
            if (courseListItemRepository.findByStudentAndOnlineCourseIdAndListType(
                    student,
                    course.getId(),
                    listType
            ).isPresent()) {
                continue;
            }
            courseListItemRepository.save(CourseListItem.builder()
                    .student(student)
                    .onlineCourse(course)
                    .listType(listType)
                    .build());
        }
    }

    private Map<String, ClassroomTuitionPayment> seedTuitionPayments(
            JsonNode paymentsNode,
            Map<String, ClassSection> sectionsByCode,
            Map<String, User> usersByEmail
    ) {
        Map<String, ClassroomTuitionPayment> byNaturalKey = new HashMap<>();
        if (paymentsNode == null || !paymentsNode.isArray()) {
            return byNaturalKey;
        }
        for (JsonNode node : paymentsNode) {
            String naturalKey = node.path("naturalKey").asText(null);
            ClassEnrollment enrollment = resolveClassEnrollment(
                    sectionsByCode,
                    node.path("classCode").asText(),
                    node.path("learnerEmail").asText("").toLowerCase(),
                    usersByEmail
            );
            if (enrollment == null) {
                continue;
            }
            if (naturalKey != null) {
                Optional<ClassroomTuitionPayment> existing = classroomTuitionPaymentRepository
                        .findByEnrollmentIdOrderByCreatedAtDesc(enrollment.getId())
                        .stream()
                        .filter(row -> naturalKey.equals(row.getNote()))
                        .findFirst();
                if (existing.isPresent()) {
                    byNaturalKey.put(naturalKey, existing.get());
                    continue;
                }
            }
            User recordedBy = usersByEmail.get(node.path("recordedByEmail").asText("").toLowerCase());
            ClassroomTuitionPayment payment = ClassroomTuitionPayment.builder()
                    .enrollment(enrollment)
                    .amount(BigDecimal.valueOf(node.path("amount").asLong(0)))
                    .paymentKind(TuitionPaymentKind.valueOf(node.path("paymentKind").asText("PARTIAL")))
                    .note(naturalKey != null ? naturalKey : node.path("note").asText(null))
                    .recordedBy(recordedBy)
                    .build();
            payment = classroomTuitionPaymentRepository.save(payment);
            if (naturalKey != null) {
                byNaturalKey.put(naturalKey, payment);
            }
        }
        return byNaturalKey;
    }

    private void seedTuitionProofs(
            JsonNode proofsNode,
            Map<String, ClassroomTuitionPayment> tuitionByNaturalKey,
            Map<String, ClassSection> sectionsByCode,
            Map<String, User> usersByEmail
    ) {
        if (proofsNode == null || !proofsNode.isArray()) {
            return;
        }
        for (JsonNode node : proofsNode) {
            String naturalKey = node.path("naturalKey").asText(null);
            if (naturalKey != null && classroomTuitionPaymentProofRepository.existsByFileUrlEndingWith(naturalKey)) {
                continue;
            }
            ClassEnrollment enrollment = null;
            TuitionPaymentKind paymentKind = TuitionPaymentKind.PARTIAL;
            BigDecimal amount = BigDecimal.valueOf(node.path("amount").asLong(0));

            String paymentKey = node.path("tuitionPaymentNaturalKey").asText("");
            ClassroomTuitionPayment linked = paymentKey.isBlank() ? null : tuitionByNaturalKey.get(paymentKey);
            if (linked != null) {
                enrollment = linked.getEnrollment();
                paymentKind = linked.getPaymentKind();
                if (amount.signum() == 0 && linked.getAmount() != null) {
                    amount = linked.getAmount();
                }
            }
            if (enrollment == null) {
                String classCode = node.path("classCode").asText("");
                String learnerEmail = node.path("learnerEmail").asText("").toLowerCase();
                String fileUrlHint = node.path("fileUrl").asText("");
                // fileUrl: master-demo://tuition-proof/{classCode}/{learnerEmail}/...
                if ((classCode.isBlank() || learnerEmail.isBlank()) && fileUrlHint.contains("tuition-proof/")) {
                    String rest = fileUrlHint.substring(fileUrlHint.indexOf("tuition-proof/") + "tuition-proof/".length());
                    String[] parts = rest.split("/");
                    if (parts.length >= 2) {
                        if (classCode.isBlank()) {
                            classCode = parts[0];
                        }
                        if (learnerEmail.isBlank()) {
                            learnerEmail = parts[1].toLowerCase();
                        }
                    }
                }
                enrollment = resolveClassEnrollment(sectionsByCode, classCode, learnerEmail, usersByEmail);
                if (node.hasNonNull("paymentKind")) {
                    paymentKind = TuitionPaymentKind.valueOf(node.path("paymentKind").asText("PARTIAL"));
                }
            }
            if (enrollment == null) {
                continue;
            }
            User reviewedBy = usersByEmail.get(node.path("reviewedByEmail").asText("").toLowerCase());
            String fileUrl = node.path("fileUrl").asText(MASTER_REF_PREFIX + (naturalKey == null ? "proof" : naturalKey));
            ClassroomTuitionPaymentProof proof = ClassroomTuitionPaymentProof.builder()
                    .enrollment(enrollment)
                    .amount(amount)
                    .paymentKind(paymentKind)
                    .fileUrl(fileUrl)
                    .note(naturalKey != null ? naturalKey : node.path("note").asText(null))
                    .status(TuitionProofStatus.valueOf(node.path("status").asText("PENDING")))
                    .reviewNote(node.path("reviewNote").asText(null))
                    .reviewedBy(reviewedBy)
                    .reviewedAt(parseDateTime(node.path("reviewedAt").asText(null)))
                    .build();
            classroomTuitionPaymentProofRepository.save(proof);
        }
    }

    private void seedTeacherCredentials(JsonNode credentialsNode, Map<String, User> usersByEmail) {
        if (credentialsNode == null || !credentialsNode.isArray()) {
            return;
        }
        for (JsonNode node : credentialsNode) {
            User teacher = usersByEmail.get(node.path("teacherEmail").asText("").toLowerCase());
            if (teacher == null || MasterDemoMarkers.isPreservedEmail(teacher.getEmail())) {
                continue;
            }
            String naturalKey = node.path("naturalKey").asText(null);
            String documentUrl = node.hasNonNull("documentUrl")
                    ? node.path("documentUrl").asText()
                    : (naturalKey != null ? MASTER_REF_PREFIX + naturalKey : null);
            if (documentUrl == null) {
                continue;
            }
            boolean exists = teacherCredentialRepository.findByTeacherIdOrderByIssuedDateDescIdDesc(teacher.getId()).stream()
                    .anyMatch(row -> documentUrl.equals(row.getDocumentUrl()));
            if (exists) {
                continue;
            }
            User verifiedBy = usersByEmail.get(node.path("verifiedByEmail").asText("").toLowerCase());
            TeacherCredential credential = TeacherCredential.builder()
                    .teacher(teacher)
                    .type(node.path("type").asText())
                    .title(node.path("title").asText())
                    .issuer(node.path("issuer").asText())
                    .credentialNumber(node.path("credentialNumber").asText(null))
                    .issuedDate(parseDate(node.path("issuedDate").asText(null)))
                    .expiryDate(parseDate(node.path("expiryDate").asText(null)))
                    .documentUrl(documentUrl)
                    .verificationStatus(CredentialVerificationStatus.valueOf(
                            node.path("verificationStatus").asText("PENDING")
                    ))
                    .verifiedBy(verifiedBy)
                    .verifiedAt(parseDateTime(node.path("verifiedAt").asText(null)))
                    .verificationNote(node.path("verificationNote").asText(null))
                    .build();
            teacherCredentialRepository.save(credential);
        }
    }

    private void seedPracticeAttempts(
            JsonNode attemptsNode,
            Map<String, ClassSection> sectionsByCode,
            Map<String, User> usersByEmail
    ) {
        if (attemptsNode == null || !attemptsNode.isArray()) {
            return;
        }
        ExerciseBankItem exercise = ensureMasterPracticeExercise(usersByEmail);
        for (JsonNode node : attemptsNode) {
            ClassSection section = sectionsByCode.get(node.path("classCode").asText());
            User student = usersByEmail.get(node.path("learnerEmail").asText("").toLowerCase());
            if (section == null || student == null || MasterDemoMarkers.isPreservedEmail(student.getEmail())) {
                continue;
            }
            String naturalKey = node.path("naturalKey").asText(null);
            String markerText = naturalKey == null ? null : markedContent(naturalKey, "");
            int attemptNumber = node.path("attemptNumber").asInt(1);
            boolean exists = practiceAttemptHistoryRepository
                    .findByClassSectionIdAndStudentIdOrderByCompletedAtDesc(section.getId(), student.getId())
                    .stream()
                    .anyMatch(row -> attemptNumber == row.getAttemptNumber()
                            && (markerText == null || markerText.equals(row.getResponseText())));
            if (exists) {
                continue;
            }
            LocalDateTime completedAt = parseDateTime(node.path("completedAt").asText(null));
            if (completedAt == null) {
                completedAt = LocalDateTime.now();
            }
            ClassroomPracticeAttemptHistory attempt = ClassroomPracticeAttemptHistory.builder()
                    .classSection(section)
                    .student(student)
                    .exercise(exercise)
                    .attemptNumber(attemptNumber)
                    .responseText(markerText != null ? markerText : node.path("responseText").asText(null))
                    .answersJson(node.path("answersJson").asText(null))
                    .correctAnswers(node.path("correctAnswers").isNull() ? null : node.path("correctAnswers").asInt())
                    .totalQuestions(node.path("totalQuestions").isNull() ? null : node.path("totalQuestions").asInt())
                    .scorePercent(node.path("scorePercent").isNull() ? null : node.path("scorePercent").asDouble())
                    .durationSeconds(node.path("durationSeconds").isNull() ? null : node.path("durationSeconds").asInt())
                    .startedAt(parseDateTime(node.path("startedAt").asText(null)))
                    .completedAt(completedAt)
                    .build();
            practiceAttemptHistoryRepository.save(attempt);
        }
    }

    private ExerciseBankItem ensureMasterPracticeExercise(Map<String, User> usersByEmail) {
        final String code = "ilc-ex-classroom-practice";
        return exerciseBankItemRepository.findAllByOrderByUpdatedAtDesc().stream()
                .filter(item -> code.equalsIgnoreCase(item.getCode()))
                .findFirst()
                .orElseGet(() -> {
                    User createdBy = usersByEmail.values().stream()
                            .filter(u -> u.getEmail() != null && (
                                    u.getEmail().equalsIgnoreCase("content.manager@englishlab.vn")
                                            || u.getEmail().startsWith("cm.")
                            ))
                            .findFirst()
                            .orElse(null);
                    Map<String, Object> contentData = new HashMap<>();
                    contentData.put("questions", List.of(
                            Map.of("prompt", "Choose the best paraphrase for 'affordable'.", "answer", "budget-friendly")
                    ));
                    ExerciseBankItem created = ExerciseBankItem.builder()
                            .bankType("EXERCISE")
                            .code(code)
                            .title("Classroom Practice Drill")
                            .skill("READING")
                            .status("PUBLISHED")
                            .contentData(contentData)
                            .createdBy(createdBy)
                            .build();
                    return exerciseBankItemRepository.save(created);
                });
    }

    private void seedFlashcardSets(JsonNode flashcardSetsNode, Map<String, User> usersByEmail) {
        if (flashcardSetsNode == null || !flashcardSetsNode.isArray()) {
            return;
        }
        for (JsonNode node : flashcardSetsNode) {
            String code = node.path("code").asText(null);
            if (code == null || code.isBlank()) {
                code = "ilc-fc-" + node.path("naturalKey").asText("set");
            }
            if (!MasterDemoMarkers.isOwnedFlashcardCode(code)) {
                continue;
            }
            String finalCode = code;
            Optional<ContentBankItem> existing = contentBankItemRepository.findByCodeIgnoreCaseAndBankType(
                    finalCode,
                    ContentBankType.FLASHCARD
            );
            User createdBy = usersByEmail.get(node.path("createdByEmail").asText("").toLowerCase());
            Map<String, Object> contentData = new HashMap<>();
            if (node.has("cards")) {
                contentData.put("cards", objectMapper.convertValue(node.path("cards"), List.class));
            } else if (node.has("contentData")) {
                contentData.putAll(objectMapper.convertValue(node.path("contentData"), Map.class));
            }
            ContentBankItem item = existing.orElseGet(() -> ContentBankItem.builder()
                    .bankType(ContentBankType.FLASHCARD)
                    .code(finalCode)
                    .build());
            item.setTitle(node.path("title").asText(item.getTitle()));
            item.setDescription(node.path("description").asText(item.getDescription()));
            item.setExamCategory(node.path("examCategory").asText(item.getExamCategory()));
            item.setSkill(node.path("skill").asText(item.getSkill()));
            item.setStatus(node.path("status").asText(item.getStatus() == null ? "PUBLISHED" : item.getStatus()));
            if (!contentData.isEmpty()) {
                item.setContentData(contentData);
            }
            if (item.getCreatedBy() == null && createdBy != null) {
                item.setCreatedBy(createdBy);
            }
            contentBankItemRepository.save(item);
        }
    }

    private void seedCenterLibrary(JsonNode libraryNode, Map<String, User> usersByEmail) {
        if (libraryNode == null || !libraryNode.isArray()) {
            return;
        }
        for (JsonNode node : libraryNode) {
            String naturalKey = node.path("naturalKey").asText(null);
            String fileUrl = node.path("fileUrl").asText(MASTER_REF_PREFIX + naturalKey);
            if (centerMaterialLibraryItemRepository.existsByFileUrlEndingWith(
                    naturalKey == null ? fileUrl : naturalKey
            )) {
                continue;
            }
            User createdBy = usersByEmail.get(node.path("createdByEmail").asText("").toLowerCase());
            CenterMaterialLibraryItem item = CenterMaterialLibraryItem.builder()
                    .title(node.path("title").asText())
                    .description(node.path("description").asText(null))
                    .fileUrl(fileUrl)
                    .fileType(node.path("fileType").asText("application/pdf"))
                    .materialType(node.path("materialType").asText(null))
                    .provider(node.path("provider").asText(null))
                    .examCategory(node.path("examCategory").asText(null))
                    .skill(node.path("skill").asText(null))
                    .tags(node.path("tags").asText(null))
                    .status(node.path("status").asText("PUBLISHED"))
                    .createdBy(createdBy)
                    .build();
            centerMaterialLibraryItemRepository.save(item);
        }
    }

    private void seedLessonNotes(
            JsonNode notesNode,
            Map<String, OnlineCourse> coursesBySlug,
            Map<String, User> usersByEmail
    ) {
        if (notesNode == null || !notesNode.isArray()) {
            return;
        }
        for (JsonNode node : notesNode) {
            User user = usersByEmail.get(node.path("userEmail").asText("").toLowerCase());
            OnlineCourse course = coursesBySlug.get(node.path("courseSlug").asText());
            if (user == null || course == null || MasterDemoMarkers.isPreservedEmail(user.getEmail())
                    || MasterDemoMarkers.isProtectedCourseSlug(course.getSlug())) {
                continue;
            }
            OnlineLesson lesson = resolveLesson(course, node.path("lessonStableKey").asText(null));
            if (lesson == null) {
                continue;
            }
            String naturalKey = node.path("naturalKey").asText(null);
            String content = node.path("content").asText("");
            String storedContent = naturalKey == null ? content : markedContent(naturalKey, content);
            boolean exists = learnerLessonNoteRepository.findByUserOrderByUpdatedAtDesc(user).stream()
                    .anyMatch(note -> note.getLesson() != null
                            && note.getLesson().getId().equals(lesson.getId())
                            && storedContent.equals(note.getContent()));
            if (exists) {
                continue;
            }
            learnerLessonNoteRepository.save(LearnerLessonNote.builder()
                    .user(user)
                    .lesson(lesson)
                    .content(storedContent)
                    .selectedText(node.path("selectedText").asText(null))
                    .build());
        }
    }

    private ClassEnrollment resolveClassEnrollment(
            Map<String, ClassSection> sectionsByCode,
            String classCode,
            String learnerEmail,
            Map<String, User> usersByEmail
    ) {
        ClassSection section = sectionsByCode.get(classCode);
        User learner = usersByEmail.get(learnerEmail);
        if (section == null || learner == null || MasterDemoMarkers.isPreservedEmail(learner.getEmail())) {
            return null;
        }
        return classEnrollmentRepository.findByStudentIdAndClassSectionId(learner.getId(), section.getId()).orElse(null);
    }

    private OnlineLesson resolveLesson(OnlineCourse course, String stableLessonKey) {
        if (course == null) {
            return null;
        }
        if (stableLessonKey == null || stableLessonKey.isBlank()) {
            return course.getPublishedModules().stream()
                    .flatMap(module -> module.getLessons().stream())
                    .findFirst()
                    .orElse(null);
        }
        return course.getPublishedModules().stream()
                .flatMap(module -> module.getLessons().stream())
                .filter(lesson -> stableLessonKey.equals(lesson.getStableLessonKey()))
                .findFirst()
                .orElse(null);
    }

    private String markedContent(String naturalKey, String visibleContent) {
        return NATURAL_KEY_PREFIX + naturalKey + "\n" + (visibleContent == null ? "" : visibleContent);
    }

    private boolean changeRequestExists(User requester, String naturalKey) {
        String marker = "\"masterNaturalKey\":\"" + naturalKey + "\"";
        return classroomChangeRequestRepository.findByRequesterIdOrderByCreatedAtDesc(requester.getId()).stream()
                .anyMatch(row -> row.getOldValuesJson() != null && row.getOldValuesJson().contains(marker));
    }

    private String wrapChangeRequestMarker(String naturalKey, String oldValuesJson) {
        if (naturalKey == null || naturalKey.isBlank()) {
            return oldValuesJson;
        }
        String marker = "\"masterNaturalKey\":\"" + naturalKey + "\"";
        if (oldValuesJson == null || oldValuesJson.isBlank()) {
            return "{" + marker + "}";
        }
        if (oldValuesJson.contains("masterNaturalKey")) {
            return oldValuesJson;
        }
        if (oldValuesJson.startsWith("{") && oldValuesJson.endsWith("}")) {
            return oldValuesJson.substring(0, oldValuesJson.length() - 1) + "," + marker + "}";
        }
        return "{" + marker + ",\"payload\":" + oldValuesJson + "}";
    }

    private LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return LocalDate.parse(raw);
    }

    private CourseLevel mapCourseLevel(String raw) {
        return switch (raw) {
            case "FOUNDATION" -> CourseLevel.BEGINNER;
            case "UPPER_INTERMEDIATE" -> CourseLevel.INTERMEDIATE;
            default -> CourseLevel.valueOf(raw);
        };
    }

    private ClassroomApprovalStatus mapProposalStatus(String raw) {
        return switch (raw) {
            case "APPROVED" -> ClassroomApprovalStatus.APPROVED;
            case "REJECTED" -> ClassroomApprovalStatus.REJECTED;
            case "DRAFT" -> ClassroomApprovalStatus.DRAFT;
            default -> ClassroomApprovalStatus.PENDING_APPROVAL;
        };
    }

    private EnrollmentRequestStatus mapRegistrationStatus(String raw) {
        return switch (raw) {
            case "APPROVED" -> EnrollmentRequestStatus.UNDER_STAFF_REVIEW;
            case "ASSIGNED" -> EnrollmentRequestStatus.CLASS_ASSIGNED;
            case "WAITING_PAYMENT" -> EnrollmentRequestStatus.WAITING_FOR_CLASS;
            case "REJECTED" -> EnrollmentRequestStatus.REJECTED;
            default -> EnrollmentRequestStatus.valueOf(raw);
        };
    }

    private LocalDateTime parseDateTime(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return LocalDateTime.parse(raw);
    }
}
