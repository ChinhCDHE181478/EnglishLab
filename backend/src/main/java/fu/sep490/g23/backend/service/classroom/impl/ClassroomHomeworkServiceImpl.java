package fu.sep490.g23.backend.service.classroom.impl;
import fu.sep490.g23.backend.entity.classroom.enums.HomeworkSubmissionStatus;
import fu.sep490.g23.backend.entity.classroom.enums.HomeworkGradingMode;
import fu.sep490.g23.backend.entity.classroom.ClassEnrollment;
import fu.sep490.g23.backend.entity.classroom.enums.HomeworkActivityType;
import fu.sep490.g23.backend.entity.classroom.ClassSchedule;
import fu.sep490.g23.backend.entity.classroom.enums.HomeworkStatus;
import fu.sep490.g23.backend.service.classroom.ClassroomRegistrationSupport;
import fu.sep490.g23.backend.entity.classroom.ClassroomHomeworkSubmission;
import fu.sep490.g23.backend.entity.classroom.enums.GradebookEntryStatus;
import fu.sep490.g23.backend.entity.classroom.ClassroomGradebookEntry;
import fu.sep490.g23.backend.service.classroom.HomeworkTextAnnotationCodec;
import fu.sep490.g23.backend.repository.classroom.ClassroomGradebookEntryRepository;
import fu.sep490.g23.backend.repository.classroom.ClassEnrollmentRepository;
import fu.sep490.g23.backend.service.classroom.ClassroomHomeworkObjectiveGrader;
import fu.sep490.g23.backend.service.classroom.ClassroomHomeworkGradingCatalogService;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomRegistrationStatus;
import fu.sep490.g23.backend.service.classroom.ClassroomHomeworkService;
import fu.sep490.g23.backend.service.classroom.ClassroomHomeworkAiGradingService;
import fu.sep490.g23.backend.entity.classroom.ClassSection;
import fu.sep490.g23.backend.entity.classroom.ClassroomHomework;
import fu.sep490.g23.backend.service.classroom.ClassroomHomeworkScoreCalculator;
import fu.sep490.g23.backend.repository.classroom.ClassSectionRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomHomeworkRepository;
import fu.sep490.g23.backend.service.classroom.ClassroomMapper;
import fu.sep490.g23.backend.repository.classroom.ClassScheduleRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomHomeworkSubmissionRepository;


import fu.sep490.g23.backend.dto.request.classroom.CreateHomeworkRequest;
import fu.sep490.g23.backend.dto.request.classroom.GradeHomeworkRequest;
import fu.sep490.g23.backend.dto.request.classroom.SaveHomeworkAnnotationsRequest;
import fu.sep490.g23.backend.dto.request.classroom.SubmitHomeworkRequest;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomHomeworkResponse;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomHomeworkSubmissionResponse;
import fu.sep490.g23.backend.dto.response.classroom.HomeworkAiAssessmentOptionResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.assessment.AssessmentRubric;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentType;
import fu.sep490.g23.backend.entity.classroom.*;
import fu.sep490.g23.backend.entity.course.CourseUnit;
import fu.sep490.g23.backend.entity.curriculum.AssessmentBankItem;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.course.CourseUnitRepository;
import fu.sep490.g23.backend.repository.curriculum.AssessmentBankItemRepository;
import fu.sep490.g23.backend.security.ClassroomAccessHelper;
import fu.sep490.g23.backend.service.classroom.*;
import fu.sep490.g23.backend.service.mail.ClassroomHomeworkMailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ClassroomHomeworkServiceImpl implements ClassroomHomeworkService {

    private static final Set<ClassroomRegistrationStatus> HAS_LEARNING_ACCESS = ClassroomRegistrationSupport.HAS_LEARNING_ACCESS;

    private final ClassroomHomeworkRepository homeworkRepository;
    private final ClassroomHomeworkSubmissionRepository submissionRepository;
    private final ClassSectionRepository offeringRepository;
    private final ClassScheduleRepository sessionRepository;
    private final CourseUnitRepository courseUnitRepository;
    private final AssessmentBankItemRepository assessmentBankItemRepository;
    private final ClassEnrollmentRepository enrollmentRepository;
    private final ClassroomGradebookEntryRepository gradebookEntryRepository;
    private final UserRepository userRepository;
    private final ClassroomMapper mapper;
    private final ClassroomAccessHelper accessHelper;
    private final ClassroomHomeworkMailService classroomHomeworkMailService;
    private final ClassroomHomeworkGradingCatalogService homeworkGradingCatalogService;
    private final ClassroomHomeworkAiGradingService homeworkAiGradingService;
    private final ClassroomHomeworkScoreCalculator homeworkScoreCalculator;
    private final ClassroomHomeworkObjectiveGrader homeworkObjectiveGrader;
    private final HomeworkTextAnnotationCodec homeworkTextAnnotationCodec;


    @Override
    @Transactional(readOnly = true)
    public List<ClassroomHomeworkSubmissionResponse> listSubmissions(Long homeworkId, String teacherEmail) {
        User teacher = accessHelper.requireUser(teacherEmail);
        accessHelper.assertTeacher(teacher);
        ClassroomHomework homework = findHomework(homeworkId);
        Map<Long, ClassroomHomeworkSubmission> submissionsByStudent = submissionRepository
                .findByHomeworkId(homework.getId()).stream()
                .collect(Collectors.toMap(
                        submission -> submission.getStudent().getId(),
                        Function.identity()
                ));
        return enrollmentRepository.findByClassSectionIdAndRegistrationStatusIn(
                        homework.getClassSection().getId(), HAS_LEARNING_ACCESS
                ).stream()
                .map(ClassEnrollment::getStudent)
                .filter(student -> student != null)
                .sorted(Comparator.comparing(
                        User::getFullName,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                ))
                .map(student -> mapper.toHomeworkSubmissionResponse(
                        homework,
                        student,
                        submissionsByStudent.get(student.getId())
                ))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<HomeworkAiAssessmentOptionResponse> listAiAssessmentOptions(String teacherEmail) {
        User teacher = accessHelper.requireUser(teacherEmail);
        accessHelper.assertTeacher(teacher);
        return assessmentBankItemRepository
                .findByTypeAndStatusAndActiveTrueAndSkillInOrderByDisplayOrderAscUpdatedAtDescIdDesc(
                        AssessmentType.MODULE_TEST,
                        "PUBLISHED",
                    List.of(
                            AssessmentSkill.LISTENING,
                            AssessmentSkill.READING,
                            AssessmentSkill.WRITING,
                            AssessmentSkill.SPEAKING
                    )
                ).stream()
                .filter(item -> item.getSkill() == AssessmentSkill.LISTENING
                        || item.getSkill() == AssessmentSkill.READING
                        || (item.getRubric() != null && item.getRubric().isActive()))
                .map(item -> HomeworkAiAssessmentOptionResponse.builder()
                        .id(item.getId())
                        .title(item.getTitle())
                        .description(item.getDescription())
                        .skill(item.getSkill())
                        .instructions(item.getInstructions())
                        .uiConfigJson(item.getUiConfigJson())
                        .maxScore(item.getMaxScore())
                        .timeLimitMinutes(item.getTimeLimitMinutes())
                        .rubricId(item.getRubric() == null ? null : item.getRubric().getId())
                        .rubricName(item.getRubric() == null ? null : item.getRubric().getName())
                        .build())
                .toList();
    }

    @Override
    public ClassroomHomeworkResponse create(Long offeringId, CreateHomeworkRequest request, String creatorEmail) {
        User creator = accessHelper.requireUser(creatorEmail);
        accessHelper.assertTeacher(creator);

        ClassSection offering = offeringRepository.findById(offeringId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp học."));
        ClassSchedule session = null;
        if (request.getSessionId() != null) {
            session = sessionRepository.findById(request.getSessionId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy buổi học."));
        }
        CourseUnit courseUnit = resolveCourseUnit(offering, request.getCourseUnitId());

        ClassroomHomework homework = ClassroomHomework.builder()
                .classSection(offering)
                .session(session)
                .courseUnit(courseUnit)
                .title(request.getTitle().trim())
                .instruction(request.getInstruction())
                .deadline(request.getDeadline())
                .maxScore(request.getMaxScore() == null ? BigDecimal.TEN : request.getMaxScore())
                .allowResubmission(Boolean.TRUE.equals(request.getAllowResubmission()))
                .attachmentUrl(request.getAttachmentUrl())
                .activityType(request.getActivityType() == null ? HomeworkActivityType.TEXT_RESPONSE : request.getActivityType())
                .activityConfigJson(request.getActivityConfigJson())
                .status(request.getStatus() == null ? HomeworkStatus.DRAFT : request.getStatus())
                .createdBy(creator)
                .build();
        applyGradingConfig(homework, request);
        linkCourseUnit(homework, courseUnit);

        ClassroomHomework saved = homeworkRepository.save(homework);
        if (saved.getStatus() == HomeworkStatus.OPEN) {
            notifyStudents(saved);
        }
        return mapper.toHomeworkResponse(saved, null);
    }

    @Override
    public ClassroomHomeworkResponse update(Long homeworkId, CreateHomeworkRequest request) {
        ClassroomHomework homework = findHomework(homeworkId);
        boolean wasOpen = homework.getStatus() == HomeworkStatus.OPEN;
        homework.setTitle(request.getTitle().trim());
        homework.setInstruction(request.getInstruction());
        homework.setDeadline(request.getDeadline());
        if (request.getMaxScore() != null) {
            homework.setMaxScore(request.getMaxScore());
        }
        if (request.getAllowResubmission() != null) {
            homework.setAllowResubmission(request.getAllowResubmission());
        }
        homework.setAttachmentUrl(request.getAttachmentUrl());
        if (request.getStatus() != null) {
            homework.setStatus(request.getStatus());
        }
        if (request.getSessionId() != null) {
            homework.setSession(sessionRepository.findById(request.getSessionId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy buổi học.")));
        } else {
            homework.setSession(null);
        }
        homework.setCourseUnit(resolveCourseUnit(homework.getClassSection(), request.getCourseUnitId()));
        linkCourseUnit(homework, homework.getCourseUnit());
        homework.setActivityType(request.getActivityType() == null ? HomeworkActivityType.TEXT_RESPONSE : request.getActivityType());
        homework.setActivityConfigJson(request.getActivityConfigJson());
        applyGradingConfig(homework, request);
        ClassroomHomework saved = homeworkRepository.save(homework);
        if (!wasOpen && saved.getStatus() == HomeworkStatus.OPEN) {
            notifyStudents(saved);
        }
        return mapper.toHomeworkResponse(saved, null);
    }

    @Override
    public void delete(Long homeworkId) {
        homeworkRepository.delete(findHomework(homeworkId));
    }


    @Override
    public ClassroomHomeworkSubmissionResponse grade(Long homeworkId, Long studentId, GradeHomeworkRequest request, String graderEmail) {
        User grader = accessHelper.requireUser(graderEmail);
        accessHelper.assertTeacher(grader);

        ClassroomHomework homework = findHomework(homeworkId);
        if (homework.getGradingMode() == HomeworkGradingMode.AUTO || homeworkObjectiveGrader.supports(homework)) {
            throw new RuntimeException("Điểm trắc nghiệm do hệ thống chấm theo đáp án và không thể sửa thủ công.");
        }
        ClassroomHomeworkSubmission submission = submissionRepository.findByHomeworkIdAndStudentId(homeworkId, studentId)
                .orElseThrow(() -> new RuntimeException("Học viên chưa nộp bài tập."));

        if (submission.getStatus() != HomeworkSubmissionStatus.SUBMITTED
                && submission.getStatus() != HomeworkSubmissionStatus.GRADED) {
            throw new RuntimeException("Bài nộp chưa sẵn sàng để chấm điểm.");
        }
        if (request.getScore() == null) {
            throw new RuntimeException("Vui lòng nhập điểm.");
        }
        validateScore(request.getScore(), homework.getMaxScore());
        if (request.getAnnotations() != null && !request.getAnnotations().isEmpty()
                && !hasText(submission.getTextAnswer())) {
            throw new IllegalArgumentException("Bài nộp không có nội dung văn bản để ghi chú theo đoạn.");
        }

        submission.setScore(request.getScore());
        submission.setTeacherFeedback(request.getTeacherFeedback());
        submission.setTeacherAnnotationsJson(homeworkTextAnnotationCodec.validateAndSerialize(
                submission.getTextAnswer(), request.getAnnotations()
        ));
        submission.setGradedAt(LocalDateTime.now());
        submission.setGradedBy(grader);
        submission.setStatus(HomeworkSubmissionStatus.GRADED);

        ClassroomHomeworkSubmission savedSubmission = submissionRepository.save(submission);
        syncHomeworkScoreToGradebook(homework, studentId, grader);

        return mapper.toHomeworkSubmissionResponse(savedSubmission);
    }

    @Override
    public ClassroomHomeworkSubmissionResponse saveAnnotations(
            Long homeworkId,
            Long studentId,
            SaveHomeworkAnnotationsRequest request,
            String teacherEmail
    ) {
        User teacher = accessHelper.requireUser(teacherEmail);
        accessHelper.assertTeacher(teacher);
        findHomework(homeworkId);

        ClassroomHomeworkSubmission submission = submissionRepository.findByHomeworkIdAndStudentId(homeworkId, studentId)
                .orElseThrow(() -> new RuntimeException("Học viên chưa nộp bài tập."));
        if (submission.getStatus() != HomeworkSubmissionStatus.SUBMITTED
                && submission.getStatus() != HomeworkSubmissionStatus.GRADED) {
            throw new RuntimeException("Bài nộp chưa sẵn sàng để nhận xét.");
        }
        if (!hasText(submission.getTextAnswer())) {
            throw new IllegalArgumentException("Bài nộp không có nội dung văn bản để ghi chú theo đoạn.");
        }

        submission.setTeacherAnnotationsJson(homeworkTextAnnotationCodec.validateAndSerialize(
                submission.getTextAnswer(), request == null ? null : request.getAnnotations()
        ));
        return mapper.toHomeworkSubmissionResponse(submissionRepository.save(submission));
    }

    private void validateScore(BigDecimal score, BigDecimal maxScore) {
        if (score.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Điểm không được âm.");
        }
        if (maxScore != null && score.compareTo(maxScore) > 0) {
            throw new RuntimeException("Điểm không được vượt quá điểm tối đa (" + maxScore.stripTrailingZeros().toPlainString() + ").");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }



    private CourseUnit resolveCourseUnit(ClassSection offering, Long unitId) {
        if (unitId == null) {
            return null;
        }
        CourseUnit unit = courseUnitRepository.findById(unitId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy unit trong giáo trình."));
        if (offering.getInstructorLedCourse() == null
                || unit.getInstructorLedCourse() == null
                || !unit.getInstructorLedCourse().getId().equals(offering.getInstructorLedCourse().getId())) {
            throw new RuntimeException("Unit được chọn không thuộc giáo trình của lớp học này.");
        }
        return unit;
    }

    private void linkCourseUnit(ClassroomHomework homework, CourseUnit unit) {
        if (unit == null || unit.getId() == null) {
            homework.setCourseUnit(null);
            return;
        }
        if (courseUnitRepository.existsById(unit.getId())) {
            homework.setCourseUnit(courseUnitRepository.getReferenceById(unit.getId()));
        } else {
            homework.setCourseUnit(null);
        }
    }


    private void notifyStudents(ClassroomHomework homework) {
        enrollmentRepository.findByClassSectionIdAndRegistrationStatusIn(
                        homework.getClassSection().getId(), HAS_LEARNING_ACCESS
                ).stream()
                .map(ClassEnrollment::getStudent)
                .filter(student -> student != null && student.getEmail() != null && !student.getEmail().isBlank())
                .forEach(student -> classroomHomeworkMailService.sendHomeworkAssigned(student, homework));
    }

    private void applyGradingConfig(ClassroomHomework homework, CreateHomeworkRequest request) {
        AssessmentBankItem assessment = null;
        if (request.getAssessmentBankItemId() != null) {
            assessment = assessmentBankItemRepository
                    .findByIdAndTypeAndStatusAndActiveTrue(
                            request.getAssessmentBankItemId(), AssessmentType.MODULE_TEST, "PUBLISHED")
                    .orElseThrow(() -> new RuntimeException("Đề hệ thống không tồn tại hoặc chưa được xuất bản."));
            if (request.getSkill() != null && request.getSkill() != assessment.getSkill()) {
                throw new RuntimeException("Kỹ năng đã chọn không phù hợp với đề trong ngân hàng.");
            }
            if (homework.getActivityType() != HomeworkActivityType.TEXT_RESPONSE
                    && homework.getActivityType() != HomeworkActivityType.SKILL_PRACTICE
                    && homework.getActivityType() != HomeworkActivityType.MIXED) {
                throw new RuntimeException("Hình thức bài tập này không hỗ trợ chọn đề từ ngân hàng.");
            }
            homework.setAssessmentBankItem(assessment);
            homework.setSkill(assessment.getSkill());
            homework.setRubric(assessment.getRubric());
            homework.setActivityConfigJson(assessment.getUiConfigJson());
        } else {
            homework.setAssessmentBankItem(null);
            homework.setSkill(request.getSkill());
            homework.setRubric(null);
        }

        validateActivitySkillCompatibility(homework.getActivityType(), homework.getSkill());

        boolean aiEnabled = Boolean.TRUE.equals(request.getAiReviewEnabled());
        if (homework.getActivityType() == HomeworkActivityType.SKILL_PRACTICE) {
            if (homework.getSkill() == AssessmentSkill.SPEAKING || homework.getSkill() == AssessmentSkill.WRITING) {
                throw new RuntimeException("Bài Speaking hoặc Writing cần học viên nộp bài để giáo viên chấm, không dùng loại trắc nghiệm.");
            }
            if (!homeworkObjectiveGrader.supports(homework)) {
                throw new RuntimeException("Bài trắc nghiệm phải có đáp án đúng để hệ thống tự chấm.");
            }
        }
        homework.setAiReviewEnabled(aiEnabled);
        homework.setGradingMode(aiEnabled
                ? HomeworkGradingMode.AI
                : homeworkObjectiveGrader.supports(homework) ? HomeworkGradingMode.AUTO : HomeworkGradingMode.TEACHER);
        if (!aiEnabled) {
            return;
        }
        if (assessment == null) {
            throw new RuntimeException("Chấm điểm AI chỉ dùng được khi chọn đề MODULE_TEST của hệ thống.");
        }
        if (assessment.getSkill() != AssessmentSkill.SPEAKING && assessment.getSkill() != AssessmentSkill.WRITING) {
            throw new RuntimeException("Chấm điểm AI chỉ hỗ trợ MODULE_TEST Writing hoặc Speaking.");
        }
        Long rubricId = request.getRubricId() != null
                ? request.getRubricId()
                : assessment.getRubric() == null ? null : assessment.getRubric().getId();
        if (rubricId == null) {
            throw new RuntimeException("MODULE_TEST đã chọn chưa có bộ tiêu chí chấm AI.");
        }
        AssessmentRubric rubric = homeworkGradingCatalogService.requireActiveRubric(rubricId);
        if (rubric.getSkill() != assessment.getSkill()) {
            throw new RuntimeException("Bộ tiêu chí của MODULE_TEST không khớp với kỹ năng bài thi.");
        }
        homework.setRubric(rubric);
    }

    private void validateActivitySkillCompatibility(HomeworkActivityType activityType, AssessmentSkill skill) {
        if (activityType == null || skill == null) {
            throw new RuntimeException("Vui lòng chọn hình thức bài tập và kỹ năng.");
        }
        if (activityType == HomeworkActivityType.FLASHCARD_REVIEW && skill != AssessmentSkill.VOCABULARY) {
            throw new RuntimeException("Bài ôn flashcard chỉ hỗ trợ kỹ năng Vocabulary.");
        }
        if (activityType == HomeworkActivityType.SKILL_PRACTICE
                && skill != AssessmentSkill.LISTENING
                && skill != AssessmentSkill.READING
                && skill != AssessmentSkill.VOCABULARY) {
            throw new RuntimeException("Bài trắc nghiệm chỉ hỗ trợ kỹ năng Listening, Reading hoặc Vocabulary.");
        }
    }
}
