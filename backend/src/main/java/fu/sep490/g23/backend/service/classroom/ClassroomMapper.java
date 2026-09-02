package fu.sep490.g23.backend.service.classroom;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fu.sep490.g23.backend.entity.classroom.enums.HomeworkStatus;
import fu.sep490.g23.backend.entity.curriculum.FlashcardSet;
import fu.sep490.g23.backend.entity.classroom.enums.HomeworkSubmissionTiming;
import fu.sep490.g23.backend.entity.classroom.CenterMaterialLibraryItem;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomTuitionPaymentResponse;
import fu.sep490.g23.backend.entity.classroom.enums.HomeworkSubmissionStatus;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomSessionStatus;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomChangeRequestStatus;
import fu.sep490.g23.backend.entity.classroom.ClassroomAnnouncement;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomChangeRequestType;
import fu.sep490.g23.backend.entity.classroom.ClassroomSyllabusItem;
import fu.sep490.g23.backend.entity.course.InstructorLedCourse;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import fu.sep490.g23.backend.dto.response.classroom.AppNotificationResponse;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomSyllabusItemResponse;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomAnnouncementResponse;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomMaterialResponse;
import fu.sep490.g23.backend.entity.course.CourseUnit;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomHomeworkSubmissionResponse;
import fu.sep490.g23.backend.entity.classroom.ClassroomHomeworkSubmission;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomRegistrationStatus;
import fu.sep490.g23.backend.entity.classroom.ClassEnrollment;
import fu.sep490.g23.backend.entity.classroom.ClassroomHomework;
import fu.sep490.g23.backend.entity.classroom.ClassroomChangeRequest;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomOfferingResponse;
import fu.sep490.g23.backend.entity.classroom.ClassSchedule;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomChangeRequestResponse;
import fu.sep490.g23.backend.entity.classroom.ClassSection;
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

