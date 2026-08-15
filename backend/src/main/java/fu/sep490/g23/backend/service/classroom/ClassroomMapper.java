package fu.sep490.g23.backend.service.classroom;
import fu.sep490.g23.backend.entity.classroom.enums.HomeworkStatus;
import fu.sep490.g23.backend.entity.curriculum.FlashcardSet;
import fu.sep490.g23.backend.entity.classroom.enums.HomeworkSubmissionTiming;
import fu.sep490.g23.backend.entity.classroom.CenterMaterialLibraryItem;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomTuitionPaymentResponse;
import fu.sep490.g23.backend.entity.classroom.enums.HomeworkSubmissionStatus;
import fu.sep490.g23.backend.entity.classroom.enums.LarkMeetingStatus;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomSessionStatus;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomChangeRequestStatus;
import fu.sep490.g23.backend.entity.classroom.ClassroomAnnouncement;
import fu.sep490.g23.backend.entity.curriculum.CurriculumFlashcardRef;
import fu.sep490.g23.backend.entity.classroom.TrainingProgram;
import fu.sep490.g23.backend.entity.curriculum.CurriculumAssessmentRef;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomChangeRequestType;
import fu.sep490.g23.backend.entity.classroom.ClassroomSyllabusItem;
import fu.sep490.g23.backend.entity.curriculum.CurriculumMaterialRef;
import fu.sep490.g23.backend.entity.curriculum.CurriculumProgram;
import fu.sep490.g23.backend.entity.curriculum.CurriculumExerciseRef;
import fu.sep490.g23.backend.dto.response.classroom.TrainingProgramResponse;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import fu.sep490.g23.backend.dto.response.classroom.AppNotificationResponse;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomSyllabusItemResponse;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomAnnouncementResponse;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomMaterialResponse;
import fu.sep490.g23.backend.entity.curriculum.CurriculumUnit;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomHomeworkSubmissionResponse;
import fu.sep490.g23.backend.entity.classroom.ClassroomHomeworkSubmission;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomRegistrationStatus;
import fu.sep490.g23.backend.entity.classroom.ClassroomEnrollment;
import fu.sep490.g23.backend.entity.classroom.ClassroomHomework;
import fu.sep490.g23.backend.entity.classroom.ClassroomChangeRequest;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomOfferingResponse;
import fu.sep490.g23.backend.entity.classroom.ClassroomSession;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomChangeRequestResponse;
import fu.sep490.g23.backend.entity.classroom.ClassroomOffering;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomHomeworkResponse;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomTeacherSummaryResponse;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomGradebookResponse;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomAttendanceResponse;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomSessionResponse;
import fu.sep490.g23.backend.entity.classroom.ClassroomMaterial;
import fu.sep490.g23.backend.entity.classroom.ClassroomTeacherAssignment;
import fu.sep490.g23.backend.entity.classroom.ClassroomAttendance;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomEnrollmentResponse;
import fu.sep490.g23.backend.entity.classroom.ClassroomGradebookEntry;

