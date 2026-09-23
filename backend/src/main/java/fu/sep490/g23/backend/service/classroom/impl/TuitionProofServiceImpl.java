package fu.sep490.g23.backend.service.classroom.impl;

import fu.sep490.g23.backend.dto.request.classroom.RecordTuitionPaymentRequest;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomTuitionPaymentResponse;
import fu.sep490.g23.backend.dto.response.classroom.HomeworkAttachmentUploadResponse;
import fu.sep490.g23.backend.dto.response.classroom.TuitionProofResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.classroom.ClassEnrollment;
import fu.sep490.g23.backend.entity.classroom.ClassSection;
import fu.sep490.g23.backend.entity.classroom.ClassroomTuitionPaymentProof;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomRegistrationStatus;
import fu.sep490.g23.backend.entity.classroom.enums.TuitionPaymentKind;
import fu.sep490.g23.backend.entity.classroom.enums.TuitionProofStatus;
import fu.sep490.g23.backend.repository.classroom.ClassEnrollmentRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomTuitionPaymentProofRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomTuitionPaymentRepository;
import fu.sep490.g23.backend.security.ClassroomAccessHelper;
import fu.sep490.g23.backend.service.classroom.ClassroomOfferingService;
import fu.sep490.g23.backend.service.classroom.ClassroomRegistrationSupport;
import fu.sep490.g23.backend.service.classroom.HomeworkAttachmentStorageService;
import fu.sep490.g23.backend.service.classroom.TuitionProofService;
import fu.sep490.g23.backend.service.notification.ClassroomNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class TuitionProofServiceImpl implements TuitionProofService {

    private static final Set<ClassroomRegistrationStatus> ACTIVE_REGISTRATIONS = ClassroomRegistrationSupport.ACTIVE_REGISTRATIONS;

    private final ClassroomTuitionPaymentProofRepository proofRepository;
    private final ClassEnrollmentRepository enrollmentRepository;
    private final ClassroomTuitionPaymentRepository tuitionPaymentRepository;
    private final HomeworkAttachmentStorageService attachmentStorageService;
    private final ClassroomAccessHelper accessHelper;
    private final ClassroomNotificationService notificationService;
    private final ClassroomOfferingService classSectionService;

    //PENDING_CONFIRMATION (doi staff xac nhan dang ky hoc), PENDING_TUITION_PAYMENT (doi bang chung nop tien), DEPOSIT_PAID (da dat coc),
    //PARTIALLY_PAID (da thanh toan 1 phan), FULLY_PAID
    @Override
    public TuitionProofResponse submitProof(
            Long offeringId,
            MultipartFile file,
            BigDecimal amount,
            String paymentKind,
            String note,
            String learnerEmail,
            String publicUrlBase
    ) {
        User learner = accessHelper.requireUser(learnerEmail);
        ClassEnrollment enrollment = requireActiveEnrollment(offeringId, learner.getId());

        if (enrollment.getRegistrationStatus() == ClassroomRegistrationStatus.PENDING_CONFIRMATION) {
            throw new RuntimeException("Đăng ký đang chờ Nhân viên đào tạo xác nhận.");
        }
        if (enrollment.getRegistrationStatus() == ClassroomRegistrationStatus.WAITLIST) {
            throw new RuntimeException("Bạn đang ở trong danh sách chờ và chưa cần thanh toán học phí.");
        }
        if (enrollment.getRegistrationStatus() == ClassroomRegistrationStatus.ASSIGNED) {
            throw new RuntimeException("Bạn đã được xếp lớp, không cần nộp thêm minh chứng.");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Số tiền chuyển khoản phải lớn hơn 0.");
        }
        if (ClassroomRegistrationSupport.isTuitionPaymentOverdue(
                enrollment, ClassroomRegistrationSupport.currentBusinessTime())) {
            throw new RuntimeException("Đăng ký đã quá hạn thanh toán học phí.");
        }
        BigDecimal pendingAmount = proofRepository.findByEnrollmentIdOrderByCreatedAtDesc(enrollment.getId())
                .stream()
                .filter(item -> item.getStatus() == TuitionProofStatus.PENDING)
                .map(ClassroomTuitionPaymentProof::getAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (amount.add(pendingAmount).compareTo(enrollment.tuitionBalance()) > 0) {
            throw new RuntimeException("Tổng minh chứng chờ xác nhận vượt quá học phí còn lại.");
        }
        TuitionPaymentKind requestedKind = resolvePaymentKind(paymentKind);
        TuitionPaymentKind kind = ClassroomRegistrationSupport.classifyTuitionPayment(
                enrollment.getTuitionAmountDue(), enrollment.getTuitionAmountPaid(), amount);
        if (requestedKind == TuitionPaymentKind.DEPOSIT) {
            if (ClassroomRegistrationSupport.requiresFullTuitionPayment(enrollment)) {
                throw new RuntimeException("Đăng ký gần ngày khai giảng cần thanh toán toàn bộ học phí.");
            }
            BigDecimal required = ClassroomRegistrationSupport.remainingDeposit(
                    enrollment.getTuitionAmountDue(), enrollment.getTuitionAmountPaid());
            if (required.compareTo(BigDecimal.ZERO) <= 0 || amount.compareTo(required) != 0) {
                throw new RuntimeException("Số tiền cọc phải bằng đúng số tiền cọc còn thiếu.");
            }
            kind = TuitionPaymentKind.DEPOSIT;
        } else if (requestedKind == TuitionPaymentKind.FULL
                && amount.compareTo(enrollment.tuitionBalance()) != 0) {
            throw new RuntimeException("Thanh toán toàn bộ phải bằng đúng học phí còn lại.");
        }

        HomeworkAttachmentUploadResponse uploaded = attachmentStorageService.store(file, publicUrlBase, learnerEmail);
        ClassroomTuitionPaymentProof proof = proofRepository.save(ClassroomTuitionPaymentProof.builder()
                .enrollment(enrollment)
                .amount(amount)
                .paymentKind(kind)
                .fileUrl(uploaded.getUrl())
                .note(StringUtils.hasText(note) ? note.trim() : null)
                .status(TuitionProofStatus.PENDING)
                .build());

        String classTitle = enrollment.getClassSection().getTitle();
        notificationService.notifyTrainingStaff(
                "CLASSROOM_TUITION_PROOF_SUBMITTED",
                "Minh chứng thanh toán mới",
                learner.getFullName() + " vừa nộp minh chứng chuyển khoản " + amount.toPlainString()
                        + " VND cho lớp " + classTitle + ".",
                Map.of(
                        "proofId", proof.getId(),
                        "enrollmentId", enrollment.getId(),
                        "classroomId", enrollment.getClassSection().getId()
                )
        );
        notificationService.notifyUser(
                learner,
                "CLASSROOM_TUITION_PROOF_SUBMITTED",
                "Đã gửi minh chứng thanh toán",
                "Minh chứng thanh toán lớp " + classTitle + " đang chờ Nhân viên đào tạo xác nhận.",
                Map.of("proofId", proof.getId(), "classroomId", enrollment.getClassSection().getId())
        );
        return toResponse(proof);
    }

    @Override
    public TuitionProofResponse submitAndConfirmStaffProof(
            Long enrollmentId,
            MultipartFile file,
            String paymentKind,
            String note,
            String actorEmail,
            String publicUrlBase
    ) {
        User actor = accessHelper.requireUser(actorEmail);
        accessHelper.assertStaffOperator(actor);
        ClassEnrollment enrollment = enrollmentRepository.findByIdForUpdate(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hồ sơ ghi danh."));
        if (!ACTIVE_REGISTRATIONS.contains(enrollment.getRegistrationStatus())
                || enrollment.getRegistrationStatus() == ClassroomRegistrationStatus.WAITLIST
                || enrollment.getRegistrationStatus() == ClassroomRegistrationStatus.ASSIGNED) {
            throw new RuntimeException("Hồ sơ hiện không thể ghi nhận minh chứng học phí.");
        }
        if (ClassroomRegistrationSupport.isTuitionPaymentOverdue(
                enrollment, ClassroomRegistrationSupport.currentBusinessTime())) {
            throw new RuntimeException("Đăng ký đã quá hạn thanh toán học phí.");
        }

        TuitionPaymentKind kind = resolvePaymentKind(paymentKind);
        if (kind != TuitionPaymentKind.DEPOSIT && kind != TuitionPaymentKind.FULL) {
            throw new RuntimeException("Chỉ hỗ trợ xác nhận đặt cọc hoặc thanh toán toàn bộ.");
        }
        BigDecimal amount;
        if (kind == TuitionPaymentKind.DEPOSIT) {
            if (ClassroomRegistrationSupport.requiresFullTuitionPayment(enrollment)) {
                throw new RuntimeException("Lớp gần ngày khai giảng cần thanh toán toàn bộ học phí.");
            }
            amount = ClassroomRegistrationSupport.remainingDeposit(
                    enrollment.getTuitionAmountDue(), enrollment.getTuitionAmountPaid());
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("Khoản đặt cọc đã được thanh toán đủ.");
            }
        } else {
            amount = enrollment.tuitionBalance().max(BigDecimal.ZERO);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("Học phí đã được thanh toán đủ.");
            }
        }

        HomeworkAttachmentUploadResponse uploaded = attachmentStorageService.store(
                file, publicUrlBase, actorEmail);
        ClassroomTuitionPaymentProof proof = proofRepository.save(ClassroomTuitionPaymentProof.builder()
                .enrollment(enrollment)
                .amount(amount)
                .paymentKind(kind)
                .fileUrl(uploaded.getUrl())
                .note(StringUtils.hasText(note) ? note.trim() : null)
                .status(TuitionProofStatus.PENDING)
                .build());

        RecordTuitionPaymentRequest paymentRequest = new RecordTuitionPaymentRequest();
        paymentRequest.setAmount(amount);
        paymentRequest.setPaymentKind(kind);
        paymentRequest.setNote("Staff xác nhận từ minh chứng #" + proof.getId());
        paymentRequest.setAssignIfFullyPaid(true);
        classSectionService.recordTuitionPayment(enrollment.getId(), paymentRequest, actorEmail);

        proof.setStatus(TuitionProofStatus.CONFIRMED);
        proof.setReviewedBy(actor);
        proof.setReviewedAt(LocalDateTime.now());
        proof = proofRepository.save(proof);

        notificationService.notifyUser(
                enrollment.getStudent(),
                "CLASSROOM_TUITION_PROOF_CONFIRMED",
                "Đã ghi nhận thanh toán học phí",
                "EnglishLab đã ghi nhận " + amount.toPlainString() + " VND cho lớp "
                        + enrollment.getClassSection().getTitle() + ".",
                Map.of("proofId", proof.getId(), "classroomId", enrollment.getClassSection().getId())
        );
        return toResponse(proof);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TuitionProofResponse> getMyProofs(Long offeringId, String learnerEmail) {
        User learner = accessHelper.requireUser(learnerEmail);
        ClassEnrollment enrollment = enrollmentRepository
                .findByStudentIdAndClassSectionId(learner.getId(), offeringId)
                .orElse(null);
        if (enrollment == null) {
            return List.of();
        }
        return proofRepository.findByEnrollmentIdOrderByCreatedAtDesc(enrollment.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomTuitionPaymentResponse> getMyTuitionHistory(Long offeringId, String learnerEmail) {
        User learner = accessHelper.requireUser(learnerEmail);
        ClassEnrollment enrollment = enrollmentRepository
                .findByStudentIdAndClassSectionId(learner.getId(), offeringId)
                .orElse(null);
        if (enrollment == null) {
            return List.of();
        }
        return tuitionPaymentRepository.findByEnrollmentIdOrderByCreatedAtDesc(enrollment.getId())
                .stream()
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
    @Transactional(readOnly = true)
    public List<TuitionProofResponse> listPendingProofs() {
        return proofRepository.findByStatusOrderByCreatedAtAsc(TuitionProofStatus.PENDING)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TuitionProofResponse> listProofsForEnrollment(Long enrollmentId) {
        return proofRepository.findByEnrollmentIdOrderByCreatedAtDesc(enrollmentId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public TuitionProofResponse confirmProof(Long proofId, String actorEmail) {
        User actor = accessHelper.requireUser(actorEmail);
        accessHelper.assertStaffOperator(actor);
        ClassroomTuitionPaymentProof proof = findPendingProof(proofId);
        ClassEnrollment enrollment = proof.getEnrollment();

        RecordTuitionPaymentRequest paymentRequest = new RecordTuitionPaymentRequest();
        paymentRequest.setAmount(proof.getAmount());
        paymentRequest.setPaymentKind(proof.getPaymentKind());
        paymentRequest.setNote("Xác nhận từ minh chứng chuyển khoản #" + proof.getId());
        paymentRequest.setAssignIfFullyPaid(true);
        classSectionService.recordTuitionPayment(enrollment.getId(), paymentRequest, actorEmail);

        proof.setStatus(TuitionProofStatus.CONFIRMED);
        proof.setReviewedBy(actor);
        proof.setReviewedAt(LocalDateTime.now());
        proof = proofRepository.save(proof);

        notificationService.notifyUser(
                enrollment.getStudent(),
                "CLASSROOM_TUITION_PROOF_CONFIRMED",
                "Minh chứng thanh toán được xác nhận",
                "Minh chứng chuyển khoản " + proof.getAmount().toPlainString() + " VND cho lớp "
                        + enrollment.getClassSection().getTitle()
                        + " đã được xác nhận. Mã xác nhận: TP-" + proof.getId() + ".",
                Map.of("proofId", proof.getId(), "classroomId", enrollment.getClassSection().getId())
        );
        return toResponse(proof);
    }

    @Override
    public TuitionProofResponse rejectProof(Long proofId, String reason, String actorEmail) {
        User actor = accessHelper.requireUser(actorEmail);
        accessHelper.assertStaffOperator(actor);
        if (!StringUtils.hasText(reason)) {
            throw new RuntimeException("Vui lòng nhập lý do từ chối minh chứng.");
        }
        ClassroomTuitionPaymentProof proof = findPendingProof(proofId);
        ClassEnrollment enrollment = proof.getEnrollment();

        proof.setStatus(TuitionProofStatus.REJECTED);
        proof.setReviewNote(reason.trim());
        proof.setReviewedBy(actor);
        proof.setReviewedAt(LocalDateTime.now());
        proof = proofRepository.save(proof);

        notificationService.notifyUser(
                enrollment.getStudent(),
                "CLASSROOM_TUITION_PROOF_REJECTED",
                "Minh chứng thanh toán bị từ chối",
                "Minh chứng chuyển khoản cho lớp "
                        + enrollment.getClassSection().getTitle()
                        + " bị từ chối: " + reason.trim(),
                Map.of("proofId", proof.getId(), "classroomId", enrollment.getClassSection().getId())
        );
        return toResponse(proof);
    }

    private ClassEnrollment requireActiveEnrollment(Long offeringId, Long learnerId) {
        return enrollmentRepository.findByStudentIdAndClassSectionId(learnerId, offeringId)
                .filter(item -> ACTIVE_REGISTRATIONS.contains(item.getRegistrationStatus()))
                .orElseThrow(() -> new RuntimeException("Bạn chưa có đăng ký hiệu lực cho lớp này."));
    }

    private ClassroomTuitionPaymentProof findPendingProof(Long proofId) {
        ClassroomTuitionPaymentProof proof = proofRepository.findByIdForUpdate(proofId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy minh chứng thanh toán."));
        if (proof.getStatus() != TuitionProofStatus.PENDING) {
            throw new RuntimeException("Minh chứng đã được xử lý trước đó.");
        }
        return proof;
    }

    private TuitionPaymentKind resolvePaymentKind(String value) {
        if (!StringUtils.hasText(value)) {
            return TuitionPaymentKind.PARTIAL;
        }
        try {
            TuitionPaymentKind kind = TuitionPaymentKind.valueOf(value.trim().toUpperCase(Locale.ROOT));
            if (kind == TuitionPaymentKind.MANUAL_CONFIRMATION || kind == TuitionPaymentKind.REFUND) {
                throw new RuntimeException("Loại thanh toán không hợp lệ.");
            }
            return kind;
        } catch (IllegalArgumentException exception) {
            throw new RuntimeException("Loại thanh toán không hợp lệ.");
        }
    }

    private TuitionProofResponse toResponse(ClassroomTuitionPaymentProof proof) {
        ClassEnrollment enrollment = proof.getEnrollment();
        ClassSection offering = enrollment.getClassSection();
        return TuitionProofResponse.builder()
                .id(proof.getId())
                .enrollmentId(enrollment.getId())
                .classSectionId(offering.getId())
                .classroomTitle(offering.getTitle())
                .studentId(enrollment.getStudent().getId())
                .studentName(enrollment.getStudent().getFullName())
                .studentEmail(enrollment.getStudent().getEmail())
                .amount(proof.getAmount())
                .paymentKind(proof.getPaymentKind())
                .paymentKindLabel(ClassroomRegistrationSupport.tuitionPaymentKindLabel(proof.getPaymentKind()))
                .fileUrl(proof.getFileUrl())
                .note(proof.getNote())
                .status(proof.getStatus())
                .statusLabel(proofStatusLabel(proof.getStatus()))
                .reviewNote(proof.getReviewNote())
                .reviewedByName(proof.getReviewedBy() == null ? null : proof.getReviewedBy().getFullName())
                .reviewedAt(proof.getReviewedAt())
                .createdAt(proof.getCreatedAt())
                .build();
    }

    private String proofStatusLabel(TuitionProofStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case PENDING -> "Chờ xác nhận";
            case CONFIRMED -> "Đã xác nhận";
            case REJECTED -> "Bị từ chối";
        };
    }
}
