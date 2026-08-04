package fu.sap490.g23.backend.service.teacher;

import fu.sap490.g23.backend.dto.request.teacher.*;
import fu.sap490.g23.backend.dto.response.teacher.*;

import java.util.List;

public interface TeacherProfessionalService {

    List<TeacherProfessionalResponse> listTeachers(String actorEmail);

    TeacherProfessionalResponse getTeacher(Long teacherId, String actorEmail);

    TeacherProfessionalResponse getMyProfile(String teacherEmail);

    TeacherProfessionalResponse updateProfile(Long teacherId, UpdateTeacherProfileRequest request, String actorEmail);

    TeacherCredentialResponse createCredential(Long teacherId, UpsertTeacherCredentialRequest request, String actorEmail);

    TeacherCredentialResponse updateCredential(
            Long teacherId,
            Long credentialId,
            UpsertTeacherCredentialRequest request,
            String actorEmail
    );

    TeacherCredentialResponse verifyCredential(
            Long teacherId,
            Long credentialId,
            VerifyTeacherCredentialRequest request,
            String actorEmail
    );

    void deleteCredential(Long teacherId, Long credentialId, String actorEmail);

    TeacherEvaluationResponse createEvaluation(
            Long teacherId,
            UpsertTeacherEvaluationRequest request,
            String actorEmail
    );

    TeacherEvaluationResponse updateEvaluation(
            Long teacherId,
            Long evaluationId,
            UpsertTeacherEvaluationRequest request,
            String actorEmail
    );

    TeacherEvaluationResponse publishEvaluation(Long teacherId, Long evaluationId, String actorEmail);

    void deleteEvaluation(Long teacherId, Long evaluationId, String actorEmail);
}
