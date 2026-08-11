package fu.sep490.g23.backend.repository.classroom;

import fu.sep490.g23.backend.entity.classroom.ClassroomProposal;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClassroomProposalRepository extends JpaRepository<ClassroomProposal, Long> {
    List<ClassroomProposal> findAllByOrderByCreatedAtDesc();

    List<ClassroomProposal> findByApprovalStatusOrderByCreatedAtAsc(ClassroomApprovalStatus status);
}
