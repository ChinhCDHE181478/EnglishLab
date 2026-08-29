package fu.sep490.g23.backend.controller.classroom;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomTuitionPaymentResponse;
import fu.sep490.g23.backend.service.classroom.ClassroomPracticeService;
import fu.sep490.g23.backend.service.classroom.HomeworkAttachmentStorageService;
import fu.sep490.g23.backend.dto.response.classroom.HomeworkAttachmentUploadResponse;
import fu.sep490.g23.backend.dto.response.classroom.TuitionProofResponse;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomAnnouncementResponse;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomSessionResponse;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomMaterialResponse;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomHomeworkSubmissionResponse;
import fu.sep490.g23.backend.service.classroom.ClassroomContentService;
import fu.sep490.g23.backend.dto.request.classroom.CompletePracticeRequest;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomGradebookResponse;
import fu.sep490.g23.backend.service.classroom.ClassroomAttendanceService;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomSyllabusItemResponse;
import fu.sep490.g23.backend.service.classroom.HomeworkAttachmentAccessService;
import fu.sep490.g23.backend.dto.request.classroom.SubmitHomeworkRequest;
import fu.sep490.g23.backend.service.classroom.TuitionProofService;
import fu.sep490.g23.backend.service.classroom.ClassroomHomeworkService;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomPracticeResponse;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomHomeworkResponse;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomOfferingResponse;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomAttendanceResponse;
import fu.sep490.g23.backend.service.classroom.ClassroomGradebookService;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomPracticeAttemptResponse;
import fu.sep490.g23.backend.service.classroom.ClassroomOfferingService;

import fu.sep490.g23.backend.service.classroom.*;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/student/classrooms")
@RequiredArgsConstructor
public class StudentClassroomController {

}
