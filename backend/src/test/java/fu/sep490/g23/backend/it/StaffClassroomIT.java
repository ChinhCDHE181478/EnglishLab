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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Test – Manage Classrooms
 * Excel sheet: IT_CLASS | SRS: UC-36a Create Classroom, UC-36b View Classrooms, UC-36c Update Classroom
 * Chạy: mvnw -Dtest=StaffClassroomIT test
 */
@EnglishLabIT
public class StaffClassroomIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("IT_CLASS_01")
    void itClass01() throws Exception {
        String token = login(mockMvc, STAFF, PASSWORD);
        mockMvc.perform(get("/api/staff/classrooms").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_CLASS_02")
    void itClass02() throws Exception {
        String token = login(mockMvc, STAFF, PASSWORD);
        MvcResult progs = mockMvc.perform(get("/api/staff/classrooms/instructor-led-courses")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn();
        JsonNode arr = mapper().readTree(progs.getResponse().getContentAsString());
        assertFalse(arr.isEmpty(), "A published training program fixture is required");
        long pid = arr.get(0).path("id").asLong();
        LocalDate start = LocalDate.now().plusDays(21);
        LocalDate end = start.plusDays(28);
        String body = """
                {
                  "title":"IT Class Proposal %s",
                  "courseOfferingId":%d,
                  "capacity":20,
                  "plannedStartDate":"%s",
                  "endDate":"%s",
                  "weekdays":["MONDAY","WEDNESDAY"],
                  "sessionStartTime":"18:00:00",
                  "sessionEndTime":"20:00:00",
                  "note":"IT_CLASS_02"
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8), pid, start, end);
        mockMvc.perform(post("/api/staff/classroom-proposals")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_CLASS_03")
    void itClass03() throws Exception {
        String token = login(mockMvc, STAFF, PASSWORD);
        MvcResult list = mockMvc.perform(get("/api/staff/classrooms").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn();
        JsonNode items = mapper().readTree(list.getResponse().getContentAsString());
        if (!items.isArray() || items.isEmpty()) throw new AssertionError("A classroom fixture is required");
        long oid = 0;
        JsonNode detail = null;
        for (JsonNode o : items) {
            MvcResult d = mockMvc.perform(get("/api/staff/classrooms/" + o.path("id").asLong())
                            .header("Authorization", bearer(token)))
                    .andExpect(status().isOk()).andReturn();
            JsonNode candidate = mapper().readTree(d.getResponse().getContentAsString());
            if (candidate.path("primaryTeacherId").canConvertToLong()) {
                oid = o.path("id").asLong();
                detail = candidate;
                break;
            }
        }
        if (detail == null) throw new AssertionError("A classroom with a primary teacher is required");
        String updatedTitle = detail.path("title").asText("IT Class") + " - IT update";
        var body = mapper().createObjectNode();
        body.put("title", updatedTitle);
        body.put("deliveryMode", detail.path("deliveryMode").asText());
        body.put("capacity", detail.path("capacity").asInt(20));
        body.put("primaryTeacherId", detail.path("primaryTeacherId").asLong());
        body.put("shortDescription", "IT_CLASS_03 persisted update");
        copyIfPresent(detail, body, "classroomStatus", "classroomStatus");
        copyIfPresent(detail, body, "packageStatus", "packageStatus");
        copyIfPresent(detail, body, "instructorLedCourseId", "instructorLedCourseId");
        copyIfPresent(detail, body, "startDate", "startDate");
        copyIfPresent(detail, body, "endDate", "endDate");
        copyIfPresent(detail, body, "roomId", "roomId");
        copyIfPresent(detail, body, "offlineAddress", "offlineAddress");
        copyIfPresent(detail, body, "price", "price");
        copyIfPresent(detail, body, "salePrice", "salePrice");
        mockMvc.perform(put("/api/staff/classrooms/" + oid)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(updatedTitle));
        mockMvc.perform(get("/api/staff/classrooms/" + oid)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(updatedTitle));
    }

    @Test
    @DisplayName("IT_CLASS_04")
    void itClass04RejectsIncompletePrelaunchPlan() throws Exception {
        String token = login(mockMvc, STAFF, PASSWORD);
        JsonNode items = mapper().readTree(mockMvc.perform(get("/api/staff/classrooms")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());

        JsonNode detail = null;
        for (JsonNode item : items) {
            String classroomStatus = item.path("classroomStatus").asText();
            if (!"DRAFT".equals(classroomStatus) && !"UPCOMING".equals(classroomStatus)) continue;
            JsonNode candidate = mapper().readTree(mockMvc.perform(get("/api/staff/classrooms/" + item.path("id").asLong())
                            .header("Authorization", bearer(token)))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
            if (candidate.path("primaryTeacherId").canConvertToLong()
                    && candidate.path("instructorLedCourseId").canConvertToLong()
                    && candidate.path("sessions").isArray()
                    && !candidate.path("sessions").isEmpty()) {
                detail = candidate;
                break;
            }
        }
        Assumptions.assumeTrue(detail != null, "A planned classroom fixture is required");

        var classroom = mapper().createObjectNode();
        copyIfPresent(detail, classroom, "title", "title");
        copyIfPresent(detail, classroom, "deliveryMode", "deliveryMode");
        copyIfPresent(detail, classroom, "classroomStatus", "classroomStatus");
        copyIfPresent(detail, classroom, "instructorLedCourseId", "instructorLedCourseId");
        copyIfPresent(detail, classroom, "capacity", "capacity");
        copyIfPresent(detail, classroom, "startDate", "startDate");
        copyIfPresent(detail, classroom, "endDate", "endDate");
        copyIfPresent(detail, classroom, "primaryTeacherId", "primaryTeacherId");
        copyIfPresent(detail, classroom, "regularRoomId", "roomId");
        copyIfPresent(detail, classroom, "tuitionFeeVnd", "price");
        copyIfPresent(detail, classroom, "offlineAddress", "offlineAddress");

        var schedules = mapper().createArrayNode();
        long omittedLessonId = detail.path("sessions").valueStream()
                .map(session -> session.path("courseLessonId"))
                .filter(JsonNode::canConvertToLong)
                .mapToLong(JsonNode::asLong)
                .findFirst()
                .orElseThrow(() -> new AssertionError("A scheduled course lesson is required"));
        for (JsonNode session : detail.path("sessions")) {
            var schedule = mapper().createObjectNode();
            for (String field : new String[]{
                    "id", "sessionDate", "startTime", "endTime", "teacherId", "status",
                    "deliveryModeOverride", "roomId", "sessionContent", "note"
            }) {
                copyIfPresent(session, schedule, field, field);
            }
            if (session.path("courseLessonId").asLong(-1) == omittedLessonId) {
                schedule.put("sessionContent", "Buổi đặc biệt kiểm thử");
            } else {
                copyIfPresent(session, schedule, "courseLessonId", "courseLessonId");
            }
            schedule.put("status", "SCHEDULED");
            schedules.add(schedule);
        }
        var body = mapper().createObjectNode();
        body.set("classroom", classroom);
        body.set("schedules", schedules);

        mockMvc.perform(put("/api/staff/classrooms/" + detail.path("id").asLong() + "/prelaunch-plan")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Lịch học chưa phân bổ đủ số buổi cho toàn bộ bài học của khóa học."));
    }

    private void copyIfPresent(JsonNode source, com.fasterxml.jackson.databind.node.ObjectNode target,
                               String sourceField, String targetField) {
        JsonNode value = source.get(sourceField);
        if (value != null && !value.isNull() && !value.isMissingNode()) target.set(targetField, value);
    }
}
