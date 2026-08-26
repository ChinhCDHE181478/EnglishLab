package fu.sep490.g23.backend.service.classroom.impl;

import fu.sep490.g23.backend.entity.classroom.ClassSection;
import fu.sep490.g23.backend.entity.classroom.ClassSchedule;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomOfferingStatus;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomSessionStatus;
import fu.sep490.g23.backend.entity.classroom.enums.LarkMeetingStatus;
import fu.sep490.g23.backend.repository.classroom.ClassSectionRepository;
import fu.sep490.g23.backend.repository.classroom.ClassScheduleRepository;
import fu.sep490.g23.backend.service.classroom.ClassroomLifecycleService;
import fu.sep490.g23.backend.service.classroom.VirtualAttendanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ClassroomLifecycleServiceImpl implements ClassroomLifecycleService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final long SESSION_COMPLETION_GRACE_MINUTES = 30;
    private static final Set<ClassroomSessionStatus> ENDABLE_SESSION_STATUSES = EnumSet.of(
            ClassroomSessionStatus.OPEN,
            ClassroomSessionStatus.IN_PROGRESS
    );
    private static final Set<ClassroomOfferingStatus> RUNNING_OFFERING_STATUSES = EnumSet.of(
            ClassroomOfferingStatus.UPCOMING,
            ClassroomOfferingStatus.ACTIVE
    );

    private final ClassScheduleRepository sessionRepository;
    private final ClassSectionRepository offeringRepository;
    private final VirtualAttendanceService virtualAttendanceService;

    @Scheduled(
            fixedDelayString = "${englishlab.classroom.lifecycle-delay-ms:60000}",
            initialDelayString = "${englishlab.classroom.lifecycle-initial-delay-ms:5000}"
    )
    public void reconcileScheduledStatuses() {
        reconcileStatuses(LocalDateTime.now(BUSINESS_ZONE));
    }

    @Override
    public void reconcileStatuses(LocalDateTime now) {
        LocalDateTime cutoff = now.minusMinutes(SESSION_COMPLETION_GRACE_MINUTES);
        List<ClassSchedule> endedSessions = sessionRepository.findSessionsEndedBefore(
                ENDABLE_SESSION_STATUSES,
                cutoff.toLocalDate(),
                cutoff.toLocalTime()
        );

        for (ClassSchedule session : endedSessions) {
            if (session.getDeliveryMode() == ClassroomDeliveryMode.VIRTUAL) {
                try {
                    virtualAttendanceService.finalizeVirtualAttendance(session);
                } catch (RuntimeException exception) {
                    log.warn(
                            "Không thể tự chốt điểm danh cho buổi học {} khi đối soát trạng thái: {}",
                            session.getId(),
                            exception.getMessage()
                    );
                }
                session.setLarkMeetingStatus(LarkMeetingStatus.ENDED);
            }
            session.setStatus(ClassroomSessionStatus.COMPLETED);
            session.setLocked(true);
        }
        sessionRepository.saveAll(endedSessions);

        List<ClassSection> offerings = offeringRepository.findByStatusIn(RUNNING_OFFERING_STATUSES);
        for (ClassSection offering : offerings) {
            List<ClassSchedule> schedules = sessionRepository
                    .findByClassSectionIdOrderBySessionDateAscStartTimeAsc(offering.getId());
            if (shouldCompleteOffering(offering, schedules, now)) {
                offering.setStatus(ClassroomOfferingStatus.COMPLETED);
            } else if (offering.getStatus() == ClassroomOfferingStatus.UPCOMING
                    && offering.getStartDate() != null
                    && !offering.getStartDate().isAfter(now.toLocalDate())) {
                offering.setStatus(ClassroomOfferingStatus.ACTIVE);
            }
        }
        offeringRepository.saveAll(offerings);
    }

    private boolean shouldCompleteOffering(
            ClassSection offering,
            List<ClassSchedule> schedules,
            LocalDateTime now
    ) {
        List<ClassSchedule> nonCancelledSessions = schedules.stream()
                .filter(session -> session.getStatus() != ClassroomSessionStatus.CANCELLED)
                .toList();
        boolean hasSessionTodayOrLater = nonCancelledSessions.stream()
                .anyMatch(session -> !session.getSessionDate().isBefore(now.toLocalDate()));
        if (hasSessionTodayOrLater) {
            return false;
        }
        if (offering.getPlannedEndDate() != null) {
            return offering.getPlannedEndDate().isBefore(now.toLocalDate());
        }
        return !nonCancelledSessions.isEmpty()
                && nonCancelledSessions.stream().allMatch(session ->
                        session.getStatus() == ClassroomSessionStatus.COMPLETED
                                && !session.getEndDateTime().isAfter(now)
                );
    }
}
