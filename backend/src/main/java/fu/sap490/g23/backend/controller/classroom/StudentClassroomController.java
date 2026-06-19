package fu.sap490.g23.backend.controller.classroom;

import fu.sap490.g23.backend.dto.request.classroom.*;
import fu.sap490.g23.backend.dto.response.classroom.*;
import fu.sap490.g23.backend.service.classroom.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/student/classrooms")
@RequiredArgsConstructor
public class StudentClassroomController {

    private final ClassroomOfferingService classroomOfferingService;
    private final ClassroomHomeworkService classroomHomeworkService;
    private final ClassroomGradebookService classroomGradebookService;
    private final ClassroomContentService classroomContentService;
    private final ClassroomAttendanceService classroomAttendanceService;

    @GetMapping({"/my-classrooms", "/my-classes"})
    public ResponseEntity<List<ClassroomOfferingResponse>> getMyClasses(Authentication authentication) {
        return ResponseEntity.ok(classroomOfferingService.getMyClasses(authentication.getName()));
    }

    @PostMapping("/{id}/register")
    public ResponseEntity<ClassroomEnrollmentResponse> registerForClass(
            @PathVariable Long id,
            @RequestBody(required = false) RegisterClassRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(classroomOfferingService.registerForClass(id, request, authentication.getName()));
    }

    @GetMapping("/{id}/registration/me")
    public ResponseEntity<ClassroomEnrollmentResponse> getMyRegistration(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(classroomOfferingService.getMyRegistration(id, authentication.getName()));
    }

    @GetMapping("/registrations/me")
    public ResponseEntity<List<ClassroomEnrollmentResponse>> getMyRegistrations(Authentication authentication) {
        return ResponseEntity.ok(classroomOfferingService.getMyRegistrations(authentication.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClassroomOfferingResponse> getClassroom(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(classroomOfferingService.getOffering(id, true));
    }

    @GetMapping("/{id}/sessions")
    public ResponseEntity<List<ClassroomSessionResponse>> getSessions(@PathVariable Long id) {
        return ResponseEntity.ok(classroomOfferingService.getSessions(id));
    }

    @GetMapping("/{id}/homework")
    public ResponseEntity<List<ClassroomHomeworkResponse>> getHomework(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(classroomHomeworkService.listForClass(id, authentication.getName()));
    }

    @PostMapping("/homework/{homeworkId}/submit")
    public ResponseEntity<ClassroomHomeworkSubmissionResponse> submitHomework(
            @PathVariable Long homeworkId,
            @RequestBody SubmitHomeworkRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(classroomHomeworkService.submit(homeworkId, request, authentication.getName()));
    }

    @GetMapping("/my-homework")
    public ResponseEntity<List<ClassroomHomeworkResponse>> getMyHomework(Authentication authentication) {
        return ResponseEntity.ok(classroomHomeworkService.listForLearner(authentication.getName()));
    }

    @GetMapping("/{id}/gradebook/me")
    public ResponseEntity<ClassroomGradebookResponse> getMyGradebook(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(classroomGradebookService.getMyGradebook(id, authentication.getName()));
    }

    @GetMapping("/{id}/materials")
    public ResponseEntity<List<ClassroomMaterialResponse>> getMaterials(@PathVariable Long id) {
        return ResponseEntity.ok(classroomContentService.getMaterials(id));
    }

    @GetMapping("/{id}/announcements")
    public ResponseEntity<List<ClassroomAnnouncementResponse>> getAnnouncements(@PathVariable Long id) {
        return ResponseEntity.ok(classroomContentService.getAnnouncements(id));
    }

    @GetMapping("/{id}/syllabus")
    public ResponseEntity<List<ClassroomSyllabusItemResponse>> getSyllabus(@PathVariable Long id) {
        return ResponseEntity.ok(classroomContentService.getSyllabus(id));
    }

    @GetMapping("/{id}/attendance/me")
    public ResponseEntity<List<ClassroomAttendanceResponse>> getMyAttendance(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(classroomAttendanceService.getByClassForStudent(id, authentication.getName()));
    }
}
