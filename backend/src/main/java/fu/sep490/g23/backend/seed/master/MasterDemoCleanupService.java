package fu.sep490.g23.backend.seed.master;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.course.CourseDiscussionPost;
import fu.sep490.g23.backend.entity.course.enums.CourseDiscussionPostType;
import fu.sep490.g23.backend.entity.curriculum.enums.ContentBankType;
import fu.sep490.g23.backend.entity.commerce.enums.CourseListType;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.admin.AdminBroadcastRepository;
import fu.sep490.g23.backend.repository.classroom.CenterMaterialLibraryItemRepository;
import fu.sep490.g23.backend.repository.classroom.ClassEnrollmentRepository;
import fu.sep490.g23.backend.repository.classroom.ClassScheduleRepository;
import fu.sep490.g23.backend.repository.classroom.ClassSectionRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomAnnouncementRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomAttendanceRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomChangeRequestRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomHomeworkRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomHomeworkSubmissionRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomMaterialRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomProposalRepository;
import fu.sep490.g23.backend.repository.classroom.CourseRegistrationRequestRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomPracticeAttemptHistoryRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomTuitionPaymentProofRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomTuitionPaymentRepository;
import fu.sep490.g23.backend.repository.classroom.RoomRepository;
import fu.sep490.g23.backend.repository.commerce.CourseListItemRepository;
import fu.sep490.g23.backend.repository.course.CourseDiscussionPostRepository;
import fu.sep490.g23.backend.repository.course.CourseDiscussionReactionRepository;
import fu.sep490.g23.backend.repository.course.CourseDiscussionReportRepository;
import fu.sep490.g23.backend.repository.course.InstructorLedCourseRepository;
import fu.sep490.g23.backend.repository.course.LearnerLessonNoteRepository;
import fu.sep490.g23.backend.repository.course.LessonProgressRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseEnrollmentRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseVersionRepository;
import fu.sep490.g23.backend.repository.curriculum.ContentBankItemRepository;
import fu.sep490.g23.backend.repository.notification.AppNotificationRepository;
import fu.sep490.g23.backend.repository.payment.DiscountCodeRepository;
import fu.sep490.g23.backend.repository.payment.PaymentOrderRepository;
import fu.sep490.g23.backend.repository.support.SupportTicketMessageRepository;
import fu.sep490.g23.backend.repository.support.SupportTicketRepository;
import fu.sep490.g23.backend.repository.teacher.TeacherCourseFeedbackRepository;
import fu.sep490.g23.backend.repository.teacher.TeacherCredentialRepository;
import fu.sep490.g23.backend.repository.teacher.TeacherPerformanceEvaluationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Deletes ONLY MASTER-marked demo rows. Never touches preserved Gmail accounts,
 * protected E2/Vocabulary course content, or Flyway history.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MasterDemoCleanupService {

    private static final String MASTER_REF_PREFIX = "master-demo://";

    private final UserRepository userRepository;
    private final ClassSectionRepository classSectionRepository;
    private final ClassScheduleRepository classScheduleRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;
    private final ClassroomAttendanceRepository attendanceRepository;
    private final ClassroomHomeworkRepository homeworkRepository;
    private final ClassroomHomeworkSubmissionRepository homeworkSubmissionRepository;
    private final ClassroomMaterialRepository materialRepository;
    private final RoomRepository roomRepository;
    private final OnlineCourseRepository onlineCourseRepository;
    private final OnlineCourseEnrollmentRepository onlineCourseEnrollmentRepository;
    private final OnlineCourseVersionRepository onlineCourseVersionRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final InstructorLedCourseRepository instructorLedCourseRepository;
    private final PaymentOrderRepository paymentOrderRepository;
    private final DiscountCodeRepository discountCodeRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final AppNotificationRepository appNotificationRepository;
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
    private final ClassroomProposalRepository classroomProposalRepository;
    private final CourseRegistrationRequestRepository courseRegistrationRequestRepository;
    private final ContentBankItemRepository contentBankItemRepository;
    private final CenterMaterialLibraryItemRepository centerMaterialLibraryItemRepository;
    private final LearnerLessonNoteRepository learnerLessonNoteRepository;
    private final TeacherPerformanceEvaluationRepository teacherPerformanceEvaluationRepository;

    @Transactional
    public void cleanupMasterOnly() {
        log.info("[MasterDemo] Cleanup MASTER rows only");

        List<User> masterUsers = userRepository.findAll().stream()
                .filter(user -> MasterDemoMarkers.isMasterEmail(user.getEmail()))
                .filter(user -> !MasterDemoMarkers.isPreservedEmail(user.getEmail()))
                .toList();

        cleanupDiscussionsOnDemoCourses();
        cleanupMasterFlashcards();
        cleanupCenterLibraryItems();
        cleanupAdminBroadcasts(masterUsers);

        for (User user : masterUsers) {
            learnerLessonNoteRepository.findByUserOrderByUpdatedAtDesc(user).forEach(learnerLessonNoteRepository::delete);
            courseListItemRepository.findByStudentAndListTypeOrderByAddedAtDesc(user, CourseListType.CART)
                    .forEach(courseListItemRepository::delete);
            courseListItemRepository.findByStudentAndListTypeOrderByAddedAtDesc(user, CourseListType.WISHLIST)
                    .forEach(courseListItemRepository::delete);
            if (!MasterDemoMarkers.isPreservedEmail(user.getEmail())) {
                teacherCredentialRepository.findByTeacherIdOrderByIssuedDateDescIdDesc(user.getId())
                        .stream()
                        .filter(credential -> credential.getDocumentUrl() != null
                                && credential.getDocumentUrl().startsWith(MASTER_REF_PREFIX))
                        .forEach(teacherCredentialRepository::delete);
            }
            supportTicketRepository.findByRequesterIdOrderByUpdatedAtDesc(user.getId()).forEach(ticket -> {
                supportTicketMessageRepository.findByTicketIdOrderByCreatedAtAsc(ticket.getId())
                        .forEach(supportTicketMessageRepository::delete);
                supportTicketRepository.delete(ticket);
            });
            appNotificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).forEach(appNotificationRepository::delete);
            paymentOrderRepository.findAll().stream()
                    .filter(order -> order.getStudent() != null && order.getStudent().getId().equals(user.getId()))
                    .filter(order -> order.getOrderCode() != null && order.getOrderCode() >= 900000 && order.getOrderCode() < 1000000)
                    .forEach(paymentOrderRepository::delete);
        }

        classSectionRepository.findAll().stream()
                .filter(section -> section.getCode() != null && section.getCode().startsWith(MasterDemoMarkers.CLASS_CODE_PREFIX))
                .forEach(section -> {
                    classroomChangeRequestRepository.findAll().stream()
                            .filter(row -> row.getClassSection() != null
                                    && Objects.equals(row.getClassSection().getId(), section.getId()))
                            .forEach(classroomChangeRequestRepository::delete);
                    classroomAnnouncementRepository.findByClassSectionIdOrderByCreatedAtDesc(section.getId())
                            .forEach(classroomAnnouncementRepository::delete);
                    teacherCourseFeedbackRepository.findAll().stream()
                            .filter(row -> row.getClassSection() != null
                                    && Objects.equals(row.getClassSection().getId(), section.getId()))
                            .forEach(teacherCourseFeedbackRepository::delete);
                    practiceAttemptHistoryRepository.findAll().stream()
                            .filter(row -> row.getClassSection() != null
                                    && Objects.equals(row.getClassSection().getId(), section.getId()))
                            .forEach(practiceAttemptHistoryRepository::delete);
                    classEnrollmentRepository.findAll().stream()
                            .filter(enr -> enr.getClassSection() != null && enr.getClassSection().getId().equals(section.getId()))
                            .forEach(enrollment -> {
                                classroomTuitionPaymentProofRepository.findByEnrollmentIdOrderByCreatedAtDesc(enrollment.getId())
                                        .forEach(classroomTuitionPaymentProofRepository::delete);
                                classroomTuitionPaymentRepository.findByEnrollmentIdOrderByCreatedAtDesc(enrollment.getId())
                                        .forEach(classroomTuitionPaymentRepository::delete);
                            });
                    classScheduleRepository.findByClassSectionIdOrderBySessionDateAscStartTimeAsc(section.getId())
                            .forEach(session -> {
                                attendanceRepository.findAll().stream()
                                        .filter(row -> row.getSession() != null && row.getSession().getId().equals(session.getId()))
                                        .forEach(attendanceRepository::delete);
                                classScheduleRepository.delete(session);
                            });
                    homeworkRepository.findByClassSectionIdOrderByCreatedAtDesc(section.getId()).forEach(hw -> {
                        homeworkSubmissionRepository.findAll().stream()
                                .filter(sub -> sub.getHomework() != null && sub.getHomework().getId().equals(hw.getId()))
                                .forEach(homeworkSubmissionRepository::delete);
                        homeworkRepository.delete(hw);
                    });
                    materialRepository.findByClassSectionIdOrderByCreatedAtDesc(section.getId())
                            .forEach(materialRepository::delete);
                    classEnrollmentRepository.findAll().stream()
                            .filter(enr -> enr.getClassSection() != null && enr.getClassSection().getId().equals(section.getId()))
                            .forEach(classEnrollmentRepository::delete);
                    classSectionRepository.delete(section);
                });

        roomRepository.findAll().stream()
                .filter(room -> room.getName() != null && room.getName().startsWith("Phòng học "))
                .forEach(roomRepository::delete);

        onlineCourseRepository.findAll().stream()
                .filter(course -> course.getSlug() != null && course.getSlug().startsWith(MasterDemoMarkers.COURSE_SLUG_PREFIX))
                .filter(course -> !MasterDemoMarkers.isProtectedCourseSlug(course.getSlug()))
                .forEach(course -> {
                    onlineCourseEnrollmentRepository.findByOnlineCourse(course).forEach(enrollment -> {
                        lessonProgressRepository.findByEnrollment(enrollment)
                                .forEach(lessonProgressRepository::delete);
                        onlineCourseEnrollmentRepository.delete(enrollment);
                    });
                    onlineCourseVersionRepository.findByOnlineCourseOrderByVersionNumberDesc(course)
                            .forEach(onlineCourseVersionRepository::delete);
                    onlineCourseRepository.delete(course);
                });

        instructorLedCourseRepository.findAll().stream()
                .filter(course -> course.getCode() != null && course.getCode().startsWith(MasterDemoMarkers.ILC_CODE_PREFIX))
                .forEach(course -> {
                    classroomProposalRepository.findAllByOrderByCreatedAtDesc().stream()
                            .filter(proposal -> proposal.getCourseOffering() != null
                                    && Objects.equals(proposal.getCourseOffering().getId(), course.getId()))
                            .forEach(classroomProposalRepository::delete);
                    courseRegistrationRequestRepository.findAllByOrderByCreatedAtDesc().stream()
                            .filter(request -> request.getCourseOffering() != null
                                    && Objects.equals(request.getCourseOffering().getId(), course.getId()))
                            .forEach(courseRegistrationRequestRepository::delete);
                    instructorLedCourseRepository.delete(course);
                });

        for (User user : masterUsers) {
            teacherPerformanceEvaluationRepository.findByTeacherIdOrderByPeriodEndDescIdDesc(user.getId())
                    .forEach(teacherPerformanceEvaluationRepository::delete);
        }

        discountCodeRepository.findAll().stream()
                .filter(code -> code.getCode() != null && code.getCode().startsWith("DEMO_"))
                .forEach(discountCodeRepository::delete);

        // After practice attempts on demo classes are gone
        cleanupMasterExercises();

        userRepository.deleteAll(masterUsers);
        log.info("[MasterDemo] Cleanup finished ({} master users removed)", masterUsers.size());
    }

    private void cleanupMasterExercises() {
        contentBankItemRepository.findByBankTypeOrderByUpdatedAtDescIdDesc(ContentBankType.EXERCISE).stream()
                .filter(item -> item.getCode() != null && item.getCode().startsWith("demo-ex-"))
                .forEach(contentBankItemRepository::delete);
    }

    private void cleanupDiscussionsOnDemoCourses() {
        List<CourseDiscussionPost> demoPosts = discussionPostRepository.findAll().stream()
                .filter(post -> post.getCourse() != null
                        && post.getCourse().getSlug() != null
                        && post.getCourse().getSlug().startsWith(MasterDemoMarkers.COURSE_SLUG_PREFIX)
                        && !MasterDemoMarkers.isProtectedCourseSlug(post.getCourse().getSlug()))
                .toList();
        for (CourseDiscussionPost post : demoPosts) {
            discussionReportRepository.findAll().stream()
                    .filter(report -> report.getPost() != null && report.getPost().getId().equals(post.getId()))
                    .forEach(discussionReportRepository::delete);
            discussionReactionRepository.findByPost(post).forEach(discussionReactionRepository::delete);
        }
        demoPosts.stream()
                .filter(post -> post.getPostType() == CourseDiscussionPostType.REPLY)
                .forEach(discussionPostRepository::delete);
        demoPosts.stream()
                .filter(post -> post.getPostType() == CourseDiscussionPostType.THREAD)
                .forEach(discussionPostRepository::delete);
    }

    private void cleanupMasterFlashcards() {
        contentBankItemRepository.findByBankTypeOrderByUpdatedAtDescIdDesc(ContentBankType.FLASHCARD).stream()
                .filter(item -> item.getCode() != null && item.getCode().startsWith("demo-fc-"))
                .forEach(contentBankItemRepository::delete);
    }

    private void cleanupCenterLibraryItems() {
        centerMaterialLibraryItemRepository.findAllByOrderByUpdatedAtDescIdDesc().stream()
                .filter(item -> item.getFileUrl() != null && item.getFileUrl().startsWith(MASTER_REF_PREFIX))
                .forEach(centerMaterialLibraryItemRepository::delete);
    }

    private void cleanupAdminBroadcasts(List<User> masterUsers) {
        List<Long> masterUserIds = masterUsers.stream().map(User::getId).toList();
        adminBroadcastRepository.findAll().stream()
                .filter(broadcast -> broadcast.getCreatedBy() != null
                        && masterUserIds.contains(broadcast.getCreatedBy().getId()))
                .forEach(adminBroadcastRepository::delete);
        adminBroadcastRepository.findAll().stream()
                .filter(broadcast -> broadcast.getActionPath() != null
                        && broadcast.getActionPath().startsWith(MASTER_REF_PREFIX))
                .forEach(adminBroadcastRepository::delete);
    }
}
