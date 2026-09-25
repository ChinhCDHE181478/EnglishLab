package fu.sep490.g23.backend.repository.classroom;

import fu.sep490.g23.backend.entity.classroom.ClassroomProposal;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomApprovalStatus;
import fu.sep490.g23.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ClassroomProposalRepository extends JpaRepository<ClassroomProposal, Long> {
    List<ClassroomProposal> findAllByOrderByCreatedAtDesc();

    List<ClassroomProposal> findByApprovalStatusOrderByCreatedAtAsc(ClassroomApprovalStatus status);

    List<ClassroomProposal> findByCreatedByOrderByCreatedAtDesc(User createdBy);

    List<ClassroomProposal> findByCreatedByAndApprovalStatusOrderByCreatedAtAsc(
            User createdBy,
            ClassroomApprovalStatus status
    );

    Optional<ClassroomProposal> findFirstByCreatedByInAndStaffNoteStartingWithOrderByCreatedAtDescIdDesc(
            Collection<User> createdBy,
            String staffNotePrefix
    );
}
