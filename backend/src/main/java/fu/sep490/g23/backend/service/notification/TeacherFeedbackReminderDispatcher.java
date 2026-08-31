package fu.sep490.g23.backend.service.notification;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.classroom.ClassEnrollment;
import fu.sep490.g23.backend.entity.classroom.ClassSection;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomRegistrationStatus;
import fu.sep490.g23.backend.repository.classroom.ClassEnrollmentRepository;
import fu.sep490.g23.backend.repository.classroom.ClassSectionRepository;
import fu.sep490.g23.backend.service.mail.LearningReminderMailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class TeacherFeedbackReminderDispatcher {

    private final ClassSectionRepository offeringRepository;
    private final ClassEnrollmentRepository enrollmentRepository;
    private final AppNotificationService notificationService;
    private final NotificationPreferenceService preferenceService;
    private final LearningReminderMailService mailService;

    @Transactional(readOnly = true)
    public void dispatchForClassroom(
            Long classroomId,
            LocalDate today,
            int opensDaysBeforeEnd,
            int closesDaysAfterEnd,
            int closingReminderDays
    ) {
        ClassSection classroom = offeringRepository.findById(classroomId).orElse(null);
        if (classroom == null || classroom.getPlannedEndDate() == null) {
            return;
        }

        LocalDate opensOn = classroom.getPlannedEndDate().minusDays(Math.max(0, opensDaysBeforeEnd));
        LocalDate closesOn = classroom.getPlannedEndDate().plusDays(Math.max(0, closesDaysAfterEnd));
        if (today.isBefore(opensOn) || today.isAfter(closesOn)) {
            return;
        }

        boolean closingSoon = !today.isBefore(closesOn.minusDays(Math.max(0, closingReminderDays)));
        String title = closingSoon ? "Sắp hết hạn đánh giá giáo viên" : "Đã mở phiếu đánh giá giáo viên";
        String classTitle = classroom.getInstructorLedCourse() == null
                ? "lớp #" + classroom.getId()
                : classroom.getInstructorLedCourse().getTitle();
        String body = closingSoon
                ? "Phiếu đánh giá giáo viên của " + classTitle + " sẽ đóng ngày " + formatDate(closesOn)
                        + ". Bạn có thể gửi mới hoặc chỉnh sửa phản hồi đã gửi."
                : "Bạn có thể đánh giá giáo viên của " + classTitle + " từ hôm nay đến hết "
                        + formatDate(closesOn) + ". Phản hồi được bảo mật danh tính.";
        String actionPath = "/my-classrooms/" + classroom.getId() + "/teacher-feedback";
        String window = closingSoon ? "CLOSING" : "OPEN";

        for (ClassEnrollment enrollment : enrollmentRepository
                .findByClassSectionIdAndRegistrationStatusIn(
                        classroom.getId(),
                        Set.of(ClassroomRegistrationStatus.ASSIGNED)
                )) {
            notifyLearner(enrollment.getStudent(), classroom, title, body, actionPath, window);
        }
    }

    private void notifyLearner(
            User learner,
            ClassSection classroom,
            String title,
            String body,
            String actionPath,
            String window
    ) {
        if (learner == null || !preferenceService.isStudyAlertEnabled(learner)) {
            return;
        }

        boolean created;
        try {
            created = notificationService.createForUserOnce(
                    learner,
                    "TEACHER_FEEDBACK",
                    title,
                    body,
                    actionPath,
                    "TEACHER_FEEDBACK_" + classroom.getId() + "_" + window,
                    Map.of("classroomId", classroom.getId())
            );
        } catch (RuntimeException exception) {
            log.warn(
                    "Không thể tạo thông báo đánh giá giáo viên cho học viên #{} của lớp #{}: {}",
                    learner.getId(),
                    classroom.getId(),
                    exception.getMessage()
            );
            return;
        }

        if (created && preferenceService.isEmailEnabled(learner)) {
            try {
                mailService.sendReminder(learner, title + " - EnglishLab", title, body, actionPath);
            } catch (RuntimeException exception) {
                log.warn(
                        "Không thể gửi email nhắc đánh giá giáo viên cho học viên #{} của lớp #{}: {}",
                        learner.getId(),
                        classroom.getId(),
                        exception.getMessage()
                );
            }
        }
    }

    private String formatDate(LocalDate date) {
        return "%02d/%02d/%04d".formatted(date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }
}
