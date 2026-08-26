package fu.sep490.g23.backend.service.teacher.impl;
import fu.sep490.g23.backend.dto.response.teacher.AnonymizedTeacherFeedbackResponse;
import fu.sep490.g23.backend.dto.response.teacher.ManagerTeacherFeedbackDetailResponse;
import fu.sep490.g23.backend.dto.response.teacher.LearnerTeacherFeedbackResponse;
import fu.sep490.g23.backend.dto.response.teacher.TeacherFeedbackAggregateResponse;
import fu.sep490.g23.backend.dto.response.teacher.ClassroomFeedbackAggregateResponse;

import fu.sep490.g23.backend.dto.request.teacher.UpsertTeacherCourseFeedbackRequest;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.classroom.ClassEnrollment;
import fu.sep490.g23.backend.entity.classroom.ClassSection;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomRegistrationStatus;
import fu.sep490.g23.backend.entity.teacher.TeacherCourseFeedback;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.classroom.ClassEnrollmentRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomTeacherAssignmentRepository;
import fu.sep490.g23.backend.repository.teacher.TeacherCourseFeedbackRepository;
import fu.sep490.g23.backend.service.admin.AuditLogService;
import fu.sep490.g23.backend.service.teacher.TeacherFeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TeacherFeedbackServiceImpl implements TeacherFeedbackService {
    private final UserRepository userRepository;
    private final ClassEnrollmentRepository enrollmentRepository;
    private final ClassroomTeacherAssignmentRepository teacherAssignmentRepository;
    private final TeacherCourseFeedbackRepository feedbackRepository;
    private final AuditLogService auditLogService;

    @Value("${englishlab.teacher-feedback.opens-days-before-end:7}")
    private int opensDaysBeforeEnd;
    @Value("${englishlab.teacher-feedback.closes-days-after-end:14}")
    private int closesDaysAfterEnd;
    @Value("${englishlab.teacher-feedback.anonymity-threshold:3}")
    private int anonymityThreshold;

    @Override
    @Transactional(readOnly = true)
    public List<LearnerTeacherFeedbackResponse> getLearnerForms(Long classroomId, String learnerEmail) {
        User learner = requireUser(learnerEmail);
        ClassEnrollment enrollment = requireAssignedEnrollment(learner.getId(), classroomId);
        ClassSection classroom = enrollment.getClassSection();
        return assignedTeachers(classroom).stream()
                .map(teacher -> toLearnerResponse(classroom, teacher,
                        feedbackRepository.findByEnrollmentIdAndTeacherId(enrollment.getId(), teacher.getId()).orElse(null)))
                .toList();
    }

    @Override
    public LearnerTeacherFeedbackResponse saveLearnerFeedback(
            Long classroomId,
            Long teacherId,
            String learnerEmail,
            UpsertTeacherCourseFeedbackRequest request
    ) {
        User learner = requireUser(learnerEmail);
        ClassEnrollment enrollment = requireAssignedEnrollment(learner.getId(), classroomId);
        ClassSection classroom = enrollment.getClassSection();
        requireWindowOpen(classroom);
        User teacher = assignedTeachers(classroom).stream()
                .filter(candidate -> Objects.equals(candidate.getId(), teacherId))
                .findFirst()
                .orElseThrow(() -> badRequest("Giáo viên này không được phân công giảng dạy lớp học."));

        validateNarrative(request.getStrengths(), "Điểm mạnh");
        validateNarrative(request.getImprovementSuggestions(), "Góp ý cải thiện");
        if (normalize(request.getStrengths()).equalsIgnoreCase(normalize(request.getImprovementSuggestions()))) {
            throw badRequest("Điểm mạnh và góp ý cải thiện cần là hai nội dung khác nhau.");
        }

        TeacherCourseFeedback feedback = feedbackRepository
                .findByEnrollmentIdAndTeacherId(enrollment.getId(), teacherId)
                .orElseGet(() -> TeacherCourseFeedback.builder()
                        .enrollment(enrollment)
                        .classSection(classroom)
                        .teacher(teacher)
                        .submittedAt(LocalDateTime.now())
                        .build());
        boolean creating = feedback.getId() == null;
        copyRequest(request, feedback);
        TeacherCourseFeedback saved = feedbackRepository.save(feedback);
        auditLogService.record(
                learnerEmail,
                creating ? "TEACHER_FEEDBACK_SUBMITTED" : "TEACHER_FEEDBACK_UPDATED",
                "TEACHER_COURSE_FEEDBACK",
                saved.getId().toString(),
                "Học viên " + (creating ? "gửi" : "cập nhật") + " đánh giá ẩn danh cho lớp #" + classroomId
        );
        return toLearnerResponse(classroom, teacher, saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherFeedbackAggregateResponse> getManagerDashboard() {
        return feedbackRepository.findAll().stream()
                .collect(Collectors.groupingBy(item -> item.getTeacher().getId(), LinkedHashMap::new, Collectors.toList()))
                .values().stream()
                .map(items -> aggregate(items, false))
                .sorted(Comparator.comparing(
                        TeacherFeedbackAggregateResponse::getTeacherName,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ManagerTeacherFeedbackDetailResponse getManagerDetail(Long teacherId) {
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy giáo viên."));
        List<TeacherCourseFeedback> items = feedbackRepository.findByTeacherIdOrderBySubmittedAtDesc(teacherId);
        return ManagerTeacherFeedbackDetailResponse.builder()
                .aggregate(items.isEmpty() ? emptyAggregate(teacher) : aggregate(items, false))
                .feedback(items.stream().map(this::toAnonymousResponse).toList())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public TeacherFeedbackAggregateResponse getTeacherSummary(String teacherEmail) {
        User teacher = requireUser(teacherEmail);
        List<TeacherCourseFeedback> items = feedbackRepository.findByTeacherIdOrderBySubmittedAtDesc(teacher.getId());
        return items.isEmpty() ? emptyAggregate(teacher) : aggregate(items, true);
    }

    private void copyRequest(UpsertTeacherCourseFeedbackRequest request, TeacherCourseFeedback feedback) {
        feedback.setClarityScore(request.getClarityScore());
        feedback.setEngagementScore(request.getEngagementScore());
        feedback.setLearnerSupportScore(request.getLearnerSupportScore());
        feedback.setFeedbackTimelinessScore(request.getFeedbackTimelinessScore());
        feedback.setProfessionalismScore(request.getProfessionalismScore());
        feedback.setPace(request.getPace());
        feedback.setWouldRecommend(Boolean.TRUE.equals(request.getWouldRecommend()));
        feedback.setStrengths(normalize(request.getStrengths()));
        feedback.setImprovementSuggestions(normalize(request.getImprovementSuggestions()));
        feedback.setAdditionalComment(blankToNull(request.getAdditionalComment()));
        feedback.setSubmittedAt(LocalDateTime.now());
    }

    private User requireUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản."));
    }

    private ClassEnrollment requireAssignedEnrollment(Long learnerId, Long classroomId) {
        ClassEnrollment enrollment = enrollmentRepository
                .findByStudentIdAndClassSectionId(learnerId, classroomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bạn chưa được xếp vào lớp học này."));
        if (enrollment.getRegistrationStatus() != ClassroomRegistrationStatus.ASSIGNED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ học viên đang học chính thức mới được đánh giá.");
        }
        return enrollment;
    }

    private List<User> assignedTeachers(ClassSection classroom) {
        Map<Long, User> teachers = new LinkedHashMap<>();
        if (classroom.getPrimaryTeacher() != null) {
            teachers.put(classroom.getPrimaryTeacher().getId(), classroom.getPrimaryTeacher());
        }
        teacherAssignmentRepository.findByClassSectionId(classroom.getId()).forEach(assignment -> {
            User teacher = assignment.getTeacher();
            boolean intersects = (assignment.getEffectiveTo() == null || classroom.getStartDate() == null
                    || !assignment.getEffectiveTo().isBefore(classroom.getStartDate()))
                    && (assignment.getEffectiveFrom() == null || classroom.getPlannedEndDate() == null
                    || !assignment.getEffectiveFrom().isAfter(classroom.getPlannedEndDate()));
            if (teacher != null && intersects) {
                teachers.put(teacher.getId(), teacher);
            }
        });
        if (teachers.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Lớp học chưa có giáo viên để đánh giá.");
        }
        return new ArrayList<>(teachers.values());
    }

    private void requireWindowOpen(ClassSection classroom) {
        LocalDate today = LocalDate.now();
        if (today.isBefore(opensOn(classroom))) {
            throw badRequest("Phiếu đánh giá mở từ ngày " + formatDate(opensOn(classroom)) + ".");
        }
        if (today.isAfter(closesOn(classroom))) {
            throw badRequest("Thời hạn đánh giá đã kết thúc ngày " + formatDate(closesOn(classroom)) + ".");
        }
    }

    private LocalDate opensOn(ClassSection classroom) {
        if (classroom.getPlannedEndDate() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Lớp học chưa có ngày kết thúc.");
        }
        return classroom.getPlannedEndDate().minusDays(Math.max(0, opensDaysBeforeEnd));
    }

    private LocalDate closesOn(ClassSection classroom) {
        return classroom.getPlannedEndDate().plusDays(Math.max(0, closesDaysAfterEnd));
    }

    private LearnerTeacherFeedbackResponse toLearnerResponse(
            ClassSection classroom, User teacher, TeacherCourseFeedback feedback
    ) {
        LocalDate open = opensOn(classroom);
        LocalDate close = closesOn(classroom);
        boolean windowOpen = !LocalDate.now().isBefore(open) && !LocalDate.now().isAfter(close);
        return LearnerTeacherFeedbackResponse.builder()
                .feedbackId(feedback == null ? null : feedback.getId())
                .classroomId(classroom.getId()).classroomTitle(classroomTitle(classroom))
                .teacherId(teacher.getId()).teacherName(teacher.getFullName())
                .opensOn(open).closesOn(close).windowOpen(windowOpen)
                .submitted(feedback != null).editable(windowOpen)
                .clarityScore(feedback == null ? 0 : feedback.getClarityScore())
                .engagementScore(feedback == null ? 0 : feedback.getEngagementScore())
                .learnerSupportScore(feedback == null ? 0 : feedback.getLearnerSupportScore())
                .feedbackTimelinessScore(feedback == null ? 0 : feedback.getFeedbackTimelinessScore())
                .professionalismScore(feedback == null ? 0 : feedback.getProfessionalismScore())
                .pace(feedback == null ? null : feedback.getPace())
                .wouldRecommend(feedback == null ? null : feedback.isWouldRecommend())
                .strengths(feedback == null ? "" : feedback.getStrengths())
                .improvementSuggestions(feedback == null ? "" : feedback.getImprovementSuggestions())
                .additionalComment(feedback == null ? "" : feedback.getAdditionalComment())
                .submittedAt(feedback == null ? null : feedback.getSubmittedAt())
                .updatedAt(feedback == null ? null : feedback.getUpdatedAt())
                .build();
    }

    private TeacherFeedbackAggregateResponse aggregate(List<TeacherCourseFeedback> items, boolean protectTeacher) {
        User teacher = items.getFirst().getTeacher();
        if (protectTeacher && items.size() < anonymityThreshold) {
            return protectedAggregate(teacher, items.size());
        }
        Map<String, Long> pace = items.stream().collect(Collectors.groupingBy(
                item -> item.getPace().name(), LinkedHashMap::new, Collectors.counting()));
        Map<Integer, Long> distribution = items.stream().collect(Collectors.groupingBy(
                this::roundedOverall, TreeMap::new, Collectors.counting()));
        List<ClassroomFeedbackAggregateResponse> classrooms = items.stream()
                .collect(Collectors.groupingBy(
                        item -> item.getClassSection().getId(), LinkedHashMap::new, Collectors.toList()))
                .values().stream()
                .filter(classroomItems -> !protectTeacher || classroomItems.size() >= anonymityThreshold)
                .map(this::classroomAggregate)
                .toList();
        return TeacherFeedbackAggregateResponse.builder()
                .teacherId(teacher.getId()).teacherName(teacher.getFullName())
                .responseCount(items.size()).anonymityThreshold(anonymityThreshold).protectedByAnonymity(false)
                .overallScore(averageOverall(items))
                .clarityScore(averageScore(items, TeacherCourseFeedback::getClarityScore))
                .engagementScore(averageScore(items, TeacherCourseFeedback::getEngagementScore))
                .learnerSupportScore(averageScore(items, TeacherCourseFeedback::getLearnerSupportScore))
                .feedbackTimelinessScore(averageScore(items, TeacherCourseFeedback::getFeedbackTimelinessScore))
                .professionalismScore(averageScore(items, TeacherCourseFeedback::getProfessionalismScore))
                .recommendationPercent(percent(items.stream().filter(TeacherCourseFeedback::isWouldRecommend).count(), items.size()))
                .paceDistribution(pace).overallRatingDistribution(distribution).classrooms(classrooms)
                .build();
    }

    private TeacherFeedbackAggregateResponse emptyAggregate(User teacher) {
        return protectedAggregate(teacher, 0);
    }

    private TeacherFeedbackAggregateResponse protectedAggregate(User teacher, long count) {
        return TeacherFeedbackAggregateResponse.builder()
                .teacherId(teacher.getId()).teacherName(teacher.getFullName())
                .responseCount(count).anonymityThreshold(anonymityThreshold).protectedByAnonymity(true)
                .paceDistribution(Map.of()).overallRatingDistribution(Map.of()).classrooms(List.of())
                .build();
    }

    private ClassroomFeedbackAggregateResponse classroomAggregate(List<TeacherCourseFeedback> items) {
        ClassSection classroom = items.getFirst().getClassSection();
        return ClassroomFeedbackAggregateResponse.builder()
                .classroomId(classroom.getId()).classroomTitle(classroomTitle(classroom))
                .endDate(classroom.getPlannedEndDate()).responseCount(items.size())
                .overallScore(averageOverall(items))
                .recommendationPercent(percent(items.stream().filter(TeacherCourseFeedback::isWouldRecommend).count(), items.size()))
                .build();
    }

    private AnonymizedTeacherFeedbackResponse toAnonymousResponse(TeacherCourseFeedback item) {
        return AnonymizedTeacherFeedbackResponse.builder()
                .feedbackId(item.getId()).classroomId(item.getClassSection().getId())
                .classroomTitle(classroomTitle(item.getClassSection())).overallScore(overall(item))
                .clarityScore(item.getClarityScore()).engagementScore(item.getEngagementScore())
                .learnerSupportScore(item.getLearnerSupportScore())
                .feedbackTimelinessScore(item.getFeedbackTimelinessScore())
                .professionalismScore(item.getProfessionalismScore()).pace(item.getPace())
                .wouldRecommend(item.isWouldRecommend()).strengths(item.getStrengths())
                .improvementSuggestions(item.getImprovementSuggestions())
                .additionalComment(item.getAdditionalComment())
                .submittedAt(item.getSubmittedAt()).updatedAt(item.getUpdatedAt()).build();
    }

    private BigDecimal averageScore(List<TeacherCourseFeedback> items, ToIntFunction<TeacherCourseFeedback> score) {
        return decimal(items.stream().mapToInt(score).average().orElse(0));
    }

    private BigDecimal averageOverall(List<TeacherCourseFeedback> items) {
        return decimal(items.stream().mapToDouble(item -> overall(item).doubleValue()).average().orElse(0));
    }

    private BigDecimal overall(TeacherCourseFeedback item) {
        return decimal((item.getClarityScore() + item.getEngagementScore() + item.getLearnerSupportScore()
                + item.getFeedbackTimelinessScore() + item.getProfessionalismScore()) / 5.0);
    }

    private int roundedOverall(TeacherCourseFeedback item) {
        return overall(item).setScale(0, RoundingMode.HALF_UP).intValue();
    }

    private BigDecimal percent(long part, long total) {
        return total == 0 ? BigDecimal.ZERO : decimal(part * 100.0 / total);
    }

    private BigDecimal decimal(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private String classroomTitle(ClassSection classroom) {
        return classroom.getInstructorLedCourse() == null
                ? "Lớp #" + classroom.getId()
                : classroom.getTitle();
    }

    private void validateNarrative(String text, String label) {
        long words = Arrays.stream(normalize(text).toLowerCase(Locale.ROOT).split("\\s+"))
                .map(word -> word.replaceAll("[^\\p{L}\\p{N}]", ""))
                .filter(word -> word.length() >= 2).distinct().count();
        if (words < 4) {
            throw badRequest(label + " cần ít nhất 4 từ có ý nghĩa khác nhau.");
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    private String blankToNull(String value) {
        String normalized = normalize(value);
        return normalized.isBlank() ? null : normalized;
    }

    private String formatDate(LocalDate date) {
        return "%02d/%02d/%04d".formatted(date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
