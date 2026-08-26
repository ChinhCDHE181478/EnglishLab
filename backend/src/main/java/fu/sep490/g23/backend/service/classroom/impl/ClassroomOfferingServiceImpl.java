package fu.sep490.g23.backend.service.classroom.impl;
import fu.sep490.g23.backend.entity.course.enums.EnrollmentStatus;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomEnrollmentStatus;
import fu.sep490.g23.backend.entity.classroom.enums.ClassScheduleType;
import fu.sep490.g23.backend.entity.classroom.ClassroomGradebookEntry;
import fu.sep490.g23.backend.entity.classroom.enums.LarkMeetingStatus;
import fu.sep490.g23.backend.entity.classroom.ClassroomTuitionPayment;
import fu.sep490.g23.backend.entity.classroom.enums.GradebookEntryStatus;
import fu.sep490.g23.backend.entity.classroom.enums.TuitionSettlementStatus;
import fu.sep490.g23.backend.entity.classroom.enums.TuitionSettlementType;
import fu.sep490.g23.backend.entity.course.enums.PackageStatus;
import fu.sep490.g23.backend.service.classroom.ClassroomRegistrationSupport;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomOfferingStatus;
import fu.sep490.g23.backend.entity.course.enums.PackageTypeCode;
import fu.sep490.g23.backend.entity.classroom.Room;
import fu.sep490.g23.backend.entity.classroom.ClassroomTeacherAssignment;
import fu.sep490.g23.backend.entity.classroom.TrainingProgram;
import fu.sep490.g23.backend.entity.classroom.ClassSchedule;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomTeacherSummaryResponse;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import fu.sep490.g23.backend.repository.classroom.ClassSectionRepository;
import fu.sep490.g23.backend.dto.request.classroom.EnrollStudentRequest;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomSessionResponse;
import fu.sep490.g23.backend.service.classroom.ClassroomScheduleLockService;
import fu.sep490.g23.backend.service.classroom.ClassroomMapper;
import fu.sep490.g23.backend.repository.classroom.ClassScheduleRepository;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomTeacherRole;
import fu.sep490.g23.backend.service.classroom.VirtualAttendanceService;
import fu.sep490.g23.backend.repository.classroom.ClassroomGradebookEntryRepository;
import fu.sep490.g23.backend.repository.classroom.LarkMeetingParticipantRepository;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomEnrollmentResponse;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomSessionStatus;
import fu.sep490.g23.backend.repository.classroom.ClassEnrollmentRepository;
import fu.sep490.g23.backend.repository.classroom.TrainingProgramRepository;
import fu.sep490.g23.backend.dto.request.classroom.ResolveTuitionSettlementRequest;
import fu.sep490.g23.backend.dto.request.classroom.RecordTuitionPaymentRequest;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomTuitionPaymentResponse;
import fu.sep490.g23.backend.repository.classroom.RoomRepository;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomRegistrationStatus;
import fu.sep490.g23.backend.repository.classroom.ClassroomTuitionPaymentRepository;
import fu.sep490.g23.backend.entity.classroom.ClassEnrollment;
import fu.sep490.g23.backend.dto.request.classroom.CreateClassroomOfferingRequest;
import fu.sep490.g23.backend.dto.request.classroom.AssignToClassRequest;
import fu.sep490.g23.backend.dto.request.classroom.ReorderWaitlistRequest;
import fu.sep490.g23.backend.dto.request.classroom.ConflictCheckRequest;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomOfferingResponse;
import fu.sep490.g23.backend.dto.response.classroom.ConflictCheckResultResponse;
import fu.sep490.g23.backend.dto.request.classroom.TransferStudentRequest;
import fu.sep490.g23.backend.dto.request.classroom.CreateClassroomSessionRequest;
import fu.sep490.g23.backend.dto.request.classroom.RejectRegistrationRequest;
import fu.sep490.g23.backend.entity.classroom.enums.TuitionPaymentKind;
import fu.sep490.g23.backend.repository.classroom.ClassroomTeacherAssignmentRepository;
import fu.sep490.g23.backend.service.classroom.VirtualMeetingService;
import fu.sep490.g23.backend.dto.request.classroom.TransferEnrollmentRequest;
import fu.sep490.g23.backend.entity.classroom.ClassSection;
import fu.sep490.g23.backend.dto.request.classroom.UpdateLarkLinkRequest;


