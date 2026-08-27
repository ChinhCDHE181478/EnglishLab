package fu.sep490.g23.backend.it;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

import static fu.sep490.g23.backend.it.ItSupport.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tìm lớp học dùng chung cho các case cần cả giáo viên demo lẫn học viên demo
 * (giao bài, chấm bài, nộp bài, tài liệu lớp, bài luyện tập).
 * Không có @Test — không chạy file này.
 */
public final class ItClassroomFixture {

    private ItClassroomFixture() {
    }

    /** Lớp mà TEACHER demo được phân công và LEARNER demo đang học. */
    public static long sharedClassroomId(MockMvc mockMvc, String teacherToken) throws Exception {
        String learnerToken = login(mockMvc, LEARNER, PASSWORD);
        long learnerId = currentUserId(mockMvc, learnerToken);

        MvcResult assigned = mockMvc.perform(get("/api/teacher/classrooms/assigned")
                        .header("Authorization", bearer(teacherToken)))
                .andExpect(status().isOk())
                .andReturn();

        for (JsonNode classroom : items(json(assigned))) {
            long classroomId = classroom.path("id").asLong();
            MvcResult detail = mockMvc.perform(get("/api/teacher/classrooms/" + classroomId)
                            .header("Authorization", bearer(teacherToken)))
                    .andExpect(status().isOk())
                    .andReturn();
            for (JsonNode enrollment : json(detail).path("enrollments")) {
                if (enrollment.path("studentId").asLong() == learnerId) {
                    return classroomId;
                }
            }
        }
        throw new IllegalStateException(
                "Dữ liệu demo phải có ít nhất một lớp mà giáo viên demo dạy và học viên demo đang học");
    }

