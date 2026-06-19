package fu.sap490.g23.backend.controller.classroom;

import fu.sap490.g23.backend.dto.request.classroom.*;
import fu.sap490.g23.backend.dto.response.classroom.*;
import fu.sap490.g23.backend.service.classroom.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teacher/classrooms")
@RequiredArgsConstructor
public class TeacherClassroomController {

    private final ClassroomOfferingService classroomOfferingService;
    private final ClassroomChangeRequestService changeRequestService;
    private final ClassroomAttendanceService attendanceService;
    private final ClassroomHomeworkService homeworkService;
    private final ClassroomGradebookService gradebookService;
    private final ClassroomContentService contentService;

    @GetMapping("/assigned")
    public ResponseEntity<List<ClassroomOfferingResponse>> getAssignedClasses(Authentication authentication) {
        return ResponseEntity.ok(classroomOfferingService.getAssignedClasses(authentication.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClassroomOfferingResponse> getClassroom(@PathVariable Long id) {
        return ResponseEntity.ok(classroomOfferingService.getOffering(id, true));
    }

    @GetMapping("/{id}/sessions")
    public ResponseEntity<List<ClassroomSessionResponse>> getSessions(@PathVariable Long id) {
        return ResponseEntity.ok(classroomOfferingService.getSessions(id));
    }

    @PostMapping("/{id}/sessions")
    public ResponseEntity<ClassroomSessionResponse> createSession(
            @PathVariable Long id,
            @Valid @RequestBody CreateClassroomSessionRequest request
    ) {
        return ResponseEntity.ok(classroomOfferingService.createSession(id, request));
    }

    @PutMapping("/sessions/{sessionId}")
    public ResponseEntity<ClassroomSessionResponse> updateSession(
            @PathVariable Long sessionId,
            @Valid @RequestBody CreateClassroomSessionRequest request
    ) {
        return ResponseEntity.ok(classroomOfferingService.updateSession(sessionId, request));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> deleteSession(@PathVariable Long sessionId) {
        classroomOfferingService.deleteSession(sessionId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/sessions/{sessionId}/open")
    public ResponseEntity<ClassroomSessionResponse> openVirtualSession(@PathVariable Long sessionId) {
        return ResponseEntity.ok(classroomOfferingService.openVirtualSession(sessionId));
    }

    @PostMapping("/sessions/{sessionId}/close")
    public ResponseEntity<ClassroomSessionResponse> closeVirtualSession(@PathVariable Long sessionId) {
        return ResponseEntity.ok(classroomOfferingService.closeVirtualSession(sessionId));
    }

    @PatchMapping("/sessions/{sessionId}/lark-link")
    public ResponseEntity<ClassroomSessionResponse> updateLarkLink(
            @PathVariable Long sessionId,
            @Valid @RequestBody UpdateLarkLinkRequest request
    ) {
        return ResponseEntity.ok(classroomOfferingService.updateSessionLarkLink(sessionId, request));
    }

    @GetMapping("/sessions/{sessionId}/attendance")
    public ResponseEntity<List<ClassroomAttendanceResponse>> getSessionAttendance(@PathVariable Long sessionId) {
        return ResponseEntity.ok(attendanceService.getBySession(sessionId));
    }

    @PostMapping("/attendance")
    public ResponseEntity<List<ClassroomAttendanceResponse>> saveAttendance(
            @Valid @RequestBody SaveAttendanceRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(attendanceService.saveBulk(request, authentication.getName()));
    }

    @GetMapping("/{id}/homework")
    public ResponseEntity<List<ClassroomHomeworkResponse>> getHomework(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(homeworkService.listForClass(id, authentication.getName()));
    }

    @PostMapping("/{id}/homework")
    public ResponseEntity<ClassroomHomeworkResponse> createHomework(
            @PathVariable Long id,
            @Valid @RequestBody CreateHomeworkRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(homeworkService.create(id, request, authentication.getName()));
    }

    @PutMapping("/homework/{homeworkId}")
    public ResponseEntity<ClassroomHomeworkResponse> updateHomework(
            @PathVariable Long homeworkId,
            @Valid @RequestBody CreateHomeworkRequest request
    ) {
        return ResponseEntity.ok(homeworkService.update(homeworkId, request));
    }

    @DeleteMapping("/homework/{homeworkId}")
    public ResponseEntity<Void> deleteHomework(@PathVariable Long homeworkId) {
        homeworkService.delete(homeworkId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/homework/{homeworkId}/students/{studentId}/grade")
    public ResponseEntity<ClassroomHomeworkSubmissionResponse> gradeHomework(
            @PathVariable Long homeworkId,
            @PathVariable Long studentId,
            @RequestBody GradeHomeworkRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(homeworkService.grade(homeworkId, studentId, request, authentication.getName()));
    }

    @GetMapping("/{id}/gradebook")
    public ResponseEntity<List<ClassroomGradebookResponse>> getGradebook(@PathVariable Long id) {
        return ResponseEntity.ok(gradebookService.getClassGradebook(id));
    }

    @PutMapping("/{id}/gradebook")
    public ResponseEntity<ClassroomGradebookResponse> updateGradebookEntry(
            @PathVariable Long id,
            @Valid @RequestBody UpdateGradebookRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(gradebookService.updateEntry(id, request, authentication.getName()));
    }

    @PostMapping("/{id}/gradebook/publish")
    public ResponseEntity<List<ClassroomGradebookResponse>> publishGradebook(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(gradebookService.publishGradebook(id, authentication.getName()));
    }

    @GetMapping("/{id}/materials")
    public ResponseEntity<List<ClassroomMaterialResponse>> getMaterials(@PathVariable Long id) {
        return ResponseEntity.ok(contentService.getMaterials(id));
    }

    @GetMapping("/{id}/announcements")
    public ResponseEntity<List<ClassroomAnnouncementResponse>> getAnnouncements(@PathVariable Long id) {
        return ResponseEntity.ok(contentService.getAnnouncements(id));
    }

    @PostMapping("/requests")
    public ResponseEntity<ClassroomChangeRequestResponse> createChangeRequest(
            @Valid @RequestBody CreateChangeRequestRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(changeRequestService.create(request, authentication.getName()));
    }

    @GetMapping("/requests/mine")
    public ResponseEntity<List<ClassroomChangeRequestResponse>> listMyRequests(Authentication authentication) {
        return ResponseEntity.ok(changeRequestService.listMine(authentication.getName()));
    }
}
