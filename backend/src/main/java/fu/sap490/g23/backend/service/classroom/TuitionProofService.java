package fu.sap490.g23.backend.service.classroom;

import fu.sap490.g23.backend.dto.response.classroom.ClassroomTuitionPaymentResponse;
import fu.sap490.g23.backend.dto.response.classroom.TuitionProofResponse;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

public interface TuitionProofService {

    TuitionProofResponse submitProof(
            Long offeringId,
            MultipartFile file,
            BigDecimal amount,
            String paymentKind,
            String note,
            String learnerEmail,
            String publicUrlBase
    );

    List<TuitionProofResponse> getMyProofs(Long offeringId, String learnerEmail);

    List<ClassroomTuitionPaymentResponse> getMyTuitionHistory(Long offeringId, String learnerEmail);

    List<TuitionProofResponse> listPendingProofs();

    List<TuitionProofResponse> listProofsForEnrollment(Long enrollmentId);

    TuitionProofResponse confirmProof(Long proofId, String actorEmail);

    TuitionProofResponse rejectProof(Long proofId, String reason, String actorEmail);
}
