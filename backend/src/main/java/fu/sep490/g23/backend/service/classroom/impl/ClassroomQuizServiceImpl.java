package fu.sep490.g23.backend.service.classroom.impl;
import fu.sep490.g23.backend.entity.classroom.ClassroomQuizQuestion;
import fu.sep490.g23.backend.entity.classroom.ClassroomSession;
import fu.sep490.g23.backend.entity.classroom.ClassroomGradebookEntry;
import fu.sep490.g23.backend.entity.classroom.ClassroomEnrollment;
import fu.sep490.g23.backend.entity.classroom.ClassroomOffering;
import fu.sep490.g23.backend.entity.classroom.ClassroomQuizAttempt;
import fu.sep490.g23.backend.entity.classroom.ClassroomQuiz;
import fu.sep490.g23.backend.repository.classroom.ClassroomOfferingRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomQuizRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomSessionRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomQuizAttemptRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomGradebookEntryRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomEnrollmentRepository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fu.sep490.g23.backend.dto.request.classroom.CreateClassroomQuizRequest;
import fu.sep490.g23.backend.dto.request.classroom.QuizQuestionRequest;
import fu.sep490.g23.backend.dto.request.classroom.SubmitClassroomQuizRequest;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomQuizResponse;
import fu.sep490.g23.backend.dto.response.classroom.QuizQuestionResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomQuizStatus;
import fu.sep490.g23.backend.entity.classroom.enums.GradebookEntryStatus;
import fu.sep490.g23.backend.entity.classroom.*;
import fu.sep490.g23.backend.security.ClassroomAccessHelper;
import fu.sep490.g23.backend.service.classroom.ClassroomQuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class ClassroomQuizServiceImpl implements ClassroomQuizService {

    private final ClassroomQuizRepository quizRepository;
    private final ClassroomQuizAttemptRepository attemptRepository;
    private final ClassroomOfferingRepository offeringRepository;
    private final ClassroomSessionRepository sessionRepository;
    private final ClassroomGradebookEntryRepository gradebookEntryRepository;
    private final ClassroomEnrollmentRepository enrollmentRepository;
    private final ClassroomAccessHelper accessHelper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomQuizResponse> listForClass(Long offeringId, String userEmail) {
        accessHelper.requireUser(userEmail);
        return quizRepository.findByClassroomOfferingIdOrderByCreatedAtDesc(offeringId).stream()
                .map(quiz -> toResponse(quiz, null, true))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomQuizResponse> listForLearner(String learnerEmail) {
        User learner = accessHelper.requireUser(learnerEmail);
        List<Long> offeringIds = enrollmentRepository.findByStudentIdAndStatusIn(
                        learner.getId(),
                        java.util.EnumSet.of(
                                fu.sep490.g23.backend.entity.classroom.enums.ClassroomEnrollmentStatus.ENROLLED,
                                fu.sep490.g23.backend.entity.classroom.enums.ClassroomEnrollmentStatus.COMPLETED
                        ))
                .stream()
                .map(enrollment -> enrollment.getClassroomOffering().getId())
                .distinct()
                .toList();
        List<ClassroomQuizResponse> responses = new ArrayList<>();
        for (Long offeringId : offeringIds) {
            quizRepository.findByClassroomOfferingIdAndStatusOrderByCreatedAtDesc(offeringId, ClassroomQuizStatus.OPEN)
                    .forEach(quiz -> {
                        ClassroomQuizAttempt attempt = attemptRepository.findByQuizIdAndStudentId(quiz.getId(), learner.getId()).orElse(null);
                        responses.add(toResponse(quiz, attempt, false));
                    });
        }
        return responses;
    }

    @Override
    public ClassroomQuizResponse create(Long offeringId, CreateClassroomQuizRequest request, String creatorEmail) {
        User creator = accessHelper.requireUser(creatorEmail);
        accessHelper.assertTeacher(creator);
        ClassroomOffering offering = offeringRepository.findById(offeringId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp học."));
        ClassroomSession session = null;
        if (request.getSessionId() != null) {
            session = sessionRepository.findById(request.getSessionId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy buổi học."));
        }
        ClassroomQuiz quiz = ClassroomQuiz.builder()
                .classroomOffering(offering)
                .session(session)
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .timeLimitMinutes(request.getTimeLimitMinutes())
                .passingScore(request.getPassingScore() == null ? 50 : request.getPassingScore())
                .dueAt(request.getDueAt())
                .status(ClassroomQuizStatus.DRAFT)
                .build();
        List<ClassroomQuizQuestion> questions = new ArrayList<>();
        int order = 0;
        for (QuizQuestionRequest questionRequest : request.getQuestions()) {
            questions.add(ClassroomQuizQuestion.builder()
                    .quiz(quiz)
                    .sortOrder(questionRequest.getSortOrder() == null ? order++ : questionRequest.getSortOrder())
                    .prompt(questionRequest.getPrompt().trim())
                    .optionsJson(questionRequest.getOptionsJson())
                    .correctAnswer(questionRequest.getCorrectAnswer().trim())
                    .explanation(questionRequest.getExplanation())
                    .build());
        }
        quiz.setQuestions(questions);
        return toResponse(quizRepository.save(quiz), null, true);
    }

    @Override
    public ClassroomQuizResponse open(Long quizId) {
        ClassroomQuiz quiz = requireQuiz(quizId);
        if (quiz.getQuestions() == null || quiz.getQuestions().isEmpty()) {
            throw new RuntimeException("Bài kiểm tra cần ít nhất một câu hỏi trước khi mở.");
        }
        quiz.setStatus(ClassroomQuizStatus.OPEN);
        return toResponse(quizRepository.save(quiz), null, true);
    }

    @Override
    public ClassroomQuizResponse close(Long quizId) {
        ClassroomQuiz quiz = requireQuiz(quizId);
        quiz.setStatus(ClassroomQuizStatus.CLOSED);
        return toResponse(quizRepository.save(quiz), null, true);
    }

    @Override
    public ClassroomQuizResponse submit(Long quizId, SubmitClassroomQuizRequest request, String learnerEmail) {
        User learner = accessHelper.requireUser(learnerEmail);
        ClassroomQuiz quiz = requireQuiz(quizId);
        if (quiz.getStatus() != ClassroomQuizStatus.OPEN) {
            throw new RuntimeException("Bài kiểm tra hiện không mở để nộp.");
        }
        if (quiz.getDueAt() != null && LocalDateTime.now().isAfter(quiz.getDueAt())) {
            throw new RuntimeException("Đã quá hạn nộp bài kiểm tra.");
        }
        enrollmentRepository.findByStudentIdAndClassroomOfferingId(learner.getId(), quiz.getClassroomOffering().getId())
                .filter(ClassroomEnrollment::hasClassAccess)
                .orElseThrow(() -> new RuntimeException("Bạn không thuộc lớp học này."));
        if (attemptRepository.findByQuizIdAndStudentId(quizId, learner.getId()).isPresent()) {
            throw new RuntimeException("Bạn đã nộp bài kiểm tra này.");
        }

        Map<String, String> answers = parseAnswers(request.getAnswersJson());
        int correct = 0;
        int total = quiz.getQuestions().size();
        for (ClassroomQuizQuestion question : quiz.getQuestions()) {
            String submitted = answers.get(String.valueOf(question.getId()));
            if (submitted != null && submitted.trim().equalsIgnoreCase(question.getCorrectAnswer().trim())) {
                correct++;
            }
        }
        BigDecimal score = total == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(correct * 10.0 / total).setScale(2, RoundingMode.HALF_UP);
        boolean passed = score.multiply(BigDecimal.TEN).intValue() >= quiz.getPassingScore();

        ClassroomQuizAttempt attempt = attemptRepository.save(ClassroomQuizAttempt.builder()
                .quiz(quiz)
                .student(learner)
                .answersJson(request.getAnswersJson())
                .score(score)
                .correctCount(correct)
                .totalQuestions(total)
                .passed(passed)
                .build());

        syncGradebook(quiz, learner, score);
        return toResponse(quiz, attempt, false);
    }

    @Override
    public void delete(Long quizId) {
        ClassroomQuiz quiz = requireQuiz(quizId);
        if (quiz.getStatus() == ClassroomQuizStatus.OPEN) {
            throw new RuntimeException("Không thể xóa bài kiểm tra đang mở.");
        }
        quizRepository.delete(quiz);
    }

    private void syncGradebook(ClassroomQuiz quiz, User learner, BigDecimal score) {
        ClassroomGradebookEntry entry = gradebookEntryRepository
                .findByClassroomOfferingIdAndStudentId(quiz.getClassroomOffering().getId(), learner.getId())
                .orElseGet(() -> ClassroomGradebookEntry.builder()
                        .classroomOffering(quiz.getClassroomOffering())
                        .student(learner)
                        .status(GradebookEntryStatus.PENDING)
                        .build());
        entry.setQuizScore(score);
        gradebookEntryRepository.save(entry);
    }

    private ClassroomQuiz requireQuiz(Long quizId) {
        return quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài kiểm tra."));
    }

    private Map<String, String> parseAnswers(String answersJson) {
        try {
            return objectMapper.readValue(answersJson, new TypeReference<>() {});
        } catch (Exception ex) {
            throw new RuntimeException("Định dạng câu trả lời không hợp lệ.");
        }
    }

    private ClassroomQuizResponse toResponse(ClassroomQuiz quiz, ClassroomQuizAttempt attempt, boolean includeAnswers) {
        List<QuizQuestionResponse> questions = quiz.getQuestions() == null ? List.of() : quiz.getQuestions().stream()
                .map(question -> QuizQuestionResponse.builder()
                        .id(question.getId())
                        .sortOrder(question.getSortOrder())
                        .prompt(question.getPrompt())
                        .optionsJson(question.getOptionsJson())
                        .correctAnswer(includeAnswers ? question.getCorrectAnswer() : null)
                        .explanation(includeAnswers ? question.getExplanation() : null)
                        .build())
                .toList();
        return ClassroomQuizResponse.builder()
                .id(quiz.getId())
                .classroomOfferingId(quiz.getClassroomOffering().getId())
                .sessionId(quiz.getSession() == null ? null : quiz.getSession().getId())
                .title(quiz.getTitle())
                .description(quiz.getDescription())
                .timeLimitMinutes(quiz.getTimeLimitMinutes())
                .passingScore(quiz.getPassingScore())
                .status(quiz.getStatus().name())
                .dueAt(quiz.getDueAt())
                .submitted(attempt != null)
                .myScore(attempt == null ? null : attempt.getScore())
                .questions(questions)
                .createdAt(quiz.getCreatedAt())
                .build();
    }
}
