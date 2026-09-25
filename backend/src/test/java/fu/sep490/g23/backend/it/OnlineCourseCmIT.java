package fu.sep490.g23.backend.it;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static fu.sep490.g23.backend.it.ItSupport.*;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Test – Manage Online Courses
 * Excel sheet: IT_ONLINE | SRS: UC-33a Create Online Course, UC-33b View Online Courses, UC-33c Update Online Course, UC-33d Deactivate Online Course
 * Chạy: mvnw -Dtest=OnlineCourseCmIT test
 */
@EnglishLabIT
public class OnlineCourseCmIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("IT_ONLINE_01")
    void itOnline01() throws Exception {
        String token = login(mockMvc, CM, PASSWORD);
        mockMvc.perform(get("/api/content-manager/online-courses").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_ONLINE_02")
    void itOnline02() throws Exception {
        String token = login(mockMvc, CM, PASSWORD);
        String title = "IT Online " + UUID.randomUUID().toString().substring(0, 8);
        MvcResult created = mockMvc.perform(post("/api/content-manager/online-courses")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(courseBody(title)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value(title))
                .andReturn();
        long courseId = json(created).path("id").asLong();
        mockMvc.perform(get("/api/content-manager/online-courses/" + courseId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(title));
        mockMvc.perform(get("/api/content-manager/online-courses/" + courseId + "/preview")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.course.title").value(title));
    }

    @Test
    @DisplayName("IT_ONLINE_03")
    void itOnline03() throws Exception {
        String token = login(mockMvc, CM, PASSWORD);
        MvcResult created = mockMvc.perform(post("/api/content-manager/online-courses")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(courseBody("IT Online before")))
                .andExpect(status().isCreated()).andReturn();
        long id = json(created).path("id").asLong();
        mockMvc.perform(put("/api/content-manager/online-courses/" + id)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(courseBody("IT Online updated")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("IT Online updated"));
        mockMvc.perform(get("/api/content-manager/online-courses/" + id)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("IT Online updated"));
    }

    @Test
    @DisplayName("IT_ONLINE_04")
    void itOnline04() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        mockMvc.perform(get("/api/content-manager/online-courses").header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("IT_ONLINE_05 - Public home returns public teachers and the latest five-star reviews")
    void itOnline05() throws Exception {
        String token = login(mockMvc, CM, PASSWORD);
        String title = "IT Home " + UUID.randomUUID().toString().substring(0, 8);
        long courseId = json(mockMvc.perform(post("/api/content-manager/online-courses")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(courseBody(title)))
                .andExpect(status().isCreated())
                .andReturn()).path("id").asLong();

        long teacherId = jdbcTemplate.queryForObject("select id from users where email=?", Long.class, TEACHER);
        long learnerId = jdbcTemplate.queryForObject("select id from users where email=?", Long.class, LEARNER);
        String teacherName = jdbcTemplate.queryForObject("select full_name from users where id=?", String.class, teacherId);
        String comment = "Trải nghiệm học tập rất hiệu quả " + UUID.randomUUID();
        jdbcTemplate.update("update users set teacher_public_profile=true where id=?", teacherId);
        jdbcTemplate.update("update online_courses set status='PUBLISHED' where id=?", courseId);
        jdbcTemplate.update("""
                insert into online_course_enrollments(
                    student_id, online_course_id, status, progress_percent, registered_at,
                    review_rating, review_comment, reviewed_at, created_at, updated_at
                ) values (?, ?, 'COMPLETED', 100, current_timestamp, 5, ?,
                    current_timestamp + interval '1 day', current_timestamp, current_timestamp)
                """, learnerId, courseId, comment);

        mockMvc.perform(get("/api/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teachers[*].name", hasItem(teacherName)))
                .andExpect(jsonPath("$.testimonials[0].comment").value(comment))
                .andExpect(jsonPath("$.testimonials[0].rating").value(5))
                .andExpect(jsonPath("$.testimonials[0].courseTitle").value(title));
    }

    private String courseBody(String title) {
        return """
                {
                  "title":"%s",
                  "shortDescription":"Integration-tested course",
                  "description":"Created by OnlineCourseCmIT",
                  "category":"IELTS",
                  "level":"BEGINNER",
                  "status":"DRAFT",
                  "targetScore":"6.5",
                  "targetBand":6.5,
                  "price":100000,
                  "totalLessons":0,
                  "totalHours":0,
                  "displayOrder":99,
                  "featured":false,
                  "modules":[]
                }
                """.formatted(title);
    }
}
