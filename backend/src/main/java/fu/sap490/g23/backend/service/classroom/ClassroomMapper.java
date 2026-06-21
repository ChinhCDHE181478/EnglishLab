package fu.sap490.g23.backend.service.classroom;

import fu.sap490.g23.backend.dto.response.assessment.AssessmentRubricResponse;
import fu.sap490.g23.backend.dto.response.classroom.*;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.classroom.*;
import fu.sap490.g23.backend.entity.classroom.enums.*;
import fu.sap490.g23.backend.entity.course.LearningPackage;
import fu.sap490.g23.backend.entity.notification.AppNotification;
import fu.sap490.g23.backend.repository.classroom.ClassroomEnrollmentRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomHomeworkSubmissionRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomTeacherAssignmentRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomTuitionPaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ClassroomMapper {

    private static final Set<ClassroomRegistrationStatus> OCCUPIES_CLASS_SLOT = ClassroomRegistrationSupport.OCCUPIES_CLASS_SLOT;
    private static final Set<ClassroomRegistrationStatus> ACTIVE_REGISTRATIONS = ClassroomRegistrationSupport.ACTIVE_REGISTRATIONS;
    private static final Set<ClassroomRegistrationStatus> WAITLIST_STATUSES = EnumSet.of(ClassroomRegistrationStatus.WAITLIST);

    private final ClassroomEnrollmentRepository enrollmentRepository;
    private final ClassroomTeacherAssignmentRepository teacherAssignmentRepository;
    private final ClassroomHomeworkSubmissionRepository homeworkSubmissionRepository;
    private final ClassroomHomeworkGradingCatalogService homeworkGradingCatalogService;
    private final ClassroomTuitionPaymentRepository tuitionPaymentRepository;
    private final LarkMeetingService larkMeetingService;

    public ClassroomOfferingResponse toOfferingResponse(ClassroomOffering offering) {
        return toOfferingResponse(offering, false, null, null, false);
    }

    public ClassroomOfferingResponse toOfferingResponse(
            ClassroomOffering offering,
            boolean includeDetails,
            Long viewerStudentId,
            ClassroomEnrollment enrollment,
            boolean includeSessions
    ) {
        LearningPackage learningPackage = offering.getLearningPackage();
        long enrolledCount = enrollmentRepository.countByOfferingAndRegistrationStatuses(offering.getId(), OCCUPIES_CLASS_SLOT);
        long waitlistCount = enrollmentRepository.countByOfferingAndRegistrationStatuses(offering.getId(), WAITLIST_STATUSES);
        List<ClassroomSession> sessions = includeSessions
                ? offering.getSessions()
                : List.of();

        ClassroomSessionResponse nextSession = resolveNextSession(sessions);
        Integer progressPercent = viewerStudentId == null ? null : computeProgressPercent(sessions);

        return ClassroomOfferingResponse.builder()
                .id(offering.getId())
                .packageId(learningPackage.getId())
                .title(learningPackage.getTitle())
                .slug(learningPackage.getSlug())
                .shortDescription(learningPackage.getShortDescription())
                .description(learningPackage.getDescription())
                .deliveryMode(offering.getDeliveryMode())
                .deliveryModeLabel(deliveryModeLabel(offering.getDeliveryMode()))
                .classroomStatus(offering.getStatus())
                .packageStatus(learningPackage.getStatus())
                .entryLevel(offering.getEntryLevel())
                .targetOutcome(offering.getTargetOutcome())
                .maxCapacity(offering.getMaxCapacity())
                .enrolledCount((int) enrolledCount)
                .startDate(offering.getStartDate())
                .endDate(offering.getEndDate())
                .primaryTeacherId(offering.getPrimaryTeacher() == null ? null : offering.getPrimaryTeacher().getId())
                .primaryTeacherName(offering.getPrimaryTeacher() == null ? null : offering.getPrimaryTeacher().getFullName())
                .roomId(offering.getDefaultRoom() == null ? null : offering.getDefaultRoom().getId())
                .roomName(offering.getDefaultRoom() == null ? null : offering.getDefaultRoom().getName())
                .offlineAddress(offering.getOfflineAddress())
                .locationNote(offering.getLocationNote())
                .defaultLarkMeetingUrl(larkMeetingService.isDemoUrl(offering.getDefaultLarkMeetingUrl())
                        ? null
                        : offering.getDefaultLarkMeetingUrl())
                .larkMeetingStatus(offering.getLarkMeetingStatus())
                .larkPlatformName(larkMeetingService.getPlatformName())
                .recordingUrl(offering.getRecordingUrl())
                .recordingVisible(offering.isRecordingVisible())
                .syllabusSummary(offering.getSyllabusSummary())
                .price(learningPackage.getPrice())
                .thumbnailUrl(learningPackage.getThumbnailUrl())
                .nextSession(nextSession)
                .progressPercent(progressPercent)
                .enrollmentId(enrollment == null ? null : enrollment.getId())
                .enrolled(enrollment != null && enrollment.hasClassAccess())
                .registered(enrollment != null && ACTIVE_REGISTRATIONS.contains(enrollment.getRegistrationStatus()))
                .hasClassAccess(enrollment != null && enrollment.hasClassAccess())
                .registrationStatus(enrollment == null ? null : enrollment.getRegistrationStatus())
                .registrationStatusLabel(ClassroomRegistrationSupport.registrationStatusLabel(
                        enrollment == null ? null : enrollment.getRegistrationStatus()))
                .holdSpot(enrollment != null && enrollment.isHoldSpot())
                .tuitionAmountDue(enrollment == null ? null : enrollment.getTuitionAmountDue())
                .tuitionAmountPaid(enrollment == null ? null : enrollment.getTuitionAmountPaid())
                .tuitionRemaining(enrollment == null ? null : enrollment.tuitionBalance())
                .tuitionSettlementType(enrollment == null ? null : enrollment.getTuitionSettlementType())
                .tuitionSettlementTypeLabel(ClassroomRegistrationSupport.tuitionSettlementLabel(
                        enrollment == null ? null : enrollment.getTuitionSettlementType()))
                .tuitionSettlementNote(enrollment == null ? null : enrollment.getTuitionSettlementNote())
                .waitlistCount((int) waitlistCount)
                .createdAt(offering.getCreatedAt())
                .updatedAt(offering.getUpdatedAt())
                .sessions(includeDetails ? sessions.stream().map(this::toSessionResponse).toList() : null)
                .enrollments(includeDetails
                        ? enrollmentRepository.findByClassroomOfferingIdAndRegistrationStatusIn(offering.getId(), ACTIVE_REGISTRATIONS)
                        .stream().map(this::toEnrollmentResponse).toList()
                        : null)
                .teachers(teacherAssignmentRepository.findByClassroomOfferingId(offering.getId())
                        .stream()
                        .filter(this::isActiveTeacherAssignment)
                        .map(this::toTeacherSummary)
                        .toList())
                .build();
    }

    private boolean isActiveTeacherAssignment(ClassroomTeacherAssignment assignment) {
        LocalDate today = LocalDate.now();
        return (assignment.getEffectiveFrom() == null || !assignment.getEffectiveFrom().isAfter(today))
                && (assignment.getEffectiveTo() == null || !assignment.getEffectiveTo().isBefore(today));
    }

    public ClassroomSessionResponse toSessionResponse(ClassroomSession session) {
        User teacher = session.getTeacher();
        LarkMeetingStatus larkStatus = session.getLarkMeetingStatus();
        return ClassroomSessionResponse.builder()
                .id(session.getId())
                .classroomOfferingId(session.getClassroomOffering().getId())
                .classroomTitle(session.getClassroomOffering().getLearningPackage().getTitle())
                .sessionDate(session.getSessionDate())
                .startTime(session.getStartTime())
                .endTime(session.getEndTime())
                .teacherId(teacher == null ? null : teacher.getId())
                .teacherName(teacher == null ? null : teacher.getFullName())
                .status(session.getStatus())
                .deliveryMode(session.getDeliveryMode())
                .deliveryModeLabel(deliveryModeLabel(session.getDeliveryMode()))
                .roomId(session.getRoom() == null ? null : session.getRoom().getId())
                .roomName(session.getRoom() == null ? null : session.getRoom().getName())
                .offlineAddress(session.getClassroomOffering().getOfflineAddress())
                .larkMeetingUrl(larkMeetingService.isDemoUrl(session.getLarkMeetingUrl())
                        ? null
                        : session.getLarkMeetingUrl())
                .larkMeetingStatus(larkStatus)
                .larkJoinable(larkMeetingService.isJoinable(session.getLarkMeetingUrl(), larkStatus))
                .larkPlatformName(larkMeetingService.getPlatformName())
                .larkSyncStatus(session.getLarkSyncStatus())
                .larkSyncError(session.getLarkSyncError())
                .larkSyncedAt(session.getLarkSyncedAt())
                .recordingUrl(session.getRecordingUrl())
                .sessionContent(session.getSessionContent())
                .note(session.getNote())
                .locked(session.isLocked())
                .rescheduled(session.getStatus() == ClassroomSessionStatus.RESCHEDULED)
                .cancelled(session.getStatus() == ClassroomSessionStatus.CANCELLED)
                .build();
    }

    public ClassroomEnrollmentResponse toEnrollmentResponse(ClassroomEnrollment enrollment) {
        User confirmedBy = enrollment.getConfirmedBy();
        User assignedBy = enrollment.getAssignedBy();
        User tuitionRecordedBy = enrollment.getTuitionRecordedBy();
        ClassroomOffering offering = enrollment.getClassroomOffering();
        BigDecimal remaining = enrollment.tuitionBalance();
        return ClassroomEnrollmentResponse.builder()
                .id(enrollment.getId())
                .studentId(enrollment.getStudent().getId())
                .studentName(enrollment.getStudent().getFullName())
                .studentEmail(enrollment.getStudent().getEmail())
                .classroomOfferingId(offering.getId())
                .classroomTitle(offering.getLearningPackage().getTitle())
                .deliveryMode(offering.getDeliveryMode())
                .deliveryModeLabel(deliveryModeLabel(offering.getDeliveryMode()))
                .registrationStatus(enrollment.getRegistrationStatus())
                .registrationStatusLabel(ClassroomRegistrationSupport.registrationStatusLabel(enrollment.getRegistrationStatus()))
                .holdSpot(enrollment.isHoldSpot())
                .tuitionAmountDue(enrollment.getTuitionAmountDue())
                .tuitionAmountPaid(enrollment.getTuitionAmountPaid())
                .tuitionDepositPaid(enrollment.getTuitionDepositPaid())
                .tuitionRemaining(remaining)
                .tuitionSettlementType(enrollment.getTuitionSettlementType())
                .tuitionSettlementTypeLabel(ClassroomRegistrationSupport.tuitionSettlementLabel(enrollment.getTuitionSettlementType()))
                .tuitionSettlementNote(enrollment.getTuitionSettlementNote())
                .hasClassAccess(enrollment.hasClassAccess())
                .transferredFromEnrollmentId(enrollment.getTransferredFromEnrollmentId())
                .enrolledAt(enrollment.getEnrolledAt())
                .assignedAt(enrollment.getAssignedAt())
                .assignedByName(assignedBy == null ? null : assignedBy.getFullName())
                .assignmentNote(enrollment.getAssignmentNote())
                .confirmedAt(enrollment.getConfirmedAt())
                .confirmedByName(confirmedBy == null ? null : confirmedBy.getFullName())
                .tuitionRecordedAt(enrollment.getTuitionRecordedAt())
                .tuitionRecordedByName(tuitionRecordedBy == null ? null : tuitionRecordedBy.getFullName())
                .note(enrollment.getNote())
                .tuitionPayments(tuitionPaymentRepository.findByEnrollmentIdOrderByCreatedAtDesc(enrollment.getId()).stream()
                        .map(payment -> ClassroomTuitionPaymentResponse.builder()
                                .id(payment.getId())
                                .amount(payment.getAmount())
                                .paymentKind(payment.getPaymentKind() == null ? null : payment.getPaymentKind().name())
                                .paymentKindLabel(ClassroomRegistrationSupport.tuitionPaymentKindLabel(payment.getPaymentKind()))
                                .note(payment.getNote())
                                .recordedByName(payment.getRecordedBy() == null ? null : payment.getRecordedBy().getFullName())
                                .createdAt(payment.getCreatedAt())
                                .build())
                        .toList())
                .build();
    }

    public ClassroomTeacherSummaryResponse toTeacherSummary(ClassroomTeacherAssignment assignment) {
        return ClassroomTeacherSummaryResponse.builder()
                .teacherId(assignment.getTeacher().getId())
                .teacherName(assignment.getTeacher().getFullName())
                .role(assignment.getRole())
                .build();
    }

    public ClassroomChangeRequestResponse toChangeRequestResponse(ClassroomChangeRequest request) {
        User reviewer = request.getReviewer();
        return ClassroomChangeRequestResponse.builder()
                .id(request.getId())
                .requestType(request.getRequestType())
                .requestTypeLabel(changeRequestTypeLabel(request.getRequestType()))
                .requesterId(request.getRequester().getId())
                .requesterName(request.getRequester().getFullName())
                .classroomOfferingId(request.getClassroomOffering().getId())
                .classroomTitle(request.getClassroomOffering().getLearningPackage().getTitle())
                .targetSessionId(request.getTargetSession() == null ? null : request.getTargetSession().getId())
                .oldValuesJson(request.getOldValuesJson())
                .newValuesJson(request.getNewValuesJson())
                .reason(request.getReason())
                .status(request.getStatus())
                .statusLabel(changeRequestStatusLabel(request.getStatus()))
                .reviewerId(reviewer == null ? null : reviewer.getId())
                .reviewerName(reviewer == null ? null : reviewer.getFullName())
                .reviewedAt(request.getReviewedAt())
                .reviewNote(request.getReviewNote())
                .createdAt(request.getCreatedAt())
                .build();
    }

    public ClassroomAttendanceResponse toAttendanceResponse(ClassroomAttendance attendance) {
        return ClassroomAttendanceResponse.builder()
                .id(attendance.getId())
                .sessionId(attendance.getSession().getId())
                .studentId(attendance.getStudent().getId())
                .studentName(attendance.getStudent().getFullName())
                .status(attendance.getStatus())
                .note(attendance.getNote())
                .joinTime(attendance.getJoinTime())
                .leaveTime(attendance.getLeaveTime())
                .durationMinutes(attendance.getDurationMinutes())
                .teacherConfirmed(attendance.isTeacherConfirmed())
                .build();
    }

    public ClassroomHomeworkResponse toHomeworkResponse(ClassroomHomework homework, Long studentId) {
        ClassroomHomeworkSubmissionResponse mySubmission = null;
        if (studentId != null) {
            mySubmission = homeworkSubmissionRepository.findByHomeworkIdAndStudentId(homework.getId(), studentId)
                    .map(this::toHomeworkSubmissionResponse)
                    .orElse(null);
        }
        boolean overdue = homework.getDeadline() != null
                && homework.getDeadline().isBefore(LocalDateTime.now())
                && homework.getStatus() == HomeworkStatus.OPEN;
        Integer submissionCount = null;
        Integer gradedCount = null;
        Integer pendingGradingCount = null;
        if (studentId == null) {
            submissionCount = (int) homeworkSubmissionRepository.countByHomeworkId(homework.getId());
            gradedCount = (int) homeworkSubmissionRepository.countByHomeworkIdAndStatus(
                    homework.getId(),
                    HomeworkSubmissionStatus.GRADED
            );
            pendingGradingCount = (int) homeworkSubmissionRepository.countByHomeworkIdAndStatus(
                    homework.getId(),
                    HomeworkSubmissionStatus.SUBMITTED
            );
        }
        return ClassroomHomeworkResponse.builder()
                .id(homework.getId())
                .classroomOfferingId(homework.getClassroomOffering().getId())
                .sessionId(homework.getSession() == null ? null : homework.getSession().getId())
                .title(homework.getTitle())
                .instruction(homework.getInstruction())
                .deadline(homework.getDeadline())
                .maxScore(homework.getMaxScore())
                .allowResubmission(homework.isAllowResubmission())
                .attachmentUrl(homework.getAttachmentUrl())
                .status(homework.getStatus())
                .gradingMode(homework.getGradingMode())
                .skill(homework.getSkill())
                .rubricId(homework.getRubric() == null ? null : homework.getRubric().getId())
                .rubricName(homework.getRubric() == null ? null : homework.getRubric().getName())
                .rubric(homeworkGradingCatalogService.mapRubric(homework.getRubric()))
                .overdue(overdue)
                .mySubmission(mySubmission)
                .submissionCount(submissionCount)
                .gradedCount(gradedCount)
                .pendingGradingCount(pendingGradingCount)
                .build();
    }

    public ClassroomHomeworkSubmissionResponse toHomeworkSubmissionResponse(ClassroomHomeworkSubmission submission) {
        return ClassroomHomeworkSubmissionResponse.builder()
                .id(submission.getId())
                .homeworkId(submission.getHomework().getId())
                .studentId(submission.getStudent().getId())
                .studentName(submission.getStudent().getFullName())
                .textAnswer(submission.getTextAnswer())
                .attachmentUrl(submission.getAttachmentUrl())
                .submittedAt(submission.getSubmittedAt())
                .status(submission.getStatus())
                .score(submission.getScore())
                .teacherFeedback(submission.getTeacherFeedback())
                .gradedAt(submission.getGradedAt())
                .build();
    }

    public ClassroomGradebookResponse toGradebookResponse(ClassroomGradebookEntry entry) {
        return ClassroomGradebookResponse.builder()
                .id(entry.getId())
                .studentId(entry.getStudent().getId())
                .studentName(entry.getStudent().getFullName())
                .homeworkScore(entry.getHomeworkScore())
                .quizScore(entry.getQuizScore())
                .attendancePercent(entry.getAttendancePercent())
                .participationScore(entry.getParticipationScore())
                .finalResult(entry.getFinalResult())
                .teacherComment(entry.getTeacherComment())
                .status(entry.getStatus())
                .build();
    }

    public ClassroomMaterialResponse toMaterialResponse(ClassroomMaterial material) {
        return ClassroomMaterialResponse.builder()
                .id(material.getId())
                .title(material.getTitle())
                .fileUrl(material.getFileUrl())
                .fileType(material.getFileType())
                .visibility(material.getVisibility())
                .sessionId(material.getSession() == null ? null : material.getSession().getId())
                .build();
    }

    public ClassroomAnnouncementResponse toAnnouncementResponse(ClassroomAnnouncement announcement) {
        return ClassroomAnnouncementResponse.builder()
                .id(announcement.getId())
                .title(announcement.getTitle())
                .content(announcement.getContent())
                .createdByName(announcement.getCreatedBy() == null ? null : announcement.getCreatedBy().getFullName())
                .createdAt(announcement.getCreatedAt())
                .build();
    }

    public ClassroomSyllabusItemResponse toSyllabusItemResponse(ClassroomSyllabusItem item) {
        return ClassroomSyllabusItemResponse.builder()
                .id(item.getId())
                .title(item.getTitle())
                .description(item.getDescription())
                .displayOrder(item.getDisplayOrder())
                .sessionPlan(item.getSessionPlan())
                .status(item.getStatus())
                .build();
    }

    public AppNotificationResponse toNotificationResponse(AppNotification notification) {
        return AppNotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .body(notification.getBody())
                .read(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }

    public String deliveryModeLabel(ClassroomDeliveryMode mode) {
        if (mode == null) {
            return null;
        }
        return switch (mode) {
            case OFFLINE -> "Tại trung tâm";
            case VIRTUAL -> "Trực tuyến với giảng viên";
        };
    }

    public String changeRequestTypeLabel(ClassroomChangeRequestType type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case RESCHEDULE_SESSION -> "Đổi lịch buổi học";
            case CHANGE_ROOM -> "Đổi phòng học";
            case CHANGE_TEACHER -> "Đổi giáo viên";
            case CANCEL_SESSION -> "Hủy buổi học";
            case CREATE_MAKEUP_SESSION -> "Tạo buổi học bù";
            case TRANSFER_STUDENT -> "Chuyển học viên";
            case TRANSFER_CLASS -> "Chuyển lớp";
            case UPDATE_LARK_LINK -> "Cập nhật link Lark";
        };
    }

    public String changeRequestStatusLabel(ClassroomChangeRequestStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case PENDING -> "Chờ duyệt";
            case APPROVED -> "Đã duyệt";
            case REJECTED -> "Đã từ chối";
            case CANCELLED -> "Đã hủy";
            case APPLIED -> "Đã áp dụng";
        };
    }

    private ClassroomSessionResponse resolveNextSession(List<ClassroomSession> sessions) {
        LocalDate today = LocalDate.now();
        return sessions.stream()
                .filter(session -> session.getStatus() != ClassroomSessionStatus.CANCELLED
                        && session.getStatus() != ClassroomSessionStatus.COMPLETED)
                .filter(session -> !session.getSessionDate().isBefore(today))
                .sorted((a, b) -> {
                    int dateCompare = a.getSessionDate().compareTo(b.getSessionDate());
                    if (dateCompare != 0) {
                        return dateCompare;
                    }
                    return a.getStartTime().compareTo(b.getStartTime());
                })
                .findFirst()
                .map(this::toSessionResponse)
                .orElse(null);
    }

    private Integer computeProgressPercent(List<ClassroomSession> sessions) {
        if (sessions.isEmpty()) {
            return 0;
        }
        long completed = sessions.stream()
                .filter(session -> session.getStatus() == ClassroomSessionStatus.COMPLETED)
                .count();
        return (int) Math.round((completed * 100.0) / sessions.size());
    }
}