import fu.sep490.g23.backend.dto.response.curriculum.InstructorLedCourseResponse;
import fu.sep490.g23.backend.dto.response.curriculum.CourseUnitContentRefResponse;
import fu.sep490.g23.backend.dto.response.curriculum.CourseLessonResponse;
import fu.sep490.g23.backend.dto.response.curriculum.CourseUnitResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.classroom.*;
import fu.sep490.g23.backend.entity.course.CourseLesson;
import fu.sep490.g23.backend.entity.course.CourseUnit;
import fu.sep490.g23.backend.entity.course.CourseUnitContentRef;
import fu.sep490.g23.backend.entity.course.InstructorLedCourse;
import fu.sep490.g23.backend.entity.curriculum.*;
import fu.sep490.g23.backend.entity.notification.AppNotification;
import fu.sep490.g23.backend.repository.classroom.ClassEnrollmentRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomHomeworkSubmissionRepository;
import fu.sep490.g23.backend.repository.classroom.ClassScheduleRepository;
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
import java.util.Objects;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ClassroomMapper {
    private static final ObjectMapper CONTENT_JSON_MAPPER = new ObjectMapper();

    private final HomeworkTextAnnotationCodec homeworkTextAnnotationCodec;

    private static final Set<ClassroomRegistrationStatus> OCCUPIES_CLASS_SLOT = ClassroomRegistrationSupport.OCCUPIES_CLASS_SLOT;
    private static final Set<ClassroomRegistrationStatus> ACTIVE_REGISTRATIONS = ClassroomRegistrationSupport.ACTIVE_REGISTRATIONS;
    private static final Set<ClassroomRegistrationStatus> WAITLIST_STATUSES = EnumSet.of(ClassroomRegistrationStatus.WAITLIST);

    private final ClassEnrollmentRepository enrollmentRepository;
    private final ClassroomTeacherAssignmentRepository teacherAssignmentRepository;
    private final ClassroomHomeworkSubmissionRepository homeworkSubmissionRepository;
    private final ClassroomHomeworkGradingCatalogService homeworkGradingCatalogService;
    private final ClassroomTuitionPaymentRepository tuitionPaymentRepository;
    private final ClassScheduleRepository sessionRepository;
    private final VirtualMeetingService virtualMeetingService;
    private final ClassroomHomeworkObjectiveGrader homeworkObjectiveGrader;

    public ClassroomOfferingResponse toOfferingResponse(ClassSection offering) {
        return toOfferingResponse(offering, false, null, null, false);
    }

    public ClassroomOfferingResponse toOfferingResponse(
            ClassSection offering,
            boolean includeDetails,
            Long viewerStudentId,
            ClassEnrollment enrollment,
            boolean includeSessions
    ) {
        InstructorLedCourse course = offering.getInstructorLedCourse();
        long enrolledCount = enrollmentRepository.countByOfferingAndRegistrationStatuses(offering.getId(), OCCUPIES_CLASS_SLOT);
        long waitlistCount = enrollmentRepository.countByOfferingAndRegistrationStatuses(offering.getId(), WAITLIST_STATUSES);
        List<ClassSchedule> sessions = includeSessions
                ? offering.getSchedules()
                : List.of();

        ClassroomSessionResponse nextSession = resolveNextSession(sessions);
        Integer progressPercent = viewerStudentId == null
                ? null
                : computeProgressPercent(offering, sessions, includeSessions);
        ScheduleSummary scheduleSummary = computeScheduleSummary(offering);

        return ClassroomOfferingResponse.builder()
                .id(offering.getId())
                .code(offering.getCode())
                .title(offering.getName())
                .slug(offering.getCode())
                .shortDescription(course.getShortDescription())
                .description(course.getDescription())
                .deliveryMode(offering.getDeliveryMode())
                .deliveryModeLabel(deliveryModeLabel(offering.getDeliveryMode()))
                .classroomStatus(offering.getStatus())
                .instructorLedCourseId(course.getId())
                .instructorLedCourseTitle(course.getTitle())
                .instructorLedCourseCode(course.getCode())
                .instructorLedCourseExamType(course.getExamType())
                .instructorLedCourseStatus(course.getPublicationStatus() == null ? null : course.getPublicationStatus().name())
                .instructorLedCourse(toInstructorLedCourseResponse(course, includeDetails))
                .entryLevel(course.getEntryLevel())
                .targetOutcome(course.getLearningOutcomes())
                .capacity(offering.getCapacity())
                .enrolledCount((int) enrolledCount)
                .startDate(offering.getStartDate())
                .endDate(offering.getPlannedEndDate())
                .primaryTeacherId(offering.getPrimaryTeacher() == null ? null : offering.getPrimaryTeacher().getId())
                .primaryTeacherName(offering.getPrimaryTeacher() == null ? null : offering.getPrimaryTeacher().getFullName())
                .roomId(offering.getRoom() == null ? null : offering.getRoom().getId())
                .roomName(offering.getRoom() == null ? null : offering.getRoom().getName())
                .offlineAddress(offering.getRoom() == null ? null : offering.getRoom().getLocationAddress())
                .googleMeetOwnerId(offering.getGoogleMeetOwner() == null ? null : offering.getGoogleMeetOwner().getId())
                .googleMeetUrl(offering.getGoogleMeetUrl())
                .googleMeetStatus(offering.getGoogleMeetStatus())
                .googleMeetSyncError(offering.getGoogleMeetSyncError())
                .teacherGuide(course.getTeacherGuide())
                .price(offering.getTuitionFeeVnd())
                .tuitionFeeVnd(offering.getTuitionFeeVnd())
                .salePrice(course.getSaleTuitionFeeVnd())
                .targetScore(course.getTargetScore() == null ? null : String.valueOf(course.getTargetScore()))
                .duration(course.getDurationLabel())
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
                        ? enrollmentRepository.findByClassSectionIdAndRegistrationStatusIn(offering.getId(), ACTIVE_REGISTRATIONS)
                        .stream().map(this::toEnrollmentResponse).toList()
                        : null)
                .teachers(teacherAssignmentRepository.findByClassSectionId(offering.getId())
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
    public ClassroomOfferingResponse toPublicOfferingDetailResponse(ClassSection offering) {
        ClassroomOfferingResponse response = toOfferingResponse(offering, true, null, null, true);
        response.setEnrollments(null);
        response.setTeacherGuide(null);
        if (response.getInstructorLedCourse() != null) {
            response.getInstructorLedCourse().setTeacherGuide(null);
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
        session.setGoogleMeetUrl(null);
        session.setGoogleMeetJoinable(false);
        session.setRecordingUrl(null);
        session.setRecordingVisible(false);
        session.setNote(null);
    }

    private boolean isActiveTeacherAssignment(ClassroomTeacherAssignment assignment) {
        LocalDate today = LocalDate.now();
        return (assignment.getEffectiveFrom() == null || !assignment.getEffectiveFrom().isAfter(today))
                && (assignment.getEffectiveTo() == null || !assignment.getEffectiveTo().isBefore(today));
    }

    public ClassroomSessionResponse toSessionResponse(ClassSchedule session) {
        return toSessionResponse(session, false);
    }

    public ClassroomSessionResponse toManagerSessionResponse(ClassSchedule session) {
        return toSessionResponse(session, true);
    }

    private ClassroomSessionResponse toSessionResponse(ClassSchedule session, boolean includeHiddenRecording) {
        User teacher = session.getEffectiveTeacher();
        CourseLesson courseLesson = session.getCourseLesson();
        fu.sep490.g23.backend.entity.course.CourseUnit courseUnit =
                courseLesson == null ? null : courseLesson.getCourseUnit();
        ClassSection section = session.getClassSection();
        boolean recordingAvailable = Boolean.TRUE.equals(session.getRecordingVisible());
        ClassroomDeliveryMode effectiveDeliveryMode = session.getEffectiveDeliveryMode();
        Room effectiveRoom = session.getEffectiveRoom();
        boolean googleMeetJoinable = virtualMeetingService.isJoinable(section);

        return ClassroomSessionResponse.builder()
                .id(session.getId())
                .classSectionId(section.getId())
                .classroomTitle(section.getName())
                .sessionDate(session.getSessionDate())
                .startTime(session.getStartTime())
                .endTime(session.getEndTime())
                .teacherId(teacher == null ? null : teacher.getId())
                .teacherName(teacher == null ? null : teacher.getFullName())
                .status(session.getStatus())
                .deliveryModeOverride(session.getDeliveryModeOverride())
                .effectiveDeliveryMode(effectiveDeliveryMode)
                .deliveryModeLabel(deliveryModeLabel(effectiveDeliveryMode))
                .roomId(effectiveRoom == null ? null : effectiveRoom.getId())
                .roomName(effectiveRoom == null ? null : effectiveRoom.getName())
                .offlineAddress(effectiveRoom == null ? null : effectiveRoom.getLocationAddress())
                .googleMeetUrl(section.getGoogleMeetUrl())
                .googleMeetStatus(section.getGoogleMeetStatus())
                .googleMeetJoinable(googleMeetJoinable)
                .recordingUrl(includeHiddenRecording || recordingAvailable ? session.getRecordingUrl() : null)
                .recordingVisible(includeHiddenRecording ? Boolean.TRUE.equals(session.getRecordingVisible()) : recordingAvailable)
                .recordingStatus(session.getRecordingStatus())
                .recordingSyncedAt(session.getRecordingSyncedAt())
                .recordingLastAttemptAt(session.getRecordingLastAttemptAt())
                .recordingSyncError(includeHiddenRecording ? session.getRecordingSyncError() : null)
                .recordingSyncAttempts(session.getRecordingSyncAttempts())
                .sessionContent(session.getSessionContent())
                .courseLessonId(courseLesson == null ? null : courseLesson.getId())
                .courseLessonSequenceNumber(courseLesson == null ? null : courseLesson.getSequenceNumber())
                .courseLessonTitle(courseLesson == null ? null : courseLesson.getTitle())
                .courseLessonDescription(courseLesson == null ? null : courseLesson.getDescription())
                .learningObjectives(courseLesson == null ? null : courseLesson.getLearningObjectives())
                .courseUnitId(courseUnit == null ? null : courseUnit.getId())
                .courseUnitSequenceNumber(courseUnit == null ? null : courseUnit.getSequenceNumber())
                .courseUnitTitle(courseUnit == null ? null : courseUnit.getTitle())
                .note(session.getNote())
                .cancelled(session.getStatus() == ClassroomSessionStatus.CANCELLED)
                .build();
    }

    public ClassroomEnrollmentResponse toEnrollmentResponse(ClassEnrollment enrollment) {
        User confirmedBy = enrollment.getConfirmedBy();
        User assignedBy = enrollment.getAssignedBy();
        User tuitionRecordedBy = enrollment.getTuitionRecordedBy();
        ClassSection offering = enrollment.getClassSection();
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
                .classSectionId(offering.getId())
                .classroomTitle(offering.getTitle())
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
                .sessionId(assignment.getClassSchedule() == null ? null : assignment.getClassSchedule().getId())
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
                .classSectionId(request.getClassSection().getId())
                .classroomTitle(request.getClassSection().getTitle())
                .targetSessionId(request.getTargetClassSchedule() == null ? null : request.getTargetClassSchedule().getId())
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
        ClassSchedule session = attendance.getSession();
        ClassSection offering = session.getClassSection();
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
                .classroomTitle(offering.getTitle())
                .classSectionId(offering.getId())
                .deliveryMode(session.getEffectiveDeliveryMode() != null ? session.getEffectiveDeliveryMode().name() : null)
                .roomName(session.getEffectiveRoom() != null ? session.getEffectiveRoom().getName() : null)
                .build();
    }

    /**
     * Build a placeholder attendance response for an enrolled student who does not
     * yet have an attendance record for the given session.
     */
    public ClassroomAttendanceResponse toPlaceholderAttendanceResponse(ClassSchedule session, User student) {
        ClassSection offering = session.getClassSection();
        return ClassroomAttendanceResponse.builder()
                .sessionId(session.getId())
                .studentId(student.getId())
                .studentName(student.getFullName())
                .studentEmail(student.getEmail())
                .sessionDate(session.getSessionDate())
                .startTime(session.getStartTime())
                .endTime(session.getEndTime())
                .classroomTitle(offering.getTitle())
                .classSectionId(offering.getId())
                .deliveryMode(session.getEffectiveDeliveryMode() != null ? session.getEffectiveDeliveryMode().name() : null)
                .roomName(session.getEffectiveRoom() != null ? session.getEffectiveRoom().getName() : null)
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
                .classSectionId(homework.getClassSection().getId())
                .sessionId(homework.getSession() == null ? null : homework.getSession().getId())
                .courseUnitId(homework.getCourseUnit() == null ? null : homework.getCourseUnit().getId())
                .courseUnitTitle(homework.getCourseUnit() == null ? null : homework.getCourseUnit().getTitle())
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
                .courseUnitId(material.getCourseUnit() == null ? null : material.getCourseUnit().getId())
                .courseUnitTitle(material.getCourseUnit() == null ? null : material.getCourseUnit().getTitle())
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

    private String resolveTargetScore(InstructorLedCourse curriculum) {
        if (curriculum == null) {
            return null;
        }
        if (curriculum.getTargetBand() != null) {
            return curriculum.getTargetBand().stripTrailingZeros().toPlainString();
        }
        return curriculum.getTargetScore() == null ? null : String.valueOf(curriculum.getTargetScore());
    }

    private InstructorLedCourseResponse toInstructorLedCourseResponse(InstructorLedCourse course, boolean includeUnits) {
        if (course == null) {
            return null;
        }
        List<CourseUnit> units = course.getUnits() == null ? List.of() : course.getUnits();
        return InstructorLedCourseResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .code(course.getCode())
                .examCategory(course.getExamType())
                .focusSkills(course.getFocusSkills())
                .targetBand(course.getTargetBand())
                .targetScore(course.getTargetScore())
                .entryLevel(course.getEntryLevel())
                .entryPlacementLevel(course.getEntryPlacementLevel())
                .outcomes(course.getLearningOutcomes())
                .teacherGuide(course.getTeacherGuide())
                .totalSessions(units.stream()
                        .map(CourseUnit::getLessons)
                        .filter(Objects::nonNull)
                        .flatMap(List::stream)
                        .mapToInt(lesson -> Math.max(1, Objects.requireNonNullElse(lesson.getPlannedSessionCount(), 1)))
                        .sum())
                .totalLessons(units.stream()
                        .map(CourseUnit::getLessons)
                        .filter(Objects::nonNull)
                        .mapToInt(List::size)
                        .sum())
                .totalUnits(units.size())
                .status(course.getPublicationStatus().name())
                .reviewNote(course.getReviewNote())
                .submittedByName(course.getCreatedBy() == null ? null : course.getCreatedBy().getFullName())
                .submittedAt(course.getSubmittedAt())
                .reviewedByName(course.getReviewedBy() == null ? null : course.getReviewedBy().getFullName())
                .reviewedAt(course.getReviewedAt())
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .units(includeUnits ? units.stream().map(this::toCourseUnitResponse).toList() : null)
                .build();
    }

    private CourseUnitResponse toCourseUnitResponse(CourseUnit unit) {
        List<CourseUnitContentRef> refs = unit.getContentRefs() == null ? List.of() : unit.getContentRefs();
        return CourseUnitResponse.builder()
                .id(unit.getId())
                .programId(unit.getInstructorLedCourse().getId())
                .displayOrder(unit.getSequenceNumber())
                .title(unit.getTitle())
                .description(unit.getDescription())
                .lessons(unit.getLessons() == null ? List.of() : unit.getLessons().stream()
                        .map(this::toCourseLessonResponse)
                        .toList())
                .createdAt(unit.getCreatedAt())
                .updatedAt(unit.getUpdatedAt())
                .materials(filterContentRefs(refs, "MATERIAL"))
                .exercises(filterContentRefs(refs, "EXERCISE"))
                .assessments(filterContentRefs(refs, "ASSESSMENT"))
                .flashcards(filterContentRefs(refs, "FLASHCARD"))
                .build();
    }

    private CourseLessonResponse toCourseLessonResponse(CourseLesson lesson) {
        CourseUnit unit = lesson.getCourseUnit();
        return CourseLessonResponse.builder()
                .id(lesson.getId())
                .unitId(unit.getId())
                .unitTitle(unit.getTitle())
                .programId(unit.getInstructorLedCourse().getId())
                .sessionNumber(lesson.getSequenceNumber())
                .displayOrder(lesson.getSequenceNumber())
                .plannedSessionCount(Math.max(1, Objects.requireNonNullElse(lesson.getPlannedSessionCount(), 1)))
                .title(lesson.getTitle())
                .description(lesson.getDescription())
                .learningObjectives(lesson.getLearningObjectives())
                .createdAt(lesson.getCreatedAt())
                .updatedAt(lesson.getUpdatedAt())
                .build();
    }

    private List<CourseUnitContentRefResponse> filterContentRefs(
            List<CourseUnitContentRef> refs,
            String contentType
    ) {
        return refs.stream()
                .filter(ref -> ref.getContentType() != null && ref.getContentType().name().equals(contentType))
                .map(this::toCourseUnitContentRefResponse)
                .toList();
    }

    private CourseUnitContentRefResponse toCourseUnitContentRefResponse(CourseUnitContentRef ref) {
        if (ref.getLearningResource() != null) {
            CenterMaterialLibraryItem resource = ref.getLearningResource();
            return CourseUnitContentRefResponse.builder()
                    .id(ref.getId())
                    .type(ref.getContentType().name())
                    .resourceId(resource.getId())
                    .title(resource.getTitle())
                    .subtitle(resource.getMaterialType())
                    .skill(resource.getSkill())
                    .status(resource.getStatus())
                    .fileUrl(resource.getFileUrl())
                    .displayOrder(ref.getSequenceNumber())
                    .build();
        }
        var item = ref.getContentBankItem();
        String subtitle = item == null ? null : switch (ref.getContentType()) {
            case ASSESSMENT -> payloadText(item, "type");
            case EXERCISE -> payloadText(item, "exerciseType");
            case FLASHCARD, MATERIAL -> item.getExamCategory();
        };
        String contentJson = item == null ? null : switch (ref.getContentType()) {
            case EXERCISE -> payloadText(item, "prompt");
            case FLASHCARD -> serializePayload(item);
            case ASSESSMENT -> payloadText(item, "uiConfigJson");
            case MATERIAL -> null;
        };
        return CourseUnitContentRefResponse.builder()
                .id(ref.getId())
                .type(ref.getContentType().name())
                .resourceId(item == null ? null : item.getId())
                .title(item == null ? null : item.getTitle())
                .subtitle(subtitle)
                .skill(item == null ? null : item.getSkill())
                .status(item == null ? null : item.getStatus())
                .displayOrder(ref.getSequenceNumber())
                .contentJson(contentJson)
                .build();
    }

    private String payloadText(fu.sep490.g23.backend.entity.curriculum.ContentBankItem item, String... keys) {
        if (item.getContentData() == null) {
            return null;
        }
        for (String key : keys) {
            Object value = item.getContentData().get(key);
            if (value != null) {
                return String.valueOf(value);
            }
        }
        return null;
    }

    private String serializePayload(fu.sep490.g23.backend.entity.curriculum.ContentBankItem item) {
        try {
            return CONTENT_JSON_MAPPER.writeValueAsString(item.getContentData());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Không thể chuyển nội dung kho học liệu sang JSON.", exception);
        }
    }

    private boolean isMandatoryMaterial(String sourceType) {
        return "CURRICULUM_LIBRARY".equalsIgnoreCase(sourceType);
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
            case RECREATE_GOOGLE_MEET -> "Tạo lại liên kết Google Meet";
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

    private ClassroomSessionResponse resolveNextSession(List<ClassSchedule> sessions) {
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

    private ScheduleSummary computeScheduleSummary(ClassSection offering) {
        List<ClassSchedule> allSessions;
        try {
            allSessions = offering.getSchedules();
        } catch (RuntimeException exception) {
            return null;
        }
        List<ClassSchedule> active = allSessions.stream()
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
            ClassSection offering,
            List<ClassSchedule> sessions,
            boolean includeSessions
    ) {
        if (includeSessions) {
            return percentFromSessions(sessions);
        }
        if (offering.getId() == null || sessionRepository == null) {
            return 0;
        }
        long total = sessionRepository.countByClassSectionIdAndStatusNot(
                offering.getId(),
                ClassroomSessionStatus.CANCELLED
        );
        if (total == 0) {
            return 0;
        }
        long completed = sessionRepository.countByClassSectionIdAndStatus(
                offering.getId(),
                ClassroomSessionStatus.COMPLETED
        );
        return (int) Math.round((completed * 100.0) / total);
    }

    private Integer percentFromSessions(List<ClassSchedule> sessions) {
        List<ClassSchedule> counted = sessions.stream()
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
