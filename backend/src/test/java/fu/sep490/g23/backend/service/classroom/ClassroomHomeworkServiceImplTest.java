package fu.sep490.g23.backend.service.classroom;

import fu.sep490.g23.backend.dto.response.classroom.ClassroomHomeworkSubmissionResponse;
import fu.sep490.g23.backend.dto.request.classroom.CreateHomeworkRequest;
import fu.sep490.g23.backend.dto.request.classroom.GradeHomeworkRequest;
import fu.sep490.g23.backend.dto.request.classroom.HomeworkTextAnnotationRequest;
import fu.sep490.g23.backend.dto.request.classroom.SaveHomeworkAnnotationsRequest;
import fu.sep490.g23.backend.entity.assessment.AssessmentRubric;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentType;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.classroom.ClassEnrollment;
import fu.sep490.g23.backend.entity.classroom.ClassroomHomework;
import fu.sep490.g23.backend.entity.classroom.ClassroomHomeworkSubmission;
import fu.sep490.g23.backend.entity.classroom.ClassSection;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomRegistrationStatus;
import fu.sep490.g23.backend.entity.classroom.enums.HomeworkActivityType;
import fu.sep490.g23.backend.entity.classroom.enums.HomeworkStatus;
import fu.sep490.g23.backend.entity.classroom.enums.HomeworkGradingMode;
import fu.sep490.g23.backend.entity.classroom.enums.HomeworkAnnotationType;
import fu.sep490.g23.backend.entity.classroom.enums.HomeworkSubmissionStatus;
import fu.sep490.g23.backend.entity.curriculum.AssessmentBankItem;
import fu.sep490.g23.backend.dto.request.classroom.SubmitHomeworkRequest;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.classroom.ClassEnrollmentRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomGradebookEntryRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomHomeworkRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomHomeworkSubmissionRepository;
import fu.sep490.g23.backend.repository.classroom.ClassSectionRepository;
import fu.sep490.g23.backend.repository.classroom.ClassScheduleRepository;
import fu.sep490.g23.backend.repository.curriculum.AssessmentBankItemRepository;
import fu.sep490.g23.backend.repository.course.CourseUnitRepository;
import fu.sep490.g23.backend.security.ClassroomAccessHelper;
import fu.sep490.g23.backend.service.classroom.impl.ClassroomHomeworkServiceImpl;
import fu.sep490.g23.backend.service.mail.ClassroomHomeworkMailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class ClassroomHomeworkServiceImplTest {

    @Mock private ClassroomHomeworkRepository homeworkRepository;
    @Mock private ClassroomHomeworkSubmissionRepository submissionRepository;
    @Mock private ClassSectionRepository offeringRepository;
    @Mock private ClassScheduleRepository sessionRepository;
    @Mock private CourseUnitRepository courseUnitRepository;
    @Mock private AssessmentBankItemRepository assessmentBankItemRepository;
    @Mock private ClassEnrollmentRepository enrollmentRepository;
    @Mock private ClassroomGradebookEntryRepository gradebookEntryRepository;
    @Mock private UserRepository userRepository;
    @Mock private ClassroomMapper mapper;
    @Mock private ClassroomAccessHelper accessHelper;
    @Mock private ClassroomHomeworkMailService classroomHomeworkMailService;
    @Mock private ClassroomHomeworkGradingCatalogService homeworkGradingCatalogService;
    @Mock private ClassroomHomeworkAiGradingService homeworkAiGradingService;
    @Mock private ClassroomHomeworkScoreCalculator homeworkScoreCalculator;
    @Mock private ClassroomHomeworkObjectiveGrader homeworkObjectiveGrader;
    @Mock private HomeworkTextAnnotationCodec homeworkTextAnnotationCodec;
    @Mock private fu.sep490.g23.backend.service.curriculum.ContentBankLinkSync contentBankLinkSync;

    @InjectMocks private ClassroomHomeworkServiceImpl service;

    @Test
    void listSubmissions_ReturnsEntireRosterIncludingLearnerWithoutSubmission() {
        User teacher = User.builder().id(1L).email("teacher@englishlab.vn").build();
        User submittedStudent = User.builder().id(2L).fullName("An Nguyễn").email("an@example.com").build();
        User missingStudent = User.builder().id(3L).fullName("Bình Trần").email("binh@example.com").build();
        ClassSection offering = ClassSection.builder().id(10L).build();
        ClassroomHomework homework = ClassroomHomework.builder().id(20L).classSection(offering).build();
        ClassroomHomeworkSubmission submission = ClassroomHomeworkSubmission.builder()
                .id(30L)
                .homework(homework)
                .student(submittedStudent)
                .build();
        ClassroomHomeworkSubmissionResponse submittedResponse = ClassroomHomeworkSubmissionResponse.builder()
                .studentId(submittedStudent.getId())
                .submitted(true)
                .build();
        ClassroomHomeworkSubmissionResponse missingResponse = ClassroomHomeworkSubmissionResponse.builder()
                .studentId(missingStudent.getId())
                .submitted(false)
                .build();

        when(accessHelper.requireUser(teacher.getEmail())).thenReturn(teacher);
        when(homeworkRepository.findById(homework.getId())).thenReturn(Optional.of(homework));
        when(submissionRepository.findByHomeworkId(homework.getId())).thenReturn(List.of(submission));
        when(enrollmentRepository.findByClassSectionIdAndRegistrationStatusIn(
                offering.getId(), ClassroomRegistrationSupport.HAS_LEARNING_ACCESS
        )).thenReturn(List.of(
                ClassEnrollment.builder().student(missingStudent).classSection(offering)
                        .registrationStatus(ClassroomRegistrationStatus.ASSIGNED).build(),
                ClassEnrollment.builder().student(submittedStudent).classSection(offering)
                        .registrationStatus(ClassroomRegistrationStatus.ASSIGNED).build()
        ));
        when(mapper.toHomeworkSubmissionResponse(homework, submittedStudent, submission)).thenReturn(submittedResponse);
        when(mapper.toHomeworkSubmissionResponse(homework, missingStudent, null)).thenReturn(missingResponse);

        List<ClassroomHomeworkSubmissionResponse> result = service.listSubmissions(homework.getId(), teacher.getEmail());

        assertThat(result).extracting(ClassroomHomeworkSubmissionResponse::getStudentId)
                .containsExactly(submittedStudent.getId(), missingStudent.getId());
        assertThat(result).extracting(ClassroomHomeworkSubmissionResponse::isSubmitted)
                .containsExactly(true, false);
        verify(accessHelper).assertTeacher(teacher);
        verify(mapper).toHomeworkSubmissionResponse(homework, missingStudent, null);
    }

    @Test
    void submit_AutoGradesObjectiveHomeworkAndSynchronizesResult() {
        User learner = User.builder().id(2L).fullName("An Nguyễn").email("an@example.com").build();
        ClassSection offering = ClassSection.builder().id(10L).build();
        ClassroomHomework homework = ClassroomHomework.builder()
                .id(20L)
                .classSection(offering)
                .status(HomeworkStatus.OPEN)
                .activityType(HomeworkActivityType.SKILL_PRACTICE)
                .maxScore(BigDecimal.TEN)
                .build();

        when(accessHelper.requireUser(learner.getEmail())).thenReturn(learner);
        when(homeworkRepository.findById(homework.getId())).thenReturn(Optional.of(homework));
        when(enrollmentRepository.existsByStudentIdAndClassSectionIdAndRegistrationStatusIn(
                learner.getId(), offering.getId(), ClassroomRegistrationSupport.HAS_LEARNING_ACCESS
        )).thenReturn(true);
        when(submissionRepository.findByHomeworkIdAndStudentId(homework.getId(), learner.getId()))
                .thenReturn(Optional.empty());
        when(submissionRepository.save(any(ClassroomHomeworkSubmission.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(homeworkObjectiveGrader.supports(homework)).thenReturn(true);
        when(homeworkObjectiveGrader.score(homework, "{\"responses\":{\"1\":\"A\"}}"))
                .thenReturn(new ClassroomHomeworkObjectiveGrader.ObjectiveScore(BigDecimal.TEN, 1, 1));
        when(homeworkRepository.findByClassSectionIdOrderByCreatedAtDesc(offering.getId()))
                .thenReturn(List.of(homework));
        when(submissionRepository.findAllForStudentGradebook(offering.getId(), learner.getId()))
                .thenReturn(List.of());
        when(homeworkScoreCalculator.calculateAverage(any(), any())).thenReturn(null);
        when(mapper.toHomeworkSubmissionResponse(any(ClassroomHomeworkSubmission.class)))
                .thenAnswer(invocation -> {
                    ClassroomHomeworkSubmission saved = invocation.getArgument(0);
                    return ClassroomHomeworkSubmissionResponse.builder()
                            .studentId(learner.getId())
                            .status(saved.getStatus())
                            .score(saved.getScore())
                            .teacherFeedback(saved.getTeacherFeedback())
                            .build();
                });

        ClassroomHomeworkSubmissionResponse result = service.submit(
                homework.getId(),
                SubmitHomeworkRequest.builder().textAnswer("{\"responses\":{\"1\":\"A\"}}").build(),
                learner.getEmail()
        );

        assertThat(result.getStatus()).isEqualTo(HomeworkSubmissionStatus.GRADED);
        assertThat(result.getScore()).isEqualByComparingTo("10");
        assertThat(result.getTeacherFeedback()).contains("1/1 câu đúng");
    }

    @Test
    void grade_RejectsManualScoreChangesForAutoGradedQuiz() {
        User teacher = User.builder().id(1L).email("teacher@englishlab.vn").build();
        ClassroomHomework homework = ClassroomHomework.builder()
                .id(20L)
                .gradingMode(HomeworkGradingMode.AUTO)
                .build();

        when(accessHelper.requireUser(teacher.getEmail())).thenReturn(teacher);
        when(homeworkRepository.findById(homework.getId())).thenReturn(Optional.of(homework));

        assertThatThrownBy(() -> service.grade(
                homework.getId(),
                2L,
                GradeHomeworkRequest.builder().score(BigDecimal.ONE).build(),
                teacher.getEmail()
        ))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("không thể sửa thủ công");

        verify(accessHelper).assertTeacher(teacher);
    }

    @Test
    void create_PreservesSpeakingSkillForTeacherGradedRecording() {
        User teacher = User.builder().id(1L).email("teacher@englishlab.vn").build();
        ClassSection offering = ClassSection.builder().id(10L).build();
        CreateHomeworkRequest request = CreateHomeworkRequest.builder()
                .title("Speaking response")
                .activityType(HomeworkActivityType.TEXT_RESPONSE)
                .skill(AssessmentSkill.SPEAKING)
                .status(HomeworkStatus.DRAFT)
                .build();

        when(accessHelper.requireUser(teacher.getEmail())).thenReturn(teacher);
        when(offeringRepository.findById(offering.getId())).thenReturn(Optional.of(offering));
        when(homeworkRepository.save(any(ClassroomHomework.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.create(offering.getId(), request, teacher.getEmail());

        ArgumentCaptor<ClassroomHomework> homeworkCaptor = ArgumentCaptor.forClass(ClassroomHomework.class);
        verify(homeworkRepository).save(homeworkCaptor.capture());
        assertThat(homeworkCaptor.getValue().getSkill()).isEqualTo(AssessmentSkill.SPEAKING);
        assertThat(homeworkCaptor.getValue().getGradingMode()).isEqualTo(HomeworkGradingMode.TEACHER);
    }

    @Test
    void create_RejectsFlashcardReviewWithReadingSkill() {
        User teacher = User.builder().id(1L).email("teacher@englishlab.vn").build();
        ClassSection offering = ClassSection.builder().id(10L).build();
        CreateHomeworkRequest request = CreateHomeworkRequest.builder()
                .title("Invalid flashcard review")
                .activityType(HomeworkActivityType.FLASHCARD_REVIEW)
                .skill(AssessmentSkill.READING)
                .status(HomeworkStatus.DRAFT)
                .build();

        when(accessHelper.requireUser(teacher.getEmail())).thenReturn(teacher);
        when(offeringRepository.findById(offering.getId())).thenReturn(Optional.of(offering));

        assertThatThrownBy(() -> service.create(offering.getId(), request, teacher.getEmail()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("flashcard chỉ hỗ trợ kỹ năng Vocabulary");

        verify(homeworkRepository, never()).save(any(ClassroomHomework.class));
    }

    @Test
    void create_AllowsVocabularyQuizWithObjectiveAnswerKey() {
        User teacher = User.builder().id(1L).email("teacher@englishlab.vn").build();
        ClassSection offering = ClassSection.builder().id(10L).build();
        CreateHomeworkRequest request = CreateHomeworkRequest.builder()
                .title("Vocabulary quiz")
                .activityType(HomeworkActivityType.SKILL_PRACTICE)
                .skill(AssessmentSkill.VOCABULARY)
                .activityConfigJson("{\"answerKey\":{\"1\":\"A\"}}")
                .status(HomeworkStatus.DRAFT)
                .build();

        when(accessHelper.requireUser(teacher.getEmail())).thenReturn(teacher);
        when(offeringRepository.findById(offering.getId())).thenReturn(Optional.of(offering));
        when(homeworkObjectiveGrader.supports(any(ClassroomHomework.class))).thenReturn(true);
        when(homeworkRepository.save(any(ClassroomHomework.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.create(offering.getId(), request, teacher.getEmail());

        ArgumentCaptor<ClassroomHomework> homeworkCaptor = ArgumentCaptor.forClass(ClassroomHomework.class);
        verify(homeworkRepository).save(homeworkCaptor.capture());
        assertThat(homeworkCaptor.getValue().getSkill()).isEqualTo(AssessmentSkill.VOCABULARY);
        assertThat(homeworkCaptor.getValue().getGradingMode()).isEqualTo(HomeworkGradingMode.AUTO);
    }

    @Test
    void listAiAssessmentOptions_IncludesObjectiveReadingAssessmentWithoutRubric() {
        User teacher = User.builder().id(1L).email("teacher@englishlab.vn").build();
        AssessmentBankItem readingAssessment = AssessmentBankItem.builder()
                .id(11L)
                .title("Reading practice")
                .type(AssessmentType.MODULE_TEST)
                .status("PUBLISHED")
                .active(true)
                .skill(AssessmentSkill.READING)
                .uiConfigJson("{}")
                .build();

        when(accessHelper.requireUser(teacher.getEmail())).thenReturn(teacher);
        when(assessmentBankItemRepository
                .findByTypeAndStatusAndActiveTrueAndSkillInOrderByDisplayOrderAscUpdatedAtDescIdDesc(
                        AssessmentType.MODULE_TEST,
                        "PUBLISHED",
                        List.of(
                                AssessmentSkill.LISTENING,
                                AssessmentSkill.READING,
                                AssessmentSkill.WRITING,
                                AssessmentSkill.SPEAKING
                        )
                )).thenReturn(List.of(readingAssessment));

        var result = service.listAiAssessmentOptions(teacher.getEmail());

        assertThat(result).singleElement().satisfies(option -> {
            assertThat(option.getId()).isEqualTo(readingAssessment.getId());
            assertThat(option.getSkill()).isEqualTo(AssessmentSkill.READING);
            assertThat(option.getRubricId()).isNull();
        });
        verify(accessHelper).assertTeacher(teacher);
    }

    @Test
    void create_UsesTeacherSelectedRubricForAiHomework() {
        User teacher = User.builder().id(1L).email("teacher@englishlab.vn").build();
        ClassSection offering = ClassSection.builder().id(10L).build();
        AssessmentRubric defaultRubric = AssessmentRubric.builder()
                .id(1L)
                .name("Default writing rubric")
                .skill(AssessmentSkill.WRITING)
                .build();
        AssessmentRubric selectedRubric = AssessmentRubric.builder()
                .id(2L)
                .name("Teacher selected rubric")
                .skill(AssessmentSkill.WRITING)
                .build();
        AssessmentBankItem assessment = AssessmentBankItem.builder()
                .id(11L)
                .title("IELTS Writing Task 2")
                .type(AssessmentType.MODULE_TEST)
                .status("PUBLISHED")
                .active(true)
                .skill(AssessmentSkill.WRITING)
                .rubric(defaultRubric)
                .uiConfigJson("{}")
                .build();
        CreateHomeworkRequest request = CreateHomeworkRequest.builder()
                .title("AI writing homework")
                .activityType(HomeworkActivityType.TEXT_RESPONSE)
                .status(HomeworkStatus.DRAFT)
                .aiReviewEnabled(true)
                .assessmentBankItemId(assessment.getId())
                .rubricId(selectedRubric.getId())
                .build();

        when(accessHelper.requireUser(teacher.getEmail())).thenReturn(teacher);
        when(offeringRepository.findById(offering.getId())).thenReturn(Optional.of(offering));
        when(assessmentBankItemRepository.findByIdAndTypeAndStatusAndActiveTrue(
                assessment.getId(), AssessmentType.MODULE_TEST, "PUBLISHED"
        )).thenReturn(Optional.of(assessment));
        when(homeworkGradingCatalogService.requireActiveRubric(selectedRubric.getId()))
                .thenReturn(selectedRubric);
        when(homeworkRepository.save(any(ClassroomHomework.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.create(offering.getId(), request, teacher.getEmail());

        ArgumentCaptor<ClassroomHomework> homeworkCaptor = ArgumentCaptor.forClass(ClassroomHomework.class);
        verify(homeworkRepository).save(homeworkCaptor.capture());
        ClassroomHomework savedHomework = homeworkCaptor.getValue();
        assertThat(savedHomework.getAssessmentBankItem()).isEqualTo(assessment);
        assertThat(savedHomework.getRubric()).isEqualTo(selectedRubric);
        assertThat(savedHomework.getSkill()).isEqualTo(AssessmentSkill.WRITING);
        assertThat(savedHomework.getGradingMode()).isEqualTo(HomeworkGradingMode.AI);
        assertThat(savedHomework.isAiReviewEnabled()).isTrue();
        verify(homeworkGradingCatalogService).requireActiveRubric(selectedRubric.getId());
    }

    @Test
    void create_RejectsObjectiveQuizWithoutAnswerKey() {
        User teacher = User.builder().id(1L).email("teacher@englishlab.vn").build();
        ClassSection offering = ClassSection.builder().id(10L).build();
        CreateHomeworkRequest request = CreateHomeworkRequest.builder()
                .title("Reading quiz")
                .activityType(HomeworkActivityType.SKILL_PRACTICE)
                .activityConfigJson("{\"questions\":[]}")
                .skill(AssessmentSkill.READING)
                .status(HomeworkStatus.DRAFT)
                .build();

        when(accessHelper.requireUser(teacher.getEmail())).thenReturn(teacher);
        when(offeringRepository.findById(offering.getId())).thenReturn(Optional.of(offering));

        assertThatThrownBy(() -> service.create(offering.getId(), request, teacher.getEmail()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("phải có đáp án đúng");

        verify(homeworkRepository, never()).save(any());
    }

    @Test
    void saveAnnotations_PersistsForLegacyTextHomeworkWithoutSkill() {
        User teacher = User.builder().id(1L).email("teacher@englishlab.vn").build();
        ClassroomHomework homework = ClassroomHomework.builder()
                .id(20L)
                .build();
        ClassroomHomeworkSubmission submission = ClassroomHomeworkSubmission.builder()
                .id(30L)
                .homework(homework)
                .student(User.builder().id(2L).build())
                .textAnswer("I go to school yesterday.")
                .status(HomeworkSubmissionStatus.GRADED)
                .score(BigDecimal.valueOf(8))
                .build();
        HomeworkTextAnnotationRequest annotation = HomeworkTextAnnotationRequest.builder()
                .id("annotation-1")
                .type(HomeworkAnnotationType.CORRECTION)
                .startOffset(2)
                .endOffset(4)
                .selectedText("go")
                .replacementText("went")
                .build();
        SaveHomeworkAnnotationsRequest request = SaveHomeworkAnnotationsRequest.builder()
                .annotations(List.of(annotation))
                .build();
        String serialized = "[{\"id\":\"annotation-1\"}]";

        when(accessHelper.requireUser(teacher.getEmail())).thenReturn(teacher);
        when(homeworkRepository.findById(homework.getId())).thenReturn(Optional.of(homework));
        when(submissionRepository.findByHomeworkIdAndStudentId(homework.getId(), 2L))
                .thenReturn(Optional.of(submission));
        when(homeworkTextAnnotationCodec.validateAndSerialize(submission.getTextAnswer(), request.getAnnotations()))
                .thenReturn(serialized);
        when(submissionRepository.save(submission)).thenReturn(submission);
        when(mapper.toHomeworkSubmissionResponse(submission))
                .thenReturn(ClassroomHomeworkSubmissionResponse.builder().id(submission.getId()).build());

        ClassroomHomeworkSubmissionResponse result = service.saveAnnotations(
                homework.getId(), 2L, request, teacher.getEmail()
        );

        assertThat(result.getId()).isEqualTo(submission.getId());
        assertThat(submission.getTeacherAnnotationsJson()).isEqualTo(serialized);
        assertThat(submission.getStatus()).isEqualTo(HomeworkSubmissionStatus.GRADED);
        assertThat(submission.getScore()).isEqualByComparingTo("8");
        verify(submissionRepository).save(submission);
        verify(accessHelper).assertTeacher(teacher);
    }

    @Test
    void saveAnnotations_RejectsSubmissionWithoutTextAnswer() {
        User teacher = User.builder().id(1L).email("teacher@englishlab.vn").build();
        ClassroomHomework homework = ClassroomHomework.builder().id(20L).build();
        ClassroomHomeworkSubmission submission = ClassroomHomeworkSubmission.builder()
                .id(30L)
                .homework(homework)
                .student(User.builder().id(2L).build())
                .status(HomeworkSubmissionStatus.SUBMITTED)
                .build();

        when(accessHelper.requireUser(teacher.getEmail())).thenReturn(teacher);
        when(homeworkRepository.findById(homework.getId())).thenReturn(Optional.of(homework));
        when(submissionRepository.findByHomeworkIdAndStudentId(homework.getId(), 2L))
                .thenReturn(Optional.of(submission));

        assertThatThrownBy(() -> service.saveAnnotations(
                homework.getId(), 2L, SaveHomeworkAnnotationsRequest.builder().annotations(List.of()).build(), teacher.getEmail()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("không có nội dung văn bản");

        verify(submissionRepository, never()).save(any());
    }
}
