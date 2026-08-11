package fu.sep490.g23.backend.service.classroom;

import fu.sep490.g23.backend.dto.request.classroom.CreateHomeworkRequest;
import fu.sep490.g23.backend.dto.request.classroom.GradeHomeworkRequest;
import fu.sep490.g23.backend.dto.request.classroom.SaveHomeworkAnnotationsRequest;
import fu.sep490.g23.backend.dto.request.classroom.SubmitHomeworkRequest;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomHomeworkResponse;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomHomeworkSubmissionResponse;
import fu.sep490.g23.backend.dto.response.classroom.HomeworkAiAssessmentOptionResponse;
import java.util.List;

public interface ClassroomHomeworkService {

    List<ClassroomHomeworkResponse> listForClass(Long offeringId, String userEmail);

    List<ClassroomHomeworkResponse> listForLearner(String learnerEmail);

    ClassroomHomeworkResponse create(Long offeringId, CreateHomeworkRequest request, String creatorEmail);

    ClassroomHomeworkResponse update(Long homeworkId, CreateHomeworkRequest request);

    void delete(Long homeworkId);

    ClassroomHomeworkSubmissionResponse submit(Long homeworkId, SubmitHomeworkRequest request, String learnerEmail);

    ClassroomHomeworkSubmissionResponse grade(Long homeworkId, Long studentId, GradeHomeworkRequest request, String graderEmail);

    ClassroomHomeworkSubmissionResponse saveAnnotations(
            Long homeworkId,
            Long studentId,
            SaveHomeworkAnnotationsRequest request,
            String teacherEmail
    );

    List<ClassroomHomeworkSubmissionResponse> listSubmissions(Long homeworkId, String teacherEmail);

    List<HomeworkAiAssessmentOptionResponse> listAiAssessmentOptions(String teacherEmail);
}
