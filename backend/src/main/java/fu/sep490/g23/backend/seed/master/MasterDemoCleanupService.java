package fu.sep490.g23.backend.seed.master;

import fu.sep490.g23.backend.entity.User;
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
import fu.sep490.g23.backend.repository.course.InstructorLedCourseRepository;
import fu.sep490.g23.backend.repository.course.LearnerLessonNoteRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseRepository;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Deletes ONLY MASTER-marked demo rows. Never touches preserved Gmail accounts,
 * protected E2/Vocabulary course content, or Flyway history.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MasterDemoCleanupService {

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
    private final InstructorLedCourseRepository instructorLedCourseRepository;
    private final PaymentOrderRepository paymentOrderRepository;
    private final DiscountCodeRepository discountCodeRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final AppNotificationRepository appNotificationRepository;
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
    private final JdbcTemplate jdbcTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void cleanupMasterOnly() {
        log.info("[MasterDemo] Cleanup MASTER rows only");

        List<User> masterUsers = userRepository.findAll().stream()
                .filter(user -> MasterDemoMarkers.isCleanupOwnedEmail(user.getEmail()))
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
                                && MasterDemoMarkers.isOwnedMasterRef(credential.getDocumentUrl()))
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
                .filter(section -> MasterDemoMarkers.isOwnedClassCode(section.getCode()))
                .forEach(section -> deleteOwnedClassSectionCascade(section.getId()));

        roomRepository.findAll().stream()
                .filter(room -> MasterDemoMarkers.isOwnedRoom(room.getName()))
                .forEach(room -> {
                    Long roomId = room.getId();
                    jdbcTemplate.update(
                            "UPDATE classroom_proposals SET room_id = NULL WHERE room_id = ?",
                            roomId
                    );
                    jdbcTemplate.update(
                            "UPDATE classroom_proposal_schedule_items SET room_id = NULL WHERE room_id = ?",
                            roomId
                    );
                    jdbcTemplate.update(
                            "UPDATE class_schedules SET room_id = NULL WHERE room_id = ?",
                            roomId
                    );
                    jdbcTemplate.update(
                            "UPDATE class_sections SET room_id = NULL WHERE room_id = ?",
                            roomId
                    );
                    roomRepository.delete(room);
                });

        onlineCourseRepository.findAll().stream()
                .filter(course -> MasterDemoMarkers.isOwnedCourseSlug(course.getSlug()))
                .forEach(course -> deleteOwnedOnlineCourseCascade(course.getId()));

        instructorLedCourseRepository.findAll().stream()
                .filter(course -> MasterDemoMarkers.isOwnedIlcCode(course.getCode()))
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
                .filter(code -> MasterDemoMarkers.isOwnedDiscountCode(code.getCode()))
                .forEach(discountCodeRepository::delete);

        // After practice attempts on demo classes are gone
        cleanupMasterExercises();

        // Discard stale managed ClassSection/User graphs from findAll()+JDBC deletes
        // WITHOUT flushing (flush would hit TransientPropertyValueException).
        entityManager.clear();
        detachMasterUserReferences(masterUsers);
        entityManager.clear();

        // Re-load managed refs for deleteAll after clear
        List<User> usersToDelete = userRepository.findAll().stream()
                .filter(user -> MasterDemoMarkers.isCleanupOwnedEmail(user.getEmail()))
                .filter(user -> !MasterDemoMarkers.isPreservedEmail(user.getEmail()))
                .toList();
        userRepository.deleteAll(usersToDelete);
        entityManager.flush();
        entityManager.clear();
        log.info("[MasterDemo] Cleanup finished ({} master users removed)", usersToDelete.size());
    }

    /**
     * Null or delete remaining rows that still reference cleanup-owned users so
     * {@code userRepository.deleteAll} cannot hit FK / TransientPropertyValueException.
     */
    private void detachMasterUserReferences(List<User> masterUsers) {
        if (masterUsers.isEmpty()) {
            return;
        }
        String idList = masterUsers.stream().map(u -> String.valueOf(u.getId())).collect(Collectors.joining(","));

        jdbcTemplate.update("UPDATE class_sections SET primary_teacher_id = NULL WHERE primary_teacher_id IN (" + idList + ")");
        jdbcTemplate.update("UPDATE class_sections SET google_meet_owner_id = NULL WHERE google_meet_owner_id IN (" + idList + ")");
        jdbcTemplate.update("UPDATE class_schedules SET teacher_id = NULL WHERE teacher_id IN (" + idList + ")");
        jdbcTemplate.update("UPDATE classroom_proposal_schedule_items SET teacher_id = NULL WHERE teacher_id IN (" + idList + ")");
        jdbcTemplate.update("UPDATE classroom_proposals SET primary_teacher_id = NULL WHERE primary_teacher_id IN (" + idList + ")");
        jdbcTemplate.update("UPDATE classroom_proposals SET created_by_id = NULL WHERE created_by_id IN (" + idList + ")");
        jdbcTemplate.update("UPDATE classroom_proposals SET reviewed_by_id = NULL WHERE reviewed_by_id IN (" + idList + ")");
        jdbcTemplate.update("UPDATE classroom_proposals SET submitted_by_id = NULL WHERE submitted_by_id IN (" + idList + ")");
        jdbcTemplate.update("UPDATE class_enrollments SET assigned_by_id = NULL WHERE assigned_by_id IN (" + idList + ")");
        jdbcTemplate.update("UPDATE class_enrollments SET gradebook_updated_by_id = NULL WHERE gradebook_updated_by_id IN (" + idList + ")");
        jdbcTemplate.update("UPDATE class_enrollments SET tuition_recorded_by_id = NULL WHERE tuition_recorded_by_id IN (" + idList + ")");
        jdbcTemplate.update("UPDATE class_resources SET reviewed_by_id = NULL WHERE reviewed_by_id IN (" + idList + ")");
        jdbcTemplate.update("UPDATE class_resources SET uploaded_by_id = NULL WHERE uploaded_by_id IN (" + idList + ")");
        jdbcTemplate.update("UPDATE classroom_announcements SET created_by_id = NULL WHERE created_by_id IN (" + idList + ")");
        jdbcTemplate.update("UPDATE classroom_attendance_records SET marked_by_id = NULL WHERE marked_by_id IN (" + idList + ")");
        jdbcTemplate.update("UPDATE classroom_attendance_records SET dispute_reviewed_by_id = NULL WHERE dispute_reviewed_by_id IN (" + idList + ")");
        jdbcTemplate.update("UPDATE classroom_change_requests SET reviewer_id = NULL WHERE reviewer_id IN (" + idList + ")");
        jdbcTemplate.update("UPDATE classroom_homework SET created_by_id = NULL WHERE created_by_id IN (" + idList + ")");
        jdbcTemplate.update("UPDATE classroom_homework_submissions SET graded_by_id = NULL WHERE graded_by_id IN (" + idList + ")");
        jdbcTemplate.update("UPDATE course_registration_requests SET reviewed_by_id = NULL WHERE reviewed_by_id IN (" + idList + ")");
        jdbcTemplate.update("UPDATE course_discussion_reports SET reviewed_by_id = NULL WHERE reviewed_by_id IN (" + idList + ")");
        jdbcTemplate.update("UPDATE discount_codes SET created_by_id = NULL WHERE created_by_id IN (" + idList + ")");
        jdbcTemplate.update("UPDATE instructor_led_courses SET created_by_id = NULL WHERE created_by_id IN (" + idList + ")");
        jdbcTemplate.update("UPDATE instructor_led_courses SET reviewed_by_id = NULL WHERE reviewed_by_id IN (" + idList + ")");
        jdbcTemplate.update("UPDATE online_courses SET created_by_id = NULL WHERE created_by_id IN (" + idList + ")");
        jdbcTemplate.update("UPDATE online_course_versions SET created_by_id = NULL WHERE created_by_id IN (" + idList + ")");
        jdbcTemplate.update("UPDATE online_course_versions SET published_by_id = NULL WHERE published_by_id IN (" + idList + ")");
        jdbcTemplate.update("UPDATE payment_orders SET refunded_by_id = NULL WHERE refunded_by_id IN (" + idList + ")");
        jdbcTemplate.update("UPDATE placement_test_attempts SET reviewer_id = NULL WHERE reviewer_id IN (" + idList + ")");
        jdbcTemplate.update("UPDATE support_tickets SET assignee_id = NULL WHERE assignee_id IN (" + idList + ")");
        jdbcTemplate.update("UPDATE support_tickets SET resolved_by_id = NULL WHERE resolved_by_id IN (" + idList + ")");
        jdbcTemplate.update("UPDATE teacher_credentials SET verified_by_id = NULL WHERE verified_by_id IN (" + idList + ")");
        jdbcTemplate.update("UPDATE tuition_payment_proofs SET reviewed_by_id = NULL WHERE reviewed_by_id IN (" + idList + ")");
        jdbcTemplate.update("UPDATE tuition_payments SET recorded_by_id = NULL WHERE recorded_by_id IN (" + idList + ")");
        jdbcTemplate.update("UPDATE admin_broadcasts SET created_by = NULL WHERE created_by IN (" + idList + ")");
        jdbcTemplate.update("UPDATE content_bank_items SET created_by_id = NULL WHERE created_by_id IN (" + idList + ")");
        jdbcTemplate.update("UPDATE learning_resources SET created_by_id = NULL WHERE created_by_id IN (" + idList + ")");
        jdbcTemplate.update("UPDATE learning_resources SET updated_by_id = NULL WHERE updated_by_id IN (" + idList + ")");

        // Required FK children that may survive if ownership miss — delete, not null.
        jdbcTemplate.update("DELETE FROM user_roles WHERE user_id IN (" + idList + ")");
        jdbcTemplate.update("DELETE FROM auth_tokens WHERE user_id IN (" + idList + ")");
        jdbcTemplate.update("DELETE FROM app_notifications WHERE user_id IN (" + idList + ")");
        jdbcTemplate.update("DELETE FROM course_list_items WHERE student_id IN (" + idList + ")");
        jdbcTemplate.update("DELETE FROM learner_lesson_notes WHERE user_id IN (" + idList + ")");
        jdbcTemplate.update("DELETE FROM saved_vocabularies WHERE user_id IN (" + idList + ")");
        jdbcTemplate.update("DELETE FROM vocabulary_progress WHERE student_id IN (" + idList + ")");
        jdbcTemplate.update(
                "DELETE FROM lesson_progress WHERE online_course_enrollment_id IN ("
                        + "SELECT id FROM online_course_enrollments WHERE student_id IN (" + idList + "))"
        );
        jdbcTemplate.update("DELETE FROM online_course_enrollments WHERE student_id IN (" + idList + ")");
        jdbcTemplate.update("DELETE FROM assessment_submissions WHERE student_id IN (" + idList + ")");
        jdbcTemplate.update("DELETE FROM mock_test_attempts WHERE student_id IN (" + idList + ")");
        // Preserve placement attempts for preserved learner only — delete master-owned students'.
        jdbcTemplate.update("DELETE FROM placement_test_attempts WHERE student_id IN (" + idList + ")");
        jdbcTemplate.update("DELETE FROM classroom_change_requests WHERE requester_id IN (" + idList + ")");
        jdbcTemplate.update("DELETE FROM course_discussion_reactions WHERE user_id IN (" + idList + ")");
        jdbcTemplate.update(
                "DELETE FROM course_discussion_reports WHERE reporter_id IN (" + idList + ")"
        );
        jdbcTemplate.update(
                "DELETE FROM course_discussion_posts WHERE post_type = 'REPLY' AND author_id IN (" + idList + ")"
        );
        jdbcTemplate.update(
                "DELETE FROM course_discussion_posts WHERE author_id IN (" + idList + ")"
        );
        jdbcTemplate.update("DELETE FROM teacher_course_feedback WHERE teacher_id IN (" + idList + ")");
        jdbcTemplate.update("DELETE FROM teacher_credentials WHERE teacher_id IN (" + idList + ")");
        jdbcTemplate.update("DELETE FROM teacher_performance_evaluations WHERE teacher_id IN (" + idList + ") OR evaluator_id IN (" + idList + ")");
        // Never delete preserved Meet connection (alien) — only master teacher rows.
        jdbcTemplate.update(
                "DELETE FROM teacher_google_meet_connections WHERE teacher_id IN (" + idList + ")"
        );
        jdbcTemplate.update("DELETE FROM support_ticket_messages WHERE author_id IN (" + idList + ")");
        jdbcTemplate.update("DELETE FROM support_tickets WHERE requester_id IN (" + idList + ")");
        jdbcTemplate.update(
                "DELETE FROM payment_order_items WHERE payment_order_id IN ("
                        + "SELECT id FROM payment_orders WHERE student_id IN (" + idList + "))"
        );
        jdbcTemplate.update("DELETE FROM payment_orders WHERE student_id IN (" + idList + ")");
        jdbcTemplate.update("DELETE FROM course_registration_requests WHERE learner_id IN (" + idList + ")");
        jdbcTemplate.update(
                "DELETE FROM classroom_homework_submissions WHERE student_id IN (" + idList + ")"
        );
        jdbcTemplate.update(
                "DELETE FROM classroom_attendance_records WHERE student_id IN (" + idList + ")"
        );
        jdbcTemplate.update(
                "DELETE FROM classroom_practice_attempt_history WHERE student_id IN (" + idList + ")"
        );
        jdbcTemplate.update(
                "DELETE FROM payment_order_items WHERE class_enrollment_id IN ("
                        + "SELECT id FROM class_enrollments WHERE student_id IN (" + idList + "))"
        );
        jdbcTemplate.update(
                "DELETE FROM tuition_payment_proofs WHERE enrollment_id IN ("
                        + "SELECT id FROM class_enrollments WHERE student_id IN (" + idList + "))"
        );
        jdbcTemplate.update(
                "DELETE FROM tuition_payments WHERE enrollment_id IN ("
                        + "SELECT id FROM class_enrollments WHERE student_id IN (" + idList + "))"
        );
        jdbcTemplate.update("DELETE FROM class_enrollments WHERE student_id IN (" + idList + ")");
    }

    /**
     * Ordered JDBC cascade for owned online courses — covers all live FKs to
     * online_courses / versions / modules / lessons / assessments discovered on VPS.
     * Does not touch protected (non-owned) courses such as E2 / Vocabulary.
     */
    private void deleteOwnedOnlineCourseCascade(Long courseId) {
        final String versionIds = "SELECT id FROM online_course_versions WHERE online_course_id = ?";
        final String moduleIds = "SELECT m.id FROM online_course_modules m "
                + "JOIN online_course_versions v ON v.id = m.online_course_version_id "
                + "WHERE v.online_course_id = ?";
        final String lessonIds = "SELECT ol.id FROM online_lessons ol "
                + "JOIN online_course_modules m ON m.id = ol.module_id "
                + "JOIN online_course_versions v ON v.id = m.online_course_version_id "
                + "WHERE v.online_course_id = ?";
        final String assessmentIds = "SELECT ca.id FROM course_assessments ca WHERE "
                + "ca.online_course_version_id IN (" + versionIds + ") "
                + "OR ca.module_id IN (" + moduleIds + ") "
                + "OR ca.online_lesson_id IN (" + lessonIds + ")";

        jdbcTemplate.update(
                "DELETE FROM course_discussion_reactions WHERE post_id IN "
                        + "(SELECT id FROM course_discussion_posts WHERE course_id = ?)",
                courseId
        );
        jdbcTemplate.update(
                "DELETE FROM course_discussion_reports WHERE post_id IN "
                        + "(SELECT id FROM course_discussion_posts WHERE course_id = ?)",
                courseId
        );
        jdbcTemplate.update(
                "DELETE FROM course_discussion_posts WHERE post_type = 'REPLY' AND course_id = ?",
                courseId
        );
        jdbcTemplate.update("DELETE FROM course_discussion_posts WHERE course_id = ?", courseId);

        jdbcTemplate.update("DELETE FROM course_list_items WHERE online_course_id = ?", courseId);
        jdbcTemplate.update("DELETE FROM learning_path_courses WHERE online_course_id = ?", courseId);
        jdbcTemplate.update("DELETE FROM payment_order_items WHERE online_course_id = ?", courseId);
        jdbcTemplate.update("DELETE FROM vocabulary_progress WHERE online_course_id = ?", courseId);

        jdbcTemplate.update("DELETE FROM learner_lesson_notes WHERE lesson_id IN (" + lessonIds + ")", courseId);
        jdbcTemplate.update(
                "DELETE FROM online_lesson_flashcard_refs WHERE online_lesson_id IN (" + lessonIds + ")",
                courseId
        );
        jdbcTemplate.update(
                "DELETE FROM lesson_progress WHERE online_lesson_id IN (" + lessonIds + ") "
                        + "OR online_course_enrollment_id IN "
                        + "(SELECT id FROM online_course_enrollments WHERE online_course_id = ?)",
                courseId,
                courseId
        );

        jdbcTemplate.update(
                "DELETE FROM assessment_submissions WHERE assessment_id IN (" + assessmentIds + ")",
                courseId,
                courseId,
                courseId
        );
        jdbcTemplate.update(
                "DELETE FROM course_assessments WHERE online_course_version_id IN (" + versionIds + ") "
                        + "OR module_id IN (" + moduleIds + ") "
                        + "OR online_lesson_id IN (" + lessonIds + ")",
                courseId,
                courseId,
                courseId
        );

        jdbcTemplate.update("DELETE FROM online_course_enrollments WHERE online_course_id = ?", courseId);
        jdbcTemplate.update("DELETE FROM online_lessons WHERE module_id IN (" + moduleIds + ")", courseId);
        jdbcTemplate.update(
                "DELETE FROM online_course_modules WHERE online_course_version_id IN (" + versionIds + ")",
                courseId
        );
        jdbcTemplate.update("DELETE FROM online_course_versions WHERE online_course_id = ?", courseId);
        jdbcTemplate.update("DELETE FROM online_courses WHERE id = ?", courseId);
    }

    /**
     * Ordered JDBC cascade for owned class sections — covers all live FKs to
     * class_sections / class_schedules / class_enrollments discovered on VPS.
     */
    private void deleteOwnedClassSectionCascade(Long sectionId) {
        // Detach optional FKs pointing at this section
        jdbcTemplate.update(
                "UPDATE course_registration_requests SET preferred_class_section_id = NULL WHERE preferred_class_section_id = ?",
                sectionId
        );
        jdbcTemplate.update(
                "UPDATE course_registration_requests SET assigned_class_section_id = NULL WHERE assigned_class_section_id = ?",
                sectionId
        );
        jdbcTemplate.update(
                "UPDATE course_registration_requests SET assigned_classroom_id = NULL WHERE assigned_classroom_id = ?",
                sectionId
        );
        jdbcTemplate.update(
                "UPDATE course_registration_requests SET requested_classroom_id = NULL WHERE requested_classroom_id = ?",
                sectionId
        );
        jdbcTemplate.update(
                "UPDATE classroom_proposals SET approved_classroom_id = NULL WHERE approved_classroom_id = ?",
                sectionId
        );

        jdbcTemplate.update(
                "DELETE FROM payment_order_items WHERE class_enrollment_id IN (SELECT id FROM class_enrollments WHERE class_section_id = ?)",
                sectionId
        );
        jdbcTemplate.update(
                "DELETE FROM teacher_course_feedback WHERE class_section_id = ? OR enrollment_id IN (SELECT id FROM class_enrollments WHERE class_section_id = ?)",
                sectionId,
                sectionId
        );
        jdbcTemplate.update(
                "DELETE FROM tuition_payment_proofs WHERE enrollment_id IN (SELECT id FROM class_enrollments WHERE class_section_id = ?)",
                sectionId
        );
        jdbcTemplate.update(
                "DELETE FROM tuition_payments WHERE enrollment_id IN (SELECT id FROM class_enrollments WHERE class_section_id = ?)",
                sectionId
        );
        jdbcTemplate.update(
                "UPDATE course_registration_requests SET class_enrollment_id = NULL WHERE class_enrollment_id IN (SELECT id FROM class_enrollments WHERE class_section_id = ?)",
                sectionId
        );

        jdbcTemplate.update("DELETE FROM classroom_announcements WHERE class_section_id = ?", sectionId);
        jdbcTemplate.update("DELETE FROM classroom_practice_attempt_history WHERE class_section_id = ?", sectionId);
        jdbcTemplate.update(
                "DELETE FROM classroom_change_requests WHERE class_section_id = ? OR target_session_id IN (SELECT id FROM class_schedules WHERE class_section_id = ?)",
                sectionId,
                sectionId
        );

        jdbcTemplate.update("DELETE FROM class_resources WHERE class_section_id = ? OR session_id IN (SELECT id FROM class_schedules WHERE class_section_id = ?)",
                sectionId, sectionId);
        jdbcTemplate.update(
                "DELETE FROM classroom_homework_submissions WHERE homework_id IN (SELECT id FROM classroom_homework WHERE class_section_id = ?)",
                sectionId
        );
        jdbcTemplate.update("DELETE FROM classroom_homework WHERE class_section_id = ?", sectionId);
        jdbcTemplate.update(
                "DELETE FROM classroom_attendance_records WHERE session_id IN (SELECT id FROM class_schedules WHERE class_section_id = ?)",
                sectionId
        );
        jdbcTemplate.update("DELETE FROM class_schedules WHERE class_section_id = ?", sectionId);
        jdbcTemplate.update("DELETE FROM class_enrollments WHERE class_section_id = ?", sectionId);
        jdbcTemplate.update("DELETE FROM class_sections WHERE id = ?", sectionId);
    }

    private void cleanupMasterExercises() {
        contentBankItemRepository.findByBankTypeOrderByUpdatedAtDescIdDesc(ContentBankType.EXERCISE).stream()
                .filter(item -> MasterDemoMarkers.isOwnedExerciseCode(item.getCode()))
                .forEach(contentBankItemRepository::delete);
    }

    private void cleanupDiscussionsOnDemoCourses() {
        // 1) Owned-course discussions (slug ownership). Delete REPLY before THREAD
        // so parent FK / shape_check stay valid (do NOT null parent_post_id).
        jdbcTemplate.update(
                "DELETE FROM course_discussion_reactions WHERE post_id IN ("
                        + "SELECT p.id FROM course_discussion_posts p "
                        + "JOIN online_courses oc ON oc.id = p.course_id "
                        + "WHERE oc.slug LIKE 'ilc-%' OR oc.slug LIKE 'demo-%' OR oc.slug LIKE 'center-sheet-%')"
        );
        jdbcTemplate.update(
                "DELETE FROM course_discussion_reports WHERE post_id IN ("
                        + "SELECT p.id FROM course_discussion_posts p "
                        + "JOIN online_courses oc ON oc.id = p.course_id "
                        + "WHERE oc.slug LIKE 'ilc-%' OR oc.slug LIKE 'demo-%' OR oc.slug LIKE 'center-sheet-%')"
        );
        jdbcTemplate.update(
                "DELETE FROM course_discussion_posts WHERE post_type = 'REPLY' AND course_id IN ("
                        + "SELECT id FROM online_courses "
                        + "WHERE slug LIKE 'ilc-%' OR slug LIKE 'demo-%' OR slug LIKE 'center-sheet-%')"
        );
        jdbcTemplate.update(
                "DELETE FROM course_discussion_posts WHERE course_id IN ("
                        + "SELECT id FROM online_courses "
                        + "WHERE slug LIKE 'ilc-%' OR slug LIKE 'demo-%' OR slug LIKE 'center-sheet-%')"
        );

        // 2) Discussions authored by cleanup-owned users (incl. leakage on protected E2/Vocab).
        // Preserved learner posts stay.
        List<Long> ownedAuthorIds = userRepository.findAll().stream()
                .filter(user -> MasterDemoMarkers.isCleanupOwnedEmail(user.getEmail()))
                .filter(user -> !MasterDemoMarkers.isPreservedEmail(user.getEmail()))
                .map(User::getId)
                .toList();
        if (ownedAuthorIds.isEmpty()) {
            return;
        }
        String idList = ownedAuthorIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        jdbcTemplate.update(
                "DELETE FROM course_discussion_reactions WHERE post_id IN ("
                        + "SELECT id FROM course_discussion_posts WHERE author_id IN (" + idList + "))"
        );
        jdbcTemplate.update(
                "DELETE FROM course_discussion_reports WHERE post_id IN ("
                        + "SELECT id FROM course_discussion_posts WHERE author_id IN (" + idList + "))"
        );
        // Replies may be authored by preserved users under owned-author threads — detach by deleting
        // all replies under threads authored by owned users, then owned-author posts.
        jdbcTemplate.update(
                "DELETE FROM course_discussion_reactions WHERE post_id IN ("
                        + "SELECT id FROM course_discussion_posts WHERE parent_post_id IN ("
                        + "SELECT id FROM course_discussion_posts WHERE author_id IN (" + idList + ")))"
        );
        jdbcTemplate.update(
                "DELETE FROM course_discussion_reports WHERE post_id IN ("
                        + "SELECT id FROM course_discussion_posts WHERE parent_post_id IN ("
                        + "SELECT id FROM course_discussion_posts WHERE author_id IN (" + idList + ")))"
        );
        jdbcTemplate.update(
                "DELETE FROM course_discussion_posts WHERE parent_post_id IN ("
                        + "SELECT id FROM course_discussion_posts WHERE author_id IN (" + idList + "))"
        );
        jdbcTemplate.update(
                "DELETE FROM course_discussion_posts WHERE post_type = 'REPLY' AND author_id IN (" + idList + ")"
        );
        jdbcTemplate.update(
                "DELETE FROM course_discussion_posts WHERE author_id IN (" + idList + ")"
        );
    }

    private void cleanupMasterFlashcards() {
        contentBankItemRepository.findByBankTypeOrderByUpdatedAtDescIdDesc(ContentBankType.FLASHCARD).stream()
                .filter(item -> MasterDemoMarkers.isOwnedFlashcardCode(item.getCode()))
                .forEach(contentBankItemRepository::delete);
    }

    private void cleanupCenterLibraryItems() {
        centerMaterialLibraryItemRepository.findAllByOrderByUpdatedAtDescIdDesc().stream()
                .filter(item -> MasterDemoMarkers.isOwnedMasterRef(item.getFileUrl()))
                .forEach(centerMaterialLibraryItemRepository::delete);
    }

    private void cleanupAdminBroadcasts(List<User> masterUsers) {
        List<Long> masterUserIds = masterUsers.stream().map(User::getId).toList();
        adminBroadcastRepository.findAll().stream()
                .filter(broadcast -> broadcast.getCreatedBy() != null
                        && masterUserIds.contains(broadcast.getCreatedBy().getId()))
                .forEach(adminBroadcastRepository::delete);
        adminBroadcastRepository.findAll().stream()
                .filter(broadcast -> MasterDemoMarkers.isOwnedMasterRef(broadcast.getActionPath()))
                .forEach(adminBroadcastRepository::delete);
    }
}
