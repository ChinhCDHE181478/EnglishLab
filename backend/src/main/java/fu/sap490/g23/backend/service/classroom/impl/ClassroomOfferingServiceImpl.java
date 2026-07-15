package fu.sap490.g23.backend.service.classroom.impl;

import fu.sap490.g23.backend.service.classroom.*;


import fu.sap490.g23.backend.dto.request.classroom.*;
import fu.sap490.g23.backend.dto.response.classroom.*;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.classroom.*;
import fu.sap490.g23.backend.entity.classroom.enums.*;
import fu.sap490.g23.backend.entity.course.*;
import fu.sap490.g23.backend.entity.course.enums.*;
import fu.sap490.g23.backend.entity.curriculum.CurriculumProgram;
import fu.sap490.g23.backend.repository.UserRepository;
import fu.sap490.g23.backend.repository.classroom.*;
import fu.sap490.g23.backend.repository.course.LearningPackageRepository;
import fu.sap490.g23.backend.repository.course.PackageEnrollmentRepository;
import fu.sap490.g23.backend.repository.course.PackageTypeRepository;
import fu.sap490.g23.backend.repository.curriculum.CurriculumProgramRepository;
import fu.sap490.g23.backend.security.ClassroomAccessHelper;
import fu.sap490.g23.backend.service.course.CourseEnrollmentAccessPolicy;
import fu.sap490.g23.backend.service.notification.ClassroomNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ClassroomOfferingServiceImpl implements ClassroomOfferingService {

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    private static final Set<ClassroomRegistrationStatus> OCCUPIES_CLASS_SLOT = ClassroomRegistrationSupport.OCCUPIES_CLASS_SLOT;
    private static final Set<ClassroomRegistrationStatus> ACTIVE_REGISTRATIONS = ClassroomRegistrationSupport.ACTIVE_REGISTRATIONS;
    private static final int EMPTY_ROOM_GRACE_MINUTES = 5;

    private final ClassroomOfferingRepository offeringRepository;
    private final ClassroomSessionRepository sessionRepository;
    private final ClassroomEnrollmentRepository enrollmentRepository;
    private final ClassroomTuitionPaymentRepository tuitionPaymentRepository;
    private final ClassroomTeacherAssignmentRepository teacherAssignmentRepository;
    private final ClassroomGradebookEntryRepository gradebookEntryRepository;
    private final LearningPackageRepository learningPackageRepository;
    private final PackageTypeRepository packageTypeRepository;
    private final PackageEnrollmentRepository packageEnrollmentRepository;
    private final CurriculumProgramRepository curriculumProgramRepository;
    private final TrainingProgramRepository trainingProgramRepository;
    private final ClassroomMaterialSyncService classroomMaterialSyncService;
    private final ClassroomRoomRepository roomRepository;
    private final UserRepository userRepository;
    private final ClassroomMapper mapper;
    private final ClassroomConflictService conflictService;
    private final ClassroomScheduleLockService scheduleLockService;
    private final LarkMeetingService larkMeetingService;
    private final ClassroomAccessHelper accessHelper;
    private final ClassroomNotificationService notificationService;
    private final LarkMeetingParticipantRepository larkParticipantRepository;
    private final CourseEnrollmentAccessPolicy courseEnrollmentAccessPolicy;
    private final VirtualAttendanceService virtualAttendanceService;

    @Override
    @Transactional(readOnly = true)
    public Page<ClassroomOfferingResponse> getPublicOfferings(ClassroomDeliveryMode mode, Pageable pageable) {
        return offeringRepository.findPublished(mode, pageable)
                .map(mapper::toOfferingResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ClassroomOfferingResponse getPublicOffering(String slugOrId) {
        ClassroomOffering offering = findPublicOffering(slugOrId);
        return mapper.toPublicOfferingDetailResponse(offering);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomOfferingResponse> getMyClasses(String learnerEmail) {
        User learner = accessHelper.requireUser(learnerEmail);
        return enrollmentRepository.findByStudentIdAndRegistrationStatusIn(learner.getId(), ACTIVE_REGISTRATIONS).stream()
                .map(ClassroomEnrollment::getClassroomOffering)
                .map(offering -> mapper.toOfferingResponse(
                        offering,
                        false,
                        learner.getId(),
                        enrollmentRepository.findByStudentIdAndClassroomOfferingId(learner.getId(), offering.getId()).orElse(null),
                        false
                ))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomOfferingResponse> getAssignedClasses(String teacherEmail) {
        User teacher = accessHelper.requireUser(teacherEmail);
        accessHelper.assertTeacher(teacher);
        return teacherAssignmentRepository.findByTeacherId(teacher.getId()).stream()
                .filter(this::isTeacherAssignmentActive)
                .map(ClassroomTeacherAssignment::getClassroomOffering)
                .distinct()
                .map(mapper::toOfferingResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomOfferingResponse> getManagerOfferings() {
        return offeringRepository.findAll().stream()
                .filter(this::isVisibleToTrainingManager)
                .map(offering -> mapper.toOfferingResponse(offering, true, null, null, true))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ClassroomOfferingResponse getManagerOffering(Long id) {
        ClassroomOffering offering = offeringRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp học."));
        if (offering.getLearningPackage() == null) {
            throw new RuntimeException("Không tìm thấy lớp học.");
        }
        return mapper.toOfferingResponse(offering, true, null, null, true);
    }

    @Override
    @Transactional(readOnly = true)
    public ClassroomOfferingResponse getOffering(Long id, boolean full) {
        ClassroomOffering offering = findOffering(id);
        return mapper.toOfferingResponse(offering, full, null, null, full);
    }

    @Override
    @Transactional(readOnly = true)
    public ClassroomOfferingResponse getLearnerOffering(Long id, String learnerEmail) {
        assertLearnerPortalAccess(id, learnerEmail);
        return getOffering(id, true);
    }

    private void assertLearnerPortalAccess(Long offeringId, String learnerEmail) {
        User learner = accessHelper.requireUser(learnerEmail);
        enrollmentRepository.findByStudentIdAndClassroomOfferingId(learner.getId(), offeringId)
                .filter(enrollment -> ACTIVE_REGISTRATIONS.contains(enrollment.getRegistrationStatus()))
                .orElseThrow(() -> new RuntimeException("Bạn không có quyền truy cập lớp học này."));
    }

    @Override
    public ClassroomOfferingResponse createOffering(CreateClassroomOfferingRequest request, String creatorEmail) {
        User creator = accessHelper.requireUser(creatorEmail);
        accessHelper.assertManager(creator);

        PackageType packageType = packageTypeRepository.findByCode(PackageTypeCode.CLASSROOM)
                .orElseThrow(() -> new RuntimeException("Thiếu loại gói CLASSROOM trong hệ thống."));
        CurriculumProgram curriculumProgram = resolveCurriculumProgram(
                request.getCurriculumProgramId(),
                request.getDeliveryMode()
        );
        TrainingProgram trainingProgram = resolveTrainingProgram(request.getTrainingProgramId(), request.getDeliveryMode());
        if (trainingProgram != null) {
            curriculumProgram = trainingProgram.getCurriculumProgram();
        }

        LearningPackage learningPackage = LearningPackage.builder()
                .packageType(packageType)
                .title(request.getTitle().trim())
                .slug(generateUniqueSlug(request.getTitle()))
                .shortDescription(defaultText(request.getShortDescription(), trainingProgram == null ? null : trainingProgram.getShortDescription()))
                .description(defaultText(request.getDescription(), trainingProgram == null ? null : trainingProgram.getDescription()))
                .targetScore(defaultText(request.getTargetScore(), trainingProgram == null ? null : trainingProgram.getTargetScore()))
                .duration(defaultText(request.getDuration(), trainingProgram == null ? null : trainingProgram.getDuration()))
                .studyMode(defaultText(request.getStudyMode(), trainingProgram == null ? null : trainingProgram.getStudyMode()))
                .price(defaultBigDecimal(request.getPrice() != null ? request.getPrice() : trainingProgram == null ? null : trainingProgram.getPrice()))
                .salePrice(request.getSalePrice() != null ? request.getSalePrice() : trainingProgram == null ? null : trainingProgram.getSalePrice())
                .thumbnailUrl(defaultText(request.getThumbnailUrl(), trainingProgram == null ? null : trainingProgram.getThumbnailUrl()))
                .status(request.getPackageStatus() == null ? PackageStatus.DRAFT : request.getPackageStatus())
                .displayOrder(defaultInt(request.getDisplayOrder()))
                .featured(Boolean.TRUE.equals(request.getFeatured()))
                .createdBy(creator)
                .build();


        ClassroomOffering offering = ClassroomOffering.builder()
                .learningPackage(learningPackage)
                .deliveryMode(request.getDeliveryMode())
                .trainingProgram(trainingProgram)
                .curriculumProgram(curriculumProgram)
                .status(request.getClassroomStatus() == null ? ClassroomOfferingStatus.DRAFT : request.getClassroomStatus())
                .entryLevel(defaultText(request.getEntryLevel(), trainingProgram == null ? null : trainingProgram.getEntryLevel()))
                .targetOutcome(defaultText(request.getTargetOutcome(), trainingProgram == null ? null : trainingProgram.getTargetOutcome()))
                .maxCapacity(request.getMaxCapacity() == null ? (trainingProgram == null ? 30 : trainingProgram.getDefaultCapacity()) : request.getMaxCapacity())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .primaryTeacher(resolveTeacher(request.getPrimaryTeacherId()))
                .defaultRoom(resolveRoom(request.getDefaultRoomId()))
                .offlineAddress(request.getOfflineAddress())
                .locationNote(request.getLocationNote())
                .defaultLarkMeetingUrl(request.getDefaultLarkMeetingUrl())
                .larkMeetingStatus(larkMeetingService.resolveStatus(request.getDefaultLarkMeetingUrl()))
                .recordingUrl(request.getRecordingUrl())
                .recordingVisible(Boolean.TRUE.equals(request.getRecordingVisible()))
                .syllabusSummary(defaultText(request.getSyllabusSummary(), trainingProgram == null ? null : trainingProgram.getSyllabusSummary()))
                .programOutcomes(trainingProgram == null ? null : trainingProgram.getProgramOutcomes())
                .teacherGuide(trainingProgram == null ? null : trainingProgram.getTeacherGuide())
                .interactionActivities(trainingProgram == null ? null : trainingProgram.getInteractionActivities())
                .build();

        if (offering.getPrimaryTeacher() != null) {
            offering = offeringRepository.save(offering);
            assignTeacherInternal(offering, offering.getPrimaryTeacher(), ClassroomTeacherRole.PRIMARY, "Giáo viên chính khi tạo lớp");
            classroomMaterialSyncService.synchronizeMandatoryMaterials(offering, creator);
            return mapper.toOfferingResponse(offering, true, null, null, true);
        }

        ClassroomOffering saved = offeringRepository.save(offering);
        classroomMaterialSyncService.synchronizeMandatoryMaterials(saved, creator);
        return mapper.toOfferingResponse(saved, true, null, null, true);
    }

    @Override
    public ClassroomOfferingResponse updateOffering(Long id, CreateClassroomOfferingRequest request, String actorEmail) {
        User actor = accessHelper.requireUser(actorEmail);
        accessHelper.assertTrainingManager(actor);
        ClassroomOffering offering = findOffering(id);
        LearningPackage learningPackage = offering.getLearningPackage();
        CurriculumProgram curriculumProgram = resolveCurriculumProgram(
                request.getCurriculumProgramId(),
                request.getDeliveryMode()
        );
        TrainingProgram trainingProgram = resolveTrainingProgram(request.getTrainingProgramId(), request.getDeliveryMode());
        if (trainingProgram != null) {
            curriculumProgram = trainingProgram.getCurriculumProgram();
        }

        if (Set.of(ClassroomOfferingStatus.ACTIVE, ClassroomOfferingStatus.COMPLETED, ClassroomOfferingStatus.CLOSED)
                .contains(offering.getStatus())) {
            Long currentTrainingProgramId = offering.getTrainingProgram() == null
                    ? null : offering.getTrainingProgram().getId();
            Long nextTrainingProgramId = trainingProgram == null ? null : trainingProgram.getId();
            Long currentCurriculumId = offering.getCurriculumProgram() == null
                    ? null : offering.getCurriculumProgram().getId();
            Long nextCurriculumId = curriculumProgram == null ? null : curriculumProgram.getId();
            if (!Objects.equals(currentTrainingProgramId, nextTrainingProgramId)
                    || !Objects.equals(currentCurriculumId, nextCurriculumId)
                    || offering.getDeliveryMode() != request.getDeliveryMode()) {
                throw new IllegalArgumentException(
                        "Không thể đổi chương trình hoặc hình thức đào tạo khi lớp đã bắt đầu hoặc đã kết thúc."
                );
            }
        }


        learningPackage.setTitle(request.getTitle().trim());
        learningPackage.setShortDescription(defaultText(request.getShortDescription(), trainingProgram == null ? null : trainingProgram.getShortDescription()));
        learningPackage.setDescription(defaultText(request.getDescription(), trainingProgram == null ? null : trainingProgram.getDescription()));
        learningPackage.setTargetScore(defaultText(request.getTargetScore(), trainingProgram == null ? null : trainingProgram.getTargetScore()));
        learningPackage.setDuration(defaultText(request.getDuration(), trainingProgram == null ? null : trainingProgram.getDuration()));
        learningPackage.setStudyMode(defaultText(request.getStudyMode(), trainingProgram == null ? null : trainingProgram.getStudyMode()));
        learningPackage.setPrice(defaultBigDecimal(request.getPrice() != null ? request.getPrice() : trainingProgram == null ? null : trainingProgram.getPrice()));
        learningPackage.setSalePrice(request.getSalePrice() != null ? request.getSalePrice() : trainingProgram == null ? null : trainingProgram.getSalePrice());
        learningPackage.setThumbnailUrl(defaultText(request.getThumbnailUrl(), trainingProgram == null ? null : trainingProgram.getThumbnailUrl()));
        if (request.getPackageStatus() != null) {
            learningPackage.setStatus(request.getPackageStatus());
        }
        learningPackage.setDisplayOrder(defaultInt(request.getDisplayOrder()));
        learningPackage.setFeatured(Boolean.TRUE.equals(request.getFeatured()));

        offering.setDeliveryMode(request.getDeliveryMode());
        offering.setTrainingProgram(trainingProgram);
        offering.setCurriculumProgram(curriculumProgram);
        if (request.getClassroomStatus() != null) {
            offering.setStatus(request.getClassroomStatus());
        }
        offering.setEntryLevel(defaultText(request.getEntryLevel(), trainingProgram == null ? null : trainingProgram.getEntryLevel()));
        offering.setTargetOutcome(defaultText(request.getTargetOutcome(), trainingProgram == null ? null : trainingProgram.getTargetOutcome()));
        if (request.getMaxCapacity() != null) {
            offering.setMaxCapacity(request.getMaxCapacity());
        } else if (trainingProgram != null) {
            offering.setMaxCapacity(trainingProgram.getDefaultCapacity());
        }
        offering.setStartDate(request.getStartDate());
        offering.setEndDate(request.getEndDate());
        offering.setPrimaryTeacher(resolveTeacher(request.getPrimaryTeacherId()));
        offering.setDefaultRoom(resolveRoom(request.getDefaultRoomId()));
        offering.setOfflineAddress(request.getOfflineAddress());
        offering.setLocationNote(request.getLocationNote());
        offering.setDefaultLarkMeetingUrl(request.getDefaultLarkMeetingUrl());
        offering.setLarkMeetingStatus(larkMeetingService.resolveStatus(request.getDefaultLarkMeetingUrl()));
        offering.setRecordingUrl(request.getRecordingUrl());
        if (request.getRecordingVisible() != null) {
            offering.setRecordingVisible(request.getRecordingVisible());
        }
        offering.setSyllabusSummary(defaultText(request.getSyllabusSummary(), trainingProgram == null ? null : trainingProgram.getSyllabusSummary()));
        if (trainingProgram != null) {
            offering.setProgramOutcomes(trainingProgram.getProgramOutcomes());
            offering.setTeacherGuide(trainingProgram.getTeacherGuide());
            offering.setInteractionActivities(trainingProgram.getInteractionActivities());
        }

        ClassroomOffering saved = offeringRepository.save(offering);
        classroomMaterialSyncService.synchronizeMandatoryMaterials(saved, null);
        return mapper.toOfferingResponse(saved, true, null, null, true);
    }

    @Override
    public ClassroomOfferingResponse publishOffering(Long id) {
        ClassroomOffering offering = findOffering(id);
        validatePublishableOffering(offering);
        offering.getLearningPackage().setStatus(PackageStatus.PUBLISHED);
        if (offering.getStatus() == ClassroomOfferingStatus.DRAFT) {
            offering.setStatus(ClassroomOfferingStatus.UPCOMING);
        }
        return mapper.toOfferingResponse(offering);
    }

    @Override
    public ClassroomOfferingResponse closeOffering(Long id, String actorEmail) {
        accessHelper.requireUser(actorEmail);
        ClassroomOffering offering = findOffering(id);
        if (offering.getStatus() == ClassroomOfferingStatus.CLOSED
                || offering.getStatus() == ClassroomOfferingStatus.CANCELLED) {
            throw new RuntimeException("Lớp học đã được đóng hoặc hủy trước đó.");
        }
        offering.setStatus(ClassroomOfferingStatus.CLOSED);
        offering.getLearningPackage().setStatus(PackageStatus.ARCHIVED);
        return mapper.toOfferingResponse(offeringRepository.save(offering));
    }

    private void validatePublishableOffering(ClassroomOffering offering) {
        if (offering.getStartDate() == null) {
            throw new RuntimeException("Lớp học cần có ngày khai giảng trước khi xuất bản.");
        }
        if (offering.getMaxCapacity() == null || offering.getMaxCapacity() <= 0) {
            throw new RuntimeException("Lớp học cần có sĩ số tối đa hợp lệ trước khi xuất bản.");
        }
        long sessionCount = sessionRepository.countByClassroomOfferingId(offering.getId());
        if (sessionCount <= 0) {
            throw new RuntimeException("Lớp học cần có ít nhất một buổi học trước khi xuất bản.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomSessionResponse> getSessions(Long offeringId) {
        return sessionRepository.findByClassroomOfferingIdOrderBySessionDateAscStartTimeAsc(offeringId).stream()
                .map(mapper::toSessionResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomSessionResponse> getLearnerSessions(Long offeringId, String learnerEmail) {
        User learner = accessHelper.requireUser(learnerEmail);
        ClassroomEnrollment enrollment = enrollmentRepository
                .findByStudentIdAndClassroomOfferingId(learner.getId(), offeringId)
                .orElseThrow(() -> new RuntimeException("Bạn không thuộc lớp học này."));
        if (!enrollment.hasClassAccess()) {
            throw new RuntimeException("Bạn chưa được cấp quyền truy cập lớp học này.");
        }

        return sessionRepository.findByClassroomOfferingIdOrderBySessionDateAscStartTimeAsc(offeringId).stream()
                .map(mapper::toSessionResponse)
                .peek(response -> {
                    response.setLarkMeetingUrl(null);
                    response.setLarkJoinable(false);
                })
                .toList();
    }

    @Override
    public ClassroomSessionResponse createSession(Long offeringId, CreateClassroomSessionRequest request) {
        return createSession(offeringId, request, true);
    }

    @Override
    public ClassroomSessionResponse createSession(
            Long offeringId,
            CreateClassroomSessionRequest request,
            boolean enforceConflictCheck
    ) {
        scheduleLockService.lockDate(request.getSessionDate());

        ClassroomOffering offering = findOffering(offeringId);
        User teacher = resolveTeacher(request.getTeacherId() != null ? request.getTeacherId() : getPrimaryTeacherId(offering));
        Long roomId = request.getRoomId() != null ? request.getRoomId() : getDefaultRoomId(offering);

        if (enforceConflictCheck) {
            ConflictCheckRequest conflictRequest = ConflictCheckRequest.builder()
                    .classroomOfferingId(offeringId)
                    .teacherId(teacher == null ? null : teacher.getId())
                    .roomId(roomId)
                    .sessionDate(request.getSessionDate())
                    .startTime(request.getStartTime())
                    .endTime(request.getEndTime())
                    .learnerIds(resolveActiveLearnerIds(offeringId))
                    .build();
            conflictService.assertNoBlockingConflict(conflictRequest);
        }

        ClassroomSession session = buildSession(offering, request, teacher);
        session = sessionRepository.save(session);
        if (session.getDeliveryMode() == ClassroomDeliveryMode.VIRTUAL
                && (session.getLarkMeetingUrl() == null
                || session.getLarkMeetingUrl().isBlank()
                || larkMeetingService.isDemoUrl(session.getLarkMeetingUrl()))) {
            syncLarkMeetingSafely(session);
            session = sessionRepository.save(session);
        }
        return mapper.toSessionResponse(session);
    }

    @Override
    public ClassroomSessionResponse updateSession(Long sessionId, CreateClassroomSessionRequest request) {
        ClassroomSession session = findSession(sessionId);
        scheduleLockService.lockDates(List.of(session.getSessionDate(), request.getSessionDate()));
        if (session.isLocked() || session.getStatus() == ClassroomSessionStatus.COMPLETED) {
            throw new RuntimeException("Buổi học đã hoàn thành hoặc đã khóa nên không thể chỉnh sửa.");
        }

        User teacher = resolveTeacher(request.getTeacherId() != null ? request.getTeacherId() : getPrimaryTeacherId(session.getClassroomOffering()));
        Long roomId = request.getRoomId() != null ? request.getRoomId() : getDefaultRoomId(session.getClassroomOffering());

        ConflictCheckRequest conflictRequest = ConflictCheckRequest.builder()
                .classroomOfferingId(session.getClassroomOffering().getId())
                .sessionId(sessionId)
                .excludeSessionId(sessionId)
                .teacherId(teacher == null ? null : teacher.getId())
                .roomId(roomId)
                .sessionDate(request.getSessionDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .learnerIds(resolveActiveLearnerIds(session.getClassroomOffering().getId()))
                .checkSessionLocked(true)
                .build();
        conflictService.assertNoBlockingConflict(conflictRequest);

        boolean manualLarkLinkProvided = request.getLarkMeetingUrl() != null
                && !request.getLarkMeetingUrl().isBlank()
                && !larkMeetingService.isDemoUrl(request.getLarkMeetingUrl());
        if (manualLarkLinkProvided && session.getLarkEventId() != null) {
            deleteLarkMeetingSafely(session);
            clearManagedLarkData(session);
        }

        applySessionRequest(session, request, teacher);
        if (session.getDeliveryMode() == ClassroomDeliveryMode.VIRTUAL && !manualLarkLinkProvided) {
            syncLarkMeetingSafely(session);
        }
        return mapper.toSessionResponse(sessionRepository.save(session));
    }

    @Override
    public ClassroomSessionResponse applyApprovedSessionScheduleChange(Long sessionId, CreateClassroomSessionRequest request) {
        ClassroomSession session = findSession(sessionId);
        scheduleLockService.lockDates(List.of(session.getSessionDate(), request.getSessionDate()));
        if (session.isLocked() || session.getStatus() == ClassroomSessionStatus.COMPLETED) {
            throw new RuntimeException("Buổi học đã hoàn thành hoặc đã khóa nên không thể chỉnh sửa.");
        }

        User teacher = resolveTeacher(request.getTeacherId() != null ? request.getTeacherId() : getPrimaryTeacherId(session.getClassroomOffering()));
        boolean manualLarkLinkProvided = request.getLarkMeetingUrl() != null
                && !request.getLarkMeetingUrl().isBlank()
                && !larkMeetingService.isDemoUrl(request.getLarkMeetingUrl());
        if (manualLarkLinkProvided && session.getLarkEventId() != null) {
            deleteLarkMeetingSafely(session);
            clearManagedLarkData(session);
        }

        applySessionRequest(session, request, teacher);
        if (session.getDeliveryMode() == ClassroomDeliveryMode.VIRTUAL && !manualLarkLinkProvided) {
            syncLarkMeetingSafely(session);
        }
        if (session.getStatus() == ClassroomSessionStatus.RESCHEDULED) {
            session.setStatus(ClassroomSessionStatus.SCHEDULED);
        }
        return mapper.toSessionResponse(sessionRepository.save(session));
    }

    @Override
    public void deleteSession(Long sessionId) {
        ClassroomSession session = findSession(sessionId);
        if (session.isLocked()) {
            throw new RuntimeException("Buổi học đã khóa nên không thể xóa.");
        }
        deleteLarkMeetingSafely(session);
        sessionRepository.delete(session);
    }

    @Override
    public ClassroomEnrollmentResponse enrollStudent(Long offeringId, EnrollStudentRequest request) {
        ClassroomOffering offering = findOffering(offeringId);
        User student = userRepository.findById(request.getStudentId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy học viên."));

        ConflictCheckRequest conflictRequest = ConflictCheckRequest.builder()
                .classroomOfferingId(offeringId)
                .learnerIds(List.of(student.getId()))
                .checkCapacity(false)
                .build();
        conflictService.assertNoBlockingConflict(conflictRequest);

        BigDecimal tuitionDue = resolveTuitionDue(offering);
        ClassroomEnrollment enrollment = enrollmentRepository
                .findByStudentIdAndClassroomOfferingId(student.getId(), offeringId)
                .orElseGet(() -> ClassroomEnrollment.builder()
                        .student(student)
                        .classroomOffering(offering)
                        .tuitionAmountDue(tuitionDue)
                        .tuitionAmountPaid(BigDecimal.ZERO)
                        .tuitionDepositPaid(BigDecimal.ZERO)
                        .note(request.getNote())
                        .build());
        ClassroomRegistrationStatus previousStatus = enrollment.getRegistrationStatus();

        enrollment.setTuitionAmountDue(tuitionDue);
        enrollment.setTuitionAmountPaid(tuitionDue);
        enrollment.setTuitionDepositPaid(BigDecimal.ZERO);
        ClassroomRegistrationSupport.clearOpenSettlement(enrollment);
        enrollment.setNote(request.getNote());
        enrollment.setPackageEnrollment(ensurePackageEnrollment(student, offering));

        if (isClassFull(offering) && !enrollment.hasClassAccess()) {
            enrollment.setRegistrationStatus(ClassroomRegistrationStatus.WAITLIST);
        } else if (!enrollment.hasClassAccess()) {
            tryAssignEnrollment(enrollment, offering, student, null, "Xếp lớp trực tiếp");
        }
        ClassroomRegistrationSupport.syncLegacyStatus(enrollment);
        enrollment = saveEnrollmentWithWaitlistOrder(enrollment, previousStatus);
        return mapper.toEnrollmentResponse(enrollment);
    }

    @Override
    public ClassroomEnrollmentResponse enrollStudentByEmail(Long offeringId, String studentEmail) {
        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy học viên."));
        EnrollStudentRequest request = new EnrollStudentRequest();
        request.setStudentId(student.getId());
        return enrollStudent(offeringId, request);
    }

    @Override
    public void removeStudent(Long offeringId, Long studentId) {
        ClassroomEnrollment enrollment = enrollmentRepository.findByStudentIdAndClassroomOfferingId(studentId, offeringId)
                .orElseThrow(() -> new RuntimeException("Học viên không thuộc lớp này."));
        ClassroomRegistrationStatus previousStatus = enrollment.getRegistrationStatus();
        enrollment.setRegistrationStatus(ClassroomRegistrationStatus.CANCELLED);
        ClassroomRegistrationSupport.syncLegacyStatus(enrollment);
        ClassroomRegistrationSupport.markNeedRefundForExit(enrollment, "Cần xử lý hoàn tiền do xóa khỏi lớp");
        saveEnrollmentWithWaitlistOrder(enrollment, previousStatus);
        notifyWaitlistIfSlotAvailable(enrollment.getClassroomOffering());
    }

    @Override
    @SuppressWarnings("deprecation")
    public ClassroomEnrollmentResponse transferStudent(Long offeringId, TransferStudentRequest request) {
        ClassroomOffering source = findOffering(offeringId);
        ClassroomOffering target = findOffering(request.getTargetClassroomOfferingId());
        User student = userRepository.findById(request.getStudentId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy học viên."));

        ClassroomEnrollment sourceEnrollment = enrollmentRepository.findByStudentIdAndClassroomOfferingId(student.getId(), offeringId)
                .filter(enrollment -> ACTIVE_REGISTRATIONS.contains(enrollment.getRegistrationStatus()))
                .orElseThrow(() -> new RuntimeException("Học viên không có đăng ký hợp lệ ở lớp nguồn."));

        ConflictCheckRequest conflictRequest = ConflictCheckRequest.builder()
                .targetClassroomOfferingId(target.getId())
                .learnerIds(List.of(student.getId()))
                .checkCapacity(false)
                .build();
        conflictService.assertNoBlockingConflict(conflictRequest);

        BigDecimal carriedPaid = sourceEnrollment.getTuitionAmountPaid() == null
                ? BigDecimal.ZERO
                : sourceEnrollment.getTuitionAmountPaid();
        BigDecimal targetDue = resolveTuitionDue(target);
        TuitionSettlementType settlementType = ClassroomRegistrationSupport.computeSettlement(targetDue, carriedPaid);
        String settlementNote = ClassroomRegistrationSupport.buildSettlementNote(settlementType, targetDue, carriedPaid);

        ClassroomRegistrationStatus sourcePreviousStatus = sourceEnrollment.getRegistrationStatus();
        sourceEnrollment.setRegistrationStatus(ClassroomRegistrationStatus.CANCELLED);
        sourceEnrollment.setNote(appendNote(sourceEnrollment.getNote(), "Đã chuyển sang lớp #" + target.getId()));
        ClassroomRegistrationSupport.syncLegacyStatus(sourceEnrollment);
        sourceEnrollment.setStatus(ClassroomEnrollmentStatus.TRANSFERRED);
        saveEnrollmentWithWaitlistOrder(sourceEnrollment, sourcePreviousStatus);

        if (enrollmentRepository.existsByStudentIdAndClassroomOfferingIdAndRegistrationStatusIn(
                student.getId(), target.getId(), ACTIVE_REGISTRATIONS)) {
            throw new RuntimeException("Học viên đã có đăng ký ở lớp đích.");
        }

        ClassroomEnrollment targetEnrollment = ClassroomEnrollment.builder()
                .student(student)
                .classroomOffering(target)
                .packageEnrollment(ensurePackageEnrollment(student, target))
                .holdSpot(sourceEnrollment.isHoldSpot())
                .tuitionAmountDue(targetDue)
                .tuitionAmountPaid(carriedPaid)
                .tuitionDepositPaid(sourceEnrollment.getTuitionDepositPaid())
                .tuitionSettlementType(settlementType)
                .tuitionSettlementNote(settlementNote)
                .tuitionSettlementStatus(settlementType == TuitionSettlementType.NONE
                        ? TuitionSettlementStatus.NONE
                        : TuitionSettlementStatus.PENDING)
                .transferredFromEnrollmentId(sourceEnrollment.getId())
                .note(appendNote(request.getNote(), settlementNote))
                .build();

        ClassroomRegistrationStatus paymentStatus = ClassroomRegistrationSupport.resolveRegistrationStatusAfterPayment(
                targetDue,
                carriedPaid,
                targetEnrollment.getTuitionDepositPaid(),
                null
        );
        targetEnrollment.setRegistrationStatus(paymentStatus);

        if (paymentStatus == ClassroomRegistrationStatus.FULLY_PAID) {
            tryAssignEnrollment(targetEnrollment, target, student, null, "Chuyển lớp");
        }

        ClassroomRegistrationSupport.syncLegacyStatus(targetEnrollment);
        targetEnrollment = saveEnrollmentWithWaitlistOrder(targetEnrollment, null);
        notifyWaitlistIfSlotAvailable(source);
        return mapper.toEnrollmentResponse(targetEnrollment);
    }

    @Override
    public ClassroomEnrollmentResponse registerForClass(Long offeringId, RegisterClassRequest request, String learnerEmail) {
        User learner = accessHelper.requireUser(learnerEmail);
        ClassroomOffering offering = findPublicOffering(String.valueOf(offeringId));
        assertOpenForRegistration(offering);

        if (enrollmentRepository.existsByStudentIdAndClassroomOfferingIdAndRegistrationStatusIn(
                learner.getId(), offeringId, ACTIVE_REGISTRATIONS)) {
            throw new RuntimeException("Bạn đã có đăng ký cho lớp này.");
        }

        ConflictCheckRequest conflictRequest = ConflictCheckRequest.builder()
                .classroomOfferingId(offeringId)
                .learnerIds(List.of(learner.getId()))
                .checkCapacity(false)
                .build();
        conflictService.assertNoBlockingConflict(conflictRequest);
        // Chặn ngay từ lúc đăng ký nếu học viên đang học lớp khác trùng lịch với các buổi của lớp này.
        assertLearnerScheduleForOffering(offering, learner.getId());

        BigDecimal tuitionDue = resolveTuitionDue(offering);
        ClassroomRegistrationStatus initialStatus = isClassFull(offering)
                ? ClassroomRegistrationStatus.WAITLIST
                : ClassroomRegistrationStatus.PENDING_TUITION_PAYMENT;

        var existingEnrollment = enrollmentRepository.findByStudentIdAndClassroomOfferingId(learner.getId(), offeringId);
        ClassroomEnrollment enrollment;
        if (existingEnrollment.isPresent()) {
            enrollment = existingEnrollment.get();
            ClassroomRegistrationStatus previousStatus = enrollment.getRegistrationStatus();
            enrollment.setHoldSpot(false);
            enrollment.setRegistrationStatus(initialStatus);
            enrollment.setTuitionAmountDue(tuitionDue);
            enrollment.setTuitionAmountPaid(BigDecimal.ZERO);
            enrollment.setTuitionDepositPaid(BigDecimal.ZERO);
            ClassroomRegistrationSupport.clearOpenSettlement(enrollment);
            enrollment.setNote(request == null ? null : request.getNote());
            ClassroomRegistrationSupport.syncLegacyStatus(enrollment);
            enrollment = saveEnrollmentWithWaitlistOrder(enrollment, previousStatus);
        } else {
            enrollment = ClassroomEnrollment.builder()
                    .student(learner)
                    .classroomOffering(offering)
                    .registrationStatus(initialStatus)
                    .holdSpot(false)
                    .tuitionAmountDue(tuitionDue)
                    .tuitionAmountPaid(BigDecimal.ZERO)
                    .tuitionDepositPaid(BigDecimal.ZERO)
                    .tuitionSettlementType(TuitionSettlementType.NONE)
                    .tuitionSettlementStatus(TuitionSettlementStatus.NONE)
                    .note(request == null ? null : request.getNote())
                    .build();
            ClassroomRegistrationSupport.syncLegacyStatus(enrollment);
            enrollment = saveEnrollmentWithWaitlistOrder(enrollment, null);
        }

        if (initialStatus == ClassroomRegistrationStatus.PENDING_TUITION_PAYMENT
                && tuitionDue.compareTo(BigDecimal.ZERO) <= 0) {
            ClassroomRegistrationStatus previousStatus = enrollment.getRegistrationStatus();
            assignEnrollmentToClass(enrollment, offering, learner, null, "Lớp miễn phí");
            ClassroomRegistrationSupport.syncLegacyStatus(enrollment);
            enrollment = saveEnrollmentWithWaitlistOrder(enrollment, previousStatus);
        }

        String classTitle = offering.getLearningPackage().getTitle();
        notificationService.notifyTrainingManagers(
                "CLASSROOM_REGISTRATION_CREATED",
                "Đăng ký lớp mới",
                learner.getFullName() + " vừa đăng ký lớp " + classTitle + ".",
                Map.of("enrollmentId", enrollment.getId(), "classroomId", offering.getId(), "studentId", learner.getId())
        );
        notificationService.notifyUser(
                learner,
                "CLASSROOM_REGISTRATION_SUBMITTED",
                initialStatus == ClassroomRegistrationStatus.WAITLIST
                        ? "Đã tham gia danh sách chờ"
                        : enrollment.hasClassAccess()
                                ? "Đăng ký lớp thành công"
                                : "Hoàn tất thanh toán để đăng ký lớp",
                initialStatus == ClassroomRegistrationStatus.WAITLIST
                        ? "Lớp " + classTitle + " hiện đã đủ chỗ. Bạn đã được thêm vào danh sách chờ và chưa cần thanh toán."
                        : enrollment.hasClassAccess()
                                ? "Bạn đã được đăng ký vào lớp miễn phí " + classTitle + "."
                                : "Hồ sơ tạm cho lớp " + classTitle + " đã được tạo. Vui lòng thanh toán học phí để hoàn tất đăng ký.",
                Map.of("enrollmentId", enrollment.getId(), "classroomId", offering.getId())
        );
        return mapper.toEnrollmentResponse(enrollment);
    }

    @Override
    public ClassroomEnrollmentResponse cancelMyRegistration(Long offeringId, String learnerEmail) {
        User learner = accessHelper.requireUser(learnerEmail);
        ClassroomEnrollment enrollment = enrollmentRepository
                .findByStudentIdAndClassroomOfferingId(learner.getId(), offeringId)
                .filter(item -> ACTIVE_REGISTRATIONS.contains(item.getRegistrationStatus()))
                .orElseThrow(() -> new RuntimeException("Bạn chưa có đăng ký hiệu lực cho lớp này."));
        if (enrollment.getRegistrationStatus() == ClassroomRegistrationStatus.ASSIGNED) {
            throw new RuntimeException("Bạn đã được xếp lớp. Vui lòng gửi yêu cầu hủy để Training Manager xử lý.");
        }

        ClassroomOffering offering = enrollment.getClassroomOffering();
        ClassroomRegistrationStatus previousStatus = enrollment.getRegistrationStatus();
        enrollment.setRegistrationStatus(ClassroomRegistrationStatus.CANCELLED);
        enrollment.setHoldSpot(false);
        ClassroomRegistrationSupport.markNeedRefundForExit(enrollment, "Cần xử lý hoàn tiền do học viên hủy đăng ký");
        ClassroomRegistrationSupport.syncLegacyStatus(enrollment);
        enrollment = saveEnrollmentWithWaitlistOrder(enrollment, previousStatus);

        String classTitle = offering.getLearningPackage().getTitle();
        notificationService.notifyTrainingManagers(
                "CLASSROOM_REGISTRATION_CANCELLED",
                "Học viên hủy đăng ký lớp",
                learner.getFullName() + " đã hủy đăng ký lớp " + classTitle + ".",
                Map.of("enrollmentId", enrollment.getId(), "classroomId", offering.getId(), "studentId", learner.getId())
        );
        notificationService.notifyUser(
                learner,
                "CLASSROOM_REGISTRATION_CANCELLED",
                "Đã hủy đăng ký lớp",
                "Bạn đã hủy đăng ký lớp " + classTitle + ".",
                Map.of("enrollmentId", enrollment.getId(), "classroomId", offering.getId())
        );
        notifyWaitlistIfSlotAvailable(offering);
        return mapper.toEnrollmentResponse(enrollment);
    }

    /**
     * Báo cho học viên trong danh sách chờ khi lớp vừa trống chỗ (sau khi có người hủy/bị từ chối).
     */
    private void notifyWaitlistIfSlotAvailable(ClassroomOffering offering) {
        if (offering.getMaxCapacity() == null || offering.getMaxCapacity() <= 0) {
            return;
        }
        long occupied = enrollmentRepository.countByOfferingAndRegistrationStatuses(
                offering.getId(), ClassroomRegistrationSupport.OCCUPIES_CLASS_SLOT);
        if (occupied >= offering.getMaxCapacity()) {
            return;
        }
        List<ClassroomEnrollment> waitlisted = enrollmentRepository
                .findByClassroomOfferingIdAndRegistrationStatusIn(
                        offering.getId(), Set.of(ClassroomRegistrationStatus.WAITLIST));
        if (waitlisted.isEmpty()) {
            return;
        }
        String classTitle = offering.getLearningPackage().getTitle();
        for (ClassroomEnrollment waiting : waitlisted) {
            notificationService.notifyUser(
                    waiting.getStudent(),
                    "CLASSROOM_SLOT_AVAILABLE",
                    "Lớp đã có chỗ trống",
                    "Lớp " + classTitle + " vừa có chỗ trống. Đăng ký của bạn trong danh sách chờ có thể được xử lý sớm.",
                    Map.of("enrollmentId", waiting.getId(), "classroomId", offering.getId())
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ClassroomEnrollmentResponse getMyRegistration(Long offeringId, String learnerEmail) {
        User learner = accessHelper.requireUser(learnerEmail);
        ClassroomEnrollment enrollment = enrollmentRepository.findByStudentIdAndClassroomOfferingId(learner.getId(), offeringId)
                .filter(item -> ACTIVE_REGISTRATIONS.contains(item.getRegistrationStatus()))
                .orElseThrow(() -> new RuntimeException("Bạn chưa đăng ký lớp này."));
        return mapper.toEnrollmentResponse(enrollment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomEnrollmentResponse> getMyRegistrations(String learnerEmail) {
        User learner = accessHelper.requireUser(learnerEmail);
        return enrollmentRepository.findByStudentIdAndRegistrationStatusIn(learner.getId(), ACTIVE_REGISTRATIONS).stream()
                .map(mapper::toEnrollmentResponse)
                .toList();
    }

    @Override
    public ClassroomEnrollmentResponse confirmRegistration(Long enrollmentId, String actorEmail) {
        User actor = accessHelper.requireUser(actorEmail);
        accessHelper.assertTrainingManager(actor);
        ClassroomEnrollment enrollment = findEnrollment(enrollmentId);
        ClassroomOffering offering = enrollment.getClassroomOffering();
        User learner = enrollment.getStudent();

        if (enrollment.getRegistrationStatus() != ClassroomRegistrationStatus.PENDING_CONFIRMATION
                && enrollment.getRegistrationStatus() != ClassroomRegistrationStatus.WAITLIST) {
            throw new RuntimeException("Hồ sơ không ở trạng thái có thể chuyển sang chờ thanh toán.");
        }

        ClassroomRegistrationStatus nextStatus = isClassFull(offering)
                ? ClassroomRegistrationStatus.WAITLIST
                : ClassroomRegistrationStatus.PENDING_TUITION_PAYMENT;
        ClassroomRegistrationStatus previousStatus = enrollment.getRegistrationStatus();
        enrollment.setRegistrationStatus(nextStatus);
        enrollment.setConfirmedAt(LocalDateTime.now());
        enrollment.setConfirmedBy(actor);
        ClassroomRegistrationSupport.syncLegacyStatus(enrollment);
        enrollment = saveEnrollmentWithWaitlistOrder(enrollment, previousStatus);

        String classTitle = offering.getLearningPackage().getTitle();
        notificationService.notifyUser(
                learner,
                "CLASSROOM_PAYMENT_INVITED",
                nextStatus == ClassroomRegistrationStatus.WAITLIST
                        ? "Lớp hiện vẫn chưa có chỗ"
                        : "Đã đến lượt thanh toán học phí",
                nextStatus == ClassroomRegistrationStatus.WAITLIST
                        ? "Lớp " + classTitle + " hiện vẫn đủ chỗ. Bạn tiếp tục ở trong danh sách chờ."
                        : "Lớp " + classTitle + " đã có chỗ. Vui lòng thanh toán học phí để hoàn tất đăng ký.",
                Map.of("enrollmentId", enrollment.getId(), "classroomId", offering.getId())
        );
        return mapper.toEnrollmentResponse(enrollment);
    }

    @Override
    public ClassroomEnrollmentResponse rejectRegistration(
            Long enrollmentId,
            RejectRegistrationRequest request,
            String actorEmail
    ) {
        User actor = accessHelper.requireUser(actorEmail);
        accessHelper.assertTrainingManager(actor);
        ClassroomEnrollment enrollment = findEnrollment(enrollmentId);
        ClassroomOffering offering = enrollment.getClassroomOffering();
        User learner = enrollment.getStudent();

        if (enrollment.getRegistrationStatus() != ClassroomRegistrationStatus.PENDING_CONFIRMATION
                && enrollment.getRegistrationStatus() != ClassroomRegistrationStatus.WAITLIST) {
            throw new RuntimeException("Đăng ký không thể từ chối ở trạng thái hiện tại.");
        }

        ClassroomRegistrationStatus previousStatus = enrollment.getRegistrationStatus();
        enrollment.setRegistrationStatus(ClassroomRegistrationStatus.REJECTED);
        enrollment.setNote(appendNote(enrollment.getNote(), request == null ? null : request.getReason()));
        ClassroomRegistrationSupport.markNeedRefundForExit(enrollment, "Cần xử lý hoàn tiền do từ chối đăng ký");
        ClassroomRegistrationSupport.syncLegacyStatus(enrollment);
        enrollment = saveEnrollmentWithWaitlistOrder(enrollment, previousStatus);

        notificationService.notifyUser(
                learner,
                "CLASSROOM_REGISTRATION_REJECTED",
                "Đăng ký lớp bị từ chối",
                "Đăng ký lớp " + offering.getLearningPackage().getTitle() + " đã bị từ chối.",
                Map.of("enrollmentId", enrollment.getId(), "classroomId", offering.getId())
        );
        return mapper.toEnrollmentResponse(enrollment);
    }

    @Override
    public ClassroomEnrollmentResponse recordTuitionPayment(
            Long enrollmentId,
            RecordTuitionPaymentRequest request,
            String actorEmail
    ) {
        User actor = accessHelper.requireUser(actorEmail);
        accessHelper.assertTrainingManager(actor);
        return applyTuitionPaymentInternal(
                findEnrollment(enrollmentId),
                request.getAmount(),
                request.getPaymentKind(),
                request.getNote(),
                actor,
                !Boolean.FALSE.equals(request.getAssignIfFullyPaid())
        );
    }

    @Override
    public ClassroomEnrollmentResponse resolveTuitionSettlement(
            Long enrollmentId,
            ResolveTuitionSettlementRequest request,
            String actorEmail
    ) {
        User actor = accessHelper.requireUser(actorEmail);
        accessHelper.assertTrainingManager(actor);
        ClassroomEnrollment enrollment = findEnrollment(enrollmentId);

        if (enrollment.getTuitionSettlementType() != TuitionSettlementType.NEED_REFUND) {
            throw new RuntimeException("Đăng ký này không có yêu cầu hoàn tiền học phí đang chờ xử lý.");
        }
        if (enrollment.getTuitionSettlementStatus() != TuitionSettlementStatus.PENDING) {
            throw new RuntimeException("Settlement học phí này đã được xử lý trước đó.");
        }

        String action = request == null || request.getAction() == null ? "" : request.getAction().trim().toUpperCase(Locale.ROOT);
        String note = request == null || request.getNote() == null ? "" : request.getNote().trim();

        if ("APPROVE_REFUND".equals(action)) {
            return approveTuitionRefund(enrollment, actor, note);
        }
        if ("REJECT_REFUND".equals(action)) {
            if (note.isBlank()) {
                throw new RuntimeException("Vui lòng nhập lý do từ chối hoàn tiền.");
            }
            return rejectTuitionRefund(enrollment, actor, note);
        }
        throw new RuntimeException("Thao tác settlement không hợp lệ. Dùng APPROVE_REFUND hoặc REJECT_REFUND.");
    }

    private ClassroomEnrollmentResponse approveTuitionRefund(
            ClassroomEnrollment enrollment,
            User actor,
            String note
    ) {
        BigDecimal due = enrollment.getTuitionAmountDue() == null ? BigDecimal.ZERO : enrollment.getTuitionAmountDue();
        BigDecimal paid = enrollment.getTuitionAmountPaid() == null ? BigDecimal.ZERO : enrollment.getTuitionAmountPaid();
        // Exit (cancel/reject/remove): hoàn toàn bộ đã thu. Còn trong lớp (vd. chuyển lớp overpay): chỉ hoàn phần thừa.
        boolean exitedClass = enrollment.getRegistrationStatus() == ClassroomRegistrationStatus.CANCELLED
                || enrollment.getRegistrationStatus() == ClassroomRegistrationStatus.REJECTED;
        BigDecimal refundAmount = exitedClass && paid.compareTo(BigDecimal.ZERO) > 0
                ? paid
                : paid.subtract(due);
        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Không có số tiền cần hoàn cho đăng ký này.");
        }

        enrollment.setTuitionAmountPaid(paid.subtract(refundAmount));
        String resolutionNote = note.isBlank()
                ? "Đã duyệt hoàn tiền " + refundAmount.toPlainString() + " VND."
                : note;

        tuitionPaymentRepository.save(ClassroomTuitionPayment.builder()
                .enrollment(enrollment)
                .amount(refundAmount)
                .paymentKind(TuitionPaymentKind.REFUND)
                .note(resolutionNote)
                .recordedBy(actor)
                .build());

        ClassroomRegistrationSupport.clearOpenSettlementAsResolved(enrollment, resolutionNote);
        enrollment.setTuitionSettlementResolvedAt(LocalDateTime.now());
        enrollment.setTuitionSettlementResolvedBy(actor);

        ClassroomRegistrationSupport.syncLegacyStatus(enrollment);
        enrollment = enrollmentRepository.save(enrollment);

        notificationService.notifyUser(
                enrollment.getStudent(),
                "CLASSROOM_TUITION_REFUND_APPROVED",
                "Đã duyệt hoàn học phí lớp",
                "Yêu cầu hoàn học phí lớp " + enrollment.getClassroomOffering().getLearningPackage().getTitle()
                        + " đã được duyệt: " + refundAmount.toPlainString() + " VND.",
                Map.of(
                        "enrollmentId", enrollment.getId(),
                        "classroomId", enrollment.getClassroomOffering().getId(),
                        "refundAmount", refundAmount
                )
        );
        return mapper.toEnrollmentResponse(enrollment);
    }

    private ClassroomEnrollmentResponse rejectTuitionRefund(
            ClassroomEnrollment enrollment,
            User actor,
            String note
    ) {
        enrollment.setTuitionSettlementStatus(TuitionSettlementStatus.REJECTED);
        enrollment.setTuitionSettlementResolvedAt(LocalDateTime.now());
        enrollment.setTuitionSettlementResolvedBy(actor);
        enrollment.setTuitionSettlementResolutionNote(note);
        enrollment = enrollmentRepository.save(enrollment);

        notificationService.notifyUser(
                enrollment.getStudent(),
                "CLASSROOM_TUITION_REFUND_REJECTED",
                "Từ chối hoàn học phí lớp",
                "Yêu cầu hoàn học phí lớp " + enrollment.getClassroomOffering().getLearningPackage().getTitle()
                        + " đã bị từ chối. Lý do: " + note,
                Map.of(
                        "enrollmentId", enrollment.getId(),
                        "classroomId", enrollment.getClassroomOffering().getId()
                )
        );
        return mapper.toEnrollmentResponse(enrollment);
    }

    @Override
    public ClassroomEnrollmentResponse applyPayosTuitionPayment(Long enrollmentId, BigDecimal amount, String note) {
        ClassroomEnrollment enrollment = findEnrollment(enrollmentId);
        String normalizedNote = note == null ? "" : note.trim();
        if (!normalizedNote.isBlank()) {
            boolean alreadyRecorded = tuitionPaymentRepository
                    .findByEnrollmentIdOrderByCreatedAtDesc(enrollmentId)
                    .stream()
                    .anyMatch(payment -> normalizedNote.equals(payment.getNote()));
            if (alreadyRecorded) {
                return mapper.toEnrollmentResponse(enrollment);
            }
        }

        BigDecimal balance = enrollment.tuitionBalance();
        if (balance.compareTo(BigDecimal.ZERO) <= 0) {
            return mapper.toEnrollmentResponse(enrollment);
        }

        BigDecimal paymentAmount = amount == null ? BigDecimal.ZERO : amount;
        if (paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Số tiền PayOS không hợp lệ.");
        }
        if (paymentAmount.compareTo(balance) > 0) {
            paymentAmount = balance;
        }

        TuitionPaymentKind kind = paymentAmount.compareTo(balance) >= 0
                ? TuitionPaymentKind.FULL
                : TuitionPaymentKind.PARTIAL;

        return applyTuitionPaymentInternal(
                enrollment,
                paymentAmount,
                kind,
                normalizedNote.isBlank() ? "Thanh toán PayOS" : normalizedNote,
                enrollment.getStudent(),
                true
        );
    }

    private ClassroomEnrollmentResponse applyTuitionPaymentInternal(
            ClassroomEnrollment enrollment,
            BigDecimal amount,
            TuitionPaymentKind paymentKind,
            String note,
            User recordedBy,
            boolean assignIfFullyPaid
    ) {
        ClassroomOffering offering = enrollment.getClassroomOffering();
        User learner = enrollment.getStudent();

        if (enrollment.getRegistrationStatus() == ClassroomRegistrationStatus.CANCELLED
                || enrollment.getRegistrationStatus() == ClassroomRegistrationStatus.REJECTED) {
            throw new RuntimeException("Đăng ký đã bị hủy hoặc từ chối.");
        }
        if (enrollment.getRegistrationStatus() == ClassroomRegistrationStatus.WAITLIST) {
            throw new RuntimeException("Học viên đang ở trong danh sách chờ và chưa cần thanh toán học phí.");
        }
        if (enrollment.getRegistrationStatus() == ClassroomRegistrationStatus.ASSIGNED) {
            throw new RuntimeException("Học viên đã được xếp lớp.");
        }

        BigDecimal paymentAmount = amount == null ? BigDecimal.ZERO : amount;
        BigDecimal paid = enrollment.getTuitionAmountPaid() == null ? BigDecimal.ZERO : enrollment.getTuitionAmountPaid();
        enrollment.setTuitionAmountPaid(paid.add(paymentAmount));

        if (paymentKind == TuitionPaymentKind.DEPOSIT) {
            BigDecimal deposit = enrollment.getTuitionDepositPaid() == null ? BigDecimal.ZERO : enrollment.getTuitionDepositPaid();
            enrollment.setTuitionDepositPaid(deposit.add(paymentAmount));
        }

        ClassroomRegistrationStatus previousStatus = enrollment.getRegistrationStatus();
        enrollment.setRegistrationStatus(ClassroomRegistrationSupport.resolveRegistrationStatusAfterPayment(
                enrollment.getTuitionAmountDue(),
                enrollment.getTuitionAmountPaid(),
                enrollment.getTuitionDepositPaid(),
                paymentKind
        ));
        enrollment.setTuitionRecordedAt(LocalDateTime.now());
        enrollment.setTuitionRecordedBy(recordedBy);

        tuitionPaymentRepository.save(ClassroomTuitionPayment.builder()
                .enrollment(enrollment)
                .amount(paymentAmount)
                .paymentKind(paymentKind)
                .note(note)
                .recordedBy(recordedBy)
                .build());

        ClassroomRegistrationSupport.applyComputedSettlement(enrollment);

        if (assignIfFullyPaid
                && enrollment.getRegistrationStatus() == ClassroomRegistrationStatus.FULLY_PAID) {
            tryAssignEnrollment(enrollment, offering, learner, recordedBy, null);
        }

        ClassroomRegistrationSupport.syncLegacyStatus(enrollment);
        enrollment = saveEnrollmentWithWaitlistOrder(enrollment, previousStatus);

        notificationService.notifyUser(
                learner,
                "CLASSROOM_TUITION_RECORDED",
                "Đã ghi nhận học phí",
                "Học phí lớp " + offering.getLearningPackage().getTitle() + " đã được ghi nhận: "
                        + ClassroomRegistrationSupport.registrationStatusLabel(enrollment.getRegistrationStatus()) + ".",
                Map.of("enrollmentId", enrollment.getId(), "classroomId", offering.getId())
        );
        return mapper.toEnrollmentResponse(enrollment);
    }

    @Override
    public ClassroomEnrollmentResponse assignToClass(Long enrollmentId, AssignToClassRequest request, String actorEmail) {
        User actor = accessHelper.requireUser(actorEmail);
        accessHelper.assertTrainingManager(actor);
        ClassroomEnrollment enrollment = findEnrollment(enrollmentId);
        ClassroomOffering offering = enrollment.getClassroomOffering();
        User learner = enrollment.getStudent();

        if (enrollment.getRegistrationStatus() == ClassroomRegistrationStatus.CANCELLED
                || enrollment.getRegistrationStatus() == ClassroomRegistrationStatus.REJECTED) {
            throw new RuntimeException("Đăng ký đã bị hủy hoặc từ chối.");
        }
        if (enrollment.hasClassAccess()) {
            return mapper.toEnrollmentResponse(enrollment);
        }
        if (enrollment.getRegistrationStatus() != ClassroomRegistrationStatus.FULLY_PAID) {
            throw new RuntimeException("Chỉ có thể xếp lớp sau khi học phí đã được thanh toán đầy đủ.");
        }

        ClassroomRegistrationStatus previousStatus = enrollment.getRegistrationStatus();
        assertLearnerScheduleForOffering(offering, learner.getId());
        tryAssignEnrollment(
                enrollment,
                offering,
                learner,
                actor,
                request == null ? null : request.getAssignmentNote()
        );
        ClassroomRegistrationSupport.syncLegacyStatus(enrollment);
        enrollment = saveEnrollmentWithWaitlistOrder(enrollment, previousStatus);

        if (enrollment.hasClassAccess()) {
            notificationService.notifyUser(
                    learner,
                    "CLASSROOM_ASSIGNED",
                    "Đã được xếp lớp",
                    "Bạn đã được xếp vào lớp " + offering.getLearningPackage().getTitle() + ".",
                    Map.of("enrollmentId", enrollment.getId(), "classroomId", offering.getId())
            );
        }
        return mapper.toEnrollmentResponse(enrollment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomEnrollmentResponse> listRegistrations(
            ClassroomRegistrationStatus status,
            Long classroomOfferingId,
            Boolean needsAction,
            Boolean settlementPending
    ) {
        if (Boolean.TRUE.equals(settlementPending)) {
            List<ClassroomEnrollment> pendingSettlements = classroomOfferingId == null
                    ? enrollmentRepository.findByTuitionSettlementStatus(TuitionSettlementStatus.PENDING)
                    : enrollmentRepository.findByClassroomOfferingIdAndTuitionSettlementStatus(
                            classroomOfferingId,
                            TuitionSettlementStatus.PENDING
                    );
            return pendingSettlements.stream()
                    .sorted(registrationQueueComparator())
                    .map(mapper::toEnrollmentResponse)
                    .toList();
        }

        Set<ClassroomRegistrationStatus> statuses = ClassroomRegistrationSupport.resolveRegistrationFilter(status, needsAction);
        List<ClassroomEnrollment> enrollments = classroomOfferingId == null
                ? enrollmentRepository.findByRegistrationStatusIn(statuses)
                : enrollmentRepository.findByClassroomOfferingIdAndRegistrationStatusIn(classroomOfferingId, statuses);
        return enrollments.stream()
                .sorted(registrationQueueComparator())
                .map(mapper::toEnrollmentResponse)
                .toList();
    }

    @Override
    public List<ClassroomEnrollmentResponse> reorderWaitlist(
            Long classroomOfferingId,
            ReorderWaitlistRequest request,
            String actorEmail
    ) {
        User actor = accessHelper.requireUser(actorEmail);
        accessHelper.assertTrainingManager(actor);
        offeringRepository.findByIdForUpdate(classroomOfferingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lớp học không tồn tại."));

        List<ClassroomEnrollment> waitlist = getOrderedWaitlist(classroomOfferingId);
        List<Long> requestedIds = request.getEnrollmentIds();
        Set<Long> requestedIdSet = new HashSet<>(requestedIds);
        Set<Long> currentIdSet = waitlist.stream()
                .map(ClassroomEnrollment::getId)
                .collect(java.util.stream.Collectors.toSet());
        if (requestedIds.size() != requestedIdSet.size()
                || requestedIds.size() != waitlist.size()
                || !requestedIdSet.equals(currentIdSet)) {
            throw new RuntimeException("Thứ tự mới phải chứa đúng toàn bộ học viên đang trong danh sách chờ.");
        }

        Map<Long, ClassroomEnrollment> enrollmentById = waitlist.stream()
                .collect(java.util.stream.Collectors.toMap(ClassroomEnrollment::getId, item -> item));
        for (int index = 0; index < requestedIds.size(); index++) {
            enrollmentById.get(requestedIds.get(index)).setWaitlistPriority(index + 1);
        }
        enrollmentRepository.saveAll(waitlist);

        return requestedIds.stream()
                .map(enrollmentById::get)
                .map(mapper::toEnrollmentResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ClassroomEnrollmentResponse getRegistration(Long enrollmentId) {
        return mapper.toEnrollmentResponse(findEnrollment(enrollmentId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomTuitionPaymentResponse> getTuitionHistory(Long enrollmentId) {
        ClassroomEnrollment enrollment = findEnrollment(enrollmentId);
        return tuitionPaymentRepository.findByEnrollmentIdOrderByCreatedAtDesc(enrollment.getId()).stream()
                .map(payment -> ClassroomTuitionPaymentResponse.builder()
                        .id(payment.getId())
                        .amount(payment.getAmount())
                        .paymentKind(payment.getPaymentKind() == null ? null : payment.getPaymentKind().name())
                        .paymentKindLabel(ClassroomRegistrationSupport.tuitionPaymentKindLabel(payment.getPaymentKind()))
                        .note(payment.getNote())
                        .recordedByName(payment.getRecordedBy() == null ? null : payment.getRecordedBy().getFullName())
                        .createdAt(payment.getCreatedAt())
                        .build())
                .toList();
    }

    @Override
    public ClassroomEnrollmentResponse transferEnrollment(
            Long enrollmentId,
            TransferEnrollmentRequest request,
            String actorEmail
    ) {
        User actor = accessHelper.requireUser(actorEmail);
        accessHelper.assertTrainingManager(actor);
        ClassroomEnrollment enrollment = findEnrollment(enrollmentId);
        TransferStudentRequest transferRequest = new TransferStudentRequest();
        transferRequest.setStudentId(enrollment.getStudent().getId());
        transferRequest.setTargetClassroomOfferingId(request.getTargetClassroomOfferingId());
        transferRequest.setNote(request.getNote());
        ClassroomEnrollmentResponse response = transferStudent(enrollment.getClassroomOffering().getId(), transferRequest);

        notificationService.notifyUser(
                enrollment.getStudent(),
                "CLASSROOM_TRANSFERRED",
                "Đã chuyển lớp",
                "Đăng ký của bạn đã được chuyển sang lớp mới.",
                Map.of("enrollmentId", response.getId(), "classroomId", response.getClassroomOfferingId())
        );
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public ConflictCheckResultResponse checkEnrollmentConflict(Long enrollmentId) {
        ClassroomEnrollment enrollment = findEnrollment(enrollmentId);
        ClassroomOffering offering = enrollment.getClassroomOffering();
        ConflictCheckRequest request = ConflictCheckRequest.builder()
                .classroomOfferingId(offering.getId())
                .learnerIds(List.of(enrollment.getStudent().getId()))
                .checkCapacity(true)
                .build();
        return conflictService.check(request);
    }

    @Override
    public ClassroomTeacherSummaryResponse assignTeacher(Long offeringId, Long teacherId, ClassroomTeacherRole role) {
        ClassroomOffering offering = findOffering(offeringId);
        User teacher = resolveTeacher(teacherId);
        if (teacher == null) {
            throw new RuntimeException("Giáo viên không hợp lệ.");
        }
        ClassroomTeacherRole resolvedRole = role == null ? ClassroomTeacherRole.PRIMARY : role;
        Long previousPrimaryTeacherId = getPrimaryTeacherId(offering);
        if (resolvedRole == ClassroomTeacherRole.PRIMARY) {
            deactivateOtherPrimaryTeachers(offering, teacher.getId(), "Thay đổi giáo viên chính");
            offering.setPrimaryTeacher(teacher);
            offeringRepository.save(offering);
        }
        ClassroomTeacherSummaryResponse response = assignTeacherInternal(offering, teacher, resolvedRole, null);
        if (resolvedRole == ClassroomTeacherRole.PRIMARY
                && !teacher.getId().equals(previousPrimaryTeacherId)) {
            updateUpcomingSessionsForTeacherChange(offering, previousPrimaryTeacherId, teacher);
        }
        return response;
    }

    @Override
    public ClassroomTeacherSummaryResponse replaceTeacher(Long offeringId, Long oldTeacherId, Long newTeacherId) {
        ClassroomOffering offering = findOffering(offeringId);
        User newTeacher = resolveTeacher(newTeacherId);
        if (newTeacher == null) {
            throw new RuntimeException("Giáo viên mới không hợp lệ.");
        }
        if (oldTeacherId != null && oldTeacherId.equals(newTeacherId)) {
            return assignTeacherInternal(offering, newTeacher, ClassroomTeacherRole.PRIMARY, "Tiếp tục phân công");
        }

        teacherAssignmentRepository.findByClassroomOfferingIdAndTeacherId(offeringId, oldTeacherId)
                .filter(this::isTeacherAssignmentActive)
                .ifPresent(assignment -> {
                    assignment.setEffectiveTo(LocalDate.now().minusDays(1));
                    assignment.setReason("Kết thúc phân công do thay giáo viên");
                    teacherAssignmentRepository.save(assignment);
                });
        deactivateOtherPrimaryTeachers(offering, newTeacherId, "Kết thúc phân công do thay giáo viên");
        offering.setPrimaryTeacher(newTeacher);
        offeringRepository.save(offering);
        ClassroomTeacherSummaryResponse response =
                assignTeacherInternal(offering, newTeacher, ClassroomTeacherRole.PRIMARY, "Thay giáo viên");
        updateUpcomingSessionsForTeacherChange(offering, oldTeacherId, newTeacher);
        return response;
    }

    @Override
    public ClassroomSessionResponse openVirtualSession(Long sessionId, String actorEmail) {
        ClassroomSession session = findSession(sessionId);
        User actor = assertCanManageVirtualSession(session, actorEmail);
        if (session.getDeliveryMode() != ClassroomDeliveryMode.VIRTUAL) {
            throw new RuntimeException("Chỉ buổi học trực tuyến mới có thể mở phòng ảo.");
        }
        if (session.getLarkMeetingUrl() == null
                || session.getLarkMeetingUrl().isBlank()
                || larkMeetingService.isDemoUrl(session.getLarkMeetingUrl())) {
            if (!syncLarkMeetingSafely(session)) {
                sessionRepository.save(session);
                throw new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Chưa thể mở phòng học trực tuyến. Vui lòng thử lại sau ít phút."
                );
            }
        }
        inviteTeacher(session, actor);
        session.setStatus(ClassroomSessionStatus.OPEN);
        session.setLarkMeetingStatus(LarkMeetingStatus.OPEN);
        return mapper.toSessionResponse(sessionRepository.save(session));
    }

    @Override
    public ClassroomSessionResponse joinVirtualSession(Long sessionId, String learnerEmail) {
        User learner = accessHelper.requireUser(learnerEmail);
        ClassroomSession session = findSession(sessionId);

        if (session.getDeliveryMode() != ClassroomDeliveryMode.VIRTUAL) {
            throw new RuntimeException("Buổi học này không phải lớp học trực tuyến.");
        }
        if (session.getStatus() == ClassroomSessionStatus.CANCELLED) {
            throw new RuntimeException("Buổi học đã bị hủy.");
        }

        ClassroomEnrollment enrollment = enrollmentRepository
                .findByStudentIdAndClassroomOfferingId(
                        learner.getId(),
                        session.getClassroomOffering().getId()
                )
                .orElseThrow(() -> new RuntimeException("Bạn không thuộc lớp học này."));
        if (!enrollment.hasClassAccess()) {
            throw new RuntimeException("Bạn chưa được cấp quyền tham gia lớp học này.");
        }

        if (session.getLarkMeetingUrl() == null
                || session.getLarkMeetingUrl().isBlank()
                || larkMeetingService.isDemoUrl(session.getLarkMeetingUrl())) {
            if (!syncLarkMeetingSafely(session)) {
                sessionRepository.save(session);
                throw new RuntimeException(
                        session.getLarkSyncError() == null || session.getLarkSyncError().isBlank()
                                ? "Chưa thể tạo phòng Lark cho buổi học này."
                                : session.getLarkSyncError()
                );
            }
        } else if (session.getLarkEventId() != null && larkMeetingService.isEnabled()) {
            if (!syncLarkMeetingSafely(session)) {
                sessionRepository.save(session);
                throw new RuntimeException(
                        session.getLarkSyncError() == null || session.getLarkSyncError().isBlank()
                                ? "Chưa thể cập nhật quyền tự tham gia phòng Lark."
                                : session.getLarkSyncError()
                );
            }
        }

        session.setLarkEmptySince(null);
        session.setLarkMeetingStatus(LarkMeetingStatus.OPEN);
        virtualAttendanceService.recordVirtualJoin(session, learner);
        return mapper.toSessionResponse(sessionRepository.save(session));
    }

    @Override
    public ClassroomSessionResponse joinVirtualClass(Long offeringId, Long sessionId, String learnerEmail) {
        ClassroomSession session = findSession(sessionId);
        if (!session.getClassroomOffering().getId().equals(offeringId)) {
            throw new RuntimeException("Buổi học không thuộc lớp đã chọn.");
        }
        return joinVirtualSession(sessionId, learnerEmail);
    }

    @Override
    public ClassroomSessionResponse closeVirtualSession(Long sessionId, String actorEmail) {
        ClassroomSession session = findSession(sessionId);
        assertCanManageVirtualSession(session, actorEmail);
        markVirtualSessionEnded(session);
        return mapper.toSessionResponse(sessionRepository.save(session));
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void closeEmptyVirtualRooms() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(EMPTY_ROOM_GRACE_MINUTES);
        List<ClassroomSession> emptyRooms =
                sessionRepository.findByLarkEmptySinceIsNotNullAndLarkEmptySinceBefore(cutoff);

        for (ClassroomSession session : emptyRooms) {
            if (larkParticipantRepository.countByClassroomSessionIdAndActiveTrue(session.getId()) > 0) {
                session.setLarkEmptySince(null);
                continue;
            }
            deleteLarkMeetingSafely(session);
            larkParticipantRepository.deleteByClassroomSessionId(session.getId());
            clearManagedLarkData(session);
            session.setLarkMeetingUrl(null);
            session.setLarkMeetingId(null);
            session.setLarkMeetingNo(null);
            session.setLarkMeetingStatus(LarkMeetingStatus.ENDED);
            session.setLarkSyncStatus("PENDING");
            session.setLarkSyncError(null);
            session.setLarkEmptySince(null);
        }
        sessionRepository.saveAll(emptyRooms);
    }

    @Override
    public ClassroomSessionResponse updateSessionLarkLink(Long sessionId, UpdateLarkLinkRequest request) {
        ClassroomSession session = findSession(sessionId);
        if (session.isLocked()) {
            throw new RuntimeException("Buổi học đã khóa nên không thể cập nhật link Lark.");
        }
        if (session.getLarkEventId() != null) {
            deleteLarkMeetingSafely(session);
            clearManagedLarkData(session);
        }
        session.setLarkMeetingUrl(request.getLarkMeetingUrl());
        session.setLarkMeetingStatus(larkMeetingService.resolveStatus(request.getLarkMeetingUrl()));
        session.setLarkSyncStatus("MANUAL");
        session.setLarkSyncError(null);
        return mapper.toSessionResponse(sessionRepository.save(session));
    }

    @Override
    @Transactional(readOnly = true)
    public ConflictCheckResultResponse checkConflict(ConflictCheckRequest request) {
        return conflictService.check(request);
    }

    private ClassroomTeacherSummaryResponse assignTeacherInternal(
            ClassroomOffering offering,
            User teacher,
            ClassroomTeacherRole role,
            String reason
    ) {
        ClassroomTeacherAssignment assignment = teacherAssignmentRepository
                .findByClassroomOfferingIdAndTeacherId(offering.getId(), teacher.getId())
                .orElseGet(() -> ClassroomTeacherAssignment.builder()
                        .classroomOffering(offering)
                        .teacher(teacher)
                        .role(role)
                        .reason(reason)
                        .effectiveFrom(LocalDate.now())
                        .build());
        assignment.setRole(role);
        assignment.setEffectiveFrom(LocalDate.now());
        assignment.setEffectiveTo(null);
        if (reason != null) {
            assignment.setReason(reason);
        }
        return mapper.toTeacherSummary(teacherAssignmentRepository.save(assignment));
    }

    private boolean isTeacherAssignmentActive(ClassroomTeacherAssignment assignment) {
        LocalDate today = LocalDate.now();
        return (assignment.getEffectiveFrom() == null || !assignment.getEffectiveFrom().isAfter(today))
                && (assignment.getEffectiveTo() == null || !assignment.getEffectiveTo().isBefore(today));
    }

    private void deactivateOtherPrimaryTeachers(ClassroomOffering offering, Long activeTeacherId, String reason) {
        LocalDate endedOn = LocalDate.now().minusDays(1);
        teacherAssignmentRepository.findByClassroomOfferingId(offering.getId()).stream()
                .filter(this::isTeacherAssignmentActive)
                .filter(assignment -> assignment.getRole() == ClassroomTeacherRole.PRIMARY)
                .filter(assignment -> !assignment.getTeacher().getId().equals(activeTeacherId))
                .forEach(assignment -> {
                    assignment.setEffectiveTo(endedOn);
                    assignment.setReason(reason);
                    teacherAssignmentRepository.save(assignment);
                });
    }

    private void updateUpcomingSessionsForTeacherChange(
            ClassroomOffering offering,
            Long oldTeacherId,
            User newTeacher
    ) {
        LocalDate today = LocalDate.now();
        List<ClassroomSession> sessions =
                sessionRepository.findByClassroomOfferingIdOrderBySessionDateAscStartTimeAsc(offering.getId());

        sessions.stream()
                .filter(session -> !session.getSessionDate().isBefore(today))
                .filter(session -> session.getStatus() != ClassroomSessionStatus.COMPLETED)
                .filter(session -> session.getStatus() != ClassroomSessionStatus.CANCELLED)
                .filter(session -> oldTeacherId == null
                        || session.getTeacher() == null
                        || oldTeacherId.equals(session.getTeacher().getId()))
                .forEach(session -> {
                    session.setTeacher(newTeacher);
                    if (session.getDeliveryMode() == ClassroomDeliveryMode.VIRTUAL
                            && session.getLarkEventId() != null) {
                        syncLarkMeetingSafely(session);
                        inviteTeacherSafely(session, newTeacher);
                    }
                });
        sessionRepository.saveAll(sessions);
    }

    private User assertCanManageVirtualSession(ClassroomSession session, String actorEmail) {
        User actor = accessHelper.requireUser(actorEmail);
        accessHelper.assertTeacher(actor);

        if (accessHelper.canManageClassroom(actor)
                || accessHelper.canManageTrainingOperations(actor)
                || isSessionTeacher(session, actor)
                || isPrimaryTeacher(session.getClassroomOffering(), actor)
                || hasActiveTeacherAssignment(session.getClassroomOffering(), actor)) {
            return actor;
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Bạn không còn được phân công phụ trách lớp học này."
        );
    }

    private boolean isSessionTeacher(ClassroomSession session, User actor) {
        return session.getTeacher() != null
                && session.getTeacher().getId().equals(actor.getId());
    }

    private boolean isPrimaryTeacher(ClassroomOffering offering, User actor) {
        return offering.getPrimaryTeacher() != null
                && offering.getPrimaryTeacher().getId().equals(actor.getId());
    }

    private boolean hasActiveTeacherAssignment(ClassroomOffering offering, User actor) {
        return teacherAssignmentRepository
                .findByClassroomOfferingIdAndTeacherId(offering.getId(), actor.getId())
                .filter(this::isTeacherAssignmentActive)
                .isPresent();
    }

    private void inviteTeacherSafely(ClassroomSession session, User teacher) {
        try {
            inviteTeacher(session, teacher);
        } catch (RuntimeException ex) {
            log.warn(
                    "Không thể thêm giáo viên {} vào lịch Lark của buổi học {}: {}",
                    teacher.getId(),
                    session.getId(),
                    ex.getMessage()
            );
        }
    }

    private void inviteTeacher(ClassroomSession session, User teacher) {
        larkMeetingService.inviteInternalAttendee(session, teacher.getEmail());
    }

    private ClassroomSession buildSession(ClassroomOffering offering, CreateClassroomSessionRequest request, User teacher) {
        ClassroomDeliveryMode deliveryMode = request.getDeliveryMode() != null ? request.getDeliveryMode() : offering.getDeliveryMode();
        String larkUrl = request.getLarkMeetingUrl();
        if (larkMeetingService.isDemoUrl(larkUrl)) {
            larkUrl = null;
        }
        if ((larkUrl == null || larkUrl.isBlank())
                && deliveryMode == ClassroomDeliveryMode.VIRTUAL
                && !larkMeetingService.isDemoUrl(offering.getDefaultLarkMeetingUrl())) {
            larkUrl = offering.getDefaultLarkMeetingUrl();
        }
        return ClassroomSession.builder()
                .classroomOffering(offering)
                .sessionDate(request.getSessionDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .teacher(teacher)
                .status(request.getStatus() == null ? ClassroomSessionStatus.SCHEDULED : request.getStatus())
                .deliveryMode(deliveryMode)
                .room(resolveRoom(request.getRoomId() != null ? request.getRoomId() : getDefaultRoomId(offering)))
                .larkMeetingUrl(larkUrl)
                .larkMeetingStatus(larkMeetingService.resolveStatus(larkUrl))
                .larkSyncStatus(larkUrl == null || larkUrl.isBlank() ? "PENDING" : "MANUAL")
                .sessionContent(request.getSessionContent())
                .note(request.getNote())
                .build();
    }

    private void applySessionRequest(ClassroomSession session, CreateClassroomSessionRequest request, User teacher) {
        ClassroomOffering offering = session.getClassroomOffering();
        session.setSessionDate(request.getSessionDate());
        session.setStartTime(request.getStartTime());
        session.setEndTime(request.getEndTime());
        session.setTeacher(teacher);
        if (request.getStatus() != null) {
            session.setStatus(request.getStatus());
        }
        if (request.getDeliveryMode() != null) {
            session.setDeliveryMode(request.getDeliveryMode());
        }
        if (request.getRoomId() != null) {
            session.setRoom(resolveRoom(request.getRoomId()));
        }
        if (request.getLarkMeetingUrl() != null) {
            String requestedUrl = larkMeetingService.isDemoUrl(request.getLarkMeetingUrl())
                    ? null
                    : request.getLarkMeetingUrl();
            session.setLarkMeetingUrl(requestedUrl);
            session.setLarkMeetingStatus(larkMeetingService.resolveStatus(requestedUrl));
            if (requestedUrl != null && !requestedUrl.isBlank()) {
                session.setLarkSyncStatus("MANUAL");
                session.setLarkSyncError(null);
            }
        } else if (session.getDeliveryMode() == ClassroomDeliveryMode.VIRTUAL) {
            String defaultUrl = larkMeetingService.isDemoUrl(offering.getDefaultLarkMeetingUrl())
                    ? null
                    : offering.getDefaultLarkMeetingUrl();
            if (session.getLarkEventId() == null) {
                session.setLarkMeetingUrl(defaultUrl);
                session.setLarkMeetingStatus(larkMeetingService.resolveStatus(defaultUrl));
            }
        }
        session.setSessionContent(request.getSessionContent());
        session.setNote(request.getNote());
    }

    private boolean syncLarkMeetingSafely(ClassroomSession session) {
        if (!larkMeetingService.isEnabled()) {
            session.setLarkSyncStatus("DISABLED");
            session.setLarkSyncError("Tích hợp Lark API chưa được bật.");
            return false;
        }
        try {
            larkMeetingService.syncMeeting(session);
            return true;
        } catch (RuntimeException ex) {
            session.setLarkSyncStatus("FAILED");
            session.setLarkSyncError(ex.getMessage());
            log.warn("Không thể đồng bộ buổi học {} với Lark: {}", session.getId(), ex.getMessage());
            return false;
        }
    }

    private void deleteLarkMeetingSafely(ClassroomSession session) {
        try {
            larkMeetingService.deleteMeeting(session);
        } catch (RuntimeException ex) {
            log.warn("Không thể xóa sự kiện Lark của buổi học {}: {}", session.getId(), ex.getMessage());
        }
    }

    private void clearManagedLarkData(ClassroomSession session) {
        session.setLarkCalendarId(null);
        session.setLarkEventId(null);
        session.setLarkMeetingId(null);
        session.setLarkMeetingNo(null);
        session.setLarkSyncedAt(null);
    }

    private void markVirtualSessionEnded(ClassroomSession session) {
        virtualAttendanceService.finalizeVirtualAttendance(session);
        session.setStatus(ClassroomSessionStatus.COMPLETED);
        session.setLocked(true);
        session.setLarkMeetingStatus(LarkMeetingStatus.ENDED);
    }

    private void ensureGradebookEntry(ClassroomOffering offering, User student) {
        gradebookEntryRepository.findByClassroomOfferingIdAndStudentId(offering.getId(), student.getId())
                .orElseGet(() -> gradebookEntryRepository.save(ClassroomGradebookEntry.builder()
                        .classroomOffering(offering)
                        .student(student)
                        .status(GradebookEntryStatus.PENDING)
                        .build()));
    }

    private List<Long> resolveActiveLearnerIds(Long offeringId) {
        return enrollmentRepository.findByClassroomOfferingIdAndRegistrationStatusIn(offeringId, OCCUPIES_CLASS_SLOT).stream()
                .map(enrollment -> enrollment.getStudent().getId())
                .toList();
    }

    private ClassroomEnrollment findEnrollment(Long enrollmentId) {
        return enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đăng ký lớp."));
    }

    private ClassroomEnrollment saveEnrollmentWithWaitlistOrder(
            ClassroomEnrollment enrollment,
            ClassroomRegistrationStatus previousStatus
    ) {
        Long offeringId = enrollment.getClassroomOffering().getId();
        boolean isWaitlisted = enrollment.getRegistrationStatus() == ClassroomRegistrationStatus.WAITLIST;
        if (isWaitlisted && enrollment.getWaitlistPriority() == null) {
            offeringRepository.findByIdForUpdate(offeringId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lớp học không tồn tại."));
            Integer maxPriority = enrollmentRepository.findMaxWaitlistPriority(
                    offeringId,
                    ClassroomRegistrationStatus.WAITLIST
            );
            enrollment.setWaitlistPriority(maxPriority == null ? 1 : maxPriority + 1);
        } else if (!isWaitlisted) {
            enrollment.setWaitlistPriority(null);
        }

        ClassroomEnrollment saved = enrollmentRepository.saveAndFlush(enrollment);
        if (previousStatus == ClassroomRegistrationStatus.WAITLIST && !isWaitlisted) {
            compactWaitlist(offeringId);
        }
        return saved;
    }

    private void compactWaitlist(Long classroomOfferingId) {
        offeringRepository.findByIdForUpdate(classroomOfferingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lớp học không tồn tại."));
        List<ClassroomEnrollment> waitlist = getOrderedWaitlist(classroomOfferingId);
        for (int index = 0; index < waitlist.size(); index++) {
            waitlist.get(index).setWaitlistPriority(index + 1);
        }
        enrollmentRepository.saveAll(waitlist);
    }

    private List<ClassroomEnrollment> getOrderedWaitlist(Long classroomOfferingId) {
        return enrollmentRepository
                .findByClassroomOfferingIdAndRegistrationStatusOrderByWaitlistPriorityAscEnrolledAtAscIdAsc(
                        classroomOfferingId,
                        ClassroomRegistrationStatus.WAITLIST
                );
    }

    private Comparator<ClassroomEnrollment> registrationQueueComparator() {
        return (left, right) -> {
            boolean leftWaitlisted = left.getRegistrationStatus() == ClassroomRegistrationStatus.WAITLIST;
            boolean rightWaitlisted = right.getRegistrationStatus() == ClassroomRegistrationStatus.WAITLIST;
            if (leftWaitlisted && !rightWaitlisted) {
                return -1;
            }
            if (!leftWaitlisted && rightWaitlisted) {
                return 1;
            }
            if (leftWaitlisted) {
                int priorityComparison = Comparator.nullsLast(Integer::compareTo)
                        .compare(left.getWaitlistPriority(), right.getWaitlistPriority());
                if (priorityComparison != 0) {
                    return priorityComparison;
                }
                return Comparator.nullsLast(LocalDateTime::compareTo)
                        .compare(left.getEnrolledAt(), right.getEnrolledAt());
            }
            return Comparator.nullsLast(Comparator.<LocalDateTime>reverseOrder())
                    .compare(left.getEnrolledAt(), right.getEnrolledAt());
        };
    }

    private BigDecimal resolveTuitionDue(ClassroomOffering offering) {
        LearningPackage learningPackage = offering.getLearningPackage();

        if (learningPackage == null || learningPackage.getPrice() == null) {
            return BigDecimal.ZERO;

        }
        return learningPackage.getPrice();
    }

    private boolean isClassFull(ClassroomOffering offering) {
        Integer maxCapacity = offering.getMaxCapacity();
        if (maxCapacity == null || maxCapacity <= 0) {
            return false;
        }
        long assigned = enrollmentRepository.countByOfferingAndRegistrationStatuses(offering.getId(), OCCUPIES_CLASS_SLOT);
        return assigned >= maxCapacity;
    }

    private void assertOpenForRegistration(ClassroomOffering offering) {
        if (offering.getLearningPackage().getStatus() != PackageStatus.PUBLISHED) {
            throw new RuntimeException("Lớp học chưa mở đăng ký.");
        }
        if (offering.getStatus() != ClassroomOfferingStatus.UPCOMING
                || offering.getStartDate() == null
                || !offering.getStartDate().isAfter(LocalDate.now())) {
            throw new RuntimeException("Lớp học không còn nhận đăng ký.");
        }
    }

    private PackageEnrollment ensurePackageEnrollment(User student, ClassroomOffering offering) {
        return packageEnrollmentRepository.findByStudentAndLearningPackage(student, offering.getLearningPackage())
                .map(enrollment -> {
                    if (!courseEnrollmentAccessPolicy.hasLearningAccess(enrollment)) {
                        return courseEnrollmentAccessPolicy.reactivateCancelledEnrollment(enrollment);
                    }
                    return enrollment;
                })
                .orElseGet(() -> packageEnrollmentRepository.save(PackageEnrollment.builder()
                        .student(student)
                        .learningPackage(offering.getLearningPackage())
                        .status(EnrollmentStatus.ACTIVE)
                        .progressPercent(0)
                        .build()));
    }

    private void tryAssignEnrollment(
            ClassroomEnrollment enrollment,
            ClassroomOffering offering,
            User student,
            User assigner,
            String assignmentNote
    ) {
        ClassroomOffering lockedOffering = offeringRepository.findByIdForUpdate(offering.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lớp học không tồn tại."));
        if (isClassFull(lockedOffering)) {
            enrollment.setRegistrationStatus(ClassroomRegistrationStatus.WAITLIST);
            return;
        }
        assignEnrollmentToClass(enrollment, lockedOffering, student, assigner, assignmentNote);
    }

    private void assignEnrollmentToClass(
            ClassroomEnrollment enrollment,
            ClassroomOffering offering,
            User student,
            User assigner,
            String assignmentNote
    ) {
        enrollment.setRegistrationStatus(ClassroomRegistrationStatus.ASSIGNED);
        enrollment.setAssignedAt(LocalDateTime.now());
        enrollment.setAssignedBy(assigner);
        if (assignmentNote != null && !assignmentNote.isBlank()) {
            enrollment.setAssignmentNote(assignmentNote);
        }
        enrollment.setPackageEnrollment(ensurePackageEnrollment(student, offering));
        ensureGradebookEntry(offering, student);
    }

    private void assertLearnerScheduleForOffering(ClassroomOffering offering, Long learnerId) {
        List<ClassroomSession> sessions = sessionRepository
                .findByClassroomOfferingIdOrderBySessionDateAscStartTimeAsc(offering.getId());
        for (ClassroomSession session : sessions) {
            if (session.getStatus() == ClassroomSessionStatus.CANCELLED
                    || session.getStatus() == ClassroomSessionStatus.COMPLETED) {
                continue;
            }
            ConflictCheckRequest request = ConflictCheckRequest.builder()
                    .learnerIds(List.of(learnerId))
                    .sessionDate(session.getSessionDate())
                    .startTime(session.getStartTime())
                    .endTime(session.getEndTime())
                    .excludeSessionId(session.getId())
                    .checkCapacity(false)
                    .build();
            conflictService.assertNoBlockingConflict(request);
        }
    }

    private String appendNote(String existing, String addition) {
        if (addition == null || addition.isBlank()) {
            return existing;
        }
        if (existing == null || existing.isBlank()) {
            return addition;
        }
        return existing + " | " + addition;
    }

    private ClassroomOffering findOffering(Long id) {
        return offeringRepository.findById(id)
                .filter(offering -> !offering.getLearningPackage().isDeleted())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp học."));
    }

    private boolean isVisibleToTrainingManager(ClassroomOffering offering) {
        if (offering.getLearningPackage() == null) {
            return false;
        }
        if (!offering.getLearningPackage().isDeleted()) {
            return true;
        }
        return !enrollmentRepository.findByClassroomOfferingIdAndRegistrationStatusIn(
                offering.getId(),
                ClassroomRegistrationSupport.NEEDS_ACTION_STATUSES
        ).isEmpty();
    }

    private ClassroomOffering findPublicOffering(String slugOrId) {
        try {
            Long id = Long.parseLong(slugOrId);
            return offeringRepository.findById(id)
                    .or(() -> offeringRepository.findByLearningPackageId(id))
                    .filter(found -> !found.getLearningPackage().isDeleted())
                    .filter(found -> found.getLearningPackage().getStatus() == PackageStatus.PUBLISHED)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy lớp học."));
        } catch (NumberFormatException ex) {
            return offeringRepository.findByLearningPackageSlug(slugOrId)
                    .filter(found -> found.getLearningPackage().getStatus() == PackageStatus.PUBLISHED)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy lớp học."));
        }
    }

    private ClassroomSession findSession(Long sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy buổi học."));
    }

    private User resolveTeacher(Long teacherId) {
        if (teacherId == null) {
            return null;
        }
        return userRepository.findById(teacherId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giáo viên."));
    }

    private ClassroomRoom resolveRoom(Long roomId) {
        if (roomId == null) {
            return null;
        }
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng học."));
    }

    private CurriculumProgram resolveCurriculumProgram(Long curriculumProgramId, ClassroomDeliveryMode deliveryMode) {
        if (curriculumProgramId == null) {
            return null;
        }
        CurriculumProgram program = curriculumProgramRepository.findById(curriculumProgramId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giáo trình."));
        if (deliveryMode != null && program.getDeliveryMode() != deliveryMode) {
            throw new RuntimeException("Giáo trình không khớp với hình thức đào tạo của lớp.");
        }
        if ("ARCHIVED".equalsIgnoreCase(program.getStatus())) {
            throw new RuntimeException("Giáo trình đã lưu trữ, không thể gắn vào lớp.");
        }
        if (!"PUBLISHED".equalsIgnoreCase(program.getStatus())) {
            throw new RuntimeException("Chỉ có thể mở lớp từ giáo trình đã được duyệt.");
        }
        return program;
    }

    private TrainingProgram resolveTrainingProgram(Long trainingProgramId, ClassroomDeliveryMode deliveryMode) {
        if (trainingProgramId == null) {
            return null;
        }
        TrainingProgram program = trainingProgramRepository.findById(trainingProgramId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chương trình học."));
        if (deliveryMode != null && program.getDeliveryMode() != deliveryMode) {
            throw new RuntimeException("Chương trình học không khớp với hình thức đào tạo của lớp.");
        }
        if (program.getStatus() == PackageStatus.ARCHIVED) {
            throw new RuntimeException("Chương trình học đã lưu trữ, không thể gắn vào lớp.");
        }
        if (program.getStatus() != PackageStatus.PUBLISHED) {
            throw new RuntimeException("Chỉ có thể mở lớp từ chương trình học đã xuất bản.");
        }
        return program;
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private Long getPrimaryTeacherId(ClassroomOffering offering) {
        return offering.getPrimaryTeacher() == null ? null : offering.getPrimaryTeacher().getId();
    }

    private Long getDefaultRoomId(ClassroomOffering offering) {
        return offering.getDefaultRoom() == null ? null : offering.getDefaultRoom().getId();
    }

    private BigDecimal defaultBigDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String generateUniqueSlug(String title) {
        String baseSlug = toSlug(title);
        String slug = baseSlug;
        int index = 2;
        while (learningPackageRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + index++;
        }
        return slug;
    }

    private String toSlug(String input) {
        String nowhitespace = WHITESPACE.matcher(input.trim()).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        slug = slug.replaceAll("-+", "-").toLowerCase(Locale.ENGLISH);
        return slug.isBlank() ? "classroom" : slug;
    }
}
