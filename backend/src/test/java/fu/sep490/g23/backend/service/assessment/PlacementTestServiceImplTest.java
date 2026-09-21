package fu.sep490.g23.backend.service.assessment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fu.sep490.g23.backend.dto.request.assessment.PlacementTestSubmissionRequest;
import fu.sep490.g23.backend.dto.response.assessment.PlacementTestAttemptResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.assessment.PlacementTestDefinition;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.assessment.PlacementTestAttemptRepository;
import fu.sep490.g23.backend.repository.curriculum.ContentBankItemRepository;
import fu.sep490.g23.backend.service.ai.AiEvaluationClient;
import fu.sep490.g23.backend.service.assessment.impl.PlacementTestServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlacementTestServiceImplTest {

    private PlacementTestServiceImpl newService(
            UserRepository userRepository,
            PlacementTestAttemptRepository attemptRepository,
            AiEvaluationClient aiEvaluationClient,
            AssessmentAudioStorageService audioStorageService,
            PlacementTestDefinitionService definitionService
    ) {
        return newService(
                userRepository,
                attemptRepository,
                aiEvaluationClient,
                audioStorageService,
                definitionService,
                mock(PlacementTestSessionToken.class)
        );
    }

    private PlacementTestServiceImpl newService(
            UserRepository userRepository,
            PlacementTestAttemptRepository attemptRepository,
            AiEvaluationClient aiEvaluationClient,
            AssessmentAudioStorageService audioStorageService,
            PlacementTestDefinitionService definitionService,
            PlacementTestSessionToken sessionToken
    ) {
        return new PlacementTestServiceImpl(
                userRepository,
                attemptRepository,
                aiEvaluationClient,
                audioStorageService,
                definitionService,
                sessionToken,
                mock(ContentBankItemRepository.class)
        );
    }

    @Test
    void getTestAlwaysAllowsRetakeRegardlessOfPreviousAttemptCount() {
        UserRepository userRepository = mock(UserRepository.class);
        PlacementTestAttemptRepository attemptRepository = mock(PlacementTestAttemptRepository.class);
        AiEvaluationClient aiEvaluationClient = mock(AiEvaluationClient.class);
        AssessmentAudioStorageService audioStorageService = mock(AssessmentAudioStorageService.class);
        PlacementTestDefinitionService definitionService = mock(PlacementTestDefinitionService.class);
        PlacementTestServiceImpl service = newService(
                userRepository,
                attemptRepository,
                aiEvaluationClient,
                audioStorageService,
                definitionService
        );

        User student = User.builder().id(1L).email("learner@example.com").build();
        PlacementTestDefinition definition = PlacementTestDefinition.builder()
                .testCode(PlacementTestDefinitionService.TEST_CODE)
                .title("Placement test")
                .description("Placement test description")
                .examType("IELTS")
                .maxAttempts(3)
                .status("PUBLISHED")
                .build();
        ObjectMapper objectMapper = new ObjectMapper();

        when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));
        when(definitionService.getDefinition()).thenReturn(definition);
        when(definitionService.getConfig(definition, "listening")).thenReturn(objectMapper.createObjectNode());
        when(definitionService.getConfig(definition, "reading")).thenReturn(objectMapper.createObjectNode());
        when(definitionService.getConfig(definition, "writing")).thenReturn(objectMapper.createObjectNode());
        when(definitionService.getConfig(definition, "speaking")).thenReturn(objectMapper.createObjectNode());
        when(definitionService.getConfig(definition, "toeic")).thenReturn(objectMapper.createObjectNode());
        when(attemptRepository.countByStudentAndTestCode(student, PlacementTestDefinitionService.TEST_CODE)).thenReturn(12L);
        when(attemptRepository.findTopByStudentAndTestCodeOrderBySubmittedAtDesc(
                student,
                PlacementTestDefinitionService.TEST_CODE
        )).thenReturn(Optional.empty());

        Map<String, Object> response = service.getTest(student.getEmail());

        assertThat(response.get("attemptCount")).isEqualTo(12L);
        assertThat(response.get("canRetake")).isEqualTo(true);
        assertThat(response).doesNotContainKey("maxAttempts");
    }

    @Test
    void getTestOnlyReturnsSectionsForEnabledExamTypes() {
        UserRepository userRepository = mock(UserRepository.class);
        PlacementTestAttemptRepository attemptRepository = mock(PlacementTestAttemptRepository.class);
        PlacementTestDefinitionService definitionService = mock(PlacementTestDefinitionService.class);
        PlacementTestServiceImpl service = newService(
                userRepository,
                attemptRepository,
                mock(AiEvaluationClient.class),
                mock(AssessmentAudioStorageService.class),
                definitionService
        );
        User student = User.builder().id(1L).email("learner@example.com").build();
        PlacementTestDefinition definition = PlacementTestDefinition.builder()
                .testCode(PlacementTestDefinitionService.TEST_CODE)
                .title("Placement test")
                .status("PUBLISHED")
                .ieltsEnabled(false)
                .toeicEnabled(true)
                .skillAssessmentEnabled(false)
                .build();
        ObjectNode toeicConfig = new ObjectMapper().createObjectNode();

        when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));
        when(definitionService.getDefinition()).thenReturn(definition);
        when(definitionService.getConfig(definition, "toeic")).thenReturn(toeicConfig);
        when(attemptRepository.findTopByStudentAndTestCodeOrderBySubmittedAtDesc(
                student,
                PlacementTestDefinitionService.TEST_CODE
        )).thenReturn(Optional.empty());

        Map<String, Object> response = service.getTest(student.getEmail());

        assertThat(response.get("availableExamTypes")).isEqualTo(List.of("TOEIC"));
        Map<?, ?> sections = (Map<?, ?>) response.get("sections");
        assertThat(sections).hasSize(1);
        assertThat(sections.containsKey("toeic")).isTrue();
        verify(definitionService, never()).getConfig(definition, "listening");
        verify(definitionService, never()).getConfig(definition, "reading");
        verify(definitionService, never()).getConfig(definition, "writing");
        verify(definitionService, never()).getConfig(definition, "speaking");
    }

    @Test
    void submitRejectsAnExamTypeThatHasBeenDisabled() {
        UserRepository userRepository = mock(UserRepository.class);
        PlacementTestAttemptRepository attemptRepository = mock(PlacementTestAttemptRepository.class);
        PlacementTestDefinitionService definitionService = mock(PlacementTestDefinitionService.class);
        PlacementTestServiceImpl service = newService(
                userRepository,
                attemptRepository,
                mock(AiEvaluationClient.class),
                mock(AssessmentAudioStorageService.class),
                definitionService
        );
        User student = User.builder().id(1L).email("learner@example.com").build();
        PlacementTestDefinition definition = PlacementTestDefinition.builder()
                .testCode(PlacementTestDefinitionService.TEST_CODE)
                .status("PUBLISHED")
                .ieltsEnabled(false)
                .toeicEnabled(true)
                .skillAssessmentEnabled(false)
                .build();
        PlacementTestSubmissionRequest request = new PlacementTestSubmissionRequest();
        request.setExamType("IELTS");

        when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));
        when(definitionService.getDefinition()).thenReturn(definition);

        assertThatThrownBy(() -> service.submit(request, student.getEmail()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Dạng bài đánh giá đã tạm dừng.");
        verify(attemptRepository, never()).save(any());
    }

    @Test
    void getTestRestoresDisabledToeicForItsValidInProgressSession() {
        UserRepository userRepository = mock(UserRepository.class);
        PlacementTestAttemptRepository attemptRepository = mock(PlacementTestAttemptRepository.class);
        PlacementTestDefinitionService definitionService = mock(PlacementTestDefinitionService.class);
        PlacementTestSessionToken sessionToken = mock(PlacementTestSessionToken.class);
        PlacementTestServiceImpl service = newService(
                userRepository,
                attemptRepository,
                mock(AiEvaluationClient.class),
                mock(AssessmentAudioStorageService.class),
                definitionService,
                sessionToken
        );
        User student = User.builder().id(1L).email("learner@example.com").build();
        PlacementTestDefinition definition = PlacementTestDefinition.builder()
                .testCode(PlacementTestDefinitionService.TEST_CODE)
                .title("Placement test")
                .status("PUBLISHED")
                .ieltsEnabled(true)
                .toeicEnabled(false)
                .skillAssessmentEnabled(false)
                .build();
        ObjectMapper objectMapper = new ObjectMapper();

        when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));
        when(definitionService.getDefinition()).thenReturn(definition);
        when(sessionToken.isValid("valid-session", student.getEmail(), "TOEIC")).thenReturn(true);
        when(definitionService.getConfig(definition, "listening")).thenReturn(objectMapper.createObjectNode());
        when(definitionService.getConfig(definition, "reading")).thenReturn(objectMapper.createObjectNode());
        when(definitionService.getConfig(definition, "writing")).thenReturn(objectMapper.createObjectNode());
        when(definitionService.getConfig(definition, "speaking")).thenReturn(objectMapper.createObjectNode());
        when(definitionService.getConfig(definition, "toeic")).thenReturn(objectMapper.createObjectNode());
        when(attemptRepository.findTopByStudentAndTestCodeOrderBySubmittedAtDesc(
                student,
                PlacementTestDefinitionService.TEST_CODE
        )).thenReturn(Optional.empty());

        Map<String, Object> response = service.getTest(student.getEmail(), "TOEIC", "valid-session");

        assertThat(response.get("availableExamTypes")).isEqualTo(List.of("IELTS"));
        assertThat(response.get("resumableExamType")).isEqualTo("TOEIC");
        assertThat(((Map<?, ?>) response.get("sections")).containsKey("toeic")).isTrue();
    }

    @Test
    void submitAllowsDisabledToeicForItsValidInProgressSession() {
        UserRepository userRepository = mock(UserRepository.class);
        PlacementTestAttemptRepository attemptRepository = mock(PlacementTestAttemptRepository.class);
        PlacementTestDefinitionService definitionService = mock(PlacementTestDefinitionService.class);
        PlacementTestSessionToken sessionToken = mock(PlacementTestSessionToken.class);
        PlacementTestServiceImpl service = newService(
                userRepository,
                attemptRepository,
                mock(AiEvaluationClient.class),
                mock(AssessmentAudioStorageService.class),
                definitionService,
                sessionToken
        );
        User student = User.builder().id(1L).email("learner@example.com").build();
        PlacementTestDefinition definition = PlacementTestDefinition.builder()
                .testCode(PlacementTestDefinitionService.TEST_CODE)
                .status("PUBLISHED")
                .ieltsEnabled(true)
                .toeicEnabled(false)
                .skillAssessmentEnabled(false)
                .build();
        ObjectNode toeicConfig = new ObjectMapper().createObjectNode();
        toeicConfig.set("answerKey", new ObjectMapper().createObjectNode());
        toeicConfig.set("listening", new ObjectMapper().createObjectNode());
        toeicConfig.set("reading", new ObjectMapper().createObjectNode());
        PlacementTestSubmissionRequest request = new PlacementTestSubmissionRequest();
        request.setExamType("TOEIC");
        request.setSessionToken("valid-session");
        request.setListeningAnswers(Map.of());
        request.setReadingAnswers(Map.of());
        request.setDeviceCheck(Map.of("completed", true));

        when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));
        when(definitionService.getDefinition()).thenReturn(definition);
        when(sessionToken.isValid("valid-session", student.getEmail(), "TOEIC")).thenReturn(true);
        when(definitionService.getConfig(definition, "toeic")).thenReturn(toeicConfig);
        when(attemptRepository.save(any())).thenAnswer(invocation -> {
            var attempt = invocation.getArgument(0, fu.sep490.g23.backend.entity.assessment.PlacementTestAttempt.class);
            attempt.setId(15L);
            return attempt;
        });

        PlacementTestAttemptResponse response = service.submit(request, student.getEmail());

        assertThat(response.getExamType()).isEqualTo("TOEIC");
        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        verify(attemptRepository).save(any());
    }

    @Test
    void submitSkillAssessmentScoresOnlySelectedSkillWithoutUpdatingPlacementBand() {
        UserRepository userRepository = mock(UserRepository.class);
        PlacementTestAttemptRepository attemptRepository = mock(PlacementTestAttemptRepository.class);
        AiEvaluationClient aiEvaluationClient = mock(AiEvaluationClient.class);
        AssessmentAudioStorageService audioStorageService = mock(AssessmentAudioStorageService.class);
        PlacementTestDefinitionService definitionService = mock(PlacementTestDefinitionService.class);
        PlacementTestServiceImpl service = newService(
                userRepository,
                attemptRepository,
                aiEvaluationClient,
                audioStorageService,
                definitionService
        );

        User student = User.builder().id(1L).email("learner@example.com").currentBand(5.5).build();
        PlacementTestDefinition definition = PlacementTestDefinition.builder()
                .testCode(PlacementTestDefinitionService.TEST_CODE)
                .title("Placement test")
                .examType("IELTS")
                .status("PUBLISHED")
                .build();
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode listeningConfig = objectMapper.createObjectNode();
        listeningConfig.putObject("answerKey").put("1", "A");

        PlacementTestSubmissionRequest request = new PlacementTestSubmissionRequest();
        request.setExamType("SKILL");
        request.setSelectedSkills(List.of(AssessmentSkill.LISTENING));
        request.setListeningAnswers(Map.of("1", "A"));
        request.setReadingAnswers(Map.of());
        request.setWritingAnswers(Map.of());
        request.setDeviceCheck(Map.of("completed", true));

        when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));
        when(definitionService.getDefinition()).thenReturn(definition);
        when(definitionService.getConfig(definition, "listening")).thenReturn(listeningConfig);
        when(attemptRepository.save(any())).thenAnswer(invocation -> {
            var attempt = invocation.getArgument(0, fu.sep490.g23.backend.entity.assessment.PlacementTestAttempt.class);
            attempt.setId(10L);
            return attempt;
        });

        PlacementTestAttemptResponse response = service.submit(request, student.getEmail());

        assertThat(response.getExamType()).isEqualTo("SKILL");
        assertThat(response.getSelectedSkills()).containsExactly(AssessmentSkill.LISTENING);
        assertThat(response.getListeningScore()).isNotNull();
        assertThat(response.getReadingScore()).isNull();
        assertThat(response.getWritingScore()).isNull();
        assertThat(response.getSpeakingScore()).isNull();
        assertThat(response.getOverallScore()).isEqualByComparingTo(response.getListeningScore());
        assertThat(student.getCurrentBand()).isEqualTo(5.5);
        verify(userRepository, never()).save(student);
    }
}
