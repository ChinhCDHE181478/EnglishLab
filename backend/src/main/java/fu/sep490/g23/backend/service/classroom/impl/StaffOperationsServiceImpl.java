package fu.sep490.g23.backend.service.classroom.impl;

import fu.sep490.g23.backend.dto.response.classroom.StaffActionItemResponse;
import fu.sep490.g23.backend.dto.response.classroom.StaffClassroomAlertResponse;
import fu.sep490.g23.backend.dto.response.classroom.StaffDashboardResponse;
import fu.sep490.g23.backend.dto.response.classroom.StaffDashboardScoreItemResponse;
import fu.sep490.g23.backend.entity.classroom.ClassroomChangeRequest;
import fu.sep490.g23.backend.entity.classroom.ClassEnrollment;
import fu.sep490.g23.backend.entity.classroom.ClassroomGradebookEntry;
import fu.sep490.g23.backend.entity.classroom.ClassSection;
import fu.sep490.g23.backend.entity.classroom.CourseRegistrationRequest;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomChangeRequestStatus;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomOfferingStatus;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomRegistrationStatus;
import fu.sep490.g23.backend.entity.classroom.enums.EnrollmentRequestStatus;
import fu.sep490.g23.backend.entity.classroom.enums.GradebookEntryStatus;
import fu.sep490.g23.backend.entity.teacher.TeacherPerformanceEvaluation;
import fu.sep490.g23.backend.entity.teacher.enums.TeacherEvaluationStatus;
import fu.sep490.g23.backend.repository.classroom.ClassroomChangeRequestRepository;
import fu.sep490.g23.backend.repository.classroom.ClassEnrollmentRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomGradebookEntryRepository;
import fu.sep490.g23.backend.repository.classroom.ClassSectionRepository;
import fu.sep490.g23.backend.repository.classroom.ClassScheduleRepository;
import fu.sep490.g23.backend.repository.classroom.CourseRegistrationRequestRepository;
import fu.sep490.g23.backend.repository.teacher.TeacherPerformanceEvaluationRepository;
import fu.sep490.g23.backend.service.classroom.ClassroomMapper;
import fu.sep490.g23.backend.service.classroom.ClassroomRegistrationSupport;
import fu.sep490.g23.backend.service.classroom.StaffOperationsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StaffOperationsServiceImpl implements StaffOperationsService {

    private static final Set<ClassroomOfferingStatus> ACTIVE_OFFERING_STATUSES = EnumSet.of(
            ClassroomOfferingStatus.DRAFT,
            ClassroomOfferingStatus.UPCOMING,
            ClassroomOfferingStatus.ACTIVE
    );

    private static final Set<EnrollmentRequestStatus> CONSULTED_STATUSES = EnumSet.of(
            EnrollmentRequestStatus.INVITATION_SENT,
            EnrollmentRequestStatus.TEST_SCHEDULED,
            EnrollmentRequestStatus.AWAITING_PLACEMENT_TEST,
            EnrollmentRequestStatus.PLACEMENT_TEST_COMPLETED,
            EnrollmentRequestStatus.UNDER_STAFF_REVIEW,
            EnrollmentRequestStatus.WAITING_FOR_CLASS,
            EnrollmentRequestStatus.CLASS_PROPOSED,
            EnrollmentRequestStatus.CLASS_ASSIGNED
    );

    private final ClassEnrollmentRepository enrollmentRepository;
    private final ClassroomChangeRequestRepository changeRequestRepository;
    private final ClassSectionRepository offeringRepository;
    private final ClassScheduleRepository sessionRepository;
    private final CourseRegistrationRequestRepository enrollmentRequestRepository;
    private final TeacherPerformanceEvaluationRepository evaluationRepository;
    private final ClassroomGradebookEntryRepository gradebookRepository;
    private final ClassroomMapper mapper;

    @Override
    public StaffDashboardResponse getDashboard() {
        List<ClassEnrollment> pendingRegistrations = enrollmentRepository
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
                .sorted(Comparator.comparing(ClassEnrollment::getEnrolledAt).reversed())
                .limit(12)
                .forEach(enrollment -> actionItems.add(toRegistrationActionItem(enrollment)));
        pendingRequests.stream()
                .limit(8)
                .forEach(request -> actionItems.add(toChangeRequestActionItem(request)));

        List<StaffClassroomAlertResponse> classroomAlerts = buildClassroomAlerts();
        List<CourseRegistrationRequest> enrollmentRequests = enrollmentRequestRepository.findAllByOrderByCreatedAtDesc();
        List<StaffDashboardScoreItemResponse> teacherScores = buildTeacherScores();
        List<StaffDashboardScoreItemResponse> studentScores = buildStudentScores();

        return StaffDashboardResponse.builder()
                .pendingRegistrationCount(pendingRegistrations.size())
                .pendingChangeRequestCount(pendingRequests.size())
                .pendingConfirmationCount(pendingConfirmationCount)
                .pendingTuitionCount(pendingTuitionCount)
                .readyToAssignCount(readyToAssignCount)
                .registeredLearnerCount((int) enrollmentRequests.stream()
                        .filter(item -> item.getStatus() != EnrollmentRequestStatus.CANCELLED)
                        .count())
                .consultedLearnerCount((int) enrollmentRequests.stream()
                        .filter(item -> CONSULTED_STATUSES.contains(item.getStatus()))
                        .count())
                .teacherAverageScore(averageOf(teacherScores.stream().map(StaffDashboardScoreItemResponse::getScore).toList()))
                .studentAverageScore(averageOf(studentScores.stream().map(StaffDashboardScoreItemResponse::getScore).toList()))
                .actionItems(actionItems)
                .classroomAlerts(classroomAlerts)
                .teacherScores(teacherScores.stream().limit(6).toList())
                .studentScores(studentScores.stream().limit(8).toList())
                .build();
    }

    private int countByStatus(List<ClassEnrollment> enrollments, ClassroomRegistrationStatus status) {
        return (int) enrollments.stream()
                .filter(enrollment -> enrollment.getRegistrationStatus() == status)
                .count();
    }

    private StaffActionItemResponse toRegistrationActionItem(ClassEnrollment enrollment) {
        ClassSection offering = enrollment.getClassSection();
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
                .classSectionId(classroomId)
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
                .subtitle(request.getClassSection().getLearningPackage().getTitle()
                        + " · "
                        + request.getRequester().getFullName())
                .changeRequestId(request.getId())
                .classSectionId(request.getClassSection().getId())
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
                    long sessionCount = sessionRepository.countByClassSectionId(offering.getId());
                    long enrolledCount = enrollmentRepository.countByOfferingAndRegistrationStatuses(
                            offering.getId(),
                            ClassroomRegistrationSupport.OCCUPIES_CLASS_SLOT
                    );
                    int capacity = offering.getCapacity() == null ? 0 : offering.getCapacity();
                    LocalDate startDate = offering.getStartDate();

                    if (startDate != null && !startDate.isBefore(today) && !startDate.isAfter(horizon)) {
                        if (capacity > 0 && enrolledCount < Math.max(1, capacity / 2)) {
                            alerts.add(buildAlert(
                                    offering,
                                    "LOW_ENROLLMENT",
                                    "Khai giảng trong "
                                            + java.time.temporal.ChronoUnit.DAYS.between(today, startDate)
                                            + " ngày · mới "
                                            + enrolledCount
                                            + "/"
                                            + capacity
                                            + " chỗ",
                                    (int) enrolledCount,
                                    capacity,
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
                                    capacity,
                                    (int) sessionCount
                            ));
                        }
                    } else if (offering.getStatus() == ClassroomOfferingStatus.DRAFT) {
                        alerts.add(buildAlert(
                                offering,
                                "DRAFT_NOT_PUBLISHED",
                                "Lớp chưa công bố lên lịch khai giảng",
                                (int) enrolledCount,
                                capacity,
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
            ClassSection offering,
            String alertType,
            String alertMessage,
            int enrolledCount,
            int capacity,
            int sessionCount
    ) {
        return StaffClassroomAlertResponse.builder()
                .classSectionId(offering.getId())
                .title(offering.getLearningPackage().getTitle())
                .deliveryMode(offering.getDeliveryMode() == null ? null : offering.getDeliveryMode().name())
                .startDate(offering.getStartDate())
                .enrolledCount(enrolledCount)
                .capacity(capacity)
                .sessionCount(sessionCount)
                .alertType(alertType)
                .alertMessage(alertMessage)
                .href("/staff/classrooms/" + offering.getId())
                .build();
    }

    private List<StaffDashboardScoreItemResponse> buildTeacherScores() {
        Map<Long, List<TeacherPerformanceEvaluation>> byTeacher = evaluationRepository.findAll().stream()
                .filter(item -> item.getStatus() == TeacherEvaluationStatus.PUBLISHED)
                .filter(item -> item.getTeacher() != null)
                .collect(Collectors.groupingBy(item -> item.getTeacher().getId()));
        return byTeacher.values().stream()
                .map(items -> {
                    TeacherPerformanceEvaluation first = items.getFirst();
                    BigDecimal average = averageOf(items.stream()
                            .map(TeacherPerformanceEvaluation::getOverallScore)
                            .toList());
                    return StaffDashboardScoreItemResponse.builder()
                            .name(first.getTeacher().getFullName())
                            .subtitle(first.getTeacher().getEmail())
                            .score(average)
                            .href("/staff/teachers")
                            .build();
                })
                .sorted(Comparator.comparing(StaffDashboardScoreItemResponse::getScore, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private List<StaffDashboardScoreItemResponse> buildStudentScores() {
        return gradebookRepository.findAll().stream()
                .filter(item -> item.getStatus() == GradebookEntryStatus.PUBLISHED)
                .filter(item -> item.getFinalResult() != null)
                .sorted(Comparator.comparing(ClassroomGradebookEntry::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(entry -> StaffDashboardScoreItemResponse.builder()
                        .name(entry.getStudent() == null ? "Học viên" : entry.getStudent().getFullName())
                        .subtitle(entry.getClassSection() == null
                                ? ""
                                : entry.getClassSection().getLearningPackage().getTitle())
                        .score(entry.getFinalResult())
                        .href(entry.getClassSection() == null
                                ? "/staff/classrooms"
                                : "/staff/classrooms/" + entry.getClassSection().getId())
                        .build())
                .toList();
    }

    private BigDecimal averageOf(List<BigDecimal> values) {
        List<BigDecimal> present = values.stream().filter(Objects::nonNull).toList();
        if (present.isEmpty()) {
            return null;
        }
        return present.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(present.size()), 2, RoundingMode.HALF_UP);
    }
}
