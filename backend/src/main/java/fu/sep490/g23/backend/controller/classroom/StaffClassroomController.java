package fu.sep490.g23.backend.controller.classroom;

import fu.sep490.g23.backend.dto.request.classroom.AssignToClassRequest;
import fu.sep490.g23.backend.dto.request.classroom.ConflictCheckRequest;
import fu.sep490.g23.backend.dto.request.classroom.CreateClassroomOfferingRequest;
import fu.sep490.g23.backend.dto.request.classroom.CreateClassroomSessionRequest;
import fu.sep490.g23.backend.dto.request.classroom.CreateAnnouncementRequest;
import fu.sep490.g23.backend.dto.request.classroom.EnrollStudentRequest;
import fu.sep490.g23.backend.dto.request.classroom.RecordTuitionPaymentRequest;
import fu.sep490.g23.backend.dto.request.classroom.RejectRegistrationRequest;
import fu.sep490.g23.backend.dto.request.classroom.TransferEnrollmentRequest;
import fu.sep490.g23.backend.dto.request.classroom.TransferStudentRequest;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomEnrollmentResponse;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomOfferingResponse;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomPickerOptionResponse;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomSessionResponse;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomAnnouncementResponse;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomTeacherSummaryResponse;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomTuitionPaymentResponse;
import fu.sep490.g23.backend.dto.response.classroom.ConflictCheckResultResponse;
import fu.sep490.g23.backend.dto.response.classroom.AvailableRoomOptionResponse;
import fu.sep490.g23.backend.dto.response.classroom.AvailableTeacherOptionResponse;
import fu.sep490.g23.backend.dto.response.classroom.TuitionProofResponse;
import fu.sep490.g23.backend.dto.response.classroom.InstructorLedCourseResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.classroom.Room;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomRegistrationStatus;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomTeacherRole;
import fu.sep490.g23.backend.entity.enums.RoleCodes;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.classroom.RoomRepository;
import fu.sep490.g23.backend.service.classroom.ClassroomOfferingService;
import fu.sep490.g23.backend.service.classroom.ClassroomContentService;
import fu.sep490.g23.backend.service.classroom.ClassroomScheduleAvailabilityService;
import fu.sep490.g23.backend.service.classroom.TuitionProofService;
import fu.sep490.g23.backend.service.classroom.InstructorLedCourseCatalogService;
import fu.sep490.g23.backend.service.curriculum.InstructorLedCourseManagementService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.format.annotation.DateTimeFormat;
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
import java.time.LocalDate;
import java.time.LocalTime;

@RestController
@RequestMapping("/api/staff/classrooms")
public class StaffClassroomController {

    private final ClassroomOfferingService classSectionService;
    private final TuitionProofService tuitionProofService;
    private final InstructorLedCourseCatalogService instructorLedCourseCatalogService;
    private final InstructorLedCourseManagementService instructorLedCourseManagementService;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final ClassroomScheduleAvailabilityService scheduleAvailabilityService;
    private final ClassroomContentService classroomContentService;

    public StaffClassroomController(
            ClassroomOfferingService classSectionService,
            TuitionProofService tuitionProofService,
            InstructorLedCourseCatalogService instructorLedCourseCatalogService,
            InstructorLedCourseManagementService instructorLedCourseManagementService,
            UserRepository userRepository,
            RoomRepository roomRepository,
            ClassroomScheduleAvailabilityService scheduleAvailabilityService,
            ClassroomContentService classroomContentService
    ) {
        this.classSectionService = classSectionService;
        this.tuitionProofService = tuitionProofService;
        this.instructorLedCourseCatalogService = instructorLedCourseCatalogService;
        this.instructorLedCourseManagementService = instructorLedCourseManagementService;
        this.userRepository = userRepository;
        this.roomRepository = roomRepository;
        this.scheduleAvailabilityService = scheduleAvailabilityService;
        this.classroomContentService = classroomContentService;
    }

    @GetMapping
    public ResponseEntity<List<ClassroomOfferingResponse>> listOfferings() {
        return ResponseEntity.ok(classSectionService.getStaffOfferings());
    }

    @GetMapping("/{id}/announcements")
    public ResponseEntity<List<ClassroomAnnouncementResponse>> getAnnouncements(@PathVariable Long id) {
        return ResponseEntity.ok(classroomContentService.getAnnouncements(id));
    }

