package fu.sep490.g23.backend.controller.assessment;

import fu.sep490.g23.backend.dto.response.assessment.CourseAssessmentResponse;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sep490.g23.backend.service.assessment.AiAssessmentService;
import fu.sep490.g23.backend.service.assessment.AssessmentAudioStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StudentAssessmentControllerApiTest {

    @Test
    void learnerAssessmentApi_omitsObjectiveAnswerKeyFromJson() throws Exception {
        AiAssessmentService assessmentService = mock(AiAssessmentService.class);
        AssessmentAudioStorageService audioStorageService = mock(AssessmentAudioStorageService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new StudentAssessmentController(assessmentService, audioStorageService)
        ).build();
        CourseAssessmentResponse assessment = CourseAssessmentResponse.builder()
                .id(1L)
                .courseId(7L)
                .title("Listening test")
                .skill(AssessmentSkill.LISTENING)
                .objectiveAnswerKey(null)
                .build();
        when(assessmentService.getCourseAssessments(7L, "learner@example.com")).thenReturn(List.of(assessment));

        mockMvc.perform(get("/api/student/courses/7/assessments")
                        .principal(new UsernamePasswordAuthenticationToken("learner@example.com", "N/A")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].objectiveAnswerKey").doesNotExist());
    }
}
