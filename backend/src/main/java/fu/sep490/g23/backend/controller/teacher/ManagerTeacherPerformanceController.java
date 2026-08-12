package fu.sep490.g23.backend.controller.teacher;

import fu.sep490.g23.backend.dto.response.teacher.ManagerTeacherFeedbackDetailResponse;
import fu.sep490.g23.backend.dto.response.teacher.TeacherFeedbackAggregateResponse;
import fu.sep490.g23.backend.service.teacher.TeacherFeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/manager/teacher-performance")
@RequiredArgsConstructor
public class ManagerTeacherPerformanceController {
    private final TeacherFeedbackService teacherFeedbackService;

    @GetMapping
    public ResponseEntity<List<TeacherFeedbackAggregateResponse>> list() {
        return ResponseEntity.ok(teacherFeedbackService.getManagerDashboard());
    }

    @GetMapping("/{teacherId}")
    public ResponseEntity<ManagerTeacherFeedbackDetailResponse> detail(@PathVariable Long teacherId) {
        return ResponseEntity.ok(teacherFeedbackService.getManagerDetail(teacherId));
    }
}
