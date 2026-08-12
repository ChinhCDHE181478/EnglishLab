package fu.sep490.g23.backend.controller.teacher;
import fu.sep490.g23.backend.dto.request.teacher.UpsertTeacherCredentialRequest;
import fu.sep490.g23.backend.dto.response.teacher.TeacherProfessionalResponse;
import fu.sep490.g23.backend.dto.request.teacher.VerifyTeacherCredentialRequest;
import fu.sep490.g23.backend.dto.response.teacher.TeacherCredentialResponse;
import fu.sep490.g23.backend.dto.request.teacher.UpdateTeacherProfileRequest;

import fu.sep490.g23.backend.service.teacher.TeacherProfessionalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/staff/teachers")
@RequiredArgsConstructor
public class StaffTeacherProfileController {

    private final TeacherProfessionalService teacherProfessionalService;

    @GetMapping
    public ResponseEntity<List<TeacherProfessionalResponse>> list(Authentication authentication) {
        return ResponseEntity.ok(teacherProfessionalService.listTeachers(authentication.getName()));
    }

    @GetMapping("/{teacherId}")
    public ResponseEntity<TeacherProfessionalResponse> detail(
            @PathVariable Long teacherId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(teacherProfessionalService.getTeacher(teacherId, authentication.getName()));
    }

    @PutMapping("/{teacherId}/profile")
    public ResponseEntity<TeacherProfessionalResponse> updateProfile(
            @PathVariable Long teacherId,
            @Valid @RequestBody UpdateTeacherProfileRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(teacherProfessionalService.updateProfile(
                teacherId,
                request,
                authentication.getName()
        ));
    }

    @PostMapping("/{teacherId}/credentials")
    public ResponseEntity<TeacherCredentialResponse> createCredential(
            @PathVariable Long teacherId,
            @Valid @RequestBody UpsertTeacherCredentialRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(teacherProfessionalService.createCredential(
                teacherId,
                request,
                authentication.getName()
        ));
    }

    @PutMapping("/{teacherId}/credentials/{credentialId}")
    public ResponseEntity<TeacherCredentialResponse> updateCredential(
            @PathVariable Long teacherId,
            @PathVariable Long credentialId,
            @Valid @RequestBody UpsertTeacherCredentialRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(teacherProfessionalService.updateCredential(
                teacherId,
                credentialId,
                request,
                authentication.getName()
        ));
    }

    @PatchMapping("/{teacherId}/credentials/{credentialId}/verify")
    public ResponseEntity<TeacherCredentialResponse> verifyCredential(
            @PathVariable Long teacherId,
            @PathVariable Long credentialId,
            @Valid @RequestBody VerifyTeacherCredentialRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(teacherProfessionalService.verifyCredential(
                teacherId,
                credentialId,
                request,
                authentication.getName()
        ));
    }

    @DeleteMapping("/{teacherId}/credentials/{credentialId}")
    public ResponseEntity<Void> deleteCredential(
            @PathVariable Long teacherId,
            @PathVariable Long credentialId,
            Authentication authentication
    ) {
        teacherProfessionalService.deleteCredential(teacherId, credentialId, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
