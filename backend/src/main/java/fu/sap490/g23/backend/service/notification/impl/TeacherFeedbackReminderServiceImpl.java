package fu.sap490.g23.backend.service.notification.impl;

import fu.sap490.g23.backend.repository.classroom.ClassroomOfferingRepository;
import fu.sap490.g23.backend.service.notification.TeacherFeedbackReminderDispatcher;
import fu.sap490.g23.backend.service.notification.TeacherFeedbackReminderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeacherFeedbackReminderServiceImpl implements TeacherFeedbackReminderService {
    private final ClassroomOfferingRepository offeringRepository;
    private final TeacherFeedbackReminderDispatcher reminderDispatcher;

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
    public void dispatchTeacherFeedbackReminders() {
        LocalDate today = LocalDate.now();
        offeringRepository.findByEndDateBetween(
                today.minusDays(Math.max(0, closesDaysAfterEnd)),
                today.plusDays(Math.max(0, opensDaysBeforeEnd))
        ).forEach(classroom -> {
            try {
                reminderDispatcher.dispatchForClassroom(
                        classroom.getId(),
                        today,
                        opensDaysBeforeEnd,
                        closesDaysAfterEnd,
                        closingReminderDays
                );
            } catch (RuntimeException exception) {
                log.warn("Không thể tạo nhắc đánh giá cho lớp #{}.", classroom.getId(), exception);
            }
        });
    }
}
