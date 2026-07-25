package fu.sap490.g23.backend.controller.classroom;

import fu.sap490.g23.backend.dto.request.classroom.AssignToClassRequest;
import fu.sap490.g23.backend.dto.request.classroom.ConflictCheckRequest;
import fu.sap490.g23.backend.dto.request.classroom.CreateClassroomOfferingRequest;
import fu.sap490.g23.backend.dto.request.classroom.CreateClassroomSessionRequest;
import fu.sap490.g23.backend.dto.request.classroom.EnrollStudentRequest;
import fu.sap490.g23.backend.dto.request.classroom.RecordTuitionPaymentRequest;
import fu.sap490.g23.backend.dto.request.classroom.RejectRegistrationRequest;
import fu.sap490.g23.backend.dto.request.classroom.TransferEnrollmentRequest;
import fu.sap490.g23.backend.dto.request.classroom.TransferStudentRequest;
import fu.sap490.g23.backend.dto.response.classroom.ClassroomEnrollmentResponse;
import fu.sap490.g23.backend.dto.response.classroom.ClassroomOfferingResponse;
import fu.sap490.g23.backend.dto.response.classroom.ClassroomPickerOptionResponse;
import fu.sap490.g23.backend.dto.response.classroom.ClassroomSessionResponse;
import fu.sap490.g23.backend.dto.response.classroom.ClassroomTeacherSummaryResponse;
import fu.sap490.g23.backend.dto.response.classroom.ClassroomTuitionPaymentResponse;
import fu.sap490.g23.backend.dto.response.classroom.ConflictCheckResultResponse;
import fu.sap490.g23.backend.dto.response.classroom.TuitionProofResponse;
import fu.sap490.g23.backend.dto.response.classroom.TrainingProgramResponse;
import fu.sap490.g23.backend.dto.response.curriculum.CurriculumProgramResponse;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.classroom.ClassroomRoom;
import fu.sap490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import fu.sap490.g23.backend.entity.classroom.enums.ClassroomRegistrationStatus;
import fu.sap490.g23.backend.entity.classroom.enums.ClassroomTeacherRole;
import fu.sap490.g23.backend.entity.enums.RoleEnum;
import fu.sap490.g23.backend.repository.UserRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomRoomRepository;
import fu.sap490.g23.backend.service.classroom.ClassroomOfferingService;
import fu.sap490.g23.backend.service.classroom.TuitionProofService;
import fu.sap490.g23.backend.service.classroom.TrainingProgramService;
import fu.sap490.g23.backend.service.curriculum.CurriculumProgramService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping({"/api/staff/classrooms", "/api/training-manager/classrooms"})
public class TrainingManagerClassroomController {

    private final ClassroomOfferingService classroomOfferingService;
    private final CurriculumProgramService curriculumProgramService;
    private final TuitionProofService tuitionProofService;
    private final TrainingProgramService trainingProgramService;
    private final UserRepository userRepository;
    private final ClassroomRoomRepository roomRepository;

    public TrainingManagerClassroomController(
            ClassroomOfferingService classroomOfferingService,
            CurriculumProgramService curriculumProgramService,
            TuitionProofService tuitionProofService,
            TrainingProgramService trainingProgramService,
            UserRepository userRepository,
            ClassroomRoomRepository roomRepository
    ) {
        this.classroomOfferingService = classroomOfferingService;
        this.curriculumProgramService = curriculumProgramService;
        this.tuitionProofService = tuitionProofService;
        this.trainingProgramService = trainingProgramService;
        this.userRepository = userRepository;
        this.roomRepository = roomRepository;
    }

    @GetMapping
    public ResponseEntity<List<ClassroomOfferingResponse>> listOfferings() {
        return ResponseEntity.ok(classroomOfferingService.getManagerOfferings());
    }

