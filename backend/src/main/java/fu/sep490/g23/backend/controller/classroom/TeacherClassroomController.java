package fu.sep490.g23.backend.controller.classroom;
import fu.sep490.g23.backend.dto.response.classroom.AvailableRoomOptionResponse;
import fu.sep490.g23.backend.dto.response.classroom.AvailableTeacherOptionResponse;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomChangeRequestResponse;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomHomeworkResponse;
import fu.sep490.g23.backend.dto.request.classroom.SaveAttendanceRequest;
import fu.sep490.g23.backend.service.classroom.ClassroomHomeworkService;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomOfferingResponse;
import fu.sep490.g23.backend.service.classroom.ClassroomScheduleAvailabilityService;
import fu.sep490.g23.backend.service.classroom.ClassroomGradebookService;
import fu.sep490.g23.backend.dto.request.classroom.CreateChangeRequestRequest;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomAttendanceResponse;
import fu.sep490.g23.backend.service.classroom.ClassroomHomeworkGradingCatalogService;
import fu.sep490.g23.backend.service.classroom.ClassroomOfferingService;
import fu.sep490.g23.backend.service.classroom.ClassroomChangeRequestService;
import fu.sep490.g23.backend.dto.request.classroom.CreateClassroomSessionRequest;
import fu.sep490.g23.backend.service.classroom.HomeworkAttachmentStorageService;
import fu.sep490.g23.backend.dto.response.classroom.HomeworkAttachmentUploadResponse;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomAnnouncementResponse;
import fu.sep490.g23.backend.dto.request.classroom.CreateAnnouncementRequest;
import fu.sep490.g23.backend.dto.request.classroom.CreateMaterialRequest;
import fu.sep490.g23.backend.service.classroom.TeacherClassroomAuthorizationService;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomHomeworkSubmissionResponse;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomMaterialResponse;
import fu.sep490.g23.backend.dto.request.classroom.SaveHomeworkAnnotationsRequest;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomSessionResponse;
import fu.sep490.g23.backend.service.classroom.ClassroomContentService;
import fu.sep490.g23.backend.dto.response.classroom.HomeworkAiAssessmentOptionResponse;
import fu.sep490.g23.backend.dto.request.classroom.CreateHomeworkRequest;
import fu.sep490.g23.backend.dto.request.classroom.UpdateGradebookRequest;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomGradebookResponse;
import fu.sep490.g23.backend.service.classroom.ClassroomAttendanceService;
import fu.sep490.g23.backend.dto.response.classroom.ConflictCheckResultResponse;
import fu.sep490.g23.backend.dto.request.classroom.GradeHomeworkRequest;

import fu.sep490.g23.backend.dto.response.assessment.AssessmentRubricResponse;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sep490.g23.backend.repository.classroom.ClassScheduleRepository;
import fu.sep490.g23.backend.service.classroom.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher/classrooms")
@RequiredArgsConstructor
public class TeacherClassroomController {

    private final ClassroomOfferingService classSectionService;
    private final ClassroomChangeRequestService changeRequestService;
    private final ClassroomAttendanceService attendanceService;
    private final ClassroomHomeworkService homeworkService;
    private final ClassroomGradebookService gradebookService;
    private final ClassroomContentService contentService;
    private final HomeworkAttachmentStorageService homeworkAttachmentStorageService;
    private final ClassroomScheduleAvailabilityService scheduleAvailabilityService;
    private final ClassScheduleRepository sessionRepository;
    private final ClassroomHomeworkGradingCatalogService homeworkGradingCatalogService;
    private final TeacherClassroomAuthorizationService authorizationService;

