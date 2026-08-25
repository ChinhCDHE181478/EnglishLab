package fu.sep490.g23.backend.repository.classroom;

import fu.sep490.g23.backend.entity.classroom.ClassroomProposalMember;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

public interface ClassroomProposalMemberRepository extends JpaRepository<ClassroomProposalMember, Long> {
    boolean existsByCourseRegistrationRequestIdAndProposalApprovalStatusIn(
            Long courseRegistrationRequestId,
            Collection<ClassroomApprovalStatus> statuses
    );
}
