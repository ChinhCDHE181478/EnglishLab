package fu.sep490.g23.backend.controller.curriculum;

import fu.sep490.g23.backend.config.SecurityConfig;
import fu.sep490.g23.backend.dto.response.curriculum.CurriculumSessionPlanResponse;
import fu.sep490.g23.backend.security.CustomUserDetailsService;
import fu.sep490.g23.backend.security.JwtAuthenticationFilter;
import fu.sep490.g23.backend.service.curriculum.CurriculumProgramService;
import fu.sep490.g23.backend.service.admin.ApiMonitoringService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ContentManagerCurriculumController.class)
@Import(SecurityConfig.class)
class ContentManagerCurriculumControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CurriculumProgramService curriculumProgramService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private ApiMonitoringService apiMonitoringService;

    @BeforeEach
    void passRequestsThroughJwtFilter() throws Exception {
        doAnswer(invocation -> {
            ServletRequest request = invocation.getArgument(0);
            ServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
    }

    @Test
    void staffCannotCreateCurriculumSessionPlan() throws Exception {
        mockMvc.perform(post("/api/content-manager/curriculum-units/10/session-plans")
                        .with(user("staff@englishlab.vn").roles("STAFF"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(curriculumProgramService);
    }

    @Test
    void contentManagerCanCreateCurriculumSessionPlan() throws Exception {
        when(curriculumProgramService.createSessionPlan(any(), any()))
                .thenReturn(CurriculumSessionPlanResponse.builder()
                        .id(101L)
                        .unitId(10L)
                        .programId(1L)
                        .sessionNumber(1)
                        .displayOrder(0)
                        .title("Reading Overview + Skimming")
                        .build());

        mockMvc.perform(post("/api/content-manager/curriculum-units/10/session-plans")
                        .with(user("content@englishlab.vn").roles("CONTENT_MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(101L))
                .andExpect(jsonPath("$.sessionNumber").value(1));

        verify(curriculumProgramService).createSessionPlan(any(), any());
    }

    private String validPayload() {
        return """
                {
                  "sessionNumber": 1,
                  "displayOrder": 0,
                  "title": "Reading Overview + Skimming",
                  "description": "Tổng quan kỹ thuật đọc lướt",
                  "learningObjectives": "Nắm format và áp dụng skimming"
                }
                """;
    }
}
