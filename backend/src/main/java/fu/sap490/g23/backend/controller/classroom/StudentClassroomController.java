package fu.sap490.g23.backend.controller.classroom;

import fu.sap490.g23.backend.dto.request.classroom.*;
import fu.sap490.g23.backend.dto.response.classroom.*;
import fu.sap490.g23.backend.service.classroom.*;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;
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
    private final TuitionProofService tuitionProofService;
    private final HomeworkAttachmentStorageService homeworkAttachmentStorageService;
    private final ClassroomPracticeService classroomPracticeService;

    @GetMapping({"/my-classrooms", "/my-classes"})
    public ResponseEntity<List<ClassroomOfferingResponse>> getMyClasses(Authentication authentication) {
        return ResponseEntity.ok(classroomOfferingService.getMyClasses(authentication.getName()));
    }

    @PostMapping(value = "/{id}/tuition-proofs", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TuitionProofResponse> submitTuitionProof(
            @PathVariable Long id,
            @RequestPart("file") MultipartFile file,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) String paymentKind,
            @RequestParam(required = false) String note,
            Authentication authentication
    ) {
        String publicUrlBase = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/classroom-homework/attachments")
                .toUriString();
        return ResponseEntity.ok(tuitionProofService.submitProof(
                id, file, amount, paymentKind, note, authentication.getName(), publicUrlBase
        ));
    }

    @GetMapping("/{id}/tuition-proofs")
    public ResponseEntity<List<TuitionProofResponse>> getMyTuitionProofs(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(tuitionProofService.getMyProofs(id, authentication.getName()));
    }

    @GetMapping("/{id}/tuition-history")
    public ResponseEntity<List<ClassroomTuitionPaymentResponse>> getMyTuitionHistory(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(tuitionProofService.getMyTuitionHistory(id, authentication.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClassroomOfferingResponse> getClassroom(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(classroomOfferingService.getLearnerOffering(id, authentication.getName()));
    }

    @GetMapping("/{id}/sessions")
    public ResponseEntity<List<ClassroomSessionResponse>> getSessions(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(classroomOfferingService.getLearnerSessions(
                id,
                authentication.getName()
        ));
    }

    @PostMapping("/sessions/{sessionId}/join")
    public ResponseEntity<ClassroomSessionResponse> joinVirtualSession(
            @PathVariable Long sessionId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(classroomOfferingService.joinVirtualSession(
                sessionId,
                authentication.getName()
        ));
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<ClassroomSessionResponse> joinVirtualClass(
            @PathVariable Long id,
            @RequestParam Long sessionId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(classroomOfferingService.joinVirtualClass(
                id,
                sessionId,
                authentication.getName()
        ));
    }

    @GetMapping("/{id}/homework")
    public ResponseEntity<List<ClassroomHomeworkResponse>> getHomework(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(classroomHomeworkService.listForClass(id, authentication.getName()));
    }

    @GetMapping("/{id}/practice")
    public ResponseEntity<List<ClassroomPracticeResponse>> getPractice(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(classroomPracticeService.listForLearner(id, authentication.getName()));
    }

    @GetMapping("/my-practice")
    public ResponseEntity<List<ClassroomPracticeResponse>> getMyPractice(Authentication authentication) {
        return ResponseEntity.ok(classroomPracticeService.listAllForLearner(authentication.getName()));
    }

    @PostMapping("/{id}/practice/{exerciseId}/complete")
    public ResponseEntity<ClassroomPracticeResponse> completePractice(
            @PathVariable Long id,
            @PathVariable Long exerciseId,
            @Valid @RequestBody CompletePracticeRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(classroomPracticeService.complete(id, exerciseId, request, authentication.getName()));
    }

    @PostMapping("/{id}/practice/{exerciseId}/attempts")
    public ResponseEntity<ClassroomPracticeAttemptResponse> submitPracticeAttempt(
            @PathVariable Long id,
            @PathVariable Long exerciseId,
            @Valid @RequestBody CompletePracticeRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(classroomPracticeService.submitAttempt(id, exerciseId, request, authentication.getName()));
    }

    @GetMapping("/{id}/practice/{exerciseId}/attempts")
    public ResponseEntity<List<ClassroomPracticeAttemptResponse>> getPracticeAttempts(
            @PathVariable Long id,
            @PathVariable Long exerciseId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(classroomPracticeService.listAttempts(id, exerciseId, authentication.getName()));
    }

    @PostMapping("/homework/{homeworkId}/submit")
    public ResponseEntity<ClassroomHomeworkSubmissionResponse> submitHomework(
            @PathVariable Long homeworkId,
            @RequestBody SubmitHomeworkRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(classroomHomeworkService.submit(homeworkId, request, authentication.getName()));
    }

    @PostMapping(value = "/homework/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<HomeworkAttachmentUploadResponse> uploadHomeworkSubmissionAttachment(
            @RequestPart("file") MultipartFile file,
            Authentication authentication
    ) {
        String publicUrlBase = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/classroom-homework/attachments")
                .toUriString();
        return ResponseEntity.ok(homeworkAttachmentStorageService.store(file, publicUrlBase));
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
        ClassroomGradebookResponse gradebook = classroomGradebookService.getMyGradebook(id, authentication.getName());
        return gradebook == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(gradebook);
    }

    @GetMapping("/{id}/materials")
    public ResponseEntity<List<ClassroomMaterialResponse>> getMaterials(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(classroomContentService.getLearnerMaterials(id, authentication.getName()));
    }

    @GetMapping("/{id}/announcements")
    public ResponseEntity<List<ClassroomAnnouncementResponse>> getAnnouncements(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(classroomContentService.getLearnerAnnouncements(id, authentication.getName()));
    }

    @GetMapping("/{id}/syllabus")
    public ResponseEntity<List<ClassroomSyllabusItemResponse>> getSyllabus(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(classroomContentService.getLearnerSyllabus(id, authentication.getName()));
    }

    @GetMapping("/{id}/attendance/me")
    public ResponseEntity<List<ClassroomAttendanceResponse>> getMyAttendance(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(classroomAttendanceService.getByClassForStudent(id, authentication.getName()));
    }
}
