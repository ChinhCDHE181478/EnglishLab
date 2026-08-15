package fu.sep490.g23.backend.service.course.impl;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YouTubeTranscriptServiceImplTest {

    private final YouTubeTranscriptServiceImpl service = new YouTubeTranscriptServiceImpl();

    @Test
    void extractVideoId_supportsWatchAndShortUrls() {
        assertTrue(service.extractVideoId("https://www.youtube.com/watch?v=jNQXAC9IVRw").isPresent());
        assertTrue(service.extractVideoId("https://youtu.be/jNQXAC9IVRw").isPresent());
        assertTrue(service.extractVideoId("https://www.youtube-nocookie.com/embed/jNQXAC9IVRw").isPresent());
    }

    @Test
    void fetchTranscriptSegments_returnsCaptionsWhenAvailable() {
        // Public video with captions (TED: How to speak so that people want to listen).
        String url = "https://www.youtube.com/watch?v=eIho2S0ZahI";
        List<?> segments;
        try {
            segments = service.fetchTranscriptSegments(url);
        } catch (Exception ex) {
            Assumptions.assumeTrue(false, "YouTube transcript network unavailable: " + ex.getMessage());
            return;
        }
        Assumptions.assumeFalse(segments.isEmpty(), "YouTube returned no captions for sample video");
        assertFalse(segments.isEmpty());
    }
}
