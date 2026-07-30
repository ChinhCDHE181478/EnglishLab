package fu.sap490.g23.backend.service.notification.impl;

import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.classroom.ClassroomEnrollment;
import fu.sap490.g23.backend.entity.classroom.ClassroomOffering;
import fu.sap490.g23.backend.entity.classroom.enums.ClassroomRegistrationStatus;
import fu.sap490.g23.backend.repository.classroom.ClassroomEnrollmentRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomOfferingRepository;
import fu.sap490.g23.backend.service.mail.LearningReminderMailService;
import fu.sap490.g23.backend.service.notification.AppNotificationService;
import fu.sap490.g23.backend.service.notification.NotificationPreferenceService;
import fu.sap490.g23.backend.service.notification.TeacherFeedbackReminderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeacherFeedbackReminderServiceImpl implements TeacherFeedbackReminderService {
    private final ClassroomOfferingRepository offeringRepository;
    private final ClassroomEnrollmentRepository enrollmentRepository;
    private final AppNotificationService notificationService;
    private final NotificationPreferenceService preferenceService;
    private final LearningReminderMailService mailService;

    @Value("${englishlab.teacher-feedback.opens-days-before-end:7}")
    private int opensDaysBeforeEnd;
    @Value("${englishlab.teacher-feedback.closes-days-after-end:14}")
    private int closesDaysAfterEnd;
    @Value("${englishlab.teacher-feedback.closing-reminder-days:2}")
    private int closingReminderDays;

    @Override
    @Scheduled(
            fixedDelayString = "${englishlab.teacher-feedback.reminder-scan-delay-ms:21600000}",
            initialDelayString = "${englishlab.teacher-feedback.reminder-initial-delay-ms:90000}"
    )
    @Transactional(readOnly = true)
    public void dispatchTeacherFeedbackReminders() {
        LocalDate today = LocalDate.now();
        offeringRepository.findByEndDateBetween(
                today.minusDays(Math.max(0, closesDaysAfterEnd)),
                today.plusDays(Math.max(0, opensDaysBeforeEnd))
        ).forEach(classroom -> dispatchForClassroom(classroom, today));
    }

    private void dispatchForClassroom(ClassroomOffering classroom, LocalDate today) {
        try {
            LocalDate opensOn = classroom.getEndDate().minusDays(Math.max(0, opensDaysBeforeEnd));
            LocalDate closesOn = classroom.getEndDate().plusDays(Math.max(0, closesDaysAfterEnd));
            if (today.isBefore(opensOn) || today.isAfter(closesOn)) return;
            boolean closingSoon = !today.isBefore(closesOn.minusDays(Math.max(0, closingReminderDays)));
            String title = closingSoon ? "Sắp hết hạn đánh giá giáo viên" : "Đã mở phiếu đánh giá giáo viên";
            String classTitle = classroom.getLearningPackage() == null
                    ? "lớp #" + classroom.getId()
                    : classroom.getLearningPackage().getTitle();
            String body = closingSoon
                    ? "Phiếu đánh giá giáo viên của " + classTitle + " sẽ đóng ngày " + formatDate(closesOn)
                            + ". Bạn có thể gửi mới hoặc chỉnh sửa phản hồi đã gửi."
                    : "Bạn có thể đánh giá giáo viên của " + classTitle + " từ hôm nay đến hết "
                            + formatDate(closesOn) + ". Phản hồi được bảo mật danh tính.";
            String actionPath = "/my-classrooms/" + classroom.getId() + "/teacher-feedback";
            String window = closingSoon ? "CLOSING" : "OPEN";
            for (ClassroomEnrollment enrollment : enrollmentRepository
                    .findByClassroomOfferingIdAndRegistrationStatusIn(
                            classroom.getId(),
                            Set.of(ClassroomRegistrationStatus.ASSIGNED)
                    )) {
                notifyLearner(enrollment.getStudent(), classroom, title, body, actionPath, window);
            }
        } catch (Exception exception) {
            log.warn("Không thể tạo nhắc đánh giá cho lớp #{}: {}", classroom.getId(), exception.getMessage());
        }
    }

    private void notifyLearner(
            User learner,
            ClassroomOffering classroom,
            String title,
            String body,
            String actionPath,
            String window
    ) {
        if (learner == null || !preferenceService.isStudyAlertEnabled(learner)) return;
        boolean created = notificationService.createForUserOnce(
                learner,
                "TEACHER_FEEDBACK",
                title,
                body,
                actionPath,
                "TEACHER_FEEDBACK_" + classroom.getId() + "_" + window,
                Map.of("classroomId", classroom.getId())
        );
        if (created && preferenceService.isEmailEnabled(learner)) {
            mailService.sendReminder(learner, title + " - EnglishLab", title, body, actionPath);
        }
    }

    private String formatDate(LocalDate date) {
        return "%02d/%02d/%04d".formatted(date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }
}
