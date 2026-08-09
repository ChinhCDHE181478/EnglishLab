package fu.sap490.g23.backend.service.classroom;

import org.springframework.core.io.Resource;

public interface HomeworkAttachmentAccessService {

    Resource loadAuthorized(String fileName, String requesterEmail);

    String contentType(String fileName);

    void assertLearnerUploadAccess(Long homeworkId, String learnerEmail);
}
