package fu.sep490.g23.backend.service.assessment;

import fu.sep490.g23.backend.dto.response.assessment.AssessmentAudioUploadResponse;
import java.util.Optional;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface AssessmentAudioStorageService {

    AssessmentAudioUploadResponse store(MultipartFile file, String publicUrlBase);

    Resource loadAsResource(String fileName);

    String detectContentType(String fileName);

    Optional<StoredAssessmentAudio> loadStoredAudioFromUrl(String audioUrl);

    record StoredAssessmentAudio(String fileName, String contentType, long size, byte[] bytes) {
    }
}
