package fu.sap490.g23.backend.service.classroom.impl;

import fu.sap490.g23.backend.dto.response.classroom.StaffActionItemResponse;
import fu.sap490.g23.backend.dto.response.classroom.StaffClassroomAlertResponse;
import fu.sap490.g23.backend.dto.response.classroom.StaffDashboardResponse;
import fu.sap490.g23.backend.entity.classroom.ClassroomChangeRequest;
import fu.sap490.g23.backend.entity.classroom.ClassroomEnrollment;
import fu.sap490.g23.backend.entity.classroom.ClassroomOffering;
import fu.sap490.g23.backend.entity.classroom.enums.ClassroomChangeRequestStatus;
import fu.sap490.g23.backend.entity.classroom.enums.ClassroomOfferingStatus;
import fu.sap490.g23.backend.entity.classroom.enums.ClassroomRegistrationStatus;
import fu.sap490.g23.backend.repository.classroom.ClassroomChangeRequestRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomEnrollmentRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomOfferingRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomSessionRepository;
import fu.sap490.g23.backend.service.classroom.ClassroomMapper;
import fu.sap490.g23.backend.service.classroom.ClassroomRegistrationSupport;
import fu.sap490.g23.backend.service.classroom.StaffOperationsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StaffOperationsServiceImpl implements StaffOperationsService {

    private static final Set<ClassroomOfferingStatus> ACTIVE_OFFERING_STATUSES = EnumSet.of(
            ClassroomOfferingStatus.DRAFT,
            ClassroomOfferingStatus.UPCOMING,
            ClassroomOfferingStatus.ACTIVE
    );

    private final ClassroomEnrollmentRepository enrollmentRepository;
    private final ClassroomChangeRequestRepository changeRequestRepository;
    private final ClassroomOfferingRepository offeringRepository;
    private final ClassroomSessionRepository sessionRepository;
    private final ClassroomMapper mapper;

    @Override
    public StaffDashboardResponse getDashboard() {
        List<ClassroomEnrollment> pendingRegistrations = enrollmentRepository
                .findByRegistrationStatusIn(ClassroomRegistrationSupport.NEEDS_ACTION_STATUSES);
        List<ClassroomChangeRequest> pendingRequests = changeRequestRepository
                .findByStatusOrderByCreatedAtDesc(ClassroomChangeRequestStatus.PENDING);

        int pendingConfirmationCount = countByStatus(pendingRegistrations, ClassroomRegistrationStatus.PENDING_CONFIRMATION);
        int pendingTuitionCount = countByStatus(pendingRegistrations, ClassroomRegistrationStatus.PENDING_TUITION_PAYMENT)
                + countByStatus(pendingRegistrations, ClassroomRegistrationStatus.DEPOSIT_PAID)
                + countByStatus(pendingRegistrations, ClassroomRegistrationStatus.PARTIALLY_PAID);
        int readyToAssignCount = countByStatus(pendingRegistrations, ClassroomRegistrationStatus.FULLY_PAID);

        List<StaffActionItemResponse> actionItems = new ArrayList<>();
        pendingRegistrations.stream()
                .sorted(Comparator.comparing(ClassroomEnrollment::getEnrolledAt).reversed())
                .limit(12)
                .forEach(enrollment -> actionItems.add(toRegistrationActionItem(enrollment)));
        pendingRequests.stream()
                .limit(8)
                .forEach(request -> actionItems.add(toChangeRequestActionItem(request)));

        List<StaffClassroomAlertResponse> classroomAlerts = buildClassroomAlerts();

        return StaffDashboardResponse.builder()
                .pendingRegistrationCount(pendingRegistrations.size())
                .pendingChangeRequestCount(pendingRequests.size())
                .pendingConfirmationCount(pendingConfirmationCount)
                .pendingTuitionCount(pendingTuitionCount)
                .readyToAssignCount(readyToAssignCount)
                .actionItems(actionItems)
                .classroomAlerts(classroomAlerts)
                .build();
    }

    private int countByStatus(List<ClassroomEnrollment> enrollments, ClassroomRegistrationStatus status) {
        return (int) enrollments.stream()
                .filter(enrollment -> enrollment.getRegistrationStatus() == status)
                .count();
    }

    private StaffActionItemResponse toRegistrationActionItem(ClassroomEnrollment enrollment) {
        ClassroomOffering offering = enrollment.getClassroomOffering();
        ClassroomRegistrationStatus status = enrollment.getRegistrationStatus();
        String kind = switch (status) {
            case PENDING_CONFIRMATION, PENDING_TUITION_PAYMENT, DEPOSIT_PAID, PARTIALLY_PAID -> "RECORD_TUITION";
            case FULLY_PAID -> "ASSIGN_CLASS";
            case WAITLIST -> "INVITE_PAYMENT";
            default -> "REGISTRATION";
        };
        String title = enrollment.getStudent().getFullName() == null || enrollment.getStudent().getFullName().isBlank()
                ? enrollment.getStudent().getEmail()
                : enrollment.getStudent().getFullName();
        String subtitle = offering.getLearningPackage().getTitle()
                + " · "
                + ClassroomRegistrationSupport.registrationStatusLabel(status);
        Long classroomId = offering.getId();
        return StaffActionItemResponse.builder()
                .kind(kind)
                .title(title)
                .subtitle(subtitle)
                .enrollmentId(enrollment.getId())
                .classroomOfferingId(classroomId)
                .registrationStatus(status)
                .registrationStatusLabel(ClassroomRegistrationSupport.registrationStatusLabel(status))
                .createdAt(enrollment.getEnrolledAt())
                .href("/staff/enrollment-requests?enrollmentId=" + enrollment.getId())
                .build();
    }

    private StaffActionItemResponse toChangeRequestActionItem(ClassroomChangeRequest request) {
        return StaffActionItemResponse.builder()
                .kind("APPROVE_CHANGE_REQUEST")
                .title(mapper.changeRequestTypeLabel(request.getRequestType()))
                .subtitle(request.getClassroomOffering().getLearningPackage().getTitle()
                        + " · "
                        + request.getRequester().getFullName())
                .changeRequestId(request.getId())
                .classroomOfferingId(request.getClassroomOffering().getId())
                .createdAt(request.getCreatedAt())
                .href("/staff/requests?requestId=" + request.getId())
                .build();
    }

    private List<StaffClassroomAlertResponse> buildClassroomAlerts() {
        LocalDate today = LocalDate.now();
        LocalDate horizon = today.plusDays(14);
        List<StaffClassroomAlertResponse> alerts = new ArrayList<>();

        offeringRepository.findAll().stream()
                .filter(offering -> !offering.getLearningPackage().isDeleted())
                .filter(offering -> ACTIVE_OFFERING_STATUSES.contains(offering.getStatus()))
                .forEach(offering -> {
                    long sessionCount = sessionRepository.countByClassroomOfferingId(offering.getId());
                    long enrolledCount = enrollmentRepository.countByOfferingAndRegistrationStatuses(
                            offering.getId(),
                            ClassroomRegistrationSupport.OCCUPIES_CLASS_SLOT
                    );
                    int maxCapacity = offering.getMaxCapacity() == null ? 0 : offering.getMaxCapacity();
                    LocalDate startDate = offering.getStartDate();

                    if (startDate != null && !startDate.isBefore(today) && !startDate.isAfter(horizon)) {
                        if (maxCapacity > 0 && enrolledCount < Math.max(1, maxCapacity / 2)) {
                            alerts.add(buildAlert(
                                    offering,
                                    "LOW_ENROLLMENT",
                                    "Khai giảng trong "
                                            + java.time.temporal.ChronoUnit.DAYS.between(today, startDate)
                                            + " ngày · mới "
                                            + enrolledCount
                                            + "/"
                                            + maxCapacity
                                            + " chỗ",
                                    (int) enrolledCount,
                                    maxCapacity,
                                    (int) sessionCount
                            ));
                        } else if (sessionCount == 0) {
                            alerts.add(buildAlert(
                                    offering,
                                    "MISSING_SESSIONS",
                                    "Khai giảng trong "
                                            + java.time.temporal.ChronoUnit.DAYS.between(today, startDate)
                                            + " ngày · chưa có buổi học",
                                    (int) enrolledCount,
                                    maxCapacity,
                                    (int) sessionCount
                            ));
                        }
                    } else if (offering.getStatus() == ClassroomOfferingStatus.DRAFT) {
                        alerts.add(buildAlert(
                                offering,
                                "DRAFT_NOT_PUBLISHED",
                                "Lớp chưa công bố lên lịch khai giảng",
                                (int) enrolledCount,
                                maxCapacity,
                                (int) sessionCount
                        ));
                    }
                });

        return alerts.stream()
                .sorted(Comparator.comparing(StaffClassroomAlertResponse::getStartDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(8)
                .toList();
    }

    private StaffClassroomAlertResponse buildAlert(
            ClassroomOffering offering,
            String alertType,
            String alertMessage,
            int enrolledCount,
            int maxCapacity,
            int sessionCount
    ) {
        return StaffClassroomAlertResponse.builder()
                .classroomOfferingId(offering.getId())
                .title(offering.getLearningPackage().getTitle())
                .deliveryMode(offering.getDeliveryMode() == null ? null : offering.getDeliveryMode().name())
                .startDate(offering.getStartDate())
                .enrolledCount(enrolledCount)
                .maxCapacity(maxCapacity)
                .sessionCount(sessionCount)
                .alertType(alertType)
                .alertMessage(alertMessage)
                .href("/staff/classrooms/" + offering.getId())
                .build();
    }
}