import fu.sep490.g23.backend.dto.response.curriculum.CurriculumProgramResponse;
import fu.sep490.g23.backend.dto.response.curriculum.CurriculumReferenceResponse;
import fu.sep490.g23.backend.dto.response.curriculum.CurriculumSessionPlanResponse;
import fu.sep490.g23.backend.dto.response.curriculum.CurriculumUnitResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.classroom.*;
import fu.sep490.g23.backend.entity.course.LearningPackage;
import fu.sep490.g23.backend.entity.curriculum.*;
import fu.sep490.g23.backend.entity.notification.AppNotification;
import fu.sep490.g23.backend.repository.classroom.ClassroomEnrollmentRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomHomeworkSubmissionRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomSessionRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomTeacherAssignmentRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomTuitionPaymentRepository;
import fu.sep490.g23.backend.service.classroom.ClassroomHomeworkGradingCatalogService;
import fu.sep490.g23.backend.service.classroom.ClassroomHomeworkObjectiveGrader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ClassroomMapper {

    private final HomeworkTextAnnotationCodec homeworkTextAnnotationCodec;

    private static final Set<ClassroomRegistrationStatus> OCCUPIES_CLASS_SLOT = ClassroomRegistrationSupport.OCCUPIES_CLASS_SLOT;
    private static final Set<ClassroomRegistrationStatus> ACTIVE_REGISTRATIONS = ClassroomRegistrationSupport.ACTIVE_REGISTRATIONS;
    private static final Set<ClassroomRegistrationStatus> WAITLIST_STATUSES = EnumSet.of(ClassroomRegistrationStatus.WAITLIST);

    private final ClassroomEnrollmentRepository enrollmentRepository;
    private final ClassroomTeacherAssignmentRepository teacherAssignmentRepository;
    private final ClassroomHomeworkSubmissionRepository homeworkSubmissionRepository;
    private final ClassroomHomeworkGradingCatalogService homeworkGradingCatalogService;
    private final ClassroomTuitionPaymentRepository tuitionPaymentRepository;
    private final ClassroomSessionRepository sessionRepository;
    private final VirtualMeetingService virtualMeetingService;
    private final ClassroomHomeworkObjectiveGrader homeworkObjectiveGrader;

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
        Integer progressPercent = viewerStudentId == null
                ? null
                : computeProgressPercent(offering, sessions, includeSessions);
        ScheduleSummary scheduleSummary = computeScheduleSummary(offering);

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
                .trainingProgramId(offering.getTrainingProgram() == null ? null : offering.getTrainingProgram().getId())
                .trainingProgramTitle(offering.getTrainingProgram() == null ? null : offering.getTrainingProgram().getTitle())
                .trainingProgramCode(offering.getTrainingProgram() == null ? null : offering.getTrainingProgram().getCode())
                .trainingProgramSlug(offering.getTrainingProgram() == null ? null : offering.getTrainingProgram().getSlug())
                .trainingProgramStatus(offering.getTrainingProgram() == null ? null : offering.getTrainingProgram().getStatus().name())
                .trainingProgram(toTrainingProgramSummary(offering.getTrainingProgram()))
                .curriculumProgramId(offering.getCurriculumProgram() == null ? null : offering.getCurriculumProgram().getId())
                .curriculumProgramTitle(offering.getCurriculumProgram() == null ? null : offering.getCurriculumProgram().getTitle())
                .curriculumProgramCode(offering.getCurriculumProgram() == null ? null : offering.getCurriculumProgram().getCode())
                .curriculumProgramSlug(offering.getCurriculumProgram() == null ? null : offering.getCurriculumProgram().getSlug())
                .curriculumProgramExamCategory(offering.getCurriculumProgram() == null ? null : offering.getCurriculumProgram().getExamCategory())
                .curriculumProgramStatus(offering.getCurriculumProgram() == null ? null : offering.getCurriculumProgram().getStatus())
                .curriculumProgram(toCurriculumProgramResponse(offering.getCurriculumProgram(), includeDetails))
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
                .defaultLarkMeetingUrl(virtualMeetingService.isLegacyOrPlaceholderUrl(offering.getDefaultLarkMeetingUrl())
                        ? null
                        : offering.getDefaultLarkMeetingUrl())
                .larkMeetingStatus(offering.getLarkMeetingStatus())
                .larkPlatformName(virtualMeetingService.getPlatformName())
                .recordingUrl(offering.isRecordingVisible() ? offering.getRecordingUrl() : null)
                .recordingVisible(offering.isRecordingVisible())
                .syllabusSummary(offering.getSyllabusSummary())
                .programOutcomes(offering.getProgramOutcomes())
                .teacherGuide(offering.getTeacherGuide())
                .interactionActivities(offering.getInteractionActivities())
                .price(learningPackage.getPrice())
                .salePrice(learningPackage.getSalePrice())
                .targetScore(learningPackage.getTargetScore())
                .duration(learningPackage.getDuration())
                .studyMode(learningPackage.getStudyMode())
                .displayOrder(learningPackage.getDisplayOrder())
                .featured(learningPackage.isFeatured())
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
                .waitlistPosition(enrollment != null
                        && enrollment.getRegistrationStatus() == ClassroomRegistrationStatus.WAITLIST
                        ? enrollment.getWaitlistPriority()
                        : null)
                .scheduleSummary(scheduleSummary == null ? null : scheduleSummary.summary())
                .scheduleDaysOfWeek(scheduleSummary == null ? null : scheduleSummary.daysOfWeek())
                .typicalStartTime(scheduleSummary == null ? null : scheduleSummary.startTime())
                .typicalEndTime(scheduleSummary == null ? null : scheduleSummary.endTime())
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

    /**
     * Chi tiết lớp cho trang public: có lịch buổi học + giáo trình theo buổi,
     * nhưng loại bỏ dữ liệu nội bộ (danh sách học viên, link phòng học, recording, ghi chú giáo viên).
     */
    public ClassroomOfferingResponse toPublicOfferingDetailResponse(ClassroomOffering offering) {
        ClassroomOfferingResponse response = toOfferingResponse(offering, true, null, null, true);
        response.setEnrollments(null);
        response.setTeacherGuide(null);
        response.setDefaultLarkMeetingUrl(null);
        response.setRecordingUrl(null);
        if (response.getCurriculumProgram() != null) {
            response.getCurriculumProgram().setTeacherGuide(null);
        }
        if (response.getSessions() != null) {
            response.getSessions().forEach(this::sanitizePublicSession);
        }
        if (response.getNextSession() != null) {
            sanitizePublicSession(response.getNextSession());
        }
        return response;
    }

    private void sanitizePublicSession(ClassroomSessionResponse session) {
        session.setLarkMeetingUrl(null);
        session.setLarkJoinable(false);
        session.setLarkSyncStatus(null);
        session.setLarkSyncError(null);
        session.setLarkSyncedAt(null);
        session.setRecordingUrl(null);
        session.setRecordingVisible(false);
        session.setNote(null);
    }

    private boolean isActiveTeacherAssignment(ClassroomTeacherAssignment assignment) {
        LocalDate today = LocalDate.now();
        return (assignment.getEffectiveFrom() == null || !assignment.getEffectiveFrom().isAfter(today))
                && (assignment.getEffectiveTo() == null || !assignment.getEffectiveTo().isBefore(today));
    }

    public ClassroomSessionResponse toSessionResponse(ClassroomSession session) {
        return toSessionResponse(session, false);
    }

    public ClassroomSessionResponse toManagerSessionResponse(ClassroomSession session) {
        return toSessionResponse(session, true);
    }

    private ClassroomSessionResponse toSessionResponse(ClassroomSession session, boolean includeHiddenRecording) {
        User teacher = session.getTeacher();
        CurriculumSessionPlan sessionPlan = session.getCurriculumSessionPlan();
        CurriculumUnit curriculumUnit = sessionPlan == null ? null : sessionPlan.getUnit();
        LarkMeetingStatus larkStatus = session.getLarkMeetingStatus();
        boolean recordingExpired = session.getRecordingExpiresAt() != null
                && !session.getRecordingExpiresAt().isAfter(LocalDateTime.now());
        boolean recordingAvailable = Boolean.TRUE.equals(session.getRecordingVisible()) && !recordingExpired;
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
                .larkMeetingUrl(virtualMeetingService.isLegacyOrPlaceholderUrl(session.getLarkMeetingUrl())
                        ? null
                        : session.getLarkMeetingUrl())
                .larkMeetingNo(session.getLarkMeetingNo())
                .larkMeetingStatus(larkStatus)
                .larkJoinable(virtualMeetingService.isJoinable(session.getLarkMeetingUrl(), larkStatus))
                .larkPlatformName(virtualMeetingService.getPlatformName())
                .larkSyncStatus(session.getLarkSyncStatus())
                .larkSyncError(session.getLarkSyncError())
                .larkSyncedAt(session.getLarkSyncedAt())
                .recordingUrl(includeHiddenRecording || recordingAvailable ? session.getRecordingUrl() : null)
                .recordingVisible(includeHiddenRecording
                        ? Boolean.TRUE.equals(session.getRecordingVisible())
                        : recordingAvailable)
                .recordingSyncStatus(session.getRecordingSyncStatus())
                .recordingProvider(session.getRecordingProvider())
                .recordingDurationMs(session.getRecordingDurationMs())
                .recordingSyncedAt(session.getRecordingSyncedAt())
                .recordingLastAttemptAt(session.getRecordingLastAttemptAt())
                .recordingSyncError(includeHiddenRecording ? session.getRecordingSyncError() : null)
                .recordingSyncAttempts(session.getRecordingSyncAttempts())
                .recordingPublishedAt(session.getRecordingPublishedAt())
                .recordingExpiresAt(session.getRecordingExpiresAt())
                .sessionContent(session.getSessionContent())
                .curriculumSessionPlanId(sessionPlan == null ? null : sessionPlan.getId())
                .sessionNumber(sessionPlan == null ? null : sessionPlan.getSessionNumber())
                .sessionPlanTitle(sessionPlan == null ? null : sessionPlan.getTitle())
                .sessionPlanDescription(sessionPlan == null ? null : sessionPlan.getDescription())
                .learningObjectives(sessionPlan == null ? null : sessionPlan.getLearningObjectives())
                .curriculumUnitId(curriculumUnit == null ? null : curriculumUnit.getId())
                .curriculumUnitDisplayOrder(curriculumUnit == null ? null : curriculumUnit.getDisplayOrder())
                .curriculumUnitTitle(curriculumUnit == null ? null : curriculumUnit.getTitle())
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
        boolean waitlisted = enrollment.getRegistrationStatus() == ClassroomRegistrationStatus.WAITLIST;
        Integer waitlistSize = waitlisted
                ? (int) enrollmentRepository.countByOfferingAndRegistrationStatuses(
                        offering.getId(),
                        WAITLIST_STATUSES
                )
                : null;
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
                .waitlistPriority(waitlisted ? enrollment.getWaitlistPriority() : null)
                .waitlistPosition(waitlisted ? enrollment.getWaitlistPriority() : null)
                .waitlistSize(waitlistSize)
                .tuitionAmountDue(enrollment.getTuitionAmountDue())
                .tuitionAmountPaid(enrollment.getTuitionAmountPaid())
                .tuitionDepositPaid(enrollment.getTuitionDepositPaid())
                .tuitionRemaining(remaining)
                .tuitionSettlementType(enrollment.getTuitionSettlementType())
                .tuitionSettlementTypeLabel(ClassroomRegistrationSupport.tuitionSettlementLabel(enrollment.getTuitionSettlementType()))
                .tuitionSettlementNote(enrollment.getTuitionSettlementNote())
                .tuitionSettlementStatus(enrollment.getTuitionSettlementStatus())
                .tuitionSettlementStatusLabel(ClassroomRegistrationSupport.tuitionSettlementStatusLabel(
                        enrollment.getTuitionSettlementStatus()))
                .tuitionSettlementResolvedAt(enrollment.getTuitionSettlementResolvedAt())
                .tuitionSettlementResolvedByName(enrollment.getTuitionSettlementResolvedBy() == null
                        ? null
                        : enrollment.getTuitionSettlementResolvedBy().getFullName())
                .tuitionSettlementResolutionNote(enrollment.getTuitionSettlementResolutionNote())
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
                .sessionId(assignment.getClassroomSession() == null ? null : assignment.getClassroomSession().getId())
                .effectiveFrom(assignment.getEffectiveFrom())
                .effectiveTo(assignment.getEffectiveTo())
                .reason(assignment.getReason())
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
        ClassroomSession session = attendance.getSession();
        ClassroomOffering offering = session.getClassroomOffering();
        return ClassroomAttendanceResponse.builder()
                .id(attendance.getId())
                .sessionId(session.getId())
                .studentId(attendance.getStudent().getId())
                .studentName(attendance.getStudent().getFullName())
                .studentEmail(attendance.getStudent().getEmail())
                .status(attendance.getStatus())
                .note(attendance.getNote())
                .joinTime(attendance.getJoinTime())
                .leaveTime(attendance.getLeaveTime())
                .durationMinutes(attendance.getDurationMinutes())
                .teacherConfirmed(attendance.isTeacherConfirmed())
                .sessionDate(session.getSessionDate())
                .startTime(session.getStartTime())
                .endTime(session.getEndTime())
                .classroomTitle(offering.getLearningPackage() != null ? offering.getLearningPackage().getTitle() : null)
                .classroomOfferingId(offering.getId())
                .deliveryMode(session.getDeliveryMode() != null ? session.getDeliveryMode().name() : null)
                .roomName(session.getRoom() != null ? session.getRoom().getName() : null)
                .larkMeetingUrl(session.getLarkMeetingUrl())
                .larkSyncError(session.getLarkSyncError())
                .build();
    }

    /**
     * Build a placeholder attendance response for an enrolled student who does not
     * yet have an attendance record for the given session.
     */
    public ClassroomAttendanceResponse toPlaceholderAttendanceResponse(ClassroomSession session, User student) {
        ClassroomOffering offering = session.getClassroomOffering();
        return ClassroomAttendanceResponse.builder()
                .sessionId(session.getId())
                .studentId(student.getId())
                .studentName(student.getFullName())
                .studentEmail(student.getEmail())
                .sessionDate(session.getSessionDate())
                .startTime(session.getStartTime())
                .endTime(session.getEndTime())
                .classroomTitle(offering.getLearningPackage() != null ? offering.getLearningPackage().getTitle() : null)
                .classroomOfferingId(offering.getId())
                .deliveryMode(session.getDeliveryMode() != null ? session.getDeliveryMode().name() : null)
                .roomName(session.getRoom() != null ? session.getRoom().getName() : null)
                .larkMeetingUrl(session.getLarkMeetingUrl())
                .larkSyncError(session.getLarkSyncError())
                .build();
    }

    public ClassroomHomeworkResponse toHomeworkResponse(ClassroomHomework homework, Long studentId) {
        ClassroomHomeworkSubmissionResponse mySubmission = null;
        if (studentId != null) {
            mySubmission = homeworkSubmissionRepository.findByHomeworkIdAndStudentId(homework.getId(), studentId)
                    .map(this::toLearnerHomeworkSubmissionResponse)
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
                .curriculumUnitId(homework.getCurriculumUnit() == null ? null : homework.getCurriculumUnit().getId())
                .curriculumUnitTitle(homework.getCurriculumUnit() == null ? null : homework.getCurriculumUnit().getTitle())
                .title(homework.getTitle())
                .instruction(homework.getInstruction())
                .deadline(homework.getDeadline())
                .maxScore(homework.getMaxScore())
                .allowResubmission(homework.isAllowResubmission())
                .attachmentUrl(homework.getAttachmentUrl())
                .activityType(homework.getActivityType())
                .activityConfigJson(studentId == null
                        ? homework.getActivityConfigJson()
                        : homeworkObjectiveGrader.toLearnerActivityConfig(homework.getActivityConfigJson()))
                .objectiveAnswerKey(studentId == null && homework.getAssessmentBankItem() != null
                        ? homework.getAssessmentBankItem().getObjectiveAnswerKey() : null)
                .aiReviewEnabled(homework.isAiReviewEnabled())
                .status(homework.getStatus())
                .gradingMode(homework.getGradingMode())
                .skill(homework.getSkill())
                .rubricId(homework.getRubric() == null ? null : homework.getRubric().getId())
                .rubricName(homework.getRubric() == null ? null : homework.getRubric().getName())
                .assessmentBankItemId(homework.getAssessmentBankItem() == null ? null : homework.getAssessmentBankItem().getId())
                .assessmentBankItemTitle(homework.getAssessmentBankItem() == null ? null : homework.getAssessmentBankItem().getTitle())
                .assessmentType(homework.getAssessmentBankItem() == null || homework.getAssessmentBankItem().getType() == null
                        ? null : homework.getAssessmentBankItem().getType().name())
                .rubric(homeworkGradingCatalogService.mapRubric(homework.getRubric()))
                .overdue(overdue)
                .mySubmission(mySubmission)
                .submissionCount(submissionCount)
                .gradedCount(gradedCount)
                .pendingGradingCount(pendingGradingCount)
                .build();
    }

    public ClassroomHomeworkSubmissionResponse toHomeworkSubmissionResponse(ClassroomHomeworkSubmission submission) {
        return toHomeworkSubmissionResponse(submission.getHomework(), submission.getStudent(), submission);
    }

    ClassroomHomeworkSubmissionResponse toLearnerHomeworkSubmissionResponse(ClassroomHomeworkSubmission submission) {
        ClassroomHomeworkSubmissionResponse response = toHomeworkSubmissionResponse(submission);
          if (submission.getStatus() != HomeworkSubmissionStatus.GRADED) {
              response.setScore(null);
              response.setTeacherFeedback(null);
              response.setAiFeedbackJson(null);
              response.setAnnotations(List.of());
            response.setGradedAt(null);
        }
        return response;
    }

    public ClassroomHomeworkSubmissionResponse toHomeworkSubmissionResponse(
            ClassroomHomework homework,
            User student,
            ClassroomHomeworkSubmission submission
    ) {
        boolean submitted = submission != null
                && submission.getSubmittedAt() != null
                && submission.getStatus() != HomeworkSubmissionStatus.DRAFT;
        HomeworkSubmissionTiming timing = HomeworkSubmissionTiming.NOT_SUBMITTED;
        if (submitted) {
            timing = homework.getDeadline() != null && submission.getSubmittedAt().isAfter(homework.getDeadline())
                    ? HomeworkSubmissionTiming.LATE
                    : HomeworkSubmissionTiming.ON_TIME;
        }
        return ClassroomHomeworkSubmissionResponse.builder()
                .id(submission == null ? null : submission.getId())
                .homeworkId(homework.getId())
                .studentId(student.getId())
                .studentName(student.getFullName())
                .studentEmail(student.getEmail())
                .studentAvatarUrl(student.getAvatarUrl())
                .submitted(submitted)
                .submissionTiming(timing)
                .textAnswer(submission == null ? null : submission.getTextAnswer())
                .attachmentUrl(submission == null ? null : submission.getAttachmentUrl())
                .submittedAt(submission == null ? null : submission.getSubmittedAt())
                .status(submission == null ? null : submission.getStatus())
                  .score(submission == null ? null : submission.getScore())
                  .teacherFeedback(submission == null ? null : submission.getTeacherFeedback())
                  .aiFeedbackJson(submission == null ? null : submission.getAiFeedbackJson())
                  .annotations(submission == null
                        ? List.of()
                        : homeworkTextAnnotationCodec.deserialize(submission.getTeacherAnnotationsJson()))
                .gradedAt(submission == null ? null : submission.getGradedAt())
                .build();
    }

    public ClassroomGradebookResponse toGradebookResponse(ClassroomGradebookEntry entry) {
        return ClassroomGradebookResponse.builder()
                .id(entry.getId())
                .studentId(entry.getStudent().getId())
                .studentName(entry.getStudent().getFullName())
                .homeworkAverage(entry.getHomeworkScore())
                .attendancePercent(entry.getAttendancePercent())
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
                .description(material.getDescription())
                .materialType(material.getMaterialType())
                .provider(material.getProvider())
                .visibility(material.getVisibility())
                .sourceType(material.getSourceType())
                .centerMaterialId(material.getCenterMaterialId())
                .sessionId(material.getSession() == null ? null : material.getSession().getId())
                .sessionTitle(material.getSession() == null ? null : material.getSession().getSessionContent())
                .curriculumUnitId(material.getCurriculumUnit() == null ? null : material.getCurriculumUnit().getId())
                .curriculumUnitTitle(material.getCurriculumUnit() == null ? null : material.getCurriculumUnit().getTitle())
                .mandatory(isMandatoryMaterial(material.getSourceType()))
                .uploadedByName(material.getUploadedBy() == null ? null : material.getUploadedBy().getFullName())
                .reviewStatus(material.getReviewStatus() == null ? null : material.getReviewStatus().name())
                .reviewNote(material.getReviewNote())
                .submittedForReviewAt(material.getSubmittedForReviewAt())
                .createdAt(material.getCreatedAt())
                .updatedAt(material.getUpdatedAt())
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
                .homeworkNotes(item.getHomeworkNotes())
                .quizNotes(item.getQuizNotes())
                .teacherNotes(item.getTeacherNotes())
                .sessionNumber(item.getSessionNumber())
                .linkedSessionId(item.getLinkedSessionId())
                .reviewStatus(item.getReviewStatus() == null ? null : item.getReviewStatus().name())
                .reviewNote(item.getReviewNote())
                .status(item.getStatus())
                .build();
    }

    public AppNotificationResponse toNotificationResponse(AppNotification notification) {
        return AppNotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .body(notification.getBody())
                .actionPath(notification.getActionPath())
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

    private TrainingProgramResponse toTrainingProgramSummary(TrainingProgram program) {
        if (program == null) {
            return null;
        }
        CurriculumProgram curriculum = program.getCurriculumProgram();
        return TrainingProgramResponse.builder()
                .id(program.getId())
                .title(program.getTitle())
                .code(program.getCode())
                .slug(program.getSlug())
                .deliveryMode(program.getDeliveryMode())
                .deliveryModeLabel(deliveryModeLabel(program.getDeliveryMode()))
                .curriculumProgramId(curriculum == null ? null : curriculum.getId())
                .curriculumProgramTitle(curriculum == null ? null : curriculum.getTitle())
                .curriculumProgramCode(curriculum == null ? null : curriculum.getCode())
                .curriculumProgramExamCategory(curriculum == null ? null : curriculum.getExamCategory())
                .curriculumProgramStatus(curriculum == null ? null : curriculum.getStatus())
                .entryLevel(curriculum == null ? null : curriculum.getEntryLevel())
                .targetScore(resolveTargetScore(curriculum))
                .targetOutcome(curriculum == null ? null : curriculum.getOutcomes())
                .price(program.getPrice())
                .salePrice(program.getSalePrice())
                .duration(program.getDuration())
                .studyMode(program.getStudyMode())
                .status(program.getStatus())
                .statusLabel(program.getStatus() == null ? null : program.getStatus().name())
                .classroomCount(program.getClassroomOfferings().size())
                .createdAt(program.getCreatedAt())
                .updatedAt(program.getUpdatedAt())
                .build();
    }

    private String resolveTargetScore(CurriculumProgram curriculum) {
        if (curriculum == null) {
            return null;
        }
        if (curriculum.getTargetBand() != null) {
            return curriculum.getTargetBand().stripTrailingZeros().toPlainString();
        }
        return curriculum.getTargetScore() == null ? null : String.valueOf(curriculum.getTargetScore());
    }

    private CurriculumProgramResponse toCurriculumProgramResponse(CurriculumProgram program, boolean includeUnits) {
        if (program == null) {
            return null;
        }
        return CurriculumProgramResponse.builder()
                .id(program.getId())
                .title(program.getTitle())
                .code(program.getCode())
                .slug(program.getSlug())
                .deliveryMode(program.getDeliveryMode())
                .deliveryModeLabel(deliveryModeLabel(program.getDeliveryMode()))
                .examCategory(program.getExamCategory())
                .targetBand(program.getTargetBand())
                .targetScore(program.getTargetScore())
                .entryLevel(program.getEntryLevel())
                .outcomes(program.getOutcomes())
                .teacherGuide(program.getTeacherGuide())
                .interactionActivities(program.getInteractionActivities())
                .totalSessions(program.getTotalSessions())
                .status(program.getStatus())
                .displayOrder(program.getDisplayOrder())
                .createdAt(program.getCreatedAt())
                .updatedAt(program.getUpdatedAt())
                .units(includeUnits ? program.getUnits().stream().map(this::toCurriculumUnitResponse).toList() : null)
                .build();
    }

    private CurriculumUnitResponse toCurriculumUnitResponse(CurriculumUnit unit) {
        return CurriculumUnitResponse.builder()
                .id(unit.getId())
                .programId(unit.getProgram().getId())
                .displayOrder(unit.getDisplayOrder())
                .title(unit.getTitle())
                .description(unit.getDescription())
                .sessionPlan(unit.getSessionPlan())
                .sessionPlans(unit.getSessionPlans().stream()
                        .sorted(Comparator.comparing(CurriculumSessionPlan::getSessionNumber)
                                .thenComparing(CurriculumSessionPlan::getDisplayOrder)
                                .thenComparing(CurriculumSessionPlan::getId, Comparator.nullsLast(Long::compareTo)))
                        .map(this::toCurriculumSessionPlanResponse)
                        .toList())
                .createdAt(unit.getCreatedAt())
                .updatedAt(unit.getUpdatedAt())
                .materials(unit.getMaterialRefs().stream().map(this::toCurriculumMaterialRef).toList())
                .exercises(unit.getExerciseRefs().stream().map(this::toCurriculumExerciseRef).toList())
                .assessments(unit.getAssessmentRefs().stream().map(this::toCurriculumAssessmentRef).toList())
                .flashcards(unit.getFlashcardRefs().stream().map(this::toCurriculumFlashcardRef).toList())
                .build();
    }

    private CurriculumSessionPlanResponse toCurriculumSessionPlanResponse(CurriculumSessionPlan sessionPlan) {
        CurriculumUnit unit = sessionPlan.getUnit();
        return CurriculumSessionPlanResponse.builder()
                .id(sessionPlan.getId())
                .unitId(unit.getId())
                .unitTitle(unit.getTitle())
                .programId(unit.getProgram().getId())
                .sessionNumber(sessionPlan.getSessionNumber())
                .displayOrder(sessionPlan.getDisplayOrder())
                .title(sessionPlan.getTitle())
                .description(sessionPlan.getDescription())
                .learningObjectives(sessionPlan.getLearningObjectives())
                .createdAt(sessionPlan.getCreatedAt())
                .updatedAt(sessionPlan.getUpdatedAt())
                .build();
    }

    private CurriculumReferenceResponse toCurriculumMaterialRef(CurriculumMaterialRef ref) {
        CenterMaterialLibraryItem material = ref.getMaterial();
        return CurriculumReferenceResponse.builder()
                .id(ref.getId())
                .type("MATERIAL")
                .resourceId(material.getId())
                .title(material.getTitle())
                .subtitle(material.getMaterialType())
                .skill(material.getSkill())
                .status(material.getStatus())
                .fileUrl(material.getFileUrl())
                .displayOrder(ref.getDisplayOrder())
                .note(ref.getNote())
                .build();
    }

    private boolean isMandatoryMaterial(String sourceType) {
        return "CURRICULUM_LIBRARY".equalsIgnoreCase(sourceType);
    }

    private CurriculumReferenceResponse toCurriculumExerciseRef(CurriculumExerciseRef ref) {
        var exercise = ref.getExercise();
        return CurriculumReferenceResponse.builder()
                .id(ref.getId())
                .type("EXERCISE")
                .resourceId(exercise.getId())
                .title(exercise.getTitle())
                .subtitle(exercise.getExerciseType())
                .skill(exercise.getSkill())
                .status(exercise.isActive() ? "ACTIVE" : "INACTIVE")
                .displayOrder(ref.getDisplayOrder())
                .note(ref.getNote())
                .contentJson(exercise.getPrompt())
                .build();
    }

    private CurriculumReferenceResponse toCurriculumAssessmentRef(CurriculumAssessmentRef ref) {
        var assessment = ref.getAssessment();
        return CurriculumReferenceResponse.builder()
                .id(ref.getId())
                .type("ASSESSMENT")
                .resourceId(assessment.getId())
                .title(assessment.getTitle())
                .subtitle(assessment.getType() == null ? null : assessment.getType().name())
                .skill(assessment.getSkill() == null ? null : assessment.getSkill().name())
                .status(assessment.getStatus())
                .displayOrder(ref.getDisplayOrder())
                .note(ref.getNote())
                .build();
    }

    private CurriculumReferenceResponse toCurriculumFlashcardRef(CurriculumFlashcardRef ref) {
        FlashcardSet set = ref.getFlashcardSet();
        return CurriculumReferenceResponse.builder()
                .id(ref.getId())
                .type("FLASHCARD")
                .resourceId(set.getId())
                .title(set.getTitle())
                .subtitle(set.getExamCategory())
                .skill(set.getSkill())
                .status(set.getStatus())
                .displayOrder(ref.getDisplayOrder())
                .note(ref.getNote())
                .contentJson(set.getCardsJson())
                .build();
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
            case UPDATE_LARK_LINK -> "Cập nhật liên kết Google Meet";
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

    private record ScheduleSummary(String summary, List<Integer> daysOfWeek, LocalTime startTime, LocalTime endTime) {
    }

    private static final String[] DAY_OF_WEEK_LABELS = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};

    private ScheduleSummary computeScheduleSummary(ClassroomOffering offering) {
        List<ClassroomSession> allSessions;
        try {
            allSessions = offering.getSessions();
        } catch (RuntimeException exception) {
            return null;
        }
        List<ClassroomSession> active = allSessions.stream()
                .filter(session -> session.getStatus() != ClassroomSessionStatus.CANCELLED)
                .filter(session -> session.getSessionDate() != null && session.getStartTime() != null)
                .toList();
        if (active.isEmpty()) {
            return null;
        }
        List<Integer> days = active.stream()
                .map(session -> session.getSessionDate().getDayOfWeek().getValue())
                .distinct()
                .sorted()
                .toList();
        // Khung giờ phổ biến nhất trong các buổi học
        var timeGroups = active.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        session -> session.getStartTime() + "|" + session.getEndTime(),
                        java.util.stream.Collectors.counting()));
        String typicalKey = timeGroups.entrySet().stream()
                .max(java.util.Map.Entry.comparingByValue())
                .map(java.util.Map.Entry::getKey)
                .orElse(null);
        LocalTime start = null;
        LocalTime end = null;
        if (typicalKey != null) {
            String[] parts = typicalKey.split("\\|");
            start = LocalTime.parse(parts[0]);
            end = parts.length > 1 && !"null".equals(parts[1]) ? LocalTime.parse(parts[1]) : null;
        }
        String dayText = days.stream()
                .map(day -> DAY_OF_WEEK_LABELS[day - 1])
                .collect(java.util.stream.Collectors.joining(", "));
        String timeText = start == null
                ? ""
                : " · " + start + (end == null ? "" : "–" + end);
        return new ScheduleSummary(dayText + timeText, days, start, end);
    }

    private Integer computeProgressPercent(
            ClassroomOffering offering,
            List<ClassroomSession> sessions,
            boolean includeSessions
    ) {
        if (includeSessions) {
            return percentFromSessions(sessions);
        }
        if (offering.getId() == null || sessionRepository == null) {
            return 0;
        }
        long total = sessionRepository.countByClassroomOfferingIdAndStatusNot(
                offering.getId(),
                ClassroomSessionStatus.CANCELLED
        );
        if (total == 0) {
            return 0;
        }
        long completed = sessionRepository.countByClassroomOfferingIdAndStatus(
                offering.getId(),
                ClassroomSessionStatus.COMPLETED
        );
        return (int) Math.round((completed * 100.0) / total);
    }

    private Integer percentFromSessions(List<ClassroomSession> sessions) {
        List<ClassroomSession> counted = sessions.stream()
                .filter(session -> session.getStatus() != ClassroomSessionStatus.CANCELLED)
                .toList();
        if (counted.isEmpty()) {
            return 0;
        }
        long completed = counted.stream()
                .filter(session -> session.getStatus() == ClassroomSessionStatus.COMPLETED)
                .count();
        return (int) Math.round((completed * 100.0) / counted.size());
    }
}
