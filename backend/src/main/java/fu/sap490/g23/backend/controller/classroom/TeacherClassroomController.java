package fu.sap490.g23.backend.controller.classroom;

import fu.sap490.g23.backend.dto.request.classroom.*;
import fu.sap490.g23.backend.dto.response.assessment.AssessmentRubricResponse;
import fu.sap490.g23.backend.dto.response.classroom.*;
import fu.sap490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sap490.g23.backend.repository.classroom.ClassroomSessionRepository;
import fu.sap490.g23.backend.service.classroom.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.LocalDate;
import java.time.LocalTime;
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
    private final HomeworkAttachmentStorageService homeworkAttachmentStorageService;
    private final ClassroomScheduleAvailabilityService scheduleAvailabilityService;
    private final ClassroomSessionRepository sessionRepository;
    private final ClassroomHomeworkGradingCatalogService homeworkGradingCatalogService;

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
    public ResponseEntity<ClassroomSessionResponse> openVirtualSession(
            @PathVariable Long sessionId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(classroomOfferingService.openVirtualSession(sessionId, authentication.getName()));
    }

    @PostMapping("/sessions/{sessionId}/close")
    public ResponseEntity<ClassroomSessionResponse> closeVirtualSession(
            @PathVariable Long sessionId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(classroomOfferingService.closeVirtualSession(sessionId, authentication.getName()));
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

    @PostMapping(value = "/homework/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<HomeworkAttachmentUploadResponse> uploadHomeworkAttachment(
            @RequestPart("file") MultipartFile file,
            Authentication authentication
    ) {
        String publicUrlBase = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/classroom-homework/attachments")
                .toUriString();
        return ResponseEntity.ok(homeworkAttachmentStorageService.store(file, publicUrlBase));
    }

    @GetMapping("/homework/rubrics")
    public ResponseEntity<List<AssessmentRubricResponse>> listHomeworkRubrics(
            @RequestParam(required = false) AssessmentSkill skill
    ) {
        if (skill != null) {
            return ResponseEntity.ok(homeworkGradingCatalogService.listRubricsBySkill(skill));
        }
        return ResponseEntity.ok(homeworkGradingCatalogService.listAllHomeworkRubrics());
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

    @GetMapping("/homework/{homeworkId}/submissions")
    public ResponseEntity<List<ClassroomHomeworkSubmissionResponse>> getHomeworkSubmissions(
            @PathVariable Long homeworkId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(homeworkService.listSubmissions(homeworkId, authentication.getName()));
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

    @PostMapping("/{id}/materials")
    public ResponseEntity<ClassroomMaterialResponse> createMaterial(
            @PathVariable Long id,
            @Valid @RequestBody CreateMaterialRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(contentService.createMaterial(id, request, authentication.getName()));
    }

    @DeleteMapping("/materials/{materialId}")
    public ResponseEntity<Void> deleteMaterial(@PathVariable Long materialId) {
        contentService.deleteMaterial(materialId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/announcements")
    public ResponseEntity<List<ClassroomAnnouncementResponse>> getAnnouncements(@PathVariable Long id) {
        return ResponseEntity.ok(contentService.getAnnouncements(id));
    }

    @PostMapping("/requests/check-conflict")
    public ResponseEntity<ConflictCheckResultResponse> checkChangeConflict(
            @RequestBody CreateChangeRequestRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(changeRequestService.checkConflict(request, authentication.getName()));
    }

    @GetMapping("/sessions/{sessionId}/available-rooms")
    public ResponseEntity<List<AvailableRoomOptionResponse>> getAvailableRooms(
            @PathVariable Long sessionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate sessionDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime
    ) {
        var session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy buổi học."));
        LocalDate resolvedDate = sessionDate != null ? sessionDate : session.getSessionDate();
        LocalTime resolvedStart = startTime != null ? startTime : session.getStartTime();
        LocalTime resolvedEnd = endTime != null ? endTime : session.getEndTime();
        return ResponseEntity.ok(scheduleAvailabilityService.listAvailableRooms(
                resolvedDate, resolvedStart, resolvedEnd, sessionId
        ));
    }

    @GetMapping("/sessions/{sessionId}/available-teachers")
    public ResponseEntity<List<AvailableTeacherOptionResponse>> getAvailableTeachers(
            @PathVariable Long sessionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate sessionDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime
    ) {
        var session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy buổi học."));
        LocalDate resolvedDate = sessionDate != null ? sessionDate : session.getSessionDate();
        LocalTime resolvedStart = startTime != null ? startTime : session.getStartTime();
        LocalTime resolvedEnd = endTime != null ? endTime : session.getEndTime();
        return ResponseEntity.ok(scheduleAvailabilityService.listAvailableTeachers(
                resolvedDate, resolvedStart, resolvedEnd, sessionId
        ));
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
