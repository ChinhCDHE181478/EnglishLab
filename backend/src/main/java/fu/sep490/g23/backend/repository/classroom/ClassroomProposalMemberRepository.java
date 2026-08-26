package fu.sep490.g23.backend.repository.classroom;

import fu.sep490.g23.backend.entity.classroom.ClassroomProposalMember;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/** Compatibility facade; proposal membership is stored on course_registration_requests. */
@Repository
@RequiredArgsConstructor
public class ClassroomProposalMemberRepository {
    private final CourseRegistrationRequestRepository requestRepository;

    public ClassroomProposalMember save(ClassroomProposalMember member) {
        var request = member.getCourseRegistrationRequest();
        request.setClassroomProposal(member.getProposal());
        member.setId(requestRepository.save(request).getId());
        return member;
    }
}