import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.classroom.*;
import fu.sep490.g23.backend.entity.course.LearningPackage;
import fu.sep490.g23.backend.entity.course.CourseLesson;
import fu.sep490.g23.backend.entity.course.OnlineCourseEnrollment;
import fu.sep490.g23.backend.entity.course.PackageType;
import fu.sep490.g23.backend.entity.enums.RoleEnum;
import fu.sep490.g23.backend.entity.curriculum.CurriculumProgram;
import fu.sep490.g23.backend.entity.curriculum.CurriculumSessionPlan;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.course.LearningPackageRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseEnrollmentRepository;
import fu.sep490.g23.backend.repository.course.PackageTypeRepository;
import fu.sep490.g23.backend.repository.curriculum.CurriculumProgramRepository;
import fu.sep490.g23.backend.repository.curriculum.CurriculumSessionPlanRepository;
import fu.sep490.g23.backend.repository.course.CourseLessonRepository;
import fu.sep490.g23.backend.security.ClassroomAccessHelper;
import fu.sep490.g23.backend.service.classroom.ClassroomConflictService;
import fu.sep490.g23.backend.service.classroom.ClassroomMaterialSyncService;
import fu.sep490.g23.backend.service.classroom.ClassroomOfferingService;
import fu.sep490.g23.backend.service.course.CourseEnrollmentAccessPolicy;
import fu.sep490.g23.backend.service.course.InstructorLedCourseIdResolver;
import fu.sep490.g23.backend.service.notification.ClassroomNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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
    private static final Set<ClassroomRegistrationStatus> HAS_LEARNING_ACCESS = ClassroomRegistrationSupport.HAS_LEARNING_ACCESS;
    private static final int EMPTY_ROOM_GRACE_MINUTES = 5;
    private static final int SUBSTITUTE_PREPARATION_DAYS = 3;
    private static final int SUBSTITUTE_WRAP_UP_DAYS = 1;
    private static final int VIRTUAL_MEETING_RETRY_BATCH_SIZE = 8;
    private static final Set<ClassroomSessionStatus> VIRTUAL_MEETING_RETRY_SESSION_STATUSES = Set.of(
            ClassroomSessionStatus.SCHEDULED,
            ClassroomSessionStatus.OPEN,
            ClassroomSessionStatus.IN_PROGRESS,
            ClassroomSessionStatus.RESCHEDULED,
            ClassroomSessionStatus.MAKEUP
    );
    private static final Set<String> VIRTUAL_MEETING_RETRY_SYNC_STATUSES = Set.of(
            "PENDING",
            "FAILED",
            "DISABLED"
    );

    private final ClassSectionRepository offeringRepository;
    private final ClassScheduleRepository sessionRepository;
    private final ClassEnrollmentRepository enrollmentRepository;
    private final ClassroomTuitionPaymentRepository tuitionPaymentRepository;
    private final ClassroomTeacherAssignmentRepository teacherAssignmentRepository;
    private final ClassroomGradebookEntryRepository gradebookEntryRepository;
    private final LearningPackageRepository learningPackageRepository;
    private final PackageTypeRepository packageTypeRepository;
    private final OnlineCourseEnrollmentRepository packageEnrollmentRepository;
    private final CurriculumProgramRepository curriculumProgramRepository;
    private final CurriculumSessionPlanRepository curriculumSessionPlanRepository;
    private final TrainingProgramRepository trainingProgramRepository;
    private final ClassroomMaterialSyncService classroomMaterialSyncService;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final ClassroomMapper mapper;
    private final ClassroomConflictService conflictService;
    private final ClassroomScheduleLockService scheduleLockService;
    private final VirtualMeetingService virtualMeetingService;
    private final ClassroomAccessHelper accessHelper;
    private final ClassroomNotificationService notificationService;
    private final LarkMeetingParticipantRepository larkParticipantRepository;
    private final CourseEnrollmentAccessPolicy courseEnrollmentAccessPolicy;
    private final VirtualAttendanceService virtualAttendanceService;
    private final InstructorLedCourseIdResolver instructorLedCourseIdResolver;
    private final CourseLessonRepository courseLessonRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<ClassroomOfferingResponse> getPublicOfferings(ClassroomDeliveryMode mode, Pageable pageable) {
        return offeringRepository.findPublished(mode, pageable)
                .map(mapper::toOfferingResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ClassroomOfferingResponse getPublicOffering(String slugOrId) {
        ClassSection offering = findPublicOffering(slugOrId);
        return mapper.toPublicOfferingDetailResponse(offering);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomOfferingResponse> getMyClasses(String learnerEmail) {
        User learner = accessHelper.requireUser(learnerEmail);
        return enrollmentRepository.findByStudentIdAndRegistrationStatusIn(learner.getId(), HAS_LEARNING_ACCESS).stream()
                .map(ClassEnrollment::getClassSection)
                .map(offering -> mapper.toOfferingResponse(
                        offering,
                        false,
                        learner.getId(),
                        enrollmentRepository.findByStudentIdAndClassSectionId(learner.getId(), offering.getId()).orElse(null),
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
                .map(ClassroomTeacherAssignment::getClassSection)
                .distinct()
                .map(mapper::toOfferingResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomOfferingResponse> getStaffOfferings() {
        return offeringRepository.findAll().stream()
                .filter(this::isVisibleToStaff)
                .map(offering -> mapper.toOfferingResponse(offering, true, null, null, true))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ClassroomOfferingResponse getStaffOffering(Long id) {
        ClassSection offering = offeringRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp học."));
        if (offering.getLearningPackage() == null) {
            throw new RuntimeException("Không tìm thấy lớp học.");
        }
        return mapper.toOfferingResponse(offering, true, null, null, true);
    }

    @Override
    @Transactional(readOnly = true)
    public ClassroomOfferingResponse getOffering(Long id, boolean full) {
        ClassSection offering = findOffering(id);
        return mapper.toOfferingResponse(offering, full, null, null, full);
    }

    @Override
    @Transactional(readOnly = true)
    public ClassroomOfferingResponse getLearnerOffering(Long id, String learnerEmail) {
        User learner = accessHelper.requireUser(learnerEmail);
        ClassEnrollment enrollment = enrollmentRepository
                .findByStudentIdAndClassSectionId(learner.getId(), id)
                .filter(ClassEnrollment::hasClassAccess)
                .orElseThrow(() -> new RuntimeException("Bạn không có quyền truy cập lớp học này."));
        ClassSection offering = findOffering(id);
        return mapper.toOfferingResponse(offering, true, learner.getId(), enrollment, true);
    }

    @Override
    public ClassroomOfferingResponse createOffering(CreateClassroomOfferingRequest request, String creatorEmail) {
        validateOfferingRequest(request);
        User creator = accessHelper.requireUser(creatorEmail);
        accessHelper.assertManager(creator);

        PackageType packageType = packageTypeRepository.findByCode(PackageTypeCode.CLASSROOM)
                .orElseThrow(() -> new RuntimeException("Thiếu loại gói CLASSROOM trong hệ thống."));
        CurriculumProgram curriculumProgram = resolveCurriculumProgram(request.getCurriculumProgramId());
        TrainingProgram trainingProgram = resolveTrainingProgram(request.getTrainingProgramId());
        if (trainingProgram != null) {
            curriculumProgram = trainingProgram.getCurriculumProgram();
        }
        BigDecimal effectivePrice = request.getPrice() != null
                ? request.getPrice() : trainingProgram == null ? null : trainingProgram.getPrice();
        BigDecimal effectiveSalePrice = request.getSalePrice() != null
                ? request.getSalePrice() : trainingProgram == null ? null : trainingProgram.getSalePrice();
        validatePrices(effectivePrice, effectiveSalePrice);
        User primaryTeacher = resolveTeacher(request.getPrimaryTeacherId());
        Room regularRoom = request.getDeliveryMode() == ClassroomDeliveryMode.OFFLINE
                ? resolveRoom(request.getDefaultRoomId()) : null;
        validateOfferingResources(request, primaryTeacher, regularRoom);

        LearningPackage learningPackage = LearningPackage.builder()
                .packageType(packageType)
                .title(request.getTitle().trim())
                .slug(generateUniqueSlug(request.getTitle()))
                .shortDescription(defaultText(request.getShortDescription(), trainingProgram == null ? null : trainingProgram.getShortDescription()))
                .description(defaultText(request.getDescription(), trainingProgram == null ? null : trainingProgram.getDescription()))
                .targetScore(defaultText(request.getTargetScore(), resolveTargetScore(curriculumProgram)))
                .duration(defaultText(request.getDuration(), trainingProgram == null ? null : trainingProgram.getDuration()))
                .studyMode(defaultText(request.getStudyMode(), trainingProgram == null ? null : trainingProgram.getStudyMode()))
                .price(defaultBigDecimal(effectivePrice))
                .salePrice(effectiveSalePrice)
                .thumbnailUrl(defaultText(request.getThumbnailUrl(), trainingProgram == null ? null : trainingProgram.getThumbnailUrl()))
                .status(request.getPackageStatus() == null ? PackageStatus.DRAFT : request.getPackageStatus())
                .displayOrder(defaultInt(request.getDisplayOrder()))
                .featured(Boolean.TRUE.equals(request.getFeatured()))
                .createdBy(creator)
                .build();


        ClassSection offering = ClassSection.builder()
                .learningPackage(learningPackage)
                .deliveryMode(request.getDeliveryMode())
                .trainingProgram(trainingProgram)
                .curriculumProgram(curriculumProgram)
                .status(request.getClassroomStatus() == null ? ClassroomOfferingStatus.DRAFT : request.getClassroomStatus())
                .entryLevel(defaultText(request.getEntryLevel(), curriculumProgram == null ? null : curriculumProgram.getEntryLevel()))
                .targetOutcome(defaultText(request.getTargetOutcome(), curriculumProgram == null ? null : curriculumProgram.getOutcomes()))
                .capacity(request.getCapacity() == null ? 30 : request.getCapacity())
                .startDate(request.getStartDate())
                .plannedEndDate(request.getEndDate())
                .primaryTeacher(primaryTeacher)
                .regularRoom(regularRoom)
                .offlineAddress(request.getOfflineAddress())
                .locationNote(request.getLocationNote())
                .defaultLarkMeetingUrl(request.getDefaultLarkMeetingUrl())
                .larkMeetingStatus(virtualMeetingService.resolveStatus(request.getDefaultLarkMeetingUrl()))
                .recordingUrl(request.getRecordingUrl())
                .recordingVisible(Boolean.TRUE.equals(request.getRecordingVisible()))
                .syllabusSummary(defaultText(request.getSyllabusSummary(), curriculumProgram == null ? null : curriculumProgram.getOutcomes()))
                .programOutcomes(curriculumProgram == null ? null : curriculumProgram.getOutcomes())
                .teacherGuide(curriculumProgram == null ? null : curriculumProgram.getTeacherGuide())
                .interactionActivities(curriculumProgram == null ? null : curriculumProgram.getInteractionActivities())
                .build();

        linkInstructorLedCourse(offering);

        if (offering.getPrimaryTeacher() != null) {
            offering = offeringRepository.save(offering);
            assignTeacherInternal(offering, offering.getPrimaryTeacher(), ClassroomTeacherRole.PRIMARY, "Giáo viên chính khi tạo lớp");
            classroomMaterialSyncService.synchronizeMandatoryMaterials(offering, creator);
            return mapper.toOfferingResponse(offering, true, null, null, true);
        }

        ClassSection saved = offeringRepository.save(offering);
        classroomMaterialSyncService.synchronizeMandatoryMaterials(saved, creator);
        return mapper.toOfferingResponse(saved, true, null, null, true);
    }

    @Override
    public ClassroomOfferingResponse updateOffering(Long id, CreateClassroomOfferingRequest request, String actorEmail) {
        validateOfferingRequest(request);
        User actor = accessHelper.requireUser(actorEmail);
        accessHelper.assertStaffOperator(actor);
        ClassSection offering = findOffering(id);
        LearningPackage learningPackage = offering.getLearningPackage();
        CurriculumProgram curriculumProgram = resolveCurriculumProgram(request.getCurriculumProgramId());
        TrainingProgram trainingProgram = resolveTrainingProgram(request.getTrainingProgramId());
        if (trainingProgram != null) {
            curriculumProgram = trainingProgram.getCurriculumProgram();
        }
        BigDecimal effectivePrice = request.getPrice() != null
                ? request.getPrice() : trainingProgram == null ? null : trainingProgram.getPrice();
        BigDecimal effectiveSalePrice = request.getSalePrice() != null
                ? request.getSalePrice() : trainingProgram == null ? null : trainingProgram.getSalePrice();
        validatePrices(effectivePrice, effectiveSalePrice);
        User primaryTeacher = resolveTeacher(request.getPrimaryTeacherId());
        Long previousPrimaryTeacherId = getPrimaryTeacherId(offering);
        Long requestedPrimaryTeacherId = primaryTeacher == null ? null : primaryTeacher.getId();
        boolean primaryTeacherChanged = !Objects.equals(previousPrimaryTeacherId, requestedPrimaryTeacherId);
        if (primaryTeacherChanged && primaryTeacher == null) {
            throw new IllegalArgumentException(
                    "Không thể bỏ giáo viên chính khỏi lớp. Hãy chọn giáo viên thay thế để giữ lịch học liên tục."
            );
        }
        Room regularRoom = request.getDeliveryMode() == ClassroomDeliveryMode.OFFLINE
                ? resolveRoom(request.getDefaultRoomId()) : null;
        validateOfferingResources(request, primaryTeacher, regularRoom);

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
        learningPackage.setTargetScore(defaultText(request.getTargetScore(), resolveTargetScore(curriculumProgram)));
        learningPackage.setDuration(defaultText(request.getDuration(), trainingProgram == null ? null : trainingProgram.getDuration()));
        learningPackage.setStudyMode(defaultText(request.getStudyMode(), trainingProgram == null ? null : trainingProgram.getStudyMode()));
        learningPackage.setPrice(defaultBigDecimal(effectivePrice));
        learningPackage.setSalePrice(effectiveSalePrice);
        learningPackage.setThumbnailUrl(defaultText(request.getThumbnailUrl(), trainingProgram == null ? null : trainingProgram.getThumbnailUrl()));
        if (request.getPackageStatus() != null) {
            learningPackage.setStatus(request.getPackageStatus());
        }
        learningPackage.setDisplayOrder(defaultInt(request.getDisplayOrder()));
        learningPackage.setFeatured(Boolean.TRUE.equals(request.getFeatured()));

        offering.setDeliveryMode(request.getDeliveryMode());
        offering.setTrainingProgram(trainingProgram);
        offering.setCurriculumProgram(curriculumProgram);
        linkInstructorLedCourse(offering);
        if (request.getClassroomStatus() != null) {
            offering.setStatus(request.getClassroomStatus());
        }
        offering.setEntryLevel(defaultText(request.getEntryLevel(), curriculumProgram == null ? null : curriculumProgram.getEntryLevel()));
        offering.setTargetOutcome(defaultText(request.getTargetOutcome(), curriculumProgram == null ? null : curriculumProgram.getOutcomes()));
        if (request.getCapacity() != null) {
            offering.setCapacity(request.getCapacity());
        }
        offering.setStartDate(request.getStartDate());
        offering.setPlannedEndDate(request.getEndDate());
        if (!primaryTeacherChanged) {
            offering.setPrimaryTeacher(primaryTeacher);
        }
        offering.setRegularRoom(regularRoom);
        offering.setOfflineAddress(request.getOfflineAddress());
        offering.setLocationNote(request.getLocationNote());
        offering.setDefaultLarkMeetingUrl(request.getDefaultLarkMeetingUrl());
        offering.setLarkMeetingStatus(virtualMeetingService.resolveStatus(request.getDefaultLarkMeetingUrl()));
        offering.setRecordingUrl(request.getRecordingUrl());
        if (request.getRecordingVisible() != null) {
            offering.setRecordingVisible(request.getRecordingVisible());
        }
        offering.setSyllabusSummary(defaultText(request.getSyllabusSummary(), curriculumProgram == null ? null : curriculumProgram.getOutcomes()));
        offering.setProgramOutcomes(curriculumProgram == null ? null : curriculumProgram.getOutcomes());
        offering.setTeacherGuide(curriculumProgram == null ? null : curriculumProgram.getTeacherGuide());
        offering.setInteractionActivities(curriculumProgram == null ? null : curriculumProgram.getInteractionActivities());

        ClassSection saved = offeringRepository.save(offering);
        if (primaryTeacherChanged) {
            replaceTeacher(saved.getId(), previousPrimaryTeacherId, requestedPrimaryTeacherId);
            saved = findOffering(saved.getId());
        }
        classroomMaterialSyncService.synchronizeMandatoryMaterials(saved, null);
        return mapper.toOfferingResponse(saved, true, null, null, true);
    }

    @Override
    public ClassroomOfferingResponse closeOffering(Long id, String actorEmail) {
        accessHelper.requireUser(actorEmail);
        ClassSection offering = findOffering(id);
        if (offering.getStatus() == ClassroomOfferingStatus.CLOSED
                || offering.getStatus() == ClassroomOfferingStatus.CANCELLED) {
            throw new RuntimeException("Lớp học đã được đóng hoặc hủy trước đó.");
        }
        offering.setStatus(ClassroomOfferingStatus.CLOSED);
        offering.getLearningPackage().setStatus(PackageStatus.ARCHIVED);
        return mapper.toOfferingResponse(offeringRepository.save(offering));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomSessionResponse> getSessions(Long offeringId) {
        return sessionRepository.findByClassSectionIdOrderBySessionDateAscStartTimeAsc(offeringId).stream()
                .map(mapper::toSessionResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ClassroomSessionResponse getSession(Long sessionId) {
        return mapper.toSessionResponse(findSession(sessionId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomSessionResponse> getLearnerSessions(Long offeringId, String learnerEmail) {
        User learner = accessHelper.requireUser(learnerEmail);
        ClassEnrollment enrollment = enrollmentRepository
                .findByStudentIdAndClassSectionId(learner.getId(), offeringId)
                .orElseThrow(() -> new RuntimeException("Bạn không thuộc lớp học này."));
        if (!enrollment.hasClassAccess()) {
            throw new RuntimeException("Bạn chưa được cấp quyền truy cập lớp học này.");
        }

        return sessionRepository.findByClassSectionIdOrderBySessionDateAscStartTimeAsc(offeringId).stream()
                .map(mapper::toSessionResponse)
                .toList();
    }

    @Override
    public ClassroomSessionResponse createSession(Long offeringId, CreateClassroomSessionRequest request) {
        return createSessionInternal(offeringId, request, true);
    }

    @Override
    public ClassroomSessionResponse createSession(
            Long offeringId,
            CreateClassroomSessionRequest request,
            boolean enforceConflictCheck
    ) {
        return createSessionInternal(offeringId, request, enforceConflictCheck);
    }

    private ClassroomSessionResponse createSessionInternal(
            Long offeringId,
            CreateClassroomSessionRequest request,
            boolean enforceConflictCheck
    ) {
        validateSessionRequest(request);
        scheduleLockService.lockDate(request.getSessionDate());

        ClassSection offering = findOffering(offeringId);
        User teacher = resolveTeacher(request.getTeacherId() != null ? request.getTeacherId() : getPrimaryTeacherId(offering));
        ClassroomDeliveryMode deliveryMode = resolveSessionDeliveryMode(request, offering);
        Room room = resolveSessionRoom(request, offering, deliveryMode);
        CurriculumSessionPlan sessionPlan = resolveCurriculumSessionPlan(
                request.getCurriculumSessionPlanId(),
                offering
        );
        validateRoomCapacity(room, offering.getCapacity());
        if (deliveryMode == ClassroomDeliveryMode.VIRTUAL) {
            ensureTeacherMeetingOwner(offering);
        }

        if (enforceConflictCheck) {
            ConflictCheckRequest conflictRequest = ConflictCheckRequest.builder()
                    .classSectionId(offeringId)
                    .teacherId(teacher == null ? null : teacher.getId())
                    .roomId(room == null ? null : room.getId())
                    .sessionDate(request.getSessionDate())
                    .startTime(request.getStartTime())
                    .endTime(request.getEndTime())
                    .learnerIds(resolveActiveLearnerIds(offeringId))
                    .checkCapacity(false)
                    .build();
            conflictService.assertNoBlockingConflict(conflictRequest);
        }

        ClassSchedule session = buildSession(offering, request, teacher, room, deliveryMode, sessionPlan);
        session = sessionRepository.save(session);
        synchronizeSubstituteAssignment(session);
        if (session.getDeliveryMode() == ClassroomDeliveryMode.VIRTUAL
                && needsManagedVirtualMeetingSync(session)) {
            syncVirtualMeetingSafely(session);
            session = sessionRepository.save(session);
        }
        return mapper.toSessionResponse(session);
    }

    @Override
    public ClassroomSessionResponse syncVirtualSessionMeeting(Long sessionId, String actorEmail) {
        User actor = accessHelper.requireUser(actorEmail);
        accessHelper.assertStaffOperator(actor);
        ClassSchedule session = findSession(sessionId);
        ensureTeacherMeetingOwner(session.getClassSection());
        if (session.getDeliveryMode() != ClassroomDeliveryMode.VIRTUAL) {
            throw new IllegalArgumentException("Chỉ buổi học trực tuyến mới có phòng Google Meet để đồng bộ.");
        }
        if (session.getStatus() == ClassroomSessionStatus.COMPLETED
                || session.getStatus() == ClassroomSessionStatus.CANCELLED) {
            throw new IllegalArgumentException("Không thể tạo lại phòng Google Meet cho buổi học đã kết thúc hoặc đã hủy.");
        }
        syncVirtualMeetingSafely(session);
        return mapper.toSessionResponse(sessionRepository.save(session));
    }

    @Override
    public ClassroomSessionResponse updateSession(Long sessionId, CreateClassroomSessionRequest request) {
        validateSessionRequest(request);
        ClassSchedule session = findSession(sessionId);
        scheduleLockService.lockDates(List.of(session.getSessionDate(), request.getSessionDate()));
        if (session.isLocked() || session.getStatus() == ClassroomSessionStatus.COMPLETED) {
            throw new RuntimeException("Buổi học đã hoàn thành hoặc đã khóa nên không thể chỉnh sửa.");
        }

        User teacher = resolveTeacher(request.getTeacherId() != null ? request.getTeacherId() : getPrimaryTeacherId(session.getClassSection()));
        ClassroomDeliveryMode deliveryMode = resolveSessionDeliveryMode(request, session.getClassSection());
        Room room = resolveSessionRoom(request, session.getClassSection(), deliveryMode);
        CurriculumSessionPlan sessionPlan = request.getCurriculumSessionPlanId() == null
                ? session.getCurriculumSessionPlan()
                : resolveCurriculumSessionPlan(request.getCurriculumSessionPlanId(), session.getClassSection());
        validateRoomCapacity(room, session.getClassSection().getCapacity());

        ConflictCheckRequest conflictRequest = ConflictCheckRequest.builder()
                .classSectionId(session.getClassSection().getId())
                .sessionId(sessionId)
                .excludeSessionId(sessionId)
                .teacherId(teacher == null ? null : teacher.getId())
                .roomId(room == null ? null : room.getId())
                .sessionDate(request.getSessionDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .learnerIds(resolveActiveLearnerIds(session.getClassSection().getId()))
                .checkCapacity(false)
                .checkSessionLocked(true)
                .checkCapacity(false)
                .build();
        conflictService.assertNoBlockingConflict(conflictRequest);

        boolean manualLarkLinkProvided = request.getLarkMeetingUrl() != null
                && !request.getLarkMeetingUrl().isBlank()
                && !virtualMeetingService.isLegacyOrPlaceholderUrl(request.getLarkMeetingUrl());
        if (manualLarkLinkProvided && session.getLarkMeetingId() != null) {
            deleteVirtualMeetingSafely(session);
            clearManagedVirtualMeetingData(session);
        }

        applySessionRequest(session, request, teacher, room, deliveryMode, sessionPlan);
        session = sessionRepository.save(session);
        synchronizeSubstituteAssignment(session);
        if (session.getDeliveryMode() == ClassroomDeliveryMode.VIRTUAL && !manualLarkLinkProvided) {
            syncVirtualMeetingSafely(session);
        }
        return mapper.toSessionResponse(sessionRepository.save(session));
    }

    @Override
    public ClassroomSessionResponse applyApprovedSessionScheduleChange(Long sessionId, CreateClassroomSessionRequest request) {
        validateSessionRequest(request);
        ClassSchedule session = findSession(sessionId);
        scheduleLockService.lockDates(List.of(session.getSessionDate(), request.getSessionDate()));
        if (session.isLocked() || session.getStatus() == ClassroomSessionStatus.COMPLETED) {
            throw new RuntimeException("Buổi học đã hoàn thành hoặc đã khóa nên không thể chỉnh sửa.");
        }

        User teacher = resolveTeacher(request.getTeacherId() != null ? request.getTeacherId() : getPrimaryTeacherId(session.getClassSection()));
        ClassroomDeliveryMode deliveryMode = resolveSessionDeliveryMode(request, session.getClassSection());
        Room room = resolveSessionRoom(request, session.getClassSection(), deliveryMode);
        CurriculumSessionPlan sessionPlan = request.getCurriculumSessionPlanId() == null
                ? session.getCurriculumSessionPlan()
                : resolveCurriculumSessionPlan(request.getCurriculumSessionPlanId(), session.getClassSection());
        validateRoomCapacity(room, session.getClassSection().getCapacity());
        boolean manualLarkLinkProvided = request.getLarkMeetingUrl() != null
                && !request.getLarkMeetingUrl().isBlank()
                && !virtualMeetingService.isLegacyOrPlaceholderUrl(request.getLarkMeetingUrl());
        if (manualLarkLinkProvided && session.getLarkMeetingId() != null) {
            deleteVirtualMeetingSafely(session);
            clearManagedVirtualMeetingData(session);
        }

        applySessionRequest(session, request, teacher, room, deliveryMode, sessionPlan);
        session = sessionRepository.save(session);
        synchronizeSubstituteAssignment(session);
        if (session.getDeliveryMode() == ClassroomDeliveryMode.VIRTUAL && !manualLarkLinkProvided) {
            syncVirtualMeetingSafely(session);
        }
        if (session.getStatus() == ClassroomSessionStatus.RESCHEDULED) {
            session.setStatus(ClassroomSessionStatus.SCHEDULED);
        }
        return mapper.toSessionResponse(sessionRepository.save(session));
    }

    @Override
    public void deleteSession(Long sessionId) {
        ClassSchedule session = findSession(sessionId);
        if (session.isLocked()) {
            throw new RuntimeException("Buổi học đã khóa nên không thể xóa.");
        }
        deleteVirtualMeetingSafely(session);
        teacherAssignmentRepository.findByClassScheduleId(sessionId)
                .ifPresent(teacherAssignmentRepository::delete);
        sessionRepository.delete(session);
    }

    @Override
    public ClassroomEnrollmentResponse enrollStudent(Long offeringId, EnrollStudentRequest request) {
        ClassSection offering = findOffering(offeringId);
        User student = userRepository.findById(request.getStudentId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy học viên."));

        ConflictCheckRequest conflictRequest = ConflictCheckRequest.builder()
                .classSectionId(offeringId)
                .learnerIds(List.of(student.getId()))
                .checkCapacity(false)
                .build();
        conflictService.assertNoBlockingConflict(conflictRequest);

        BigDecimal tuitionDue = resolveTuitionDue(offering);
        ClassEnrollment enrollment = enrollmentRepository
                .findByStudentIdAndClassSectionId(student.getId(), offeringId)
                .orElseGet(() -> ClassEnrollment.builder()
                        .student(student)
                        .classSection(offering)
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
        enrollment.setPackageEnrollment(ensureOnlineCourseEnrollment(student, offering));

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
        ClassEnrollment enrollment = enrollmentRepository.findByStudentIdAndClassSectionId(studentId, offeringId)
                .orElseThrow(() -> new RuntimeException("Học viên không thuộc lớp này."));
        ClassroomRegistrationStatus previousStatus = enrollment.getRegistrationStatus();
        enrollment.setRegistrationStatus(ClassroomRegistrationStatus.CANCELLED);
        ClassroomRegistrationSupport.syncLegacyStatus(enrollment);
        ClassroomRegistrationSupport.markNeedRefundForExit(enrollment, "Cần xử lý hoàn tiền do xóa khỏi lớp");
        saveEnrollmentWithWaitlistOrder(enrollment, previousStatus);
        notifyWaitlistIfSlotAvailable(enrollment.getClassSection());
    }

    @Override
    @SuppressWarnings("deprecation")
    public ClassroomEnrollmentResponse transferStudent(Long offeringId, TransferStudentRequest request) {
        ClassSection source = findOffering(offeringId);
        ClassSection target = findOffering(request.getTargetClassSectionId());
        User student = userRepository.findById(request.getStudentId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy học viên."));

        ClassEnrollment sourceEnrollment = enrollmentRepository.findByStudentIdAndClassSectionId(student.getId(), offeringId)
                .filter(enrollment -> ACTIVE_REGISTRATIONS.contains(enrollment.getRegistrationStatus()))
                .orElseThrow(() -> new RuntimeException("Học viên không có đăng ký hợp lệ ở lớp nguồn."));

        ConflictCheckRequest conflictRequest = ConflictCheckRequest.builder()
                .targetClassSectionId(target.getId())
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

        if (enrollmentRepository.existsByStudentIdAndClassSectionIdAndRegistrationStatusIn(
                student.getId(), target.getId(), ACTIVE_REGISTRATIONS)) {
            throw new RuntimeException("Học viên đã có đăng ký ở lớp đích.");
        }

        ClassEnrollment targetEnrollment = ClassEnrollment.builder()
                .student(student)
                .classSection(target)
                .packageEnrollment(ensureOnlineCourseEnrollment(student, target))
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

    /**
     * Báo cho học viên trong danh sách chờ khi lớp vừa trống chỗ (sau khi có người hủy/bị từ chối).
     */
    private void notifyWaitlistIfSlotAvailable(ClassSection offering) {
        if (offering.getCapacity() == null || offering.getCapacity() <= 0) {
            return;
        }
        long occupied = enrollmentRepository.countByOfferingAndRegistrationStatuses(
                offering.getId(), ClassroomRegistrationSupport.OCCUPIES_CLASS_SLOT);
        if (occupied >= offering.getCapacity()) {
            return;
        }
        List<ClassEnrollment> waitlisted = enrollmentRepository
                .findByClassSectionIdAndRegistrationStatusIn(
                        offering.getId(), Set.of(ClassroomRegistrationStatus.WAITLIST));
        if (waitlisted.isEmpty()) {
            return;
        }
        String classTitle = offering.getLearningPackage().getTitle();
        for (ClassEnrollment waiting : waitlisted) {
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
    public ClassroomEnrollmentResponse confirmRegistration(Long enrollmentId, String actorEmail) {
        User actor = accessHelper.requireUser(actorEmail);
        accessHelper.assertStaffOperator(actor);
        ClassEnrollment enrollment = findEnrollment(enrollmentId);
        ClassSection offering = enrollment.getClassSection();
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
        accessHelper.assertStaffOperator(actor);
        ClassEnrollment enrollment = findEnrollment(enrollmentId);
        ClassSection offering = enrollment.getClassSection();
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
        accessHelper.assertStaffOperator(actor);
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
        accessHelper.assertStaffOperator(actor);
        ClassEnrollment enrollment = findEnrollment(enrollmentId);

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
            ClassEnrollment enrollment,
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
                "Yêu cầu hoàn học phí lớp " + enrollment.getClassSection().getLearningPackage().getTitle()
                        + " đã được duyệt: " + refundAmount.toPlainString() + " VND.",
                Map.of(
                        "enrollmentId", enrollment.getId(),
                        "classroomId", enrollment.getClassSection().getId(),
                        "refundAmount", refundAmount
                )
        );
        return mapper.toEnrollmentResponse(enrollment);
    }

    private ClassroomEnrollmentResponse rejectTuitionRefund(
            ClassEnrollment enrollment,
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
                "Yêu cầu hoàn học phí lớp " + enrollment.getClassSection().getLearningPackage().getTitle()
                        + " đã bị từ chối. Lý do: " + note,
                Map.of(
                        "enrollmentId", enrollment.getId(),
                        "classroomId", enrollment.getClassSection().getId()
                )
        );
        return mapper.toEnrollmentResponse(enrollment);
    }

    @Override
    public ClassroomEnrollmentResponse applyPayosTuitionPayment(Long enrollmentId, BigDecimal amount, String note) {
        ClassEnrollment enrollment = findEnrollment(enrollmentId);
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
            ClassEnrollment enrollment,
            BigDecimal amount,
            TuitionPaymentKind paymentKind,
            String note,
            User recordedBy,
            boolean assignIfFullyPaid
    ) {
        ClassSection offering = enrollment.getClassSection();
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
        accessHelper.assertStaffOperator(actor);
        ClassEnrollment enrollment = findEnrollment(enrollmentId);
        ClassSection offering = enrollment.getClassSection();
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
            Long classSectionId,
            Boolean needsAction,
            Boolean settlementPending
    ) {
        if (Boolean.TRUE.equals(settlementPending)) {
            List<ClassEnrollment> pendingSettlements = classSectionId == null
                    ? enrollmentRepository.findByTuitionSettlementStatus(TuitionSettlementStatus.PENDING)
                    : enrollmentRepository.findByClassSectionIdAndTuitionSettlementStatus(
                            classSectionId,
                            TuitionSettlementStatus.PENDING
                    );
            return pendingSettlements.stream()
                    .sorted(registrationQueueComparator())
                    .map(mapper::toEnrollmentResponse)
                    .toList();
        }

        Set<ClassroomRegistrationStatus> statuses = ClassroomRegistrationSupport.resolveRegistrationFilter(status, needsAction);
        List<ClassEnrollment> enrollments = classSectionId == null
                ? enrollmentRepository.findByRegistrationStatusIn(statuses)
                : enrollmentRepository.findByClassSectionIdAndRegistrationStatusIn(classSectionId, statuses);
        return enrollments.stream()
                .sorted(registrationQueueComparator())
                .map(mapper::toEnrollmentResponse)
                .toList();
    }

    @Override
    public List<ClassroomEnrollmentResponse> reorderWaitlist(
            Long classSectionId,
            ReorderWaitlistRequest request,
            String actorEmail
    ) {
        User actor = accessHelper.requireUser(actorEmail);
        accessHelper.assertStaffOperator(actor);
        offeringRepository.findByIdForUpdate(classSectionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lớp học không tồn tại."));

        List<ClassEnrollment> waitlist = getOrderedWaitlist(classSectionId);
        List<Long> requestedIds = request.getEnrollmentIds();
        Set<Long> requestedIdSet = new HashSet<>(requestedIds);
        Set<Long> currentIdSet = waitlist.stream()
                .map(ClassEnrollment::getId)
                .collect(java.util.stream.Collectors.toSet());
        if (requestedIds.size() != requestedIdSet.size()
                || requestedIds.size() != waitlist.size()
                || !requestedIdSet.equals(currentIdSet)) {
            throw new RuntimeException("Thứ tự mới phải chứa đúng toàn bộ học viên đang trong danh sách chờ.");
        }

        Map<Long, ClassEnrollment> enrollmentById = waitlist.stream()
                .collect(java.util.stream.Collectors.toMap(ClassEnrollment::getId, item -> item));
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
        ClassEnrollment enrollment = findEnrollment(enrollmentId);
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
        accessHelper.assertStaffOperator(actor);
        ClassEnrollment enrollment = findEnrollment(enrollmentId);
        TransferStudentRequest transferRequest = new TransferStudentRequest();
        transferRequest.setStudentId(enrollment.getStudent().getId());
        transferRequest.setTargetClassSectionId(request.getTargetClassSectionId());
        transferRequest.setNote(request.getNote());
        ClassroomEnrollmentResponse response = transferStudent(enrollment.getClassSection().getId(), transferRequest);

        notificationService.notifyUser(
                enrollment.getStudent(),
                "CLASSROOM_TRANSFERRED",
                "Đã chuyển lớp",
                "Đăng ký của bạn đã được chuyển sang lớp mới.",
                Map.of("enrollmentId", response.getId(), "classroomId", response.getClassSectionId())
        );
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public ConflictCheckResultResponse checkEnrollmentConflict(Long enrollmentId) {
        ClassEnrollment enrollment = findEnrollment(enrollmentId);
        ClassSection offering = enrollment.getClassSection();
        ConflictCheckRequest request = ConflictCheckRequest.builder()
                .classSectionId(offering.getId())
                .learnerIds(List.of(enrollment.getStudent().getId()))
                .checkCapacity(true)
                .build();
        return conflictService.check(request);
    }

    @Override
    public ClassroomTeacherSummaryResponse assignTeacher(Long offeringId, Long teacherId, ClassroomTeacherRole role) {
        ClassSection offering = findOffering(offeringId);
        User teacher = resolveTeacher(teacherId);
        if (teacher == null) {
            throw new RuntimeException("Giáo viên không hợp lệ.");
        }
        ClassroomTeacherRole resolvedRole = role == null ? ClassroomTeacherRole.PRIMARY : role;
        Long previousPrimaryTeacherId = getPrimaryTeacherId(offering);
        if (resolvedRole == ClassroomTeacherRole.PRIMARY
                && !teacher.getId().equals(previousPrimaryTeacherId)) {
            return replaceTeacher(offeringId, previousPrimaryTeacherId, teacher.getId());
        }
        if (resolvedRole == ClassroomTeacherRole.PRIMARY) {
            deactivateOtherPrimaryTeachers(offering, teacher.getId(), "Thay đổi giáo viên chính");
            offering.setPrimaryTeacher(teacher);
            offeringRepository.save(offering);
        }
        ClassroomTeacherSummaryResponse response = assignTeacherInternal(offering, teacher, resolvedRole, null);
        return response;
    }

    @Override
    public ClassroomTeacherSummaryResponse replaceTeacher(Long offeringId, Long oldTeacherId, Long newTeacherId) {
        ClassSection offering = findOffering(offeringId);
        User newTeacher = resolveTeacher(newTeacherId);
        if (newTeacher == null) {
            throw new RuntimeException("Giáo viên mới không hợp lệ.");
        }
        if (oldTeacherId != null && oldTeacherId.equals(newTeacherId)) {
            return assignTeacherInternal(offering, newTeacher, ClassroomTeacherRole.PRIMARY, "Tiếp tục phân công");
        }

        validateReplacementTeacherAvailability(offering, oldTeacherId, newTeacher);

        if (oldTeacherId != null) {
            teacherAssignmentRepository.findAllByClassSectionIdAndTeacherId(offeringId, oldTeacherId).stream()
                    .filter(this::isTeacherAssignmentActive)
                    .filter(assignment -> assignment.getClassSchedule() == null)
                    .findFirst()
                    .ifPresent(assignment -> {
                        assignment.setEffectiveTo(LocalDate.now().minusDays(1));
                        assignment.setReason("Kết thúc phân công do thay giáo viên");
                        teacherAssignmentRepository.save(assignment);
                    });
        }
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
        ClassSchedule session = findSession(sessionId);
        User actor = assertCanManageVirtualSession(session, actorEmail);
        if (session.getDeliveryMode() != ClassroomDeliveryMode.VIRTUAL) {
            throw new RuntimeException("Chỉ buổi học trực tuyến mới có thể mở phòng ảo.");
        }
        if (session.getLarkMeetingUrl() == null
                || session.getLarkMeetingUrl().isBlank()
                || virtualMeetingService.isLegacyOrPlaceholderUrl(session.getLarkMeetingUrl())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Staff chưa tạo liên kết Google Meet cho buổi học này."
            );
        }
        inviteTeacher(session, actor);
        session.setStatus(ClassroomSessionStatus.OPEN);
        session.setLarkMeetingStatus(LarkMeetingStatus.OPEN);
        return mapper.toSessionResponse(sessionRepository.save(session));
    }

    @Override
    public ClassroomSessionResponse joinVirtualSession(Long sessionId, String learnerEmail) {
        User learner = accessHelper.requireUser(learnerEmail);
        ClassSchedule session = findSession(sessionId);

        if (session.getDeliveryMode() != ClassroomDeliveryMode.VIRTUAL) {
            throw new RuntimeException("Buổi học này không phải lớp học trực tuyến.");
        }
        if (session.getStatus() == ClassroomSessionStatus.CANCELLED) {
            throw new RuntimeException("Buổi học đã bị hủy.");
        }

        ClassEnrollment enrollment = enrollmentRepository
                .findByStudentIdAndClassSectionId(
                        learner.getId(),
                        session.getClassSection().getId()
                )
                .orElseThrow(() -> new RuntimeException("Bạn không thuộc lớp học này."));
        if (!enrollment.hasClassAccess()) {
            throw new RuntimeException("Bạn chưa được cấp quyền tham gia lớp học này.");
        }

        if (session.getLarkMeetingUrl() == null
                || session.getLarkMeetingUrl().isBlank()
                || virtualMeetingService.isLegacyOrPlaceholderUrl(session.getLarkMeetingUrl())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Staff chưa tạo liên kết Google Meet cho buổi học này."
            );
        }

        session.setLarkEmptySince(null);
        session.setLarkMeetingStatus(LarkMeetingStatus.OPEN);
        virtualAttendanceService.recordVirtualJoin(session, learner);
        return mapper.toSessionResponse(sessionRepository.save(session));
    }

    @Override
    public ClassroomSessionResponse joinVirtualClass(Long offeringId, Long sessionId, String learnerEmail) {
        ClassSchedule session = findSession(sessionId);
        if (!session.getClassSection().getId().equals(offeringId)) {
            throw new RuntimeException("Buổi học không thuộc lớp đã chọn.");
        }
        return joinVirtualSession(sessionId, learnerEmail);
    }

    @Override
    public ClassroomSessionResponse closeVirtualSession(Long sessionId, String actorEmail) {
        ClassSchedule session = findSession(sessionId);
        assertCanManageVirtualSession(session, actorEmail);
        markVirtualSessionEnded(session);
        return mapper.toSessionResponse(sessionRepository.save(session));
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void closeEmptyVirtualRooms() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(EMPTY_ROOM_GRACE_MINUTES);
        List<ClassSchedule> emptyRooms =
                sessionRepository.findByLarkEmptySinceIsNotNullAndLarkEmptySinceBefore(cutoff);

        for (ClassSchedule session : emptyRooms) {
            if (larkParticipantRepository.countByClassScheduleIdAndActiveTrue(session.getId()) > 0) {
                session.setLarkEmptySince(null);
                continue;
            }
            deleteVirtualMeetingSafely(session);
            larkParticipantRepository.deleteByClassScheduleId(session.getId());
            clearManagedVirtualMeetingData(session);
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

    /**
     * Tự khôi phục các buổi online chưa được cấp phòng do Google Meet tạm lỗi,
     * hết quota hoặc do tích hợp được bật sau khi buổi học đã được tạo.
     *
     * Mỗi lần chỉ xử lý một lô nhỏ để không vượt quota spaces.create theo phút.
     * Không giữ một transaction dài quanh các HTTP request tới Google.
     */
    @Scheduled(
            fixedDelayString = "${englishlab.google-meet.retry-delay-ms:300000}",
            initialDelayString = "${englishlab.google-meet.retry-initial-delay-ms:60000}"
    )
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void retryPendingVirtualMeetings() {
        if (!virtualMeetingService.isEnabled()) {
            return;
        }

        List<ClassSchedule> pendingSessions = sessionRepository.findVirtualMeetingsPendingSync(
                ClassroomDeliveryMode.VIRTUAL,
                VIRTUAL_MEETING_RETRY_SESSION_STATUSES,
                VIRTUAL_MEETING_RETRY_SYNC_STATUSES,
                LocalDate.now(),
                PageRequest.of(0, VIRTUAL_MEETING_RETRY_BATCH_SIZE)
        );

        for (ClassSchedule session : pendingSessions) {
            if (!requiresManagedVirtualMeeting(session)) {
                continue;
            }
            try {
                syncVirtualMeetingSafely(session);
                sessionRepository.save(session);
            } catch (RuntimeException ex) {
                log.error(
                        "Không thể lưu kết quả tự tạo lại Google Meet cho buổi học {}: {}",
                        session.getId(),
                        ex.getMessage()
                );
            }
        }
    }

    @Override
    public ClassroomSessionResponse updateSessionLarkLink(Long sessionId, UpdateLarkLinkRequest request) {
        ClassSchedule session = findSession(sessionId);
        if (session.isLocked()) {
            throw new RuntimeException("Buổi học đã khóa nên không thể cập nhật liên kết Google Meet.");
        }
        if (session.getLarkMeetingId() != null) {
            deleteVirtualMeetingSafely(session);
            clearManagedVirtualMeetingData(session);
        }
        session.setLarkMeetingUrl(request.getLarkMeetingUrl());
        session.setLarkMeetingStatus(virtualMeetingService.resolveStatus(request.getLarkMeetingUrl()));
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
            ClassSection offering,
            User teacher,
            ClassroomTeacherRole role,
            String reason
    ) {
        ClassroomTeacherAssignment assignment = teacherAssignmentRepository
                .findAllByClassSectionIdAndTeacherId(offering.getId(), teacher.getId())
                .stream()
                .filter(this::isTeacherAssignmentActive)
                .filter(candidate -> candidate.getClassSchedule() == null)
                .filter(candidate -> candidate.getRole() == role)
                .findFirst()
                .orElseGet(() -> ClassroomTeacherAssignment.builder()
                        .classSection(offering)
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

    private void deactivateOtherPrimaryTeachers(ClassSection offering, Long activeTeacherId, String reason) {
        LocalDate endedOn = LocalDate.now().minusDays(1);
        teacherAssignmentRepository.findByClassSectionId(offering.getId()).stream()
                .filter(this::isTeacherAssignmentActive)
                .filter(assignment -> assignment.getRole() == ClassroomTeacherRole.PRIMARY)
                .filter(assignment -> assignment.getClassSchedule() == null)
                .filter(assignment -> !assignment.getTeacher().getId().equals(activeTeacherId))
                .forEach(assignment -> {
                    assignment.setEffectiveTo(endedOn);
                    assignment.setReason(reason);
                    teacherAssignmentRepository.save(assignment);
                });
    }

    private void updateUpcomingSessionsForTeacherChange(
            ClassSection offering,
            Long oldTeacherId,
            User newTeacher
    ) {
        LocalDate today = LocalDate.now();
        List<ClassSchedule> schedules =
                sessionRepository.findByClassSectionIdOrderBySessionDateAscStartTimeAsc(offering.getId());

        schedules.stream()
                .filter(session -> !session.getSessionDate().isBefore(today))
                .filter(session -> session.getStatus() != ClassroomSessionStatus.COMPLETED)
                .filter(session -> session.getStatus() != ClassroomSessionStatus.CANCELLED)
                .filter(session -> oldTeacherId == null
                        || session.getTeacher() == null
                        || oldTeacherId.equals(session.getTeacher().getId()))
                .forEach(session -> {
                    session.setTeacher(newTeacher);
                    if (session.getDeliveryMode() == ClassroomDeliveryMode.VIRTUAL
                            && session.getLarkMeetingId() != null) {
                        syncVirtualMeetingSafely(session);
                        inviteTeacherSafely(session, newTeacher);
                    }
                });
        sessionRepository.saveAll(schedules);
    }

    private void validateReplacementTeacherAvailability(
            ClassSection offering,
            Long oldTeacherId,
            User newTeacher
    ) {
        LocalDate today = LocalDate.now();
        boolean hasConflict = sessionRepository
                .findByClassSectionIdOrderBySessionDateAscStartTimeAsc(offering.getId())
                .stream()
                .filter(session -> !session.getSessionDate().isBefore(today))
                .filter(session -> session.getStatus() != ClassroomSessionStatus.COMPLETED)
                .filter(session -> session.getStatus() != ClassroomSessionStatus.CANCELLED)
                .filter(session -> oldTeacherId == null
                        || session.getTeacher() == null
                        || oldTeacherId.equals(session.getTeacher().getId()))
                .anyMatch(session -> !sessionRepository.findTeacherConflicts(
                        newTeacher.getId(),
                        session.getSessionDate(),
                        session.getStartTime(),
                        session.getEndTime(),
                        VIRTUAL_MEETING_RETRY_SESSION_STATUSES,
                        session.getId()
                ).isEmpty());
        if (hasConflict) {
            throw new IllegalArgumentException(
                    "Giáo viên mới có lịch dạy khác trùng với một hoặc nhiều buổi sắp tới của lớp."
            );
        }
    }

    private void synchronizeSubstituteAssignment(ClassSchedule session) {
        ClassSection offering = session.getClassSection();
        User sessionTeacher = session.getTeacher();
        Long primaryTeacherId = getPrimaryTeacherId(offering);
        boolean usesPrimaryTeacher = sessionTeacher == null
                || (primaryTeacherId != null && primaryTeacherId.equals(sessionTeacher.getId()));

        if (usesPrimaryTeacher) {
            teacherAssignmentRepository.findByClassScheduleId(session.getId())
                    .ifPresent(teacherAssignmentRepository::delete);
            return;
        }

        ClassroomTeacherAssignment assignment = teacherAssignmentRepository
                .findByClassScheduleId(session.getId())
                .orElseGet(() -> ClassroomTeacherAssignment.builder()
                        .classSection(offering)
                        .classSchedule(session)
                        .build());
        assignment.setTeacher(sessionTeacher);
        assignment.setRole(ClassroomTeacherRole.SUBSTITUTE);
        assignment.setEffectiveFrom(session.getSessionDate().minusDays(SUBSTITUTE_PREPARATION_DAYS));
        assignment.setEffectiveTo(session.getSessionDate().plusDays(SUBSTITUTE_WRAP_UP_DAYS));
        assignment.setReason("Dạy thay buổi ngày " + session.getSessionDate());
        teacherAssignmentRepository.save(assignment);
    }

    private User assertCanManageVirtualSession(ClassSchedule session, String actorEmail) {
        User actor = accessHelper.requireUser(actorEmail);
        accessHelper.assertTeacher(actor);

        if (accessHelper.canManageClassroom(actor)
                || accessHelper.canManageTrainingOperations(actor)
                || isSessionTeacher(session, actor)
                || isPrimaryTeacher(session.getClassSection(), actor)
                || hasActiveTeacherAssignment(session.getClassSection(), actor)) {
            return actor;
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Bạn không còn được phân công phụ trách lớp học này."
        );
    }

    private boolean isSessionTeacher(ClassSchedule session, User actor) {
        return session.getTeacher() != null
                && session.getTeacher().getId().equals(actor.getId());
    }

    private boolean isPrimaryTeacher(ClassSection offering, User actor) {
        return offering.getPrimaryTeacher() != null
                && offering.getPrimaryTeacher().getId().equals(actor.getId());
    }

    private boolean hasActiveTeacherAssignment(ClassSection offering, User actor) {
        return teacherAssignmentRepository
                .findAllByClassSectionIdAndTeacherId(offering.getId(), actor.getId())
                .stream()
                .anyMatch(this::isTeacherAssignmentActive);
    }

    private void inviteTeacherSafely(ClassSchedule session, User teacher) {
        try {
            inviteTeacher(session, teacher);
        } catch (RuntimeException ex) {
            log.warn(
                    "Không thể chuẩn bị quyền Google Meet cho giáo viên {} ở buổi học {}: {}",
                    teacher.getId(),
                    session.getId(),
                    ex.getMessage()
            );
        }
    }

    private void inviteTeacher(ClassSchedule session, User teacher) {
        virtualMeetingService.inviteInternalAttendee(session, teacher.getEmail());
    }

    private ClassSchedule buildSession(
            ClassSection offering,
            CreateClassroomSessionRequest request,
            User teacher,
            Room room,
            ClassroomDeliveryMode deliveryMode,
            CurriculumSessionPlan sessionPlan
    ) {
        String larkUrl = request.getLarkMeetingUrl();
        if (virtualMeetingService.isLegacyOrPlaceholderUrl(larkUrl)) {
            larkUrl = null;
        }
        if ((larkUrl == null || larkUrl.isBlank())
                && deliveryMode == ClassroomDeliveryMode.VIRTUAL
                && !virtualMeetingService.isLegacyOrPlaceholderUrl(offering.getDefaultLarkMeetingUrl())) {
            larkUrl = offering.getDefaultLarkMeetingUrl();
        }
        return ClassSchedule.builder()
                .classSection(offering)
                .sessionDate(request.getSessionDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .teacher(teacher)
                .status(request.getStatus() == null ? ClassroomSessionStatus.SCHEDULED : request.getStatus())
                .deliveryMode(deliveryMode)
                .room(room)
                .larkMeetingUrl(larkUrl)
                .larkMeetingStatus(virtualMeetingService.resolveStatus(larkUrl))
                .larkSyncStatus(larkUrl == null || larkUrl.isBlank() ? "PENDING" : "MANUAL")
                .curriculumSessionPlan(sessionPlan)
                .scheduleType(sessionPlan == null ? ClassScheduleType.OTHER : ClassScheduleType.LESSON)
                .sessionContent(sessionPlan == null ? request.getSessionContent() : sessionPlan.getTitle())
                .note(request.getNote())
                .build();
    }

    private void applySessionRequest(
            ClassSchedule session,
            CreateClassroomSessionRequest request,
            User teacher,
            Room room,
            ClassroomDeliveryMode deliveryMode,
            CurriculumSessionPlan sessionPlan
    ) {
        ClassSection offering = session.getClassSection();
        session.setSessionDate(request.getSessionDate());
        session.setStartTime(request.getStartTime());
        session.setEndTime(request.getEndTime());
        session.setTeacher(teacher);
        if (request.getStatus() != null) {
            session.setStatus(request.getStatus());
        }
        session.setDeliveryMode(deliveryMode);
        session.setRoom(room);
        if (request.getLarkMeetingUrl() != null) {
            String requestedUrl = virtualMeetingService.isLegacyOrPlaceholderUrl(request.getLarkMeetingUrl())
                    ? null
                    : request.getLarkMeetingUrl();
            session.setLarkMeetingUrl(requestedUrl);
            session.setLarkMeetingStatus(virtualMeetingService.resolveStatus(requestedUrl));
            if (requestedUrl != null && !requestedUrl.isBlank()) {
                session.setLarkSyncStatus("MANUAL");
                session.setLarkSyncError(null);
            }
        } else if (session.getDeliveryMode() == ClassroomDeliveryMode.VIRTUAL) {
            String defaultUrl = virtualMeetingService.isLegacyOrPlaceholderUrl(offering.getDefaultLarkMeetingUrl())
                    ? null
                    : offering.getDefaultLarkMeetingUrl();
            if (session.getLarkMeetingId() == null) {
                session.setLarkMeetingUrl(defaultUrl);
                session.setLarkMeetingStatus(virtualMeetingService.resolveStatus(defaultUrl));
            }
        }
        session.setCurriculumSessionPlan(sessionPlan);
        linkCourseLesson(session, sessionPlan);
        session.setSessionContent(sessionPlan == null ? request.getSessionContent() : sessionPlan.getTitle());
        session.setNote(request.getNote());
    }

    private boolean syncVirtualMeetingSafely(ClassSchedule session) {
        if (!virtualMeetingService.isEnabled()) {
            session.setLarkSyncStatus("DISABLED");
            session.setLarkSyncError("Tích hợp Google Meet API chưa được bật.");
            return false;
        }
        try {
            virtualMeetingService.syncMeeting(session);
            return true;
        } catch (RuntimeException ex) {
            session.setLarkSyncStatus("FAILED");
            session.setLarkSyncError(toVirtualMeetingUserMessage(ex));
            log.warn("Không thể đồng bộ buổi học {} với Google Meet: {}", session.getId(), ex.getMessage());
            return false;
        }
    }

    private void ensureTeacherMeetingOwner(ClassSection offering) {
        if (offering == null) return;
        User teacher = offering.getPrimaryTeacher();
        if (teacher == null) return;
        if (offering.getVirtualMeetingOwner() == null
                || !java.util.Objects.equals(offering.getVirtualMeetingOwner().getId(), teacher.getId())) {
            offering.setVirtualMeetingOwner(teacher);
            offering.setDefaultLarkMeetingUrl(null);
            sessionRepository.findByClassSectionIdOrderBySessionDateAscStartTimeAsc(offering.getId())
                    .stream()
                    .filter(candidate -> candidate.getDeliveryMode() == ClassroomDeliveryMode.VIRTUAL)
                    .filter(candidate -> VIRTUAL_MEETING_RETRY_SESSION_STATUSES.contains(candidate.getStatus()))
                    .filter(candidate -> candidate.getLarkMeetingId() != null
                            && candidate.getLarkMeetingId().startsWith("spaces/"))
                    .forEach(candidate -> {
                        clearManagedVirtualMeetingData(candidate);
                        candidate.setLarkMeetingUrl(null);
                        candidate.setLarkMeetingStatus(LarkMeetingStatus.NOT_CREATED);
                        candidate.setLarkSyncStatus("PENDING");
                        candidate.setLarkSyncError(null);
                    });
        }
    }

    private String toVirtualMeetingUserMessage(RuntimeException exception) {
        String detail = exception.getMessage() == null ? "" : exception.getMessage();
        if (detail.contains("hết hạn hoặc bị thu hồi")) {
            return "Tài khoản Google của giáo viên đã hết hạn quyền truy cập. Hãy yêu cầu giáo viên liên kết lại Google Meet.";
        }
        if (detail.contains("GOOGLE_MEET_CLIENT_ID") || detail.contains("GOOGLE_MEET_CLIENT_SECRET")) {
            return "Tích hợp Google Meet chưa được cấu hình đầy đủ. Vui lòng liên hệ quản trị hệ thống.";
        }
        if (detail.contains("FEATURE_UNAVAILABLE_TO_USER") || detail.contains("updateAutoRecordingGeneration")) {
            return "Tài khoản Google của giáo viên chưa được phép ghi hình tự động. Nếu nút Ghi trong Google Meet bị khóa, cần cấp quyền ghi hình từ gói hoặc quản trị Google Workspace.";
        }
        if (detail.contains("không áp dụng chế độ RESTRICTED")) {
            return "Tài khoản Google của giáo viên không hỗ trợ chế độ khách phải chờ duyệt. Hãy liên kết tài khoản Google Workspace của giáo viên rồi thử lại.";
        }
        return "Không thể tạo phòng Google Meet lúc này. Vui lòng thử lại sau hoặc kiểm tra kết nối Google của giáo viên phụ trách.";
    }

    private boolean needsManagedVirtualMeetingSync(ClassSchedule session) {
        if (session.getLarkMeetingUrl() == null
                || session.getLarkMeetingUrl().isBlank()
                || virtualMeetingService.isLegacyOrPlaceholderUrl(session.getLarkMeetingUrl())) {
            return true;
        }
        ClassSection offering = session.getClassSection();
        return session.getLarkMeetingId() == null
                && offering != null
                && session.getLarkMeetingUrl().equals(offering.getDefaultLarkMeetingUrl());
    }

    private boolean requiresManagedVirtualMeeting(ClassSchedule session) {
        String meetingUrl = session.getLarkMeetingUrl();
        return meetingUrl == null
                || meetingUrl.isBlank()
                || virtualMeetingService.isLegacyOrPlaceholderUrl(meetingUrl);
    }

    private void deleteVirtualMeetingSafely(ClassSchedule session) {
        try {
            virtualMeetingService.deleteMeeting(session);
        } catch (RuntimeException ex) {
            log.warn("Không thể kết thúc phòng Google Meet của buổi học {}: {}", session.getId(), ex.getMessage());
        }
    }

    private void clearManagedVirtualMeetingData(ClassSchedule session) {
        session.setLarkCalendarId(null);
        session.setLarkEventId(null);
        session.setLarkMeetingId(null);
        session.setLarkMeetingNo(null);
        session.setLarkSyncedAt(null);
    }

    private void markVirtualSessionEnded(ClassSchedule session) {
        virtualAttendanceService.finalizeVirtualAttendance(session);
        session.setStatus(ClassroomSessionStatus.COMPLETED);
        session.setLocked(true);
        session.setLarkMeetingStatus(LarkMeetingStatus.ENDED);
    }

    private void ensureGradebookEntry(ClassSection offering, User student) {
        gradebookEntryRepository.findByClassSectionIdAndStudentId(offering.getId(), student.getId())
                .orElseGet(() -> gradebookEntryRepository.save(ClassroomGradebookEntry.builder()
                        .classSection(offering)
                        .student(student)
                        .status(GradebookEntryStatus.PENDING)
                        .build()));
    }

    private List<Long> resolveActiveLearnerIds(Long offeringId) {
        return enrollmentRepository.findByClassSectionIdAndRegistrationStatusIn(offeringId, OCCUPIES_CLASS_SLOT).stream()
                .map(enrollment -> enrollment.getStudent().getId())
                .toList();
    }

    private ClassEnrollment findEnrollment(Long enrollmentId) {
        return enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đăng ký lớp."));
    }

    private ClassEnrollment saveEnrollmentWithWaitlistOrder(
            ClassEnrollment enrollment,
            ClassroomRegistrationStatus previousStatus
    ) {
        Long offeringId = enrollment.getClassSection().getId();
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

        ClassEnrollment saved = enrollmentRepository.saveAndFlush(enrollment);
        if (previousStatus == ClassroomRegistrationStatus.WAITLIST && !isWaitlisted) {
            compactWaitlist(offeringId);
        }
        return saved;
    }

    private void compactWaitlist(Long classSectionId) {
        offeringRepository.findByIdForUpdate(classSectionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lớp học không tồn tại."));
        List<ClassEnrollment> waitlist = getOrderedWaitlist(classSectionId);
        for (int index = 0; index < waitlist.size(); index++) {
            waitlist.get(index).setWaitlistPriority(index + 1);
        }
        enrollmentRepository.saveAll(waitlist);
    }

    private List<ClassEnrollment> getOrderedWaitlist(Long classSectionId) {
        return enrollmentRepository
                .findByClassSectionIdAndRegistrationStatusOrderByWaitlistPriorityAscEnrolledAtAscIdAsc(
                        classSectionId,
                        ClassroomRegistrationStatus.WAITLIST
                );
    }

    private Comparator<ClassEnrollment> registrationQueueComparator() {
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

    private BigDecimal resolveTuitionDue(ClassSection offering) {
        LearningPackage learningPackage = offering.getLearningPackage();

        if (learningPackage == null || learningPackage.getPrice() == null) {
            return BigDecimal.ZERO;

        }
        return learningPackage.getPrice();
    }

    private boolean isClassFull(ClassSection offering) {
        Integer capacity = offering.getCapacity();
        if (capacity == null || capacity <= 0) {
            return false;
        }
        long assigned = enrollmentRepository.countByOfferingAndRegistrationStatuses(offering.getId(), OCCUPIES_CLASS_SLOT);
        return assigned >= capacity;
    }

    private OnlineCourseEnrollment ensureOnlineCourseEnrollment(User student, ClassSection offering) {
        return packageEnrollmentRepository.findByStudentAndLearningPackage(student, offering.getLearningPackage())
                .map(enrollment -> {
                    if (!courseEnrollmentAccessPolicy.hasLearningAccess(enrollment)) {
                        return courseEnrollmentAccessPolicy.reactivateCancelledEnrollment(enrollment);
                    }
                    return enrollment;
                })
                .orElseGet(() -> packageEnrollmentRepository.save(OnlineCourseEnrollment.builder()
                        .student(student)
                        .learningPackage(offering.getLearningPackage())
                        .status(EnrollmentStatus.ACTIVE)
                        .progressPercent(0)
                        .build()));
    }

    private void tryAssignEnrollment(
            ClassEnrollment enrollment,
            ClassSection offering,
            User student,
            User assigner,
            String assignmentNote
    ) {
        ClassSection lockedOffering = offeringRepository.findByIdForUpdate(offering.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lớp học không tồn tại."));
        if (isClassFull(lockedOffering)) {
            enrollment.setRegistrationStatus(ClassroomRegistrationStatus.WAITLIST);
            return;
        }
        assignEnrollmentToClass(enrollment, lockedOffering, student, assigner, assignmentNote);
    }

    private void assignEnrollmentToClass(
            ClassEnrollment enrollment,
            ClassSection offering,
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
        enrollment.setPackageEnrollment(ensureOnlineCourseEnrollment(student, offering));
        ensureGradebookEntry(offering, student);
    }

    private void assertLearnerScheduleForOffering(ClassSection offering, Long learnerId) {
        List<ClassSchedule> schedules = sessionRepository
                .findByClassSectionIdOrderBySessionDateAscStartTimeAsc(offering.getId());
        for (ClassSchedule session : schedules) {
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

    private ClassSection findOffering(Long id) {
        return offeringRepository.findById(id)
                .filter(offering -> !offering.getLearningPackage().isDeleted())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp học."));
    }

    private boolean isVisibleToStaff(ClassSection offering) {
        if (offering.getLearningPackage() == null) {
            return false;
        }
        if (!offering.getLearningPackage().isDeleted()) {
            return true;
        }
        return !enrollmentRepository.findByClassSectionIdAndRegistrationStatusIn(
                offering.getId(),
                ClassroomRegistrationSupport.NEEDS_ACTION_STATUSES
        ).isEmpty();
    }

    private ClassSection findPublicOffering(String slugOrId) {
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

    private ClassSchedule findSession(Long sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy buổi học."));
    }

    private void validateOfferingRequest(CreateClassroomOfferingRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Dữ liệu lớp học không được để trống.");
        }
        if (!StringUtils.hasText(request.getTitle())) {
            throw new IllegalArgumentException("Tiêu đề lớp học không được để trống.");
        }
        if (request.getDeliveryMode() == null) {
            throw new IllegalArgumentException("Hình thức đào tạo không được để trống.");
        }
        if (request.getCapacity() != null && request.getCapacity() < 1) {
            throw new IllegalArgumentException("Sĩ số tối đa phải lớn hơn 0.");
        }
        if (!request.isDateRangeValid()) {
            throw new IllegalArgumentException("Ngày kết thúc phải từ ngày bắt đầu trở đi.");
        }
        validatePrices(request.getPrice(), request.getSalePrice());
    }

    private void validateOfferingResources(
            CreateClassroomOfferingRequest request,
            User primaryTeacher,
            Room regularRoom
    ) {
        validateRoomCapacity(regularRoom, request.getCapacity() == null ? 30 : request.getCapacity());
        ClassroomOfferingStatus status = request.getClassroomStatus() == null
                ? ClassroomOfferingStatus.DRAFT : request.getClassroomStatus();
        if (status != ClassroomOfferingStatus.UPCOMING && status != ClassroomOfferingStatus.ACTIVE) {
            return;
        }
        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new IllegalArgumentException("Lớp sắp mở hoặc đang hoạt động phải có đủ ngày bắt đầu và kết thúc.");
        }
        if (primaryTeacher == null) {
            throw new IllegalArgumentException("Lớp sắp mở hoặc đang hoạt động phải có giáo viên chính.");
        }
        if (request.getDeliveryMode() == ClassroomDeliveryMode.OFFLINE && regularRoom == null) {
            throw new IllegalArgumentException("Lớp học trực tiếp phải có phòng học.");
        }
    }

    private void validatePrices(BigDecimal price, BigDecimal salePrice) {
        if (price != null && price.signum() < 0) {
            throw new IllegalArgumentException("Học phí không được âm.");
        }
        if (salePrice != null && salePrice.signum() < 0) {
            throw new IllegalArgumentException("Giá ưu đãi không được âm.");
        }
        if (price != null && salePrice != null && salePrice.compareTo(price) > 0) {
            throw new IllegalArgumentException("Giá ưu đãi không được lớn hơn học phí gốc.");
        }
    }

    private void validateSessionRequest(CreateClassroomSessionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Dữ liệu buổi học không được để trống.");
        }
        if (request.getSessionDate() == null || request.getStartTime() == null || request.getEndTime() == null) {
            throw new IllegalArgumentException("Ngày học, giờ bắt đầu và giờ kết thúc không được để trống.");
        }
        if (!request.isTimeRangeValid()) {
            throw new IllegalArgumentException("Giờ kết thúc phải sau giờ bắt đầu.");
        }
    }

    private ClassroomDeliveryMode resolveSessionDeliveryMode(
            CreateClassroomSessionRequest request,
            ClassSection offering
    ) {
        ClassroomDeliveryMode deliveryMode = request.getDeliveryMode() != null
                ? request.getDeliveryMode() : offering.getDeliveryMode();
        if (deliveryMode == null) {
            throw new IllegalArgumentException("Hình thức của buổi học không được để trống.");
        }
        return deliveryMode;
    }

    private Room resolveSessionRoom(
            CreateClassroomSessionRequest request,
            ClassSection offering,
            ClassroomDeliveryMode deliveryMode
    ) {
        if (deliveryMode == ClassroomDeliveryMode.VIRTUAL) {
            return null;
        }
        Long roomId = request.getRoomId() != null ? request.getRoomId() : getDefaultRoomId(offering);
        return resolveRoom(roomId);
    }

    private void validateRoomCapacity(Room room, Integer expectedCapacity) {
        if (room != null && room.getCapacity() != null && expectedCapacity != null
                && expectedCapacity > room.getCapacity()) {
            throw new IllegalArgumentException(
                    "Sĩ số tối đa của lớp vượt quá sức chứa " + room.getCapacity() + " của phòng " + room.getName() + "."
            );
        }
    }

    private User resolveTeacher(Long teacherId) {
        if (teacherId == null) {
            return null;
        }
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giáo viên."));
        if (!teacher.hasRole(RoleEnum.TEACHER)) {
            throw new IllegalArgumentException("Tài khoản được chọn không có vai trò giáo viên.");
        }
        return teacher;
    }

    private Room resolveRoom(Long roomId) {
        if (roomId == null) {
            return null;
        }
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng học."));
        if (!room.isActive()) {
            throw new IllegalArgumentException("Phòng học đã ngừng hoạt động.");
        }
        return room;
    }

    private CurriculumSessionPlan resolveCurriculumSessionPlan(
            Long curriculumSessionPlanId,
            ClassSection offering
    ) {
        if (curriculumSessionPlanId == null) {
            return null;
        }
        CurriculumSessionPlan sessionPlan = curriculumSessionPlanRepository.findById(curriculumSessionPlanId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy buổi học trong giáo trình."));
        Long offeringProgramId = offering.getCurriculumProgram() == null
                ? null
                : offering.getCurriculumProgram().getId();
        Long sessionPlanProgramId = sessionPlan.getUnit().getProgram().getId();
        if (!Objects.equals(offeringProgramId, sessionPlanProgramId)) {
            throw new IllegalArgumentException("Buổi học đã chọn không thuộc giáo trình của lớp này.");
        }
        return sessionPlan;
    }

    private CurriculumProgram resolveCurriculumProgram(Long curriculumProgramId) {
        if (curriculumProgramId == null) {
            return null;
        }
        CurriculumProgram program = curriculumProgramRepository.findById(curriculumProgramId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giáo trình."));
        if ("ARCHIVED".equalsIgnoreCase(program.getStatus())) {
            throw new RuntimeException("Giáo trình đã lưu trữ, không thể gắn vào lớp.");
        }
        if (!"PUBLISHED".equalsIgnoreCase(program.getStatus())) {
            throw new RuntimeException("Chỉ có thể mở lớp từ giáo trình đã được duyệt.");
        }
        return program;
    }

    private TrainingProgram resolveTrainingProgram(Long trainingProgramId) {
        if (trainingProgramId == null) {
            return null;
        }
        TrainingProgram program = trainingProgramRepository.findById(trainingProgramId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chương trình học."));
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

    private String resolveTargetScore(CurriculumProgram curriculumProgram) {
        if (curriculumProgram == null) {
            return null;
        }
        if (curriculumProgram.getTargetBand() != null) {
            return curriculumProgram.getTargetBand().stripTrailingZeros().toPlainString();
        }
        return curriculumProgram.getTargetScore() == null
                ? null
                : String.valueOf(curriculumProgram.getTargetScore());
    }

    private Long getPrimaryTeacherId(ClassSection offering) {
        return offering.getPrimaryTeacher() == null ? null : offering.getPrimaryTeacher().getId();
    }

    private Long getDefaultRoomId(ClassSection offering) {
        return offering.getRegularRoom() == null ? null : offering.getRegularRoom().getId();
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

    private void linkInstructorLedCourse(ClassSection offering) {
        if (offering.getTrainingProgram() != null && offering.getTrainingProgram().getId() != null) {
            offering.setInstructorLedCourse(instructorLedCourseIdResolver
                    .requireFromTrainingProgramId(offering.getTrainingProgram().getId()));
            return;
        }
        if (offering.getCurriculumProgram() != null && offering.getCurriculumProgram().getId() != null) {
            offering.setInstructorLedCourse(instructorLedCourseIdResolver
                    .resolveFromCurriculumProgramId(offering.getCurriculumProgram().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khóa học giảng viên tương ứng.")));
            return;
        }
        if (offering.getInstructorLedCourse() == null) {
            throw new IllegalArgumentException("Lớp học phải thuộc một khóa học có giảng viên.");
        }
    }

    private void linkCourseLesson(ClassSchedule session, CurriculumSessionPlan sessionPlan) {
        if (sessionPlan == null || sessionPlan.getId() == null) {
            session.setCourseLesson(null);
            session.setScheduleType(session.getStatus() == ClassroomSessionStatus.MAKEUP
                    ? ClassScheduleType.MAKEUP
                    : ClassScheduleType.OTHER);
            return;
        }
        if (courseLessonRepository.existsById(sessionPlan.getId())) {
            CourseLesson lesson = courseLessonRepository.findById(sessionPlan.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bài học trong chương trình."));
            Long sectionCourseId = session.getClassSection().getInstructorLedCourse().getId();
            Long lessonCourseId = lesson.getCourseUnit().getInstructorLedCourse().getId();
            if (!Objects.equals(sectionCourseId, lessonCourseId)) {
                throw new IllegalArgumentException("Bài học không thuộc chương trình của lớp.");
            }
            session.setCourseLesson(lesson);
            session.setScheduleType(ClassScheduleType.LESSON);
        } else {
            session.setCourseLesson(null);
            session.setScheduleType(ClassScheduleType.OTHER);
        }
    }
}
