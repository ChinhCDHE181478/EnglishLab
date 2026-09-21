package fu.sep490.g23.backend.service.schedule_job.impl;

import fu.sep490.g23.backend.entity.classroom.enums.HomeworkSubmissionStatus;
import fu.sep490.g23.backend.entity.classroom.enums.HomeworkStatus;
import fu.sep490.g23.backend.repository.classroom.ClassroomHomeworkRepository;
import fu.sep490.g23.backend.repository.classroom.ClassScheduleRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomHomeworkSubmissionRepository;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomSessionStatus;
import fu.sep490.g23.backend.service.notification.NotificationPreferenceService;
import fu.sep490.g23.backend.service.notification.AppNotificationService;
import fu.sep490.g23.backend.repository.classroom.ClassEnrollmentRepository;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomRegistrationStatus;
import fu.sep490.g23.backend.service.schedule_job.LearningReminderService;
import fu.sep490.g23.backend.service.classroom.ClassroomOfferingService;
import fu.sep490.g23.backend.service.classroom.ClassroomRegistrationSupport;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.classroom.ClassEnrollment;
import fu.sep490.g23.backend.entity.course.enums.EnrollmentStatus;
import fu.sep490.g23.backend.repository.course.OnlineCourseEnrollmentRepository;
import fu.sep490.g23.backend.service.mail.LearningReminderMailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class LearningReminderServiceImpl implements LearningReminderService {

    private static final Set<ClassroomSessionStatus> REMINDABLE_SESSION_STATUSES = EnumSet.of(
            ClassroomSessionStatus.SCHEDULED,
            ClassroomSessionStatus.OPEN
    );
    private static final Set<ClassroomRegistrationStatus> ACTIVE_REGISTRATION_STATUSES = EnumSet.of(
            ClassroomRegistrationStatus.ASSIGNED
    );
    private static final Set<ClassroomRegistrationStatus> TUITION_PENDING_STATUSES = EnumSet.of(
            ClassroomRegistrationStatus.PENDING_TUITION_PAYMENT,
            ClassroomRegistrationStatus.DEPOSIT_PAID,
            ClassroomRegistrationStatus.PARTIALLY_PAID
    );
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM/yyyy");

    private final ClassScheduleRepository sessionRepository;
    private final ClassEnrollmentRepository enrollmentRepository;
    private final ClassroomHomeworkRepository homeworkRepository;
    private final ClassroomHomeworkSubmissionRepository submissionRepository;
    private final OnlineCourseEnrollmentRepository packageEnrollmentRepository;
    private final AppNotificationService notificationService;
    private final NotificationPreferenceService preferenceService;
    private final LearningReminderMailService mailService;
    private final ClassroomOfferingService classroomOfferingService;

    @Override
    @Scheduled(
            fixedDelayString = "${englishlab.reminders.scan-delay-ms:600000}",
            initialDelayString = "${englishlab.reminders.initial-delay-ms:45000}"
    )
    @Transactional
    public void dispatchDueReminders() {
        LocalDateTime now = ClassroomRegistrationSupport.currentBusinessTime();
        runBatchSafely("lịch học", () -> dispatchSessionReminders(now));
        runBatchSafely("hạn bài tập", () -> dispatchHomeworkReminders(now));
        runBatchSafely("gián đoạn học tập", () -> dispatchStudyInactivityAlerts(now));
        runBatchSafely("hạn học phí", () -> dispatchTuitionReminders(now));
    }

    private void dispatchTuitionReminders(LocalDateTime now) {
        enrollmentRepository.findByRegistrationStatusIn(TUITION_PENDING_STATUSES)
                .forEach(enrollment -> runItemSafely("học phí đăng ký #" + enrollment.getId(), () -> {
                    LocalDateTime deadline = ClassroomRegistrationSupport.tuitionPaymentDeadline(enrollment);
                    if (deadline == null || enrollment.tuitionBalance().signum() <= 0) return;

                    User learner = enrollment.getStudent();
                    String classTitle = enrollment.getClassSection().getTitle();
                    String actionPath = "/my-classrooms/" + enrollment.getClassSection().getId() + "?tab=payment";
                    if (now.isAfter(deadline)) {
                        boolean expired = classroomOfferingService.expireOverdueTuitionEnrollment(
                                enrollment.getId(), now);
                        if (expired) {
                            mailService.sendReminder(
                                    learner,
                                    "Đăng ký lớp đã hết hạn thanh toán - EnglishLab",
                                    "Đăng ký lớp đã hết hạn thanh toán",
                                    "Đăng ký lớp “" + classTitle
                                            + "” đã bị từ chối do chưa hoàn tất học phí đúng hạn.",
                                    "/my-classrooms"
                            );
                        }
                        return;
                    }

                    long hours = Math.max(1, Duration.between(now, deadline).toHours());
                    if (hours > 72) return;
                    String window = hours <= 6 ? "6H" : hours <= 24 ? "24H" : "72H";
                    String body = "Học phí còn lại của lớp “" + classTitle + "” cần hoàn tất trước "
                            + deadline.format(DATE_TIME) + ".";
                    boolean created = notificationService.createForUserOnce(
                            learner,
                            "CLASSROOM_TUITION_DEADLINE",
                            "Nhắc hạn thanh toán học phí",
                            body,
                            actionPath,
                            "TUITION_" + enrollment.getId() + "_" + window,
                            Map.of(
                                    "enrollmentId", enrollment.getId(),
                                    "classroomId", enrollment.getClassSection().getId(),
                                    "deadline", deadline.toString()
                            )
                    );
                    if (created) {
                        mailService.sendReminder(
                                learner,
                                "Nhắc hạn thanh toán học phí - EnglishLab",
                                "Nhắc hạn thanh toán học phí",
                                body,
                                actionPath
                        );
                    }
                }));
    }

    private void dispatchSessionReminders(LocalDateTime now) {
        sessionRepository.findByStatusInAndSessionDateBetweenOrderBySessionDateAscStartTimeAsc(
                        REMINDABLE_SESSION_STATUSES,
                        now.toLocalDate(),
                        now.plusHours(24).toLocalDate()
                ).stream()
                .filter(session -> session.getSessionDate() != null && session.getStartTime() != null)
                .filter(session -> {
                    LocalDateTime start = LocalDateTime.of(session.getSessionDate(), session.getStartTime());
                    return start.isAfter(now) && !start.isAfter(now.plusHours(24));
                })
                .forEach(session -> runItemSafely("buổi học #" + session.getId(), () -> {
                    LocalDateTime start = LocalDateTime.of(session.getSessionDate(), session.getStartTime());
                    long minutes = Duration.between(now, start).toMinutes();
                    String window = minutes <= 120 ? "2H" : "24H";
                    String title = minutes <= 120 ? "Buổi học sắp bắt đầu" : "Nhắc lịch học ngày mai";
                    String classTitle = session.getClassSection().getTitle();
                    String body = classTitle + " bắt đầu lúc " + start.format(DATE_TIME) + ".";
                    String actionPath = "/my-classrooms/" + session.getClassSection().getId();
                    for (ClassEnrollment enrollment : enrollmentRepository
                            .findByClassSectionIdAndRegistrationStatusIn(
                                    session.getClassSection().getId(),
                                    ACTIVE_REGISTRATION_STATUSES
                            )) {
                        User learner = enrollment.getStudent();
                        if (!preferenceService.isClassReminderEnabled(learner)) continue;
                        String key = "SESSION_" + session.getId() + "_" + window;
                        boolean created = notificationService.createForUserOnce(
                                learner,
                                "CLASS_REMINDER",
                                title,
                                body,
                                actionPath,
                                key,
                                Map.of("sessionId", session.getId(), "classroomId", session.getClassSection().getId())
                        );
                        if (created && preferenceService.isEmailEnabled(learner)) {
                            mailService.sendReminder(learner, title + " - EnglishLab", title, body, actionPath);
                        }
                    }
                }));
    }

    private void dispatchHomeworkReminders(LocalDateTime now) {
        homeworkRepository.findByStatusAndDeadlineBetween(HomeworkStatus.OPEN, now, now.plusHours(24))
                .forEach(homework -> runItemSafely("bài tập #" + homework.getId(), () -> {
                    long minutes = Duration.between(now, homework.getDeadline()).toMinutes();
                    String window = minutes <= 120 ? "2H" : "24H";
                    String title = minutes <= 120 ? "Bài tập sắp hết hạn" : "Nhắc hạn nộp bài tập";
                    String body = "B\u00e0i \u201c" + homework.getTitle() + "\u201d h\u1ebft h\u1ea1n l\u00fac " + homework.getDeadline().format(DATE_TIME) + ".";
                    for (ClassEnrollment enrollment : enrollmentRepository
                            .findByClassSectionIdAndRegistrationStatusIn(
                                    homework.getClassSection().getId(),
                                    ACTIVE_REGISTRATION_STATUSES
                            )) {
                        User learner = enrollment.getStudent();
                        boolean alreadySubmitted = submissionRepository
                                .findByHomeworkIdAndStudentId(homework.getId(), learner.getId())
                                .map(submission -> submission.getStatus() != HomeworkSubmissionStatus.DRAFT)
                                .orElse(false);
                        if (alreadySubmitted || !preferenceService.isStudyAlertEnabled(learner)) continue;
                        String key = "HOMEWORK_" + homework.getId() + "_" + window;
                        boolean created = notificationService.createForUserOnce(
                                learner,
                                "HOMEWORK_DEADLINE",
                                title,
                                body,
                                "/my-homework",
                                key,
                                Map.of("homeworkId", homework.getId(), "classroomId", homework.getClassSection().getId())
                        );
                        if (created && preferenceService.isEmailEnabled(learner)) {
                            mailService.sendReminder(learner, title + " - EnglishLab", title, body, "/my-homework");
                        }
                    }
                }));
    }

    private void dispatchStudyInactivityAlerts(LocalDateTime now) {
        packageEnrollmentRepository.findByStatusAndProgressPercentBetweenAndUpdatedAtBefore(
                        EnrollmentStatus.ACTIVE,
                        1,
                        99,
                        now.minusDays(7)
                )
                .forEach(enrollment -> runItemSafely("ghi danh khóa #" + enrollment.getId(), () -> {
                    User learner = enrollment.getStudent();
                    if (!preferenceService.isStudyAlertEnabled(learner)) return;
                    String weeklyKey = "INACTIVE_ENROLLMENT_" + enrollment.getId() + "_"
                            + now.toLocalDate().with(DayOfWeek.MONDAY);
                    String courseTitle = enrollment.getOnlineCourse().getTitle();
                    String body = "Bạn đang ở " + enrollment.getProgressPercent()
                            + "% kh\u00f3a \u201c" + courseTitle + "\u201d. H\u00e3y ti\u1ebfp t\u1ee5c t\u1eeb n\u1ed9i dung g\u1ea7n nh\u1ea5t.";
                    boolean created = notificationService.createForUserOnce(
                            learner,
                            "STUDY_INACTIVITY",
                            "Đừng để gián đoạn mục tiêu học tập",
                            body,
                            "/courses/" + enrollment.getOnlineCourse().getSlug() + "/home",
                            weeklyKey,
                            Map.of("enrollmentId", enrollment.getId())
                    );
                    if (created && preferenceService.isEmailEnabled(learner)) {
                        mailService.sendReminder(
                                learner,
                                "Tiếp tục mục tiêu học tập - EnglishLab",
                                "Đừng để gián đoạn mục tiêu học tập",
                                body,
                                "/courses/" + enrollment.getOnlineCourse().getSlug() + "/home"
                        );
                    }
                }));
    }

    private void runBatchSafely(String category, Runnable action) {
        try {
            action.run();
        } catch (Exception exception) {
            log.error("Không thể hoàn tất lượt quét nhắc {}: {}", category, exception.getMessage(), exception);
        }
    }

    private void runItemSafely(String item, Runnable action) {
        try {
            action.run();
        } catch (Exception exception) {
            log.warn("Bỏ qua {} do không thể tạo nhắc nhở: {}", item, exception.getMessage());
        }
    }
}
