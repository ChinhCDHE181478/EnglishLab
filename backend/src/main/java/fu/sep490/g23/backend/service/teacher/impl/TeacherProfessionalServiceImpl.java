package fu.sep490.g23.backend.service.teacher.impl;
import fu.sep490.g23.backend.entity.teacher.enums.CredentialVerificationStatus;
import fu.sep490.g23.backend.entity.teacher.enums.TeacherEvaluationStatus;
import fu.sep490.g23.backend.dto.request.teacher.VerifyTeacherCredentialRequest;
import fu.sep490.g23.backend.dto.response.teacher.TeacherEvaluationResponse;
import fu.sep490.g23.backend.dto.request.teacher.UpsertTeacherEvaluationRequest;
import fu.sep490.g23.backend.dto.request.teacher.UpsertTeacherCredentialRequest;
import fu.sep490.g23.backend.dto.request.teacher.UpdateTeacherProfileRequest;
import fu.sep490.g23.backend.dto.response.teacher.TeacherProfessionalResponse;
import fu.sep490.g23.backend.dto.response.teacher.TeacherCredentialResponse;
import fu.sep490.g23.backend.repository.teacher.TeacherCredentialRepository;
import fu.sep490.g23.backend.repository.teacher.TeacherPerformanceEvaluationRepository;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomSessionStatus;
import fu.sep490.g23.backend.entity.enums.RoleEnum;
import fu.sep490.g23.backend.entity.teacher.TeacherCredential;
import fu.sep490.g23.backend.entity.teacher.TeacherPerformanceEvaluation;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.classroom.ClassScheduleRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomTeacherAssignmentRepository;
import fu.sep490.g23.backend.service.admin.AuditLogService;
import fu.sep490.g23.backend.service.teacher.TeacherProfessionalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class TeacherProfessionalServiceImpl implements TeacherProfessionalService {

    private final UserRepository userRepository;
    private final TeacherCredentialRepository credentialRepository;
    private final TeacherPerformanceEvaluationRepository evaluationRepository;
    private final ClassroomTeacherAssignmentRepository assignmentRepository;
    private final ClassScheduleRepository sessionRepository;
    private final AuditLogService auditLogService;

    @Override
    @Transactional(readOnly = true)
    public List<TeacherProfessionalResponse> listTeachers(String actorEmail) {
        User actor = requireActor(actorEmail);
        requireOperationsViewer(actor);
        return userRepository.findDistinctByRoles_CodeIn(List.of(RoleEnum.TEACHER)).stream()
                .sorted(Comparator.comparing(User::getFullName, String.CASE_INSENSITIVE_ORDER))
                .map(teacher -> buildResponse(teacher, canManagePerformance(actor), false))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TeacherProfessionalResponse getTeacher(Long teacherId, String actorEmail) {
        User actor = requireActor(actorEmail);
        requireOperationsViewer(actor);
        return buildResponse(requireTeacher(teacherId), canManagePerformance(actor), true);
    }

    @Override
    @Transactional(readOnly = true)
    public TeacherProfessionalResponse getMyProfile(String teacherEmail) {
        User teacher = requireActor(teacherEmail);
        requireTeacherRole(teacher);
        return buildResponse(teacher, false, true);
    }

    @Override
    public TeacherProfessionalResponse updateProfile(
            Long teacherId,
            UpdateTeacherProfileRequest request,
            String actorEmail
    ) {
        User actor = requireActor(actorEmail);
        requireStaff(actor);
        User teacher = requireTeacher(teacherId);
        teacher.setTeacherHeadline(clean(request.getHeadline()));
        teacher.setTeacherBiography(clean(request.getBiography()));
        teacher.setTeacherSpecializations(clean(request.getSpecializations()));
        teacher.setTeacherTeachingLanguages(clean(request.getTeachingLanguages()));
        teacher.setTeacherYearsOfExperience(request.getYearsOfExperience());
        teacher.setTeacherHighestQualification(clean(request.getHighestQualification()));
        teacher.setTeacherPublicProfile(request.isPublicProfile());
        userRepository.save(teacher);
        auditLogService.record(actorEmail, "TEACHER_PROFILE_UPDATED", "TEACHER", teacherId.toString(),
                "Cập nhật hồ sơ chuyên môn của " + teacher.getEmail());
        return buildResponse(teacher, canManagePerformance(actor), true);
    }

    @Override
    public TeacherCredentialResponse createCredential(
            Long teacherId,
            UpsertTeacherCredentialRequest request,
            String actorEmail
    ) {
        User actor = requireActor(actorEmail);
        requireStaff(actor);
        User teacher = requireTeacher(teacherId);
        validateCredential(request);
        TeacherCredential credential = TeacherCredential.builder().teacher(teacher).build();
        applyCredential(credential, request);
        TeacherCredential saved = credentialRepository.save(credential);
        auditLogService.record(actorEmail, "TEACHER_CREDENTIAL_CREATED", "TEACHER_CREDENTIAL", saved.getId().toString(),
                teacher.getEmail() + " · " + saved.getTitle());
        return toCredentialResponse(saved);
    }

    @Override
    public TeacherCredentialResponse updateCredential(
            Long teacherId,
            Long credentialId,
            UpsertTeacherCredentialRequest request,
            String actorEmail
    ) {
        User actor = requireActor(actorEmail);
        requireStaff(actor);
        validateCredential(request);
        TeacherCredential credential = requireCredential(teacherId, credentialId);
        applyCredential(credential, request);
        credential.setVerificationStatus(CredentialVerificationStatus.PENDING);
        credential.setVerifiedBy(null);
        credential.setVerifiedAt(null);
        credential.setVerificationNote(null);
        TeacherCredential saved = credentialRepository.save(credential);
        auditLogService.record(actorEmail, "TEACHER_CREDENTIAL_UPDATED", "TEACHER_CREDENTIAL", credentialId.toString(),
                saved.getTeacher().getEmail() + " · " + saved.getTitle());
        return toCredentialResponse(saved);
    }

    @Override
    public TeacherCredentialResponse verifyCredential(
            Long teacherId,
            Long credentialId,
            VerifyTeacherCredentialRequest request,
            String actorEmail
    ) {
        User actor = requireActor(actorEmail);
        requireStaff(actor);
        if (request.getStatus() != CredentialVerificationStatus.VERIFIED
                && request.getStatus() != CredentialVerificationStatus.REJECTED) {
            throw new IllegalArgumentException("Chỉ có thể xác minh hoặc từ chối minh chứng.");
        }
        if (request.getStatus() == CredentialVerificationStatus.REJECTED
                && (request.getNote() == null || request.getNote().isBlank())) {
            throw new IllegalArgumentException("Cần nhập lý do khi từ chối minh chứng.");
        }
        TeacherCredential credential = requireCredential(teacherId, credentialId);
        credential.setVerificationStatus(request.getStatus());
        credential.setVerifiedBy(actor);
        credential.setVerifiedAt(LocalDateTime.now());
        credential.setVerificationNote(clean(request.getNote()));
        TeacherCredential saved = credentialRepository.save(credential);
        auditLogService.record(actorEmail, "TEACHER_CREDENTIAL_VERIFIED", "TEACHER_CREDENTIAL", credentialId.toString(),
                request.getStatus().name() + " · " + saved.getTeacher().getEmail());
        return toCredentialResponse(saved);
    }

    @Override
    public void deleteCredential(Long teacherId, Long credentialId, String actorEmail) {
        User actor = requireActor(actorEmail);
        requireStaff(actor);
        TeacherCredential credential = requireCredential(teacherId, credentialId);
        credentialRepository.delete(credential);
        auditLogService.record(actorEmail, "TEACHER_CREDENTIAL_DELETED", "TEACHER_CREDENTIAL", credentialId.toString(),
                credential.getTeacher().getEmail() + " · " + credential.getTitle());
    }

    @Override
    public TeacherEvaluationResponse createEvaluation(
            Long teacherId,
            UpsertTeacherEvaluationRequest request,
            String actorEmail
    ) {
        User evaluator = requireActor(actorEmail);
        requirePerformanceManager(evaluator);
        User teacher = requireTeacher(teacherId);
        validateEvaluation(request);
        TeacherPerformanceEvaluation evaluation = TeacherPerformanceEvaluation.builder()
                .teacher(teacher)
                .evaluator(evaluator)
                .status(TeacherEvaluationStatus.DRAFT)
                .build();
        applyEvaluation(evaluation, request);
        TeacherPerformanceEvaluation saved = evaluationRepository.save(evaluation);
        auditLogService.record(actorEmail, "TEACHER_EVALUATION_CREATED", "TEACHER_EVALUATION", saved.getId().toString(),
                teacher.getEmail() + " · " + request.getPeriodStart() + " - " + request.getPeriodEnd());
        return toEvaluationResponse(saved);
    }

    @Override
    public TeacherEvaluationResponse updateEvaluation(
            Long teacherId,
            Long evaluationId,
            UpsertTeacherEvaluationRequest request,
            String actorEmail
    ) {
        User evaluator = requireActor(actorEmail);
        requirePerformanceManager(evaluator);
        validateEvaluation(request);
        TeacherPerformanceEvaluation evaluation = requireEvaluation(teacherId, evaluationId);
        if (evaluation.getStatus() == TeacherEvaluationStatus.PUBLISHED) {
            throw new IllegalArgumentException("Đánh giá đã công bố không thể chỉnh sửa.");
        }
        applyEvaluation(evaluation, request);
        evaluation.setEvaluator(evaluator);
        TeacherPerformanceEvaluation saved = evaluationRepository.save(evaluation);
        auditLogService.record(actorEmail, "TEACHER_EVALUATION_UPDATED", "TEACHER_EVALUATION", evaluationId.toString(),
                saved.getTeacher().getEmail());
        return toEvaluationResponse(saved);
    }

    @Override
    public TeacherEvaluationResponse publishEvaluation(Long teacherId, Long evaluationId, String actorEmail) {
        User evaluator = requireActor(actorEmail);
        requirePerformanceManager(evaluator);
        TeacherPerformanceEvaluation evaluation = requireEvaluation(teacherId, evaluationId);
        if (evaluation.getStatus() == TeacherEvaluationStatus.PUBLISHED) {
            return toEvaluationResponse(evaluation);
        }
        evaluation.setStatus(TeacherEvaluationStatus.PUBLISHED);
        evaluation.setPublishedAt(LocalDateTime.now());
        evaluation.setEvaluator(evaluator);
        TeacherPerformanceEvaluation saved = evaluationRepository.save(evaluation);
        auditLogService.record(actorEmail, "TEACHER_EVALUATION_PUBLISHED", "TEACHER_EVALUATION", evaluationId.toString(),
                saved.getTeacher().getEmail() + " · điểm " + saved.getOverallScore());
        return toEvaluationResponse(saved);
    }

    @Override
    public void deleteEvaluation(Long teacherId, Long evaluationId, String actorEmail) {
        User evaluator = requireActor(actorEmail);
        requirePerformanceManager(evaluator);
        TeacherPerformanceEvaluation evaluation = requireEvaluation(teacherId, evaluationId);
        if (evaluation.getStatus() == TeacherEvaluationStatus.PUBLISHED) {
            throw new IllegalArgumentException("Không thể xóa đánh giá đã công bố.");
        }
        evaluationRepository.delete(evaluation);
        auditLogService.record(actorEmail, "TEACHER_EVALUATION_DELETED", "TEACHER_EVALUATION", evaluationId.toString(),
                evaluation.getTeacher().getEmail());
    }

    private TeacherProfessionalResponse buildResponse(User teacher, boolean includeDrafts, boolean includeDetails) {
        List<TeacherCredentialResponse> credentials = includeDetails
                ? credentialRepository.findByTeacherIdOrderByIssuedDateDescIdDesc(teacher.getId()).stream()
                        .map(this::normalizeExpiry)
                        .map(this::toCredentialResponse)
                        .toList()
                : List.of();
        List<TeacherPerformanceEvaluation> evaluationEntities = includeDrafts
                ? evaluationRepository.findByTeacherIdOrderByPeriodEndDescIdDesc(teacher.getId())
                : evaluationRepository.findByTeacherIdAndStatusOrderByPeriodEndDescIdDesc(
                        teacher.getId(),
                        TeacherEvaluationStatus.PUBLISHED
                );
        List<BigDecimal> publishedScores = evaluationEntities.stream()
                .filter(item -> item.getStatus() == TeacherEvaluationStatus.PUBLISHED)
                .map(TeacherPerformanceEvaluation::getOverallScore)
                .filter(Objects::nonNull)
                .toList();
        BigDecimal averageScore = publishedScores.isEmpty()
                ? null
                : publishedScores.stream()
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(publishedScores.size()), 2, RoundingMode.HALF_UP);
        return TeacherProfessionalResponse.builder()
                .teacherId(teacher.getId())
                .fullName(teacher.getFullName())
                .email(teacher.getEmail())
                .phoneNumber(teacher.getPhoneNumber())
                .avatarUrl(teacher.getAvatarUrl())
                .headline(teacher.getTeacherHeadline())
                .biography(teacher.getTeacherBiography())
                .specializations(teacher.getTeacherSpecializations())
                .teachingLanguages(teacher.getTeacherTeachingLanguages())
                .yearsOfExperience(teacher.getTeacherYearsOfExperience())
                .highestQualification(teacher.getTeacherHighestQualification())
                .publicProfile(teacher.isTeacherPublicProfile())
                .assignedClassrooms(assignmentRepository.findByTeacherId(teacher.getId()).size())
                .totalSessions(sessionRepository.countByTeacherId(teacher.getId()))
                .completedSessions(sessionRepository.countByTeacherIdAndStatus(teacher.getId(), ClassroomSessionStatus.COMPLETED))
                .averagePerformanceScore(averageScore)
                .verifiedCredentials(credentialRepository.findByTeacherIdOrderByIssuedDateDescIdDesc(teacher.getId()).stream()
                        .map(this::normalizeExpiry)
                        .filter(item -> item.getVerificationStatus() == CredentialVerificationStatus.VERIFIED)
                        .count())
                .credentials(credentials)
                .evaluations(includeDetails ? evaluationEntities.stream().map(this::toEvaluationResponse).toList() : List.of())
                .build();
    }

    private TeacherCredential normalizeExpiry(TeacherCredential credential) {
        if (credential.getExpiryDate() != null
                && credential.getExpiryDate().isBefore(LocalDate.now())
                && credential.getVerificationStatus() == CredentialVerificationStatus.VERIFIED) {
            credential.setVerificationStatus(CredentialVerificationStatus.EXPIRED);
        }
        return credential;
    }

    private void applyCredential(TeacherCredential credential, UpsertTeacherCredentialRequest request) {
        credential.setType(request.getType().trim().toUpperCase(Locale.ROOT));
        credential.setTitle(request.getTitle().trim());
        credential.setIssuer(request.getIssuer().trim());
        credential.setCredentialNumber(clean(request.getCredentialNumber()));
        credential.setIssuedDate(request.getIssuedDate());
        credential.setExpiryDate(request.getExpiryDate());
        credential.setDocumentUrl(clean(request.getDocumentUrl()));
    }

    private void validateCredential(UpsertTeacherCredentialRequest request) {
        if (request.getIssuedDate() != null && request.getExpiryDate() != null
                && request.getExpiryDate().isBefore(request.getIssuedDate())) {
            throw new IllegalArgumentException("Ngày hết hạn không được trước ngày cấp.");
        }
        String url = clean(request.getDocumentUrl());
        if (url != null && !url.matches("^https?://.+")) {
            throw new IllegalArgumentException("Đường dẫn minh chứng phải bắt đầu bằng http:// hoặc https://.");
        }
    }

    private void applyEvaluation(TeacherPerformanceEvaluation evaluation, UpsertTeacherEvaluationRequest request) {
        evaluation.setPeriodStart(request.getPeriodStart());
        evaluation.setPeriodEnd(request.getPeriodEnd());
        evaluation.setLessonDeliveryScore(scale(request.getLessonDeliveryScore()));
        evaluation.setLearnerSupportScore(scale(request.getLearnerSupportScore()));
        evaluation.setGradingTimelinessScore(scale(request.getGradingTimelinessScore()));
        evaluation.setProfessionalismScore(scale(request.getProfessionalismScore()));
        evaluation.setOverallScore(request.getLessonDeliveryScore()
                .add(request.getLearnerSupportScore())
                .add(request.getGradingTimelinessScore())
                .add(request.getProfessionalismScore())
                .divide(BigDecimal.valueOf(4), 2, RoundingMode.HALF_UP));
        evaluation.setStrengths(clean(request.getStrengths()));
        evaluation.setImprovementAreas(clean(request.getImprovementAreas()));
        evaluation.setActionPlan(clean(request.getActionPlan()));
    }

    private void validateEvaluation(UpsertTeacherEvaluationRequest request) {
        if (request.getPeriodEnd().isBefore(request.getPeriodStart())) {
            throw new IllegalArgumentException("Ngày kết thúc kỳ đánh giá không được trước ngày bắt đầu.");
        }
        if (request.getPeriodEnd().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Không thể đánh giá một kỳ chưa kết thúc.");
        }
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private TeacherCredential requireCredential(Long teacherId, Long credentialId) {
        TeacherCredential credential = credentialRepository.findById(credentialId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy minh chứng chuyên môn."));
        if (!credential.getTeacher().getId().equals(teacherId)) {
            throw new RuntimeException("Minh chứng không thuộc giáo viên đã chọn.");
        }
        return credential;
    }

    private TeacherPerformanceEvaluation requireEvaluation(Long teacherId, Long evaluationId) {
        TeacherPerformanceEvaluation evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đánh giá hiệu suất."));
        if (!evaluation.getTeacher().getId().equals(teacherId)) {
            throw new RuntimeException("Đánh giá không thuộc giáo viên đã chọn.");
        }
        return evaluation;
    }

    private User requireActor(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng."));
    }

    private User requireTeacher(Long teacherId) {
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giáo viên."));
        requireTeacherRole(teacher);
        return teacher;
    }

    private void requireTeacherRole(User user) {
        if (!user.hasRole(RoleEnum.TEACHER)) {
            throw new IllegalArgumentException("Người dùng đã chọn không có vai trò giáo viên.");
        }
    }

    private void requireOperationsViewer(User user) {
        if (!user.hasAnyRole(Set.of(RoleEnum.STAFF, RoleEnum.MANAGER, RoleEnum.ADMIN))) {
            throw new RuntimeException("Bạn không có quyền xem hồ sơ đội ngũ giáo viên.");
        }
    }

    private void requireStaff(User user) {
        if (!user.hasAnyRole(Set.of(RoleEnum.STAFF, RoleEnum.ADMIN))) {
            throw new RuntimeException("Chỉ Staff được quản lý hồ sơ và minh chứng giáo viên.");
        }
    }

    private void requirePerformanceManager(User user) {
        if (!canManagePerformance(user)) {
            throw new RuntimeException("Chỉ Manager được đánh giá hiệu suất giáo viên.");
        }
    }

    private boolean canManagePerformance(User user) {
        return user.hasAnyRole(Set.of(RoleEnum.MANAGER, RoleEnum.ADMIN));
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private TeacherCredentialResponse toCredentialResponse(TeacherCredential item) {
        return TeacherCredentialResponse.builder()
                .id(item.getId())
                .type(item.getType())
                .title(item.getTitle())
                .issuer(item.getIssuer())
                .credentialNumber(item.getCredentialNumber())
                .issuedDate(item.getIssuedDate())
                .expiryDate(item.getExpiryDate())
                .documentUrl(item.getDocumentUrl())
                .verificationStatus(item.getVerificationStatus())
                .verifiedByName(item.getVerifiedBy() == null ? null : item.getVerifiedBy().getFullName())
                .verifiedAt(item.getVerifiedAt())
                .verificationNote(item.getVerificationNote())
                .build();
    }

    private TeacherEvaluationResponse toEvaluationResponse(TeacherPerformanceEvaluation item) {
        return TeacherEvaluationResponse.builder()
                .id(item.getId())
                .periodStart(item.getPeriodStart())
                .periodEnd(item.getPeriodEnd())
                .lessonDeliveryScore(item.getLessonDeliveryScore())
                .learnerSupportScore(item.getLearnerSupportScore())
                .gradingTimelinessScore(item.getGradingTimelinessScore())
                .professionalismScore(item.getProfessionalismScore())
                .overallScore(item.getOverallScore())
                .strengths(item.getStrengths())
                .improvementAreas(item.getImprovementAreas())
                .actionPlan(item.getActionPlan())
                .status(item.getStatus())
                .evaluatorName(item.getEvaluator().getFullName())
                .publishedAt(item.getPublishedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }
}
