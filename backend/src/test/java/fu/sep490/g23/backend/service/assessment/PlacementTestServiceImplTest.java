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
import fu.sep490.g23.backend.service.ai.AiEvaluationClient;
import fu.sep490.g23.backend.service.assessment.impl.PlacementTestServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlacementTestServiceImplTest {

    @Test
    void getTestAlwaysAllowsRetakeRegardlessOfPreviousAttemptCount() {
        UserRepository userRepository = mock(UserRepository.class);
        PlacementTestAttemptRepository attemptRepository = mock(PlacementTestAttemptRepository.class);
        AiEvaluationClient aiEvaluationClient = mock(AiEvaluationClient.class);
        AssessmentAudioStorageService audioStorageService = mock(AssessmentAudioStorageService.class);
        PlacementTestDefinitionService definitionService = mock(PlacementTestDefinitionService.class);
        PlacementTestServiceImpl service = new PlacementTestServiceImpl(
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
                .active(true)
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
    void submitSkillAssessmentScoresOnlySelectedSkillWithoutUpdatingPlacementBand() {
        UserRepository userRepository = mock(UserRepository.class);
        PlacementTestAttemptRepository attemptRepository = mock(PlacementTestAttemptRepository.class);
        AiEvaluationClient aiEvaluationClient = mock(AiEvaluationClient.class);
        AssessmentAudioStorageService audioStorageService = mock(AssessmentAudioStorageService.class);
        PlacementTestDefinitionService definitionService = mock(PlacementTestDefinitionService.class);
        PlacementTestServiceImpl service = new PlacementTestServiceImpl(
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
                .active(true)
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
