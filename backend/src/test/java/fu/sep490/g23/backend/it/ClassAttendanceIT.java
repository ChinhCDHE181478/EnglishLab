package fu.sep490.g23.backend.it;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static fu.sep490.g23.backend.it.ItSupport.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Test – Manage Class Attendance
 * Excel sheet: IT_ATTEND | SRS: UC-23a View Class Attendance, UC-23b Record Class Attendance
 * Chạy: mvnw -Dtest=ClassAttendanceIT test
 */
@EnglishLabIT
public class ClassAttendanceIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("IT_ATTEND_01")
    void itAttend01() throws Exception {
        String teacherToken = login(mockMvc, TEACHER, PASSWORD);
        String learnerToken = login(mockMvc, LEARNER, PASSWORD);
        long classroomId = sharedClassroomId(mockMvc, teacherToken, learnerToken);
        long sessionId = firstSessionId(mockMvc, teacherToken, classroomId);
        long studentId = currentUserId(mockMvc, learnerToken);
        saveAttendance(teacherToken, sessionId, studentId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.studentId == " + studentId + ")].status").value("PRESENT"));
    }

    @Test
    @DisplayName("IT_ATTEND_02")
    void itAttend02() throws Exception {
        String teacherToken = login(mockMvc, TEACHER, PASSWORD);
        String learnerToken = login(mockMvc, LEARNER, PASSWORD);
        long classroomId = sharedClassroomId(mockMvc, teacherToken, learnerToken);
        long sessionId = firstSessionId(mockMvc, teacherToken, classroomId);
        long studentId = currentUserId(mockMvc, learnerToken);
        saveAttendance(teacherToken, sessionId, studentId).andExpect(status().isOk());
        mockMvc.perform(get("/api/teacher/classrooms/sessions/" + sessionId + "/attendance")
                        .header("Authorization", bearer(teacherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.studentId == " + studentId + ")].status").value("PRESENT"))
                .andExpect(jsonPath("$[?(@.studentId == " + studentId + ")].note").value("IT attendance"));
    }

    private org.springframework.test.web.servlet.ResultActions saveAttendance(
            String teacherToken, long sessionId, long studentId
    ) throws Exception {
        return mockMvc.perform(post("/api/teacher/classrooms/attendance")
                .header("Authorization", bearer(teacherToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"sessionId":%d,"records":[{"studentId":%d,"status":"PRESENT","note":"IT attendance"}]}
                        """.formatted(sessionId, studentId)));
    }
}
