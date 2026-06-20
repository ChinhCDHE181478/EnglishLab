package fu.sap490.g23.backend.service.classroom;

import fu.sap490.g23.backend.dto.request.classroom.CreateHomeworkRequest;
import fu.sap490.g23.backend.dto.request.classroom.GradeHomeworkRequest;
import fu.sap490.g23.backend.dto.request.classroom.SubmitHomeworkRequest;
import fu.sap490.g23.backend.dto.response.classroom.ClassroomHomeworkResponse;
import fu.sap490.g23.backend.dto.response.classroom.ClassroomHomeworkSubmissionResponse;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.classroom.*;
import fu.sap490.g23.backend.entity.classroom.enums.*;
import fu.sap490.g23.backend.repository.UserRepository;
import fu.sap490.g23.backend.repository.classroom.*;
import fu.sap490.g23.backend.security.ClassroomAccessHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumSet;
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
    private final ClassroomEnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final ClassroomMapper mapper;
    private final ClassroomAccessHelper accessHelper;

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomHomeworkResponse> listForClass(Long offeringId, String userEmail) {
        User user = accessHelper.requireUser(userEmail);
        Long studentId = isLearnerInClass(user, offeringId) ? user.getId() : null;
        return homeworkRepository.findByClassroomOfferingIdOrderByCreatedAtDesc(offeringId).stream()
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

        ClassroomHomework homework = ClassroomHomework.builder()
                .classroomOffering(offering)
                .session(session)
                .title(request.getTitle().trim())
                .instruction(request.getInstruction())
                .deadline(request.getDeadline())
                .maxScore(request.getMaxScore() == null ? BigDecimal.TEN : request.getMaxScore())
                .allowResubmission(Boolean.TRUE.equals(request.getAllowResubmission()))
                .attachmentUrl(request.getAttachmentUrl())
                .status(request.getStatus() == null ? HomeworkStatus.DRAFT : request.getStatus())
                .createdBy(creator)
                .build();

        return mapper.toHomeworkResponse(homeworkRepository.save(homework), null);
    }

    @Override
    public ClassroomHomeworkResponse update(Long homeworkId, CreateHomeworkRequest request) {
        ClassroomHomework homework = findHomework(homeworkId);
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
        }
        return mapper.toHomeworkResponse(homeworkRepository.save(homework), null);
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

        return mapper.toHomeworkSubmissionResponse(submissionRepository.save(submission));
    }

    @Override
    public ClassroomHomeworkSubmissionResponse grade(Long homeworkId, Long studentId, GradeHomeworkRequest request, String graderEmail) {
        User grader = accessHelper.requireUser(graderEmail);
        accessHelper.assertTeacher(grader);

        ClassroomHomework homework = findHomework(homeworkId);
        ClassroomHomeworkSubmission submission = submissionRepository.findByHomeworkIdAndStudentId(homeworkId, studentId)
                .orElseThrow(() -> new RuntimeException("Học viên chưa nộp bài tập."));

        submission.setScore(request.getScore());
        submission.setTeacherFeedback(request.getTeacherFeedback());
        submission.setGradedAt(LocalDateTime.now());
        submission.setGradedBy(grader);
        submission.setStatus(HomeworkSubmissionStatus.GRADED);

        return mapper.toHomeworkSubmissionResponse(submissionRepository.save(submission));
    }

    private ClassroomHomework findHomework(Long homeworkId) {
        return homeworkRepository.findById(homeworkId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài tập."));
    }

    private boolean isLearnerInClass(User user, Long offeringId) {
        return enrollmentRepository.existsByStudentIdAndClassroomOfferingIdAndRegistrationStatusIn(
                user.getId(), offeringId, HAS_LEARNING_ACCESS
        );
    }
}