    @GetMapping("/teachers")
    public ResponseEntity<List<ClassroomPickerOptionResponse>> listTeachers() {
        List<ClassroomPickerOptionResponse> options = userRepository.findDistinctByRoles_CodeIn(Set.of(RoleEnum.TEACHER))
                .stream()
                .sorted(Comparator.comparing(User::getFullName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(user -> ClassroomPickerOptionResponse.builder()
                        .id(user.getId())
                        .label((user.getFullName() == null || user.getFullName().isBlank() ? user.getEmail() : user.getFullName())
                                + " - " + user.getEmail())
                        .build())
                .toList();
        return ResponseEntity.ok(options);
    }

    @GetMapping("/rooms")
    public ResponseEntity<List<ClassroomPickerOptionResponse>> listRooms() {
        List<ClassroomPickerOptionResponse> options = roomRepository.findByActiveTrue()
                .stream()
                .sorted(Comparator.comparing(ClassroomRoom::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(room -> ClassroomPickerOptionResponse.builder()
                        .id(room.getId())
                        .label(room.getCapacity() == null ? room.getName() : room.getName() + " - " + room.getCapacity() + " chỗ")
                        .capacity(room.getCapacity())
                        .build())
                .toList();
        return ResponseEntity.ok(options);
    }

    @GetMapping("/curriculum-programs")
    public ResponseEntity<List<CurriculumProgramResponse>> listCurriculumPrograms(
            @RequestParam(required = false) ClassroomDeliveryMode deliveryMode
    ) {
        return ResponseEntity.ok(curriculumProgramService.listPrograms(deliveryMode));
    }

    @GetMapping({"/course-offerings", "/training-programs"})
    public ResponseEntity<List<TrainingProgramResponse>> listPublishedTrainingPrograms(
            @RequestParam(required = false) ClassroomDeliveryMode deliveryMode
    ) {
        return ResponseEntity.ok(trainingProgramService.listPublishedPrograms(deliveryMode));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClassroomOfferingResponse> getOffering(@PathVariable Long id) {
        return ResponseEntity.ok(classroomOfferingService.getManagerOffering(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClassroomOfferingResponse> updateOffering(
            @PathVariable Long id,
            @Valid @RequestBody CreateClassroomOfferingRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(classroomOfferingService.updateOffering(id, request, authentication.getName()));
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<ClassroomOfferingResponse> closeOffering(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(classroomOfferingService.closeOffering(id, authentication.getName()));
    }

    @GetMapping("/{id}/sessions")
    public ResponseEntity<List<ClassroomSessionResponse>> listSessions(@PathVariable Long id) {
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

    @PostMapping("/{id}/enroll")
    public ResponseEntity<ClassroomEnrollmentResponse> enrollStudent(
            @PathVariable Long id,
            @Valid @RequestBody EnrollStudentRequest request
    ) {
        return ResponseEntity.ok(classroomOfferingService.enrollStudent(id, request));
    }

    @PostMapping("/{id}/students/{studentId}/remove")
    public ResponseEntity<Void> removeStudent(@PathVariable Long id, @PathVariable Long studentId) {
        classroomOfferingService.removeStudent(id, studentId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/transfer-student")
    public ResponseEntity<ClassroomEnrollmentResponse> transferStudent(
            @PathVariable Long id,
            @Valid @RequestBody TransferStudentRequest request
    ) {
        return ResponseEntity.ok(classroomOfferingService.transferStudent(id, request));
    }

    @PostMapping("/{id}/teachers/{teacherId}/assign")
    public ResponseEntity<ClassroomTeacherSummaryResponse> assignTeacher(
            @PathVariable Long id,
            @PathVariable Long teacherId,
            @RequestParam(defaultValue = "PRIMARY") ClassroomTeacherRole role
    ) {
        return ResponseEntity.ok(classroomOfferingService.assignTeacher(id, teacherId, role));
    }

    @PostMapping("/{id}/teachers/{oldTeacherId}/replace/{newTeacherId}")
    public ResponseEntity<ClassroomTeacherSummaryResponse> replaceTeacher(
            @PathVariable Long id,
            @PathVariable Long oldTeacherId,
            @PathVariable Long newTeacherId
    ) {
        return ResponseEntity.ok(classroomOfferingService.replaceTeacher(id, oldTeacherId, newTeacherId));
    }

    @GetMapping("/registrations")
    public ResponseEntity<List<ClassroomEnrollmentResponse>> listRegistrations(
            @RequestParam(required = false) ClassroomRegistrationStatus status,
            @RequestParam(required = false) Long classroomOfferingId,
            @RequestParam(required = false) Boolean needsAction,
            @RequestParam(required = false) Boolean settlementPending
    ) {
        return ResponseEntity.ok(classroomOfferingService.listRegistrations(
                status,
                classroomOfferingId,
                needsAction,
                settlementPending
        ));
    }

    @GetMapping("/enrollments/{enrollmentId}")
    public ResponseEntity<ClassroomEnrollmentResponse> getEnrollment(@PathVariable Long enrollmentId) {
        return ResponseEntity.ok(classroomOfferingService.getRegistration(enrollmentId));
    }

    @PostMapping("/enrollments/{enrollmentId}/confirm")
    public ResponseEntity<ClassroomEnrollmentResponse> confirmRegistration(
            @PathVariable Long enrollmentId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(classroomOfferingService.confirmRegistration(enrollmentId, authentication.getName()));
    }

    @PostMapping("/enrollments/{enrollmentId}/reject")
    public ResponseEntity<ClassroomEnrollmentResponse> rejectRegistration(
            @PathVariable Long enrollmentId,
            @RequestBody(required = false) RejectRegistrationRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(classroomOfferingService.rejectRegistration(enrollmentId, request, authentication.getName()));
    }

    @PostMapping("/enrollments/{enrollmentId}/tuition")
    public ResponseEntity<ClassroomEnrollmentResponse> recordTuitionPayment(
            @PathVariable Long enrollmentId,
            @Valid @RequestBody RecordTuitionPaymentRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(classroomOfferingService.recordTuitionPayment(enrollmentId, request, authentication.getName()));
    }

    @GetMapping("/enrollments/{enrollmentId}/tuition-history")
    public ResponseEntity<List<ClassroomTuitionPaymentResponse>> getTuitionHistory(@PathVariable Long enrollmentId) {
        return ResponseEntity.ok(classroomOfferingService.getTuitionHistory(enrollmentId));
    }

    @GetMapping("/tuition-proofs/pending")
    public ResponseEntity<List<TuitionProofResponse>> listPendingTuitionProofs() {
        return ResponseEntity.ok(tuitionProofService.listPendingProofs());
    }

    @GetMapping("/enrollments/{enrollmentId}/tuition-proofs")
    public ResponseEntity<List<TuitionProofResponse>> listTuitionProofsForEnrollment(@PathVariable Long enrollmentId) {
        return ResponseEntity.ok(tuitionProofService.listProofsForEnrollment(enrollmentId));
    }

    @PostMapping("/tuition-proofs/{proofId}/confirm")
    public ResponseEntity<TuitionProofResponse> confirmTuitionProof(
            @PathVariable Long proofId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(tuitionProofService.confirmProof(proofId, authentication.getName()));
    }

    @PostMapping("/tuition-proofs/{proofId}/reject")
    public ResponseEntity<TuitionProofResponse> rejectTuitionProof(
            @PathVariable Long proofId,
            @RequestBody(required = false) RejectRegistrationRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(tuitionProofService.rejectProof(
                proofId,
                request == null ? null : request.getReason(),
                authentication.getName()
        ));
    }

    @PostMapping("/enrollments/{enrollmentId}/assign")
    public ResponseEntity<ClassroomEnrollmentResponse> assignToClass(
            @PathVariable Long enrollmentId,
            @RequestBody(required = false) AssignToClassRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(classroomOfferingService.assignToClass(enrollmentId, request, authentication.getName()));
    }

    @PostMapping("/enrollments/{enrollmentId}/transfer")
    public ResponseEntity<ClassroomEnrollmentResponse> transferEnrollment(
            @PathVariable Long enrollmentId,
            @Valid @RequestBody TransferEnrollmentRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(classroomOfferingService.transferEnrollment(enrollmentId, request, authentication.getName()));
    }

    @PostMapping("/enrollments/{enrollmentId}/conflict-check")
    public ResponseEntity<ConflictCheckResultResponse> checkEnrollmentConflict(@PathVariable Long enrollmentId) {
        return ResponseEntity.ok(classroomOfferingService.checkEnrollmentConflict(enrollmentId));
    }

    @PostMapping("/conflict-check")
    public ResponseEntity<ConflictCheckResultResponse> checkConflict(@RequestBody ConflictCheckRequest request) {
        return ResponseEntity.ok(classroomOfferingService.checkConflict(request));
    }
}