    /**
     * Lớp đủ điều kiện nhận học viên mới: đã công bố, sắp khai giảng, còn chỗ.
     * Nếu dữ liệu demo chưa có lớp nào như vậy thì đẩy ngày khai giảng của một lớp sắp mở về tương lai,
     * nhờ vậy chạy lại nhiều lần vẫn có lớp hợp lệ để xếp.
     */
    public static long assignableClassroomId(MockMvc mockMvc, String staffToken) throws Exception {
        for (JsonNode offering : staffClassrooms(mockMvc, staffToken)) {
            if (isAssignable(offering)) {
                return offering.path("id").asLong();
            }
        }

        JsonNode detail = editableClassroom(mockMvc, staffToken, "UPCOMING");
        long classroomId = detail.path("id").asLong();
        ObjectNode payload = updatePayload(mockMvc, staffToken, detail);
        payload.put("classroomStatus", "UPCOMING");
        payload.put("packageStatus", "PUBLISHED");
        payload.put("startDate", LocalDate.now().plusDays(30).toString());
        payload.put("endDate", LocalDate.now().plusDays(90).toString());
        mockMvc.perform(put("/api/staff/classrooms/" + classroomId)
                        .header("Authorization", bearer(staffToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload.toString()))
                .andExpect(status().isOk());
        return classroomId;
    }

    /** Lớp được phép sửa: chưa bắt đầu hoặc chưa kết thúc nên không vướng luật khóa chương trình đào tạo. */
    public static JsonNode updatableClassroom(MockMvc mockMvc, String staffToken) throws Exception {
        return editableClassroom(mockMvc, staffToken, "UPCOMING", "DRAFT");
    }

    /**
     * Lớp ở một trong các trạng thái cho phép sửa và đang gắn giáo trình/chương trình đã duyệt —
     * điều kiện bắt buộc để API cập nhật lớp chấp nhận dữ liệu gửi lên.
     */
    private static JsonNode editableClassroom(MockMvc mockMvc, String staffToken, String... statuses) throws Exception {
        JsonNode offerings = staffClassrooms(mockMvc, staffToken);
        for (String status : statuses) {
            for (JsonNode offering : offerings) {
                if (!status.equals(offering.path("classroomStatus").asText())) {
                    continue;
                }
                JsonNode detail = staffClassroom(mockMvc, staffToken, offering.path("id").asLong());
                if (isPublished(detail, "instructorLedCourseStatus")) {
                    return detail;
                }
            }
        }
        throw new IllegalStateException(
                "Dữ liệu demo phải có ít nhất một lớp chưa khai giảng, gắn giáo trình đã duyệt để thử cập nhật");
    }

    private static boolean isPublished(JsonNode detail, String statusField) {
        String status = detail.path(statusField).asText("");
        return status.isBlank() || "PUBLISHED".equalsIgnoreCase(status);
    }

    /**
     * Dựng lại nguyên trạng thông tin lớp từ response chi tiết.
     * API cập nhật lớp ghi đè toàn bộ, nên phải gửi lại đủ trường để không xóa mất dữ liệu demo;
     * lớp sắp mở hoặc đang học còn bắt buộc có ngày học và giáo viên chính nên được bù nếu đang trống.
     */
    public static ObjectNode updatePayload(MockMvc mockMvc, String staffToken, JsonNode detail) throws Exception {
        ObjectNode payload = basePayload(detail);
        String status = payload.path("classroomStatus").asText("");
        if (!"UPCOMING".equals(status) && !"ACTIVE".equals(status)) {
            return payload;
        }
        if (payload.path("startDate").isNull() || payload.path("endDate").isNull()) {
            JsonNode sessions = detail.path("sessions");
            LocalDate start = sessionDate(sessions, 0, LocalDate.now().plusDays(30));
            LocalDate end = sessionDate(sessions, sessions.size() - 1, start.plusDays(60));
            payload.put("startDate", start.toString());
            payload.put("endDate", end.isBefore(start) ? start.plusDays(60).toString() : end.toString());
        }
        if (payload.path("primaryTeacherId").isNull()) {
            payload.put("primaryTeacherId", firstTeacherId(mockMvc, staffToken));
        }
        if ("OFFLINE".equals(payload.path("deliveryMode").asText())
                && payload.path("roomId").isNull()
                && payload.path("offlineAddress").isNull()) {
            payload.put("offlineAddress", "EnglishLab Hà Nội");
        }
        return payload;
    }

    private static ObjectNode basePayload(JsonNode detail) {
        ObjectNode payload = mapper().createObjectNode();
        copyText(detail, payload, "title");
        copyText(detail, payload, "shortDescription");
        copyText(detail, payload, "description");
        copyText(detail, payload, "deliveryMode");
        copyText(detail, payload, "classroomStatus");
        copyText(detail, payload, "packageStatus");
        copyText(detail, payload, "entryLevel");
        copyText(detail, payload, "targetOutcome");
        copyText(detail, payload, "offlineAddress");
        copyText(detail, payload, "locationNote");
        copyText(detail, payload, "defaultLarkMeetingUrl");
        copyText(detail, payload, "recordingUrl");
        copyText(detail, payload, "syllabusSummary");
        copyText(detail, payload, "thumbnailUrl");
        copyText(detail, payload, "duration");
        copyText(detail, payload, "studyMode");
        copyText(detail, payload, "targetScore");
        copyText(detail, payload, "startDate");
        copyText(detail, payload, "endDate");
        copyNumber(detail, payload, "instructorLedCourseId", "instructorLedCourseId");
        copyNumber(detail, payload, "primaryTeacherId", "primaryTeacherId");
        copyNumber(detail, payload, "roomId", "roomId");
        copyNumber(detail, payload, "capacity", "capacity");
        copyNumber(detail, payload, "displayOrder", "displayOrder");
        copyNumber(detail, payload, "price", "price");
        copyNumber(detail, payload, "salePrice", "salePrice");
        payload.put("recordingVisible", detail.path("recordingVisible").asBoolean(false));
        payload.put("featured", detail.path("featured").asBoolean(false));
        return payload;
    }

    private static LocalDate sessionDate(JsonNode sessions, int index, LocalDate fallback) {
        if (!sessions.isArray() || index < 0 || index >= sessions.size()) {
            return fallback;
        }
        String date = sessions.get(index).path("sessionDate").asText("");
        return date.isBlank() ? fallback : LocalDate.parse(date);
    }

    private static boolean isAssignable(JsonNode offering) {
        String startDate = offering.path("startDate").asText("");
        boolean startsLater = !startDate.isBlank() && LocalDate.parse(startDate).isAfter(LocalDate.now());
        boolean hasSeat = offering.path("enrolledCount").asInt() < offering.path("capacity").asInt(0);
        return "UPCOMING".equals(offering.path("classroomStatus").asText())
                && "PUBLISHED".equals(offering.path("packageStatus").asText())
                && startsLater
                && hasSeat;
    }

    private static JsonNode staffClassrooms(MockMvc mockMvc, String staffToken) throws Exception {
        return items(json(mockMvc.perform(get("/api/staff/classrooms")
                        .header("Authorization", bearer(staffToken)))
                .andExpect(status().isOk())
                .andReturn()));
    }

    private static JsonNode staffClassroom(MockMvc mockMvc, String staffToken, long classroomId) throws Exception {
        return json(mockMvc.perform(get("/api/staff/classrooms/" + classroomId)
                        .header("Authorization", bearer(staffToken)))
                .andExpect(status().isOk())
                .andReturn());
    }

    private static long firstTeacherId(MockMvc mockMvc, String staffToken) throws Exception {
        JsonNode teachers = items(json(mockMvc.perform(get("/api/staff/classrooms/teachers")
                        .header("Authorization", bearer(staffToken)))
                .andExpect(status().isOk())
                .andReturn()));
        if (teachers.isEmpty()) {
            throw new IllegalStateException("Dữ liệu demo phải có ít nhất một giáo viên để phân công lớp");
        }
        return teachers.get(0).path("id").asLong();
    }

    private static void copyText(JsonNode detail, ObjectNode payload, String field) {
        JsonNode value = detail.path(field);
        if (value.isMissingNode() || value.isNull() || value.asText("").isBlank()) {
            payload.putNull(field);
        } else {
            payload.put(field, value.asText());
        }
    }

    private static void copyNumber(JsonNode detail, ObjectNode payload, String sourceField, String targetField) {
        JsonNode value = detail.path(sourceField);
        if (value.isNumber()) {
            payload.set(targetField, value);
        } else {
            payload.putNull(targetField);
        }
    }
}
