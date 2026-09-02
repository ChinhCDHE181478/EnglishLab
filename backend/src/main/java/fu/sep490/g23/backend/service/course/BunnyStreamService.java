package fu.sep490.g23.backend.service.course;

import fu.sep490.g23.backend.dto.response.course.BunnyVideoUploadResponse;
import fu.sep490.g23.backend.dto.response.course.TranscriptSegmentResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

public interface BunnyStreamService {

    BunnyVideoUploadResponse uploadVideo(MultipartFile file, String title);

    Optional<BunnyVideoRef> resolveVideoRef(String videoUrl);

    List<TranscriptSegmentResponse> fetchTranscriptSegments(String libraryId, String videoId);

    record BunnyVideoRef(String libraryId, String videoId) {
    }
}
