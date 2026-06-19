package fu.sap490.g23.backend.service.classroom;

import fu.sap490.g23.backend.dto.request.classroom.*;
import fu.sap490.g23.backend.dto.response.classroom.*;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.classroom.*;
import fu.sap490.g23.backend.entity.course.*;
import fu.sap490.g23.backend.repository.UserRepository;
import fu.sap490.g23.backend.repository.classroom.*;
import fu.sap490.g23.backend.repository.course.LearningPackageRepository;
import fu.sap490.g23.backend.repository.course.PackageEnrollmentRepository;
import fu.sap490.g23.backend.repository.course.PackageTypeRepository;
import fu.sap490.g23.backend.security.ClassroomAccessHelper;
import fu.sap490.g23.backend.service.notification.ClassroomNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional
public class ClassroomOfferingServiceImpl implements ClassroomOfferingService {

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    private static final Set<ClassroomRegistrationStatus> OCCUPIES_CLASS_SLOT = ClassroomRegistrationSupport.OCCUPIES_CLASS_SLOT;
    private static final Set<ClassroomRegistrationStatus> ACTIVE_REGISTRATIONS = ClassroomRegistrationSupport.ACTIVE_REGISTRATIONS;

    private final ClassroomOfferingRepository offeringRepository;
    private final ClassroomSessionRepository sessionRepository;
    private final ClassroomEnrollmentRepository enrollmentRepository;
    private final ClassroomTuitionPaymentRepository tuitionPaymentRepository;
    private final ClassroomTeacherAssignmentRepository teacherAssignmentRepository;
    private final ClassroomGradebookEntryRepository gradebookEntryRepository;
    private final LearningPackageRepository learningPackageRepository;
    private final PackageTypeRepository packageTypeRepository;
    private final PackageEnrollmentRepository packageEnrollmentRepository;
    private final CampusRepository campusRepository;
    private final ClassroomRoomRepository roomRepository;
    private final UserRepository userRepository;
    private final ClassroomMapper mapper;
    private final ClassroomConflictService conflictService;
    private final LarkMeetingService larkMeetingService;
    private final ClassroomAccessHelper accessHelper;
    private final ClassroomNotificationService notificationService;

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
        return mapper.toOfferingResponse(offering);
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
                .map(ClassroomTeacherAssignment::getClassroomOffering)
                .map(mapper::toOfferingResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomOfferingResponse> getManagerOfferings() {
        return offeringRepository.findAll().stream()
                .filter(offering -> !offering.getLearningPackage().isDeleted())
                .map(offering -> mapper.toOfferingResponse(offering, true, null, null, true))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ClassroomOfferingResponse getOffering(Long id, boolean full) {
        ClassroomOffering offering = findOffering(id);
        return mapper.toOfferingResponse(offering, full, null, null, full);
    }

    @Override
    public ClassroomOfferingResponse createOffering(CreateClassroomOfferingRequest request, String creatorEmail) {
        User creator = accessHelper.requireUser(creatorEmail);
        accessHelper.assertManager(creator);

        PackageType packageType = packageTypeRepository.findByCode(PackageTypeCode.CLASSROOM)
                .orElseThrow(() -> new RuntimeException("Thiếu loại gói CLASSROOM trong hệ thống."));

        LearningPackage learningPackage = LearningPackage.builder()
                .packageType(packageType)
                .title(request.getTitle().trim())
                .slug(generateUniqueSlug(request.getTitle()))
                .shortDescription(request.getShortDescription())
                .description(request.getDescription())
                .targetScore(request.getTargetScore())
                .duration(request.getDuration())
                .studyMode(request.getStudyMode())
                .price(defaultBigDecimal(request.getPrice()))
                .salePrice(request.getSalePrice())
                .thumbnailUrl(request.getThumbnailUrl())
                .status(request.getPackageStatus() == null ? PackageStatus.DRAFT : request.getPackageStatus())
                .displayOrder(defaultInt(request.getDisplayOrder()))
                .featured(Boolean.TRUE.equals(request.getFeatured()))
                .createdBy(creator)
                .build();

        ClassroomOffering offering = ClassroomOffering.builder()
                .learningPackage(learningPackage)
                .deliveryMode(request.getDeliveryMode())
                .status(request.getClassroomStatus() == null ? ClassroomOfferingStatus.DRAFT : request.getClassroomStatus())
                .entryLevel(request.getEntryLevel())
                .targetOutcome(request.getTargetOutcome())
                .maxCapacity(request.getMaxCapacity() == null ? 30 : request.getMaxCapacity())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .primaryTeacher(resolveTeacher(request.getPrimaryTeacherId()))
                .defaultCampus(resolveCampus(request.getDefaultCampusId()))
                .defaultRoom(resolveRoom(request.getDefaultRoomId()))
                .offlineAddress(request.getOfflineAddress())
                .locationNote(request.getLocationNote())
                .defaultLarkMeetingUrl(request.getDefaultLarkMeetingUrl())
                .larkMeetingStatus(larkMeetingService.resolveStatus(request.getDefaultLarkMeetingUrl()))
                .recordingUrl(request.getRecordingUrl())
                .recordingVisible(Boolean.TRUE.equals(request.getRecordingVisible()))
                .syllabusSummary(request.getSyllabusSummary())
                .build();

        if (offering.getPrimaryTeacher() != null) {
            offering = offeringRepository.save(offering);
            assignTeacherInternal(offering, offering.getPrimaryTeacher(), ClassroomTeacherRole.PRIMARY, "Giáo viên chính khi tạo lớp");
            return mapper.toOfferingResponse(offering, true, null, null, true);
        }

        return mapper.toOfferingResponse(offeringRepository.save(offering), true, null, null, true);
    }

    @Override
    public ClassroomOfferingResponse updateOffering(Long id, CreateClassroomOfferingRequest request) {
        ClassroomOffering offering = findOffering(id);
        LearningPackage learningPackage = offering.getLearningPackage();

        learningPackage.setTitle(request.getTitle().trim());
        learningPackage.setShortDescription(request.getShortDescription());
        learningPackage.setDescription(request.getDescription());
        learningPackage.setTargetScore(request.getTargetScore());
        learningPackage.setDuration(request.getDuration());
        learningPackage.setStudyMode(request.getStudyMode());
        learningPackage.setPrice(defaultBigDecimal(request.getPrice()));
        learningPackage.setSalePrice(request.getSalePrice());
        learningPackage.setThumbnailUrl(request.getThumbnailUrl());
        if (request.getPackageStatus() != null) {
            learningPackage.setStatus(request.getPackageStatus());
        }
        learningPackage.setDisplayOrder(defaultInt(request.getDisplayOrder()));
        learningPackage.setFeatured(Boolean.TRUE.equals(request.getFeatured()));

        offering.setDeliveryMode(request.getDeliveryMode());
        if (request.getClassroomStatus() != null) {
            offering.setStatus(request.getClassroomStatus());
        }
        offering.setEntryLevel(request.getEntryLevel());
        offering.setTargetOutcome(request.getTargetOutcome());
        if (request.getMaxCapacity() != null) {
            offering.setMaxCapacity(request.getMaxCapacity());
        }
        offering.setStartDate(request.getStartDate());
        offering.setEndDate(request.getEndDate());
        offering.setPrimaryTeacher(resolveTeacher(request.getPrimaryTeacherId()));
        offering.setDefaultCampus(resolveCampus(request.getDefaultCampusId()));
        offering.setDefaultRoom(resolveRoom(request.getDefaultRoomId()));
        offering.setOfflineAddress(request.getOfflineAddress());
        offering.setLocationNote(request.getLocationNote());
        offering.setDefaultLarkMeetingUrl(request.getDefaultLarkMeetingUrl());
        offering.setLarkMeetingStatus(larkMeetingService.resolveStatus(request.getDefaultLarkMeetingUrl()));
        offering.setRecordingUrl(request.getRecordingUrl());
        if (request.getRecordingVisible() != null) {
            offering.setRecordingVisible(request.getRecordingVisible());
        }
        offering.setSyllabusSummary(request.getSyllabusSummary());

        return mapper.toOfferingResponse(offeringRepository.save(offering), true, null, null, true);
    }

    @Override
    public ClassroomOfferingResponse publishOffering(Long id) {
        ClassroomOffering offering = findOffering(id);
        offering.getLearningPackage().setStatus(PackageStatus.PUBLISHED);
        if (offering.getStatus() == ClassroomOfferingStatus.DRAFT) {
            offering.setStatus(ClassroomOfferingStatus.UPCOMING);
        }
        return mapper.toOfferingResponse(offering);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomSessionResponse> getSessions(Long offeringId) {
        return sessionRepository.findByClassroomOfferingIdOrderBySessionDateAscStartTimeAsc(offeringId).stream()
                .map(mapper::toSessionResponse)
                .toList();
    }

    @Override
    public ClassroomSessionResponse createSession(Long offeringId, CreateClassroomSessionRequest request) {
        ClassroomOffering offering = findOffering(offeringId);
        User teacher = resolveTeacher(request.getTeacherId() != null ? request.getTeacherId() : getPrimaryTeacherId(offering));
        Long roomId = request.getRoomId() != null ? request.getRoomId() : getDefaultRoomId(offering);

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

        ClassroomSession session = buildSession(offering, request, teacher);
        return mapper.toSessionResponse(sessionRepository.save(session));
    }

    @Override
    public ClassroomSessionResponse updateSession(Long sessionId, CreateClassroomSessionRequest request) {
        ClassroomSession session = findSession(sessionId);
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

        applySessionRequest(session, request, teacher);
        return mapper.toSessionResponse(sessionRepository.save(session));
    }

    @Override
    public void deleteSession(Long sessionId) {
        ClassroomSession session = findSession(sessionId);
        if (session.isLocked()) {
            throw new RuntimeException("Buổi học đã khóa nên không thể xóa.");
        }
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

        enrollment.setTuitionAmountDue(tuitionDue);
        enrollment.setTuitionAmountPaid(tuitionDue);
        enrollment.setTuitionDepositPaid(BigDecimal.ZERO);
        enrollment.setTuitionSettlementType(TuitionSettlementType.NONE);
        enrollment.setTuitionSettlementNote(null);
        enrollment.setNote(request.getNote());
        enrollment.setPackageEnrollment(ensurePackageEnrollment(student, offering));

        if (isClassFull(offering) && !enrollment.hasClassAccess()) {
            enrollment.setRegistrationStatus(ClassroomRegistrationStatus.WAITLIST);
        } else {
            tryAssignEnrollment(enrollment, offering, student, null, "Xếp lớp trực tiếp");
        }
        ClassroomRegistrationSupport.syncLegacyStatus(enrollment);
        enrollment = enrollmentRepository.save(enrollment);
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
        enrollment.setRegistrationStatus(ClassroomRegistrationStatus.CANCELLED);
        enrollment.setStatus(ClassroomEnrollmentStatus.CANCELLED);
        enrollmentRepository.save(enrollment);
    }

    @Override
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
        String settlementNote = buildSettlementNote(settlementType, targetDue, carriedPaid);

        sourceEnrollment.setRegistrationStatus(ClassroomRegistrationStatus.CANCELLED);
        sourceEnrollment.setStatus(ClassroomEnrollmentStatus.TRANSFERRED);
        sourceEnrollment.setNote(appendNote(sourceEnrollment.getNote(), "Đã chuyển sang lớp #" + target.getId()));
        enrollmentRepository.save(sourceEnrollment);

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

        if (paymentStatus == ClassroomRegistrationStatus.FULLY_PAID && !isClassFull(target)) {
            tryAssignEnrollment(targetEnrollment, target, student, null, "Chuyển lớp");
        } else if (isClassFull(target)) {
            targetEnrollment.setRegistrationStatus(ClassroomRegistrationStatus.WAITLIST);
        }

        ClassroomRegistrationSupport.syncLegacyStatus(targetEnrollment);
        targetEnrollment = enrollmentRepository.save(targetEnrollment);
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

        boolean holdSpot = request != null && request.isHoldSpot();
        BigDecimal tuitionDue = resolveTuitionDue(offering);
        ClassroomRegistrationStatus initialStatus = isClassFull(offering)
                ? ClassroomRegistrationStatus.WAITLIST
                : ClassroomRegistrationStatus.PENDING_CONFIRMATION;

        ClassroomEnrollment enrollment = ClassroomEnrollment.builder()
                .student(learner)
                .classroomOffering(offering)
                .packageEnrollment(ensurePackageEnrollment(learner, offering))
                .registrationStatus(initialStatus)
                .holdSpot(holdSpot)
                .tuitionAmountDue(tuitionDue)
                .tuitionAmountPaid(BigDecimal.ZERO)
                .tuitionDepositPaid(BigDecimal.ZERO)
                .tuitionSettlementType(TuitionSettlementType.NONE)
                .note(request == null ? null : request.getNote())
                .build();
        ClassroomRegistrationSupport.syncLegacyStatus(enrollment);
        enrollment = enrollmentRepository.save(enrollment);

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
                "Đã gửi đăng ký lớp",
                "Đăng ký lớp " + classTitle + " đang chờ Training Manager xác nhận.",
                Map.of("enrollmentId", enrollment.getId(), "classroomId", offering.getId())
        );
        return mapper.toEnrollmentResponse(enrollment);
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
            throw new RuntimeException("Đăng ký không ở trạng thái chờ xác nhận.");
        }

        ClassroomRegistrationStatus nextStatus = isClassFull(offering)
                ? ClassroomRegistrationStatus.WAITLIST
                : ClassroomRegistrationStatus.PENDING_TUITION_PAYMENT;
        enrollment.setRegistrationStatus(nextStatus);
        enrollment.setConfirmedAt(LocalDateTime.now());
        enrollment.setConfirmedBy(actor);
        ClassroomRegistrationSupport.syncLegacyStatus(enrollment);
        enrollment = enrollmentRepository.save(enrollment);

        String classTitle = offering.getLearningPackage().getTitle();
        notificationService.notifyUser(
                learner,
                "CLASSROOM_REGISTRATION_CONFIRMED",
                "Đăng ký lớp đã được xác nhận",
                "Đăng ký lớp " + classTitle + " đã được xác nhận. "
                        + ClassroomRegistrationSupport.registrationStatusLabel(nextStatus) + ".",
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

        enrollment.setRegistrationStatus(ClassroomRegistrationStatus.REJECTED);
        enrollment.setNote(appendNote(enrollment.getNote(), request == null ? null : request.getReason()));
        ClassroomRegistrationSupport.syncLegacyStatus(enrollment);
        enrollment = enrollmentRepository.save(enrollment);

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
        ClassroomEnrollment enrollment = findEnrollment(enrollmentId);
        ClassroomOffering offering = enrollment.getClassroomOffering();
        User learner = enrollment.getStudent();

        if (enrollment.getRegistrationStatus() == ClassroomRegistrationStatus.CANCELLED
                || enrollment.getRegistrationStatus() == ClassroomRegistrationStatus.REJECTED) {
            throw new RuntimeException("Đăng ký đã bị hủy hoặc từ chối.");
        }
        if (enrollment.getRegistrationStatus() == ClassroomRegistrationStatus.ASSIGNED) {
            throw new RuntimeException("Học viên đã được xếp lớp.");
        }

        BigDecimal amount = request.getAmount() == null ? BigDecimal.ZERO : request.getAmount();
        BigDecimal paid = enrollment.getTuitionAmountPaid() == null ? BigDecimal.ZERO : enrollment.getTuitionAmountPaid();
        enrollment.setTuitionAmountPaid(paid.add(amount));

        if (request.getPaymentKind() == TuitionPaymentKind.DEPOSIT) {
            BigDecimal deposit = enrollment.getTuitionDepositPaid() == null ? BigDecimal.ZERO : enrollment.getTuitionDepositPaid();
            enrollment.setTuitionDepositPaid(deposit.add(amount));
        }

        enrollment.setRegistrationStatus(ClassroomRegistrationSupport.resolveRegistrationStatusAfterPayment(
                enrollment.getTuitionAmountDue(),
                enrollment.getTuitionAmountPaid(),
                enrollment.getTuitionDepositPaid(),
                request.getPaymentKind()
        ));
        enrollment.setTuitionRecordedAt(LocalDateTime.now());
        enrollment.setTuitionRecordedBy(actor);

        tuitionPaymentRepository.save(ClassroomTuitionPayment.builder()
                .enrollment(enrollment)
                .amount(amount)
                .paymentKind(request.getPaymentKind())
                .note(request.getNote())
                .recordedBy(actor)
                .build());

        if (request.isAssignIfFullyPaid()
                && enrollment.getRegistrationStatus() == ClassroomRegistrationStatus.FULLY_PAID) {
            tryAssignEnrollment(enrollment, offering, learner, actor, null);
        } else if (enrollment.getRegistrationStatus() == ClassroomRegistrationStatus.FULLY_PAID && isClassFull(offering)) {
            enrollment.setRegistrationStatus(ClassroomRegistrationStatus.WAITLIST);
        }

        ClassroomRegistrationSupport.syncLegacyStatus(enrollment);
        enrollment = enrollmentRepository.save(enrollment);

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

        assertLearnerScheduleForOffering(offering, learner.getId());
        tryAssignEnrollment(
                enrollment,
                offering,
                learner,
                actor,
                request == null ? null : request.getAssignmentNote()
        );
        ClassroomRegistrationSupport.syncLegacyStatus(enrollment);
        enrollment = enrollmentRepository.save(enrollment);

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
    public List<ClassroomEnrollmentResponse> listRegistrations(ClassroomRegistrationStatus status) {
        return enrollmentRepository.findByRegistrationStatusIn(ClassroomRegistrationSupport.filterStatuses(status)).stream()
                .sorted((a, b) -> b.getEnrolledAt().compareTo(a.getEnrolledAt()))
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
        if (resolvedRole == ClassroomTeacherRole.PRIMARY) {
            offering.setPrimaryTeacher(teacher);
            offeringRepository.save(offering);
        }
        return assignTeacherInternal(offering, teacher, resolvedRole, null);
    }

    @Override
    public ClassroomTeacherSummaryResponse replaceTeacher(Long offeringId, Long oldTeacherId, Long newTeacherId) {
        ClassroomOffering offering = findOffering(offeringId);
        teacherAssignmentRepository.findByClassroomOfferingIdAndTeacherId(offeringId, oldTeacherId)
                .ifPresent(teacherAssignmentRepository::delete);
        User newTeacher = resolveTeacher(newTeacherId);
        if (newTeacher == null) {
            throw new RuntimeException("Giáo viên mới không hợp lệ.");
        }
        offering.setPrimaryTeacher(newTeacher);
        offeringRepository.save(offering);
        return assignTeacherInternal(offering, newTeacher, ClassroomTeacherRole.PRIMARY, "Thay giáo viên");
    }

    @Override
    public ClassroomSessionResponse openVirtualSession(Long sessionId) {
        ClassroomSession session = findSession(sessionId);
        if (session.getDeliveryMode() != ClassroomDeliveryMode.VIRTUAL) {
            throw new RuntimeException("Chỉ buổi học trực tuyến mới có thể mở phòng ảo.");
        }
        session.setStatus(ClassroomSessionStatus.OPEN);
        session.setLarkMeetingStatus(LarkMeetingStatus.OPEN);
        if (session.getLarkMeetingUrl() == null || session.getLarkMeetingUrl().isBlank()) {
            String defaultUrl = session.getClassroomOffering().getDefaultLarkMeetingUrl();
            session.setLarkMeetingUrl(defaultUrl);
        }
        return mapper.toSessionResponse(sessionRepository.save(session));
    }

    @Override
    public ClassroomSessionResponse closeVirtualSession(Long sessionId) {
        ClassroomSession session = findSession(sessionId);
        session.setStatus(ClassroomSessionStatus.COMPLETED);
        session.setLocked(true);
        session.setLarkMeetingStatus(LarkMeetingStatus.ENDED);
        return mapper.toSessionResponse(sessionRepository.save(session));
    }

    @Override
    public ClassroomSessionResponse updateSessionLarkLink(Long sessionId, UpdateLarkLinkRequest request) {
        ClassroomSession session = findSession(sessionId);
        if (session.isLocked()) {
            throw new RuntimeException("Buổi học đã khóa nên không thể cập nhật link Lark.");
        }
        session.setLarkMeetingUrl(request.getLarkMeetingUrl());
        session.setLarkMeetingStatus(larkMeetingService.resolveStatus(request.getLarkMeetingUrl()));
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
        if (reason != null) {
            assignment.setReason(reason);
        }
        return mapper.toTeacherSummary(teacherAssignmentRepository.save(assignment));
    }

    private ClassroomSession buildSession(ClassroomOffering offering, CreateClassroomSessionRequest request, User teacher) {
        ClassroomDeliveryMode deliveryMode = request.getDeliveryMode() != null ? request.getDeliveryMode() : offering.getDeliveryMode();
        String larkUrl = request.getLarkMeetingUrl();
        if ((larkUrl == null || larkUrl.isBlank()) && deliveryMode == ClassroomDeliveryMode.VIRTUAL) {
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
                .campus(resolveCampus(request.getCampusId() != null ? request.getCampusId() : getDefaultCampusId(offering)))
                .room(resolveRoom(request.getRoomId() != null ? request.getRoomId() : getDefaultRoomId(offering)))
                .larkMeetingUrl(larkUrl)
                .larkMeetingStatus(larkMeetingService.resolveStatus(larkUrl))
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
        if (request.getCampusId() != null) {
            session.setCampus(resolveCampus(request.getCampusId()));
        }
        if (request.getRoomId() != null) {
            session.setRoom(resolveRoom(request.getRoomId()));
        }
        if (request.getLarkMeetingUrl() != null) {
            session.setLarkMeetingUrl(request.getLarkMeetingUrl());
            session.setLarkMeetingStatus(larkMeetingService.resolveStatus(request.getLarkMeetingUrl()));
        } else if (session.getDeliveryMode() == ClassroomDeliveryMode.VIRTUAL) {
            session.setLarkMeetingUrl(offering.getDefaultLarkMeetingUrl());
            session.setLarkMeetingStatus(larkMeetingService.resolveStatus(offering.getDefaultLarkMeetingUrl()));
        }
        session.setSessionContent(request.getSessionContent());
        session.setNote(request.getNote());
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
        if (offering.getStatus() == ClassroomOfferingStatus.CANCELLED
                || offering.getStatus() == ClassroomOfferingStatus.COMPLETED) {
            throw new RuntimeException("Lớp học không còn nhận đăng ký.");
        }
    }

    private PackageEnrollment ensurePackageEnrollment(User student, ClassroomOffering offering) {
        return packageEnrollmentRepository.findByStudentAndLearningPackage(student, offering.getLearningPackage())
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
        if (isClassFull(offering)) {
            enrollment.setRegistrationStatus(ClassroomRegistrationStatus.WAITLIST);
            return;
        }
        assignEnrollmentToClass(enrollment, offering, student, assigner, assignmentNote);
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

    private String buildSettlementNote(TuitionSettlementType type, BigDecimal due, BigDecimal paid) {
        if (type == null || type == TuitionSettlementType.NONE) {
            return null;
        }
        BigDecimal diff = paid.subtract(due).abs();
        return switch (type) {
            case NEED_ADDITIONAL_PAYMENT -> "Cần thanh toán thêm " + diff.toPlainString() + " VND.";
            case HAS_BALANCE -> "Có số dư " + diff.toPlainString() + " VND.";
            case NEED_REFUND -> "Cần xử lý hoàn tiền " + diff.toPlainString() + " VND.";
            case NONE -> null;
        };
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

    private ClassroomOffering findPublicOffering(String slugOrId) {
        try {
            Long id = Long.parseLong(slugOrId);
            ClassroomOffering offering = offeringRepository.findById(id)
                    .filter(found -> !found.getLearningPackage().isDeleted())
                    .filter(found -> found.getLearningPackage().getStatus() == PackageStatus.PUBLISHED)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp học."));
            return offering;
        } catch (NumberFormatException ex) {
            return offeringRepository.findByLearningPackageSlug(slugOrId)
                    .filter(found -> found.getLearningPackage().getStatus() == PackageStatus.PUBLISHED)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp học."));
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

    private Campus resolveCampus(Long campusId) {
        if (campusId == null) {
            return null;
        }
        return campusRepository.findById(campusId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cơ sở."));
    }

    private ClassroomRoom resolveRoom(Long roomId) {
        if (roomId == null) {
            return null;
        }
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng học."));
    }

    private Long getPrimaryTeacherId(ClassroomOffering offering) {
        return offering.getPrimaryTeacher() == null ? null : offering.getPrimaryTeacher().getId();
    }

    private Long getDefaultCampusId(ClassroomOffering offering) {
        return offering.getDefaultCampus() == null ? null : offering.getDefaultCampus().getId();
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
