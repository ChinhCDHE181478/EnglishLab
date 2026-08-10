package fu.sap490.g23.backend.service.notification.impl;

import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.classroom.*;
import fu.sap490.g23.backend.entity.classroom.enums.*;
import fu.sap490.g23.backend.entity.course.PackageEnrollment;
import fu.sap490.g23.backend.entity.course.enums.EnrollmentStatus;
import fu.sap490.g23.backend.repository.classroom.*;
import fu.sap490.g23.backend.repository.course.PackageEnrollmentRepository;
import fu.sap490.g23.backend.service.mail.LearningReminderMailService;
import fu.sap490.g23.backend.service.notification.*;
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
            ClassroomSessionStatus.OPEN,
            ClassroomSessionStatus.RESCHEDULED,
            ClassroomSessionStatus.MAKEUP
    );
    private static final Set<ClassroomRegistrationStatus> ACTIVE_REGISTRATION_STATUSES = EnumSet.of(
            ClassroomRegistrationStatus.ASSIGNED,
            ClassroomRegistrationStatus.DEPOSIT_PAID,
            ClassroomRegistrationStatus.PARTIALLY_PAID,
            ClassroomRegistrationStatus.FULLY_PAID
    );
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM/yyyy");

    private final ClassroomSessionRepository sessionRepository;
    private final ClassroomEnrollmentRepository enrollmentRepository;
    private final ClassroomHomeworkRepository homeworkRepository;
    private final ClassroomHomeworkSubmissionRepository submissionRepository;
    private final PackageEnrollmentRepository packageEnrollmentRepository;
    private final AppNotificationService notificationService;
    private final NotificationPreferenceService preferenceService;
    private final LearningReminderMailService mailService;

    @Override
    @Scheduled(
            fixedDelayString = "${englishlab.reminders.scan-delay-ms:600000}",
            initialDelayString = "${englishlab.reminders.initial-delay-ms:45000}"
    )
    @Transactional
    public void dispatchDueReminders() {
        LocalDateTime now = LocalDateTime.now();
        runBatchSafely("lịch học", () -> dispatchSessionReminders(now));
        runBatchSafely("hạn bài tập", () -> dispatchHomeworkReminders(now));
        runBatchSafely("gián đoạn học tập", () -> dispatchStudyInactivityAlerts(now));
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
                    String classTitle = session.getClassroomOffering().getLearningPackage().getTitle();
                    String body = classTitle + " bắt đầu lúc " + start.format(DATE_TIME) + ".";
                    String actionPath = "/my-classrooms/" + session.getClassroomOffering().getId();
                    for (ClassroomEnrollment enrollment : enrollmentRepository
                            .findByClassroomOfferingIdAndRegistrationStatusIn(
                                    session.getClassroomOffering().getId(),
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
                                Map.of("sessionId", session.getId(), "classroomId", session.getClassroomOffering().getId())
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
                    String body = "Bài “" + homework.getTitle() + "” hết hạn lúc " + homework.getDeadline().format(DATE_TIME) + ".";
                    for (ClassroomEnrollment enrollment : enrollmentRepository
                            .findByClassroomOfferingIdAndRegistrationStatusIn(
                                    homework.getClassroomOffering().getId(),
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
                                Map.of("homeworkId", homework.getId(), "classroomId", homework.getClassroomOffering().getId())
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
                    String courseTitle = enrollment.getLearningPackage().getTitle();
                    String body = "Bạn đang ở " + enrollment.getProgressPercent()
                            + "% khóa “" + courseTitle + "”. Hãy tiếp tục từ nội dung gần nhất.";
                    boolean created = notificationService.createForUserOnce(
                            learner,
                            "STUDY_INACTIVITY",
                            "Đừng để gián đoạn mục tiêu học tập",
                            body,
                            "/courses/" + enrollment.getLearningPackage().getSlug() + "/home",
                            weeklyKey,
                            Map.of("enrollmentId", enrollment.getId())
                    );
                    if (created && preferenceService.isEmailEnabled(learner)) {
                        mailService.sendReminder(
                                learner,
                                "Tiếp tục mục tiêu học tập - EnglishLab",
                                "Đừng để gián đoạn mục tiêu học tập",
                                body,
                                "/courses/" + enrollment.getLearningPackage().getSlug() + "/home"
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