    @PostMapping("/{id}/announcements")
    public ResponseEntity<ClassroomAnnouncementResponse> createAnnouncement(
            @PathVariable Long id,
            @Valid @RequestBody CreateAnnouncementRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(classroomContentService.createAnnouncement(id, request, authentication.getName()));
    }

    @GetMapping("/teachers")
    public ResponseEntity<List<ClassroomPickerOptionResponse>> listTeachers() {
        List<ClassroomPickerOptionResponse> options = userRepository.findDistinctByRoles_CodeIn(Set.of(RoleCodes.TEACHER))
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
                .sorted(Comparator.comparing(Room::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(room -> ClassroomPickerOptionResponse.builder()
                        .id(room.getId())
                        .label(room.getCapacity() == null ? room.getName() : room.getName() + " - " + room.getCapacity() + " chỗ")
                        .capacity(room.getCapacity())
                        .build())
                .toList();
        return ResponseEntity.ok(options);
    }

    @GetMapping("/availability/teachers")
    public ResponseEntity<List<AvailableTeacherOptionResponse>> listAvailableTeachers(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate sessionDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime,
            @RequestParam(required = false) Long excludeSessionId
    ) {
        return ResponseEntity.ok(scheduleAvailabilityService.listAvailableTeachers(
                sessionDate, startTime, endTime, excludeSessionId
        ));
    }

    @GetMapping("/availability/rooms")
    public ResponseEntity<List<AvailableRoomOptionResponse>> listAvailableRooms(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate sessionDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime,
            @RequestParam(required = false) Long excludeSessionId
    ) {
        return ResponseEntity.ok(scheduleAvailabilityService.listAvailableRooms(
                sessionDate, startTime, endTime, excludeSessionId
        ));
    }

    @GetMapping("/{id}/available-replacement-teachers")
    public ResponseEntity<List<AvailableTeacherOptionResponse>> listAvailableReplacementTeachers(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(scheduleAvailabilityService.listAvailableReplacementTeachers(id));
    }

    @GetMapping("/training-programs")
    public ResponseEntity<List<InstructorLedCourseResponse>> listPublishedTrainingPrograms(
            @RequestParam(required = false) ClassroomDeliveryMode deliveryMode
    ) {
        return ResponseEntity.ok(instructorLedCourseCatalogService.listPublishedPrograms(deliveryMode));
    }

    @GetMapping("/training-programs/{id}")
    public ResponseEntity<fu.sep490.g23.backend.dto.response.curriculum.InstructorLedCourseResponse> getPublishedTrainingProgram(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(instructorLedCourseManagementService.getProgram(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClassroomOfferingResponse> getOffering(@PathVariable Long id) {
        return ResponseEntity.ok(classSectionService.getStaffOffering(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClassroomOfferingResponse> updateOffering(
            @PathVariable Long id,
            @Valid @RequestBody CreateClassroomOfferingRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(classSectionService.updateOffering(id, request, authentication.getName()));
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<ClassroomOfferingResponse> closeOffering(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(classSectionService.closeOffering(id, authentication.getName()));
    }

    @GetMapping("/{id}/sessions")
    public ResponseEntity<List<ClassroomSessionResponse>> listSessions(@PathVariable Long id) {
        return ResponseEntity.ok(classSectionService.getSessions(id));
    }

    @PostMapping("/{id}/sessions")
    public ResponseEntity<ClassroomSessionResponse> createSession(
            @PathVariable Long id,
            @Valid @RequestBody CreateClassroomSessionRequest request
    ) {
        return ResponseEntity.ok(classSectionService.createSession(id, request));
    }

    @PostMapping({
            "/sessions/{sessionId}/sync-google-meet",
            "/sessions/{sessionId}/sync-google-meet"
    })
    public ResponseEntity<ClassroomSessionResponse> syncVirtualSessionMeeting(
            @PathVariable Long sessionId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(classSectionService.syncVirtualSessionMeeting(
                sessionId,
                authentication.getName()
        ));
    }

    @PutMapping("/sessions/{sessionId}")
    public ResponseEntity<ClassroomSessionResponse> updateSession(
            @PathVariable Long sessionId,
            @Valid @RequestBody CreateClassroomSessionRequest request
    ) {
        return ResponseEntity.ok(classSectionService.updateSession(sessionId, request));
    }

    @PostMapping("/{id}/enroll")
    public ResponseEntity<ClassroomEnrollmentResponse> enrollStudent(
            @PathVariable Long id,
            @Valid @RequestBody EnrollStudentRequest request
    ) {
        return ResponseEntity.ok(classSectionService.enrollStudent(id, request));
    }

    @PostMapping("/{id}/students/{studentId}/remove")
    public ResponseEntity<Void> removeStudent(@PathVariable Long id, @PathVariable Long studentId) {
        classSectionService.removeStudent(id, studentId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/transfer-student")
    public ResponseEntity<ClassroomEnrollmentResponse> transferStudent(
            @PathVariable Long id,
            @Valid @RequestBody TransferStudentRequest request
    ) {
        return ResponseEntity.ok(classSectionService.transferStudent(id, request));
    }

    @PostMapping("/{id}/teachers/{teacherId}/assign")
    public ResponseEntity<ClassroomTeacherSummaryResponse> assignTeacher(
            @PathVariable Long id,
            @PathVariable Long teacherId,
            @RequestParam(defaultValue = "PRIMARY") ClassroomTeacherRole role
    ) {
        return ResponseEntity.ok(classSectionService.assignTeacher(id, teacherId, role));
    }

    @PostMapping("/{id}/teachers/{oldTeacherId}/replace/{newTeacherId}")
    public ResponseEntity<ClassroomTeacherSummaryResponse> replaceTeacher(
            @PathVariable Long id,
            @PathVariable Long oldTeacherId,
            @PathVariable Long newTeacherId
    ) {
        return ResponseEntity.ok(classSectionService.replaceTeacher(id, oldTeacherId, newTeacherId));
    }

    @GetMapping("/registrations")
    public ResponseEntity<List<ClassroomEnrollmentResponse>> listRegistrations(
            @RequestParam(required = false) ClassroomRegistrationStatus status,
            @RequestParam(required = false) Long classSectionId,
            @RequestParam(required = false) Boolean needsAction,
            @RequestParam(required = false) Boolean settlementPending
    ) {
        return ResponseEntity.ok(classSectionService.listRegistrations(
                status,
                classSectionId,
                needsAction,
                settlementPending
        ));
    }

    @GetMapping("/enrollments/{enrollmentId}")
    public ResponseEntity<ClassroomEnrollmentResponse> getEnrollment(@PathVariable Long enrollmentId) {
        return ResponseEntity.ok(classSectionService.getRegistration(enrollmentId));
    }

    @PostMapping("/enrollments/{enrollmentId}/confirm")
    public ResponseEntity<ClassroomEnrollmentResponse> confirmRegistration(
            @PathVariable Long enrollmentId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(classSectionService.confirmRegistration(enrollmentId, authentication.getName()));
    }

    @PostMapping("/enrollments/{enrollmentId}/reject")
    public ResponseEntity<ClassroomEnrollmentResponse> rejectRegistration(
            @PathVariable Long enrollmentId,
            @RequestBody(required = false) RejectRegistrationRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(classSectionService.rejectRegistration(enrollmentId, request, authentication.getName()));
    }

    @PostMapping("/enrollments/{enrollmentId}/tuition")
    public ResponseEntity<ClassroomEnrollmentResponse> recordTuitionPayment(
            @PathVariable Long enrollmentId,
            @Valid @RequestBody RecordTuitionPaymentRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(classSectionService.recordTuitionPayment(enrollmentId, request, authentication.getName()));
    }

    @GetMapping("/enrollments/{enrollmentId}/tuition-history")
    public ResponseEntity<List<ClassroomTuitionPaymentResponse>> getTuitionHistory(@PathVariable Long enrollmentId) {
        return ResponseEntity.ok(classSectionService.getTuitionHistory(enrollmentId));
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
        return ResponseEntity.ok(classSectionService.assignToClass(enrollmentId, request, authentication.getName()));
    }

    @PostMapping("/enrollments/{enrollmentId}/transfer")
    public ResponseEntity<ClassroomEnrollmentResponse> transferEnrollment(
            @PathVariable Long enrollmentId,
            @Valid @RequestBody TransferEnrollmentRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(classSectionService.transferEnrollment(enrollmentId, request, authentication.getName()));
    }

    @PostMapping("/enrollments/{enrollmentId}/conflict-check")
    public ResponseEntity<ConflictCheckResultResponse> checkEnrollmentConflict(@PathVariable Long enrollmentId) {
        return ResponseEntity.ok(classSectionService.checkEnrollmentConflict(enrollmentId));
    }

    @PostMapping("/conflict-check")
    public ResponseEntity<ConflictCheckResultResponse> checkConflict(@Valid @RequestBody ConflictCheckRequest request) {
        return ResponseEntity.ok(classSectionService.checkConflict(request));
    }
}
