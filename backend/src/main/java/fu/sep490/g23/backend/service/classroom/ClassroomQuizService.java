package fu.sep490.g23.backend.service.classroom;

import fu.sep490.g23.backend.dto.request.classroom.CreateClassroomQuizRequest;
import fu.sep490.g23.backend.dto.request.classroom.SubmitClassroomQuizRequest;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomQuizResponse;

import java.util.List;

public interface ClassroomQuizService {
    List<ClassroomQuizResponse> listForClass(Long offeringId, String userEmail);
    List<ClassroomQuizResponse> listForLearner(String learnerEmail);
    ClassroomQuizResponse create(Long offeringId, CreateClassroomQuizRequest request, String creatorEmail);
    ClassroomQuizResponse open(Long quizId);
    ClassroomQuizResponse close(Long quizId);
    ClassroomQuizResponse submit(Long quizId, SubmitClassroomQuizRequest request, String learnerEmail);
    void delete(Long quizId);
}
