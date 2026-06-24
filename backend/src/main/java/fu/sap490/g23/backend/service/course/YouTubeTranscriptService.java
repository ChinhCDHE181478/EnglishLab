package fu.sap490.g23.backend.service.course;

import fu.sap490.g23.backend.dto.response.course.TranscriptSegmentResponse;
import java.util.List;
import java.util.Optional;

public interface YouTubeTranscriptService {

    Optional<String> extractVideoId(String videoUrl);
    List<TranscriptSegmentResponse> fetchTranscriptSegments(String videoUrl);
}
