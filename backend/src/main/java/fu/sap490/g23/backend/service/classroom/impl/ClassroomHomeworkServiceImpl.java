package fu.sap490.g23.backend.service.classroom.impl;

import fu.sap490.g23.backend.service.classroom.*;


import fu.sap490.g23.backend.dto.request.classroom.CreateHomeworkRequest;
import fu.sap490.g23.backend.dto.request.classroom.GradeHomeworkRequest;
import fu.sap490.g23.backend.dto.request.classroom.SubmitHomeworkRequest;
import fu.sap490.g23.backend.dto.response.classroom.ClassroomHomeworkResponse;
import fu.sap490.g23.backend.dto.response.classroom.ClassroomHomeworkSubmissionResponse;
import fu.sap490.g23.backend.dto.response.classroom.HomeworkAiAssessmentOptionResponse;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.classroom.*;
import fu.sap490.g23.backend.entity.assessment.AssessmentRubric;
import fu.sap490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sap490.g23.backend.entity.assessment.enums.AssessmentType;
import fu.sap490.g23.backend.entity.classroom.enums.*;
import fu.sap490.g23.backend.entity.curriculum.CurriculumUnit;
import fu.sap490.g23.backend.entity.curriculum.AssessmentBankItem;
import fu.sap490.g23.backend.repository.UserRepository;
import fu.sap490.g23.backend.repository.classroom.*;
import fu.sap490.g23.backend.repository.curriculum.CurriculumUnitRepository;
import fu.sap490.g23.backend.repository.curriculum.AssessmentBankItemRepository;
import fu.sap490.g23.backend.security.ClassroomAccessHelper;
import fu.sap490.g23.backend.service.mail.ClassroomHomeworkMailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class ClassroomHomeworkServiceImpl implements ClassroomHomeworkService {

    private static final Set<ClassroomRegistrationStatus> HAS_LEARNING_ACCESS = ClassroomRegistrationSupport.HAS_LEARNING_ACCESS;

    private final ClassroomHomeworkRepository homeworkRepository;
    private final ClassroomHomeworkSubmissionRepository submissionRepository;
    private final ClassroomOfferingRepository offeringRepository;
    private final ClassroomSessionRepository sessionRepository;
    private final CurriculumUnitRepository curriculumUnitRepository;
    private final AssessmentBankItemRepository assessmentBankItemRepository;
    private final ClassroomEnrollmentRepository enrollmentRepository;
    private final ClassroomGradebookEntryRepository gradebookEntryRepository;
    private final UserRepository userRepository;
    private final ClassroomMapper mapper;
    private final ClassroomAccessHelper accessHelper;
    private final ClassroomHomeworkMailService classroomHomeworkMailService;
    private final ClassroomHomeworkGradingCatalogService homeworkGradingCatalogService;
    private final ClassroomHomeworkAiGradingService homeworkAiGradingService;
    private final ClassroomHomeworkScoreCalculator homeworkScoreCalculator;

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomHomeworkResponse> listForClass(Long offeringId, String userEmail) {
        User user = accessHelper.requireUser(userEmail);
        Long studentId = isLearnerInClass(user, offeringId) ? user.getId() : null;
        return homeworkRepository.findByClassroomOfferingIdOrderByCreatedAtDesc(offeringId).stream()
                .filter(homework -> studentId == null || homework.getStatus() == HomeworkStatus.OPEN)
                .map(homework -> mapper.toHomeworkResponse(homework, studentId))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomHomeworkResponse> listForLearner(String learnerEmail) {
        User learner = accessHelper.requireUser(learnerEmail);
        return enrollmentRepository.findByStudentIdAndRegistrationStatusIn(learner.getId(), HAS_LEARNING_ACCESS).stream()
                .flatMap(enrollment -> homeworkRepository
                        .findByClassroomOfferingIdAndStatusOrderByDeadlineAsc(
                                enrollment.getClassroomOffering().getId(),
                                HomeworkStatus.OPEN
                        ).stream())
                .distinct()
                .map(homework -> mapper.toHomeworkResponse(homework, learner.getId()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomHomeworkSubmissionResponse> listSubmissions(Long homeworkId, String teacherEmail) {
        User teacher = accessHelper.requireUser(teacherEmail);
        accessHelper.assertTeacher(teacher);
        ClassroomHomework homework = findHomework(homeworkId);
        return submissionRepository.findByHomeworkIdOrderBySubmittedAtDesc(homework.getId()).stream()
                .map(mapper::toHomeworkSubmissionResponse)
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
                        List.of(AssessmentSkill.WRITING, AssessmentSkill.SPEAKING)
                ).stream()
                .filter(item -> item.getRubric() != null && item.getRubric().isActive())
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

        ClassroomOffering offering = offeringRepository.findById(offeringId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp học."));
        ClassroomSession session = null;
        if (request.getSessionId() != null) {
            session = sessionRepository.findById(request.getSessionId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy buổi học."));
        }
        CurriculumUnit curriculumUnit = resolveCurriculumUnit(offering, request.getCurriculumUnitId());

        ClassroomHomework homework = ClassroomHomework.builder()
                .classroomOffering(offering)
                .session(session)
                .curriculumUnit(curriculumUnit)
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
        homework.setCurriculumUnit(resolveCurriculumUnit(homework.getClassroomOffering(), request.getCurriculumUnitId()));
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
    public ClassroomHomeworkSubmissionResponse submit(Long homeworkId, SubmitHomeworkRequest request, String learnerEmail) {
        User learner = accessHelper.requireUser(learnerEmail);
        ClassroomHomework homework = findHomework(homeworkId);

        if (!isLearnerInClass(learner, homework.getClassroomOffering().getId())) {
            throw new RuntimeException("Bạn không thuộc lớp học này.");
        }
        if (homework.getStatus() != HomeworkStatus.OPEN) {
            throw new RuntimeException("Bài tập chưa mở để nộp.");
        }
        if (homework.getDeadline() != null && homework.getDeadline().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Đã quá hạn nộp bài.");
        }

        ClassroomHomeworkSubmission submission = submissionRepository
                .findByHomeworkIdAndStudentId(homeworkId, learner.getId())
                .orElseGet(() -> ClassroomHomeworkSubmission.builder()
                        .homework(homework)
                        .student(learner)
                        .build());

        if (submission.getStatus() == HomeworkSubmissionStatus.GRADED && !homework.isAllowResubmission()) {
            throw new RuntimeException("Bài tập đã chấm điểm và không cho phép nộp lại.");
        }

        submission.setTextAnswer(request.getTextAnswer());
        submission.setAttachmentUrl(request.getAttachmentUrl());
        submission.setSubmittedAt(LocalDateTime.now());
        submission.setStatus(HomeworkSubmissionStatus.SUBMITTED);
        submission.setScore(null);
        submission.setTeacherFeedback(null);
        submission.setGradedAt(null);
        submission.setGradedBy(null);

        ClassroomHomeworkSubmission saved = submissionRepository.save(submission);
        if (homework.isAiReviewEnabled() && homeworkAiGradingService.tryAutoGrade(saved)) {
            saved = submissionRepository.save(saved);
            syncHomeworkScoreToGradebook(homework, learner.getId(), null);
        }

        return mapper.toHomeworkSubmissionResponse(saved);
    }

    @Override
    public ClassroomHomeworkSubmissionResponse grade(Long homeworkId, Long studentId, GradeHomeworkRequest request, String graderEmail) {
        User grader = accessHelper.requireUser(graderEmail);
        accessHelper.assertTeacher(grader);

        ClassroomHomework homework = findHomework(homeworkId);
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

        submission.setScore(request.getScore());
        submission.setTeacherFeedback(request.getTeacherFeedback());
        submission.setGradedAt(LocalDateTime.now());
        submission.setGradedBy(grader);
        submission.setStatus(HomeworkSubmissionStatus.GRADED);

        ClassroomHomeworkSubmission savedSubmission = submissionRepository.save(submission);
        syncHomeworkScoreToGradebook(homework, studentId, grader);

        return mapper.toHomeworkSubmissionResponse(savedSubmission);
    }

    private void validateScore(BigDecimal score, BigDecimal maxScore) {
        if (score.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Điểm không được âm.");
        }
        if (maxScore != null && score.compareTo(maxScore) > 0) {
            throw new RuntimeException("Điểm không được vượt quá điểm tối đa (" + maxScore.stripTrailingZeros().toPlainString() + ").");
        }
    }

    private void syncHomeworkScoreToGradebook(ClassroomHomework homework, Long studentId, User grader) {
        Long offeringId = homework.getClassroomOffering().getId();
        List<ClassroomHomework> homeworks = homeworkRepository
                .findByClassroomOfferingIdOrderByCreatedAtDesc(offeringId);
        BigDecimal average = homeworkScoreCalculator.calculateAverage(
                homeworks,
                submissionRepository.findAllForStudentGradebook(offeringId, studentId)
        );
        if (average == null) {
            return;
        }

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy học viên."));

        ClassroomGradebookEntry entry = gradebookEntryRepository
                .findByClassroomOfferingIdAndStudentId(offeringId, studentId)
                .orElseGet(() -> ClassroomGradebookEntry.builder()
                        .classroomOffering(homework.getClassroomOffering())
                        .student(student)
                        .status(GradebookEntryStatus.PENDING)
                        .build());

        entry.setHomeworkScore(average);
        if (entry.getStatus() == GradebookEntryStatus.PENDING) {
            entry.setStatus(GradebookEntryStatus.GRADED);
        }
        entry.setUpdatedBy(grader);
        gradebookEntryRepository.save(entry);
    }

    private ClassroomHomework findHomework(Long homeworkId) {
        return homeworkRepository.findById(homeworkId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài tập."));
    }

    private CurriculumUnit resolveCurriculumUnit(ClassroomOffering offering, Long unitId) {
        if (unitId == null) {
            return null;
        }
        CurriculumUnit unit = curriculumUnitRepository.findById(unitId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy unit trong giáo trình."));
        if (offering.getCurriculumProgram() == null
                || unit.getProgram() == null
                || !unit.getProgram().getId().equals(offering.getCurriculumProgram().getId())) {
            throw new RuntimeException("Unit được chọn không thuộc giáo trình của lớp học này.");
        }
        return unit;
    }

    private boolean isLearnerInClass(User user, Long offeringId) {
        return enrollmentRepository.existsByStudentIdAndClassroomOfferingIdAndRegistrationStatusIn(
                user.getId(), offeringId, HAS_LEARNING_ACCESS
        );
    }

    private void notifyStudents(ClassroomHomework homework) {
        enrollmentRepository.findByClassroomOfferingIdAndRegistrationStatusIn(
                        homework.getClassroomOffering().getId(), HAS_LEARNING_ACCESS
                ).stream()
                .map(ClassroomEnrollment::getStudent)
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
            homework.setAssessmentBankItem(assessment);
            homework.setSkill(assessment.getSkill());
            homework.setRubric(assessment.getRubric());
            homework.setActivityConfigJson(assessment.getUiConfigJson());
        } else {
            homework.setAssessmentBankItem(null);
            homework.setSkill(null);
            homework.setRubric(null);
        }

        boolean aiEnabled = Boolean.TRUE.equals(request.getAiReviewEnabled());
        homework.setAiReviewEnabled(aiEnabled);
        homework.setGradingMode(aiEnabled ? HomeworkGradingMode.AI : HomeworkGradingMode.TEACHER);
        if (!aiEnabled) {
            return;
        }
        if (assessment == null) {
            throw new RuntimeException("Chấm điểm AI chỉ dùng được khi chọn đề MODULE_TEST của hệ thống.");
        }
        if (assessment.getSkill() != AssessmentSkill.SPEAKING && assessment.getSkill() != AssessmentSkill.WRITING) {
            throw new RuntimeException("Chấm điểm AI chỉ hỗ trợ MODULE_TEST Writing hoặc Speaking.");
        }
        if (assessment.getRubric() == null) {
            throw new RuntimeException("MODULE_TEST đã chọn chưa có bộ tiêu chí chấm AI.");
        }
        AssessmentRubric rubric = homeworkGradingCatalogService.requireActiveRubric(assessment.getRubric().getId());
        if (rubric.getSkill() != assessment.getSkill()) {
            throw new RuntimeException("Bộ tiêu chí của MODULE_TEST không khớp với kỹ năng bài thi.");
        }
        homework.setRubric(rubric);
    }
}