    @GetMapping("/assigned")
    public ResponseEntity<List<ClassroomOfferingResponse>> getAssignedClasses(Authentication authentication) {
        return ResponseEntity.ok(classSectionService.getAssignedClasses(authentication.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClassroomOfferingResponse> getClassroom(
            @PathVariable Long id,
            Authentication authentication
    ) {
        authorizationService.assertClassroomAccess(id, authentication.getName());
        return ResponseEntity.ok(classSectionService.getOffering(id, true));
    }

    @GetMapping("/{id}/sessions")
    public ResponseEntity<List<ClassroomSessionResponse>> getSessions(
            @PathVariable Long id,
            Authentication authentication
    ) {
        authorizationService.assertClassroomAccess(id, authentication.getName());
        return ResponseEntity.ok(classSectionService.getSessions(id));
    }

    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<ClassroomSessionResponse> getSession(
            @PathVariable Long sessionId,
            Authentication authentication
    ) {
        authorizationService.assertSessionAccess(sessionId, authentication.getName());
        return ResponseEntity.ok(classSectionService.getSession(sessionId));
    }

    @PostMapping("/{id}/sessions")
    public ResponseEntity<ClassroomSessionResponse> createSession(
            @PathVariable Long id,
            @Valid @RequestBody CreateClassroomSessionRequest request,
            Authentication authentication
    ) {
        authorizationService.assertClassroomAccess(id, authentication.getName());
        return ResponseEntity.ok(classSectionService.createSession(id, request));
    }

    @PutMapping("/sessions/{sessionId}")
    public ResponseEntity<ClassroomSessionResponse> updateSession(
            @PathVariable Long sessionId,
            @Valid @RequestBody CreateClassroomSessionRequest request,
            Authentication authentication
    ) {
        authorizationService.assertSessionAccess(sessionId, authentication.getName());
        return ResponseEntity.ok(classSectionService.updateSession(sessionId, request));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> deleteSession(
            @PathVariable Long sessionId,
            Authentication authentication
    ) {
        authorizationService.assertSessionAccess(sessionId, authentication.getName());
        classSectionService.deleteSession(sessionId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/sessions/{sessionId}/open")
    public ResponseEntity<ClassroomSessionResponse> openVirtualSession(
            @PathVariable Long sessionId,
            Authentication authentication
    ) {
        authorizationService.assertSessionAccess(sessionId, authentication.getName());
        return ResponseEntity.ok(classSectionService.openVirtualSession(sessionId, authentication.getName()));
    }

    @PostMapping("/sessions/{sessionId}/close")
    public ResponseEntity<ClassroomSessionResponse> closeVirtualSession(
            @PathVariable Long sessionId,
            Authentication authentication
    ) {
        authorizationService.assertSessionAccess(sessionId, authentication.getName());
        return ResponseEntity.ok(classSectionService.closeVirtualSession(sessionId, authentication.getName()));
    }

    @GetMapping("/sessions/{sessionId}/attendance")
    public ResponseEntity<List<ClassroomAttendanceResponse>> getSessionAttendance(
            @PathVariable Long sessionId,
            Authentication authentication
    ) {
        authorizationService.assertSessionAccess(sessionId, authentication.getName());
        return ResponseEntity.ok(attendanceService.getBySession(sessionId));
    }

    @PostMapping("/attendance")
    public ResponseEntity<List<ClassroomAttendanceResponse>> saveAttendance(
            @Valid @RequestBody SaveAttendanceRequest request,
            Authentication authentication
    ) {
        authorizationService.assertSessionAccess(request.getSessionId(), authentication.getName());
        return ResponseEntity.ok(attendanceService.saveBulk(request, authentication.getName()));
    }

    @GetMapping("/{id}/homework")
    public ResponseEntity<List<ClassroomHomeworkResponse>> getHomework(
            @PathVariable Long id,
            Authentication authentication
    ) {
        authorizationService.assertClassroomAccess(id, authentication.getName());
        return ResponseEntity.ok(homeworkService.listForClass(id, authentication.getName()));
    }

    @PostMapping("/{id}/homework")
    public ResponseEntity<ClassroomHomeworkResponse> createHomework(
            @PathVariable Long id,
            @Valid @RequestBody CreateHomeworkRequest request,
            Authentication authentication
    ) {
        authorizationService.assertClassroomAccess(id, authentication.getName());
        return ResponseEntity.ok(homeworkService.create(id, request, authentication.getName()));
    }

    @PostMapping(value = "/homework/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<HomeworkAttachmentUploadResponse> uploadHomeworkAttachment(
            @RequestPart("file") MultipartFile file,
            @RequestParam Long classroomId,
            Authentication authentication
    ) {
        authorizationService.assertClassroomAccess(classroomId, authentication.getName());
        String publicUrlBase = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/classroom-homework/attachments")
                .toUriString();
        return ResponseEntity.ok(homeworkAttachmentStorageService.store(file, publicUrlBase, authentication.getName()));
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

    @GetMapping("/homework/ai-assessment-options")
    public ResponseEntity<List<HomeworkAiAssessmentOptionResponse>> listHomeworkAiAssessmentOptions(
            Authentication authentication
    ) {
        return ResponseEntity.ok(homeworkService.listAiAssessmentOptions(authentication.getName()));
    }

    @PutMapping("/homework/{homeworkId}")
    public ResponseEntity<ClassroomHomeworkResponse> updateHomework(
            @PathVariable Long homeworkId,
            @Valid @RequestBody CreateHomeworkRequest request,
            Authentication authentication
    ) {
        authorizationService.assertHomeworkAccess(homeworkId, authentication.getName());
        return ResponseEntity.ok(homeworkService.update(homeworkId, request));
    }

    @DeleteMapping("/homework/{homeworkId}")
    public ResponseEntity<Void> deleteHomework(
            @PathVariable Long homeworkId,
            Authentication authentication
    ) {
        authorizationService.assertHomeworkAccess(homeworkId, authentication.getName());
        homeworkService.delete(homeworkId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/homework/{homeworkId}/students/{studentId}/grade")
    public ResponseEntity<ClassroomHomeworkSubmissionResponse> gradeHomework(
            @PathVariable Long homeworkId,
            @PathVariable Long studentId,
            @Valid @RequestBody GradeHomeworkRequest request,
            Authentication authentication
    ) {
        authorizationService.assertHomeworkAccess(homeworkId, authentication.getName());
        return ResponseEntity.ok(homeworkService.grade(homeworkId, studentId, request, authentication.getName()));
    }

    @PutMapping("/homework/{homeworkId}/students/{studentId}/annotations")
    public ResponseEntity<ClassroomHomeworkSubmissionResponse> saveHomeworkAnnotations(
            @PathVariable Long homeworkId,
            @PathVariable Long studentId,
            @Valid @RequestBody SaveHomeworkAnnotationsRequest request,
            Authentication authentication
    ) {
        authorizationService.assertHomeworkAccess(homeworkId, authentication.getName());
        return ResponseEntity.ok(homeworkService.saveAnnotations(
                homeworkId,
                studentId,
                request,
                authentication.getName()
        ));
    }

    @GetMapping("/homework/{homeworkId}/submissions")
    public ResponseEntity<List<ClassroomHomeworkSubmissionResponse>> getHomeworkSubmissions(
            @PathVariable Long homeworkId,
            Authentication authentication
    ) {
        authorizationService.assertHomeworkAccess(homeworkId, authentication.getName());
        return ResponseEntity.ok(homeworkService.listSubmissions(homeworkId, authentication.getName()));
    }

    @GetMapping("/{id}/gradebook")
    public ResponseEntity<List<ClassroomGradebookResponse>> getGradebook(
            @PathVariable Long id,
            Authentication authentication
    ) {
        authorizationService.assertClassroomAccess(id, authentication.getName());
        return ResponseEntity.ok(gradebookService.getClassGradebook(id));
    }

    @PutMapping("/{id}/gradebook")
    public ResponseEntity<ClassroomGradebookResponse> updateGradebookEntry(
            @PathVariable Long id,
            @Valid @RequestBody UpdateGradebookRequest request,
            Authentication authentication
    ) {
        authorizationService.assertClassroomAccess(id, authentication.getName());
        return ResponseEntity.ok(gradebookService.updateEntry(id, request, authentication.getName()));
    }

    @PostMapping("/{id}/gradebook/publish")
    public ResponseEntity<List<ClassroomGradebookResponse>> publishGradebook(
            @PathVariable Long id,
            Authentication authentication
    ) {
        authorizationService.assertClassroomAccess(id, authentication.getName());
        return ResponseEntity.ok(gradebookService.publishGradebook(id, authentication.getName()));
    }

    @PostMapping("/{id}/gradebook/unpublish")
    public ResponseEntity<List<ClassroomGradebookResponse>> unpublishGradebook(
            @PathVariable Long id,
            Authentication authentication
    ) {
        authorizationService.assertClassroomAccess(id, authentication.getName());
        return ResponseEntity.ok(gradebookService.unpublishGradebook(id, authentication.getName()));
    }

    @GetMapping("/{id}/materials")
    public ResponseEntity<List<ClassroomMaterialResponse>> getMaterials(
            @PathVariable Long id,
            Authentication authentication
    ) {
        authorizationService.assertClassroomAccess(id, authentication.getName());
        return ResponseEntity.ok(contentService.getTeacherMaterials(id, authentication.getName()));
    }

    @PostMapping("/{id}/materials")
    public ResponseEntity<ClassroomMaterialResponse> createMaterial(
            @PathVariable Long id,
            @Valid @RequestBody CreateMaterialRequest request,
            Authentication authentication
    ) {
        authorizationService.assertClassroomAccess(id, authentication.getName());
        return ResponseEntity.ok(contentService.createMaterial(id, request, authentication.getName()));
    }

    @DeleteMapping("/materials/{materialId}")
    public ResponseEntity<Void> deleteMaterial(
            @PathVariable Long materialId,
            Authentication authentication
    ) {
        authorizationService.assertMaterialAccess(materialId, authentication.getName());
        contentService.deleteMaterial(materialId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/announcements")
    public ResponseEntity<List<ClassroomAnnouncementResponse>> getAnnouncements(
            @PathVariable Long id,
            Authentication authentication
    ) {
        authorizationService.assertClassroomAccess(id, authentication.getName());
        return ResponseEntity.ok(contentService.getAnnouncements(id));
    }

    @PostMapping("/{id}/announcements")
    public ResponseEntity<ClassroomAnnouncementResponse> createAnnouncement(
            @PathVariable Long id,
            @Valid @RequestBody CreateAnnouncementRequest request,
            Authentication authentication
    ) {
        authorizationService.assertClassroomAccess(id, authentication.getName());
        return ResponseEntity.ok(contentService.createAnnouncement(id, request, authentication.getName()));
    }

    @PostMapping("/requests/check-conflict")
    public ResponseEntity<ConflictCheckResultResponse> checkChangeConflict(
            @Valid @RequestBody CreateChangeRequestRequest request,
            Authentication authentication
    ) {
        authorizationService.assertClassroomAccess(request.getClassSectionId(), authentication.getName());
        return ResponseEntity.ok(changeRequestService.checkConflict(request, authentication.getName()));
    }

    @GetMapping("/sessions/{sessionId}/available-rooms")
    public ResponseEntity<List<AvailableRoomOptionResponse>> getAvailableRooms(
            @PathVariable Long sessionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate sessionDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime,
            Authentication authentication
    ) {
        authorizationService.assertSessionAccess(sessionId, authentication.getName());
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
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime,
            Authentication authentication
    ) {
        authorizationService.assertSessionAccess(sessionId, authentication.getName());
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
        authorizationService.assertClassroomAccess(request.getClassSectionId(), authentication.getName());
        return ResponseEntity.ok(changeRequestService.create(request, authentication.getName()));
    }

    @GetMapping("/requests/mine")
      public ResponseEntity<List<ClassroomChangeRequestResponse>> listMyRequests(Authentication authentication) {
          return ResponseEntity.ok(changeRequestService.listMine(authentication.getName()));
      }

      @GetMapping("/requests/mine/page")
      public ResponseEntity<Page<ClassroomChangeRequestResponse>> pageMyRequests(
              @RequestParam(required = false) String status,
              @RequestParam(required = false) String keyword,
              @RequestParam(defaultValue = "0") int page,
              @RequestParam(defaultValue = "4") int size,
              Authentication authentication
      ) {
          int safeSize = Math.min(Math.max(size, 1), 100);
          return ResponseEntity.ok(changeRequestService.pageMine(
                  authentication.getName(), status, keyword,
                  PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "createdAt"))
          ));
      }

      @GetMapping("/requests/mine/stats")
      public ResponseEntity<Map<String, Long>> getMyRequestStats(Authentication authentication) {
          return ResponseEntity.ok(changeRequestService.getMyStats(authentication.getName()));
      }
}
