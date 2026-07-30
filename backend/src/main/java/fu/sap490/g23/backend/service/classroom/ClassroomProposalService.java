package fu.sap490.g23.backend.service.classroom;

import fu.sap490.g23.backend.dto.request.classroom.CreateClassroomProposalRequest;
import fu.sap490.g23.backend.dto.request.classroom.RejectClassroomProposalRequest;
import fu.sap490.g23.backend.dto.response.classroom.ClassroomProposalResponse;
import fu.sap490.g23.backend.dto.response.classroom.ConflictCheckResultResponse;
import fu.sap490.g23.backend.entity.classroom.enums.ClassroomApprovalStatus;

import java.util.List;

public interface ClassroomProposalService {
    ClassroomProposalResponse create(CreateClassroomProposalRequest request, String staffEmail);

    ClassroomProposalResponse update(Long proposalId, CreateClassroomProposalRequest request, String staffEmail);

    ConflictCheckResultResponse validateSchedule(
            CreateClassroomProposalRequest request,
            Long excludeProposalId,
            String staffEmail
    );

    ClassroomProposalResponse submit(Long proposalId, String staffEmail);

    List<ClassroomProposalResponse> listForStaff(ClassroomApprovalStatus status, String staffEmail);

    List<ClassroomProposalResponse> listForManager(ClassroomApprovalStatus status, String managerEmail);

    ClassroomProposalResponse approve(Long proposalId, String managerEmail);

    ClassroomProposalResponse reject(
            Long proposalId,
            RejectClassroomProposalRequest request,
            String managerEmail
    );
}
