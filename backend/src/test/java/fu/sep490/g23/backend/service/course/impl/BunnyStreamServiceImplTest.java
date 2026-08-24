package fu.sep490.g23.backend.service.course.impl;

import fu.sep490.g23.backend.dto.response.course.TranscriptSegmentResponse;
import fu.sep490.g23.backend.service.course.BunnyStreamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BunnyStreamServiceImplTest {

    private final BunnyStreamServiceImpl service = new BunnyStreamServiceImpl();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "libraryId", "729032");
        ReflectionTestUtils.setField(service, "cdnHostname", "vz-example.b-cdn.net");
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
    }

    @Test
    void resolveVideoRef_parsesMediadeliveryEmbedUrl() {
        Optional<BunnyStreamService.BunnyVideoRef> ref = service.resolveVideoRef(
                "https://iframe.mediadelivery.net/embed/729032/bc1feea2-7cc5-46b5-9073-aaaaaaaaaaaa",
                null,
                null
        );
        assertTrue(ref.isPresent());
        assertEquals("729032", ref.get().libraryId());
        assertEquals("bc1feea2-7cc5-46b5-9073-aaaaaaaaaaaa", ref.get().videoId());
    }

    @Test
    void resolveVideoRef_prefersExplicitBunnyIds() {
        Optional<BunnyStreamService.BunnyVideoRef> ref = service.resolveVideoRef(
                "https://www.youtube.com/watch?v=abc",
                "video-guid",
                "111"
        );
        assertTrue(ref.isPresent());
        assertEquals("111", ref.get().libraryId());
        assertEquals("video-guid", ref.get().videoId());
    }

    @Test
    void parseWebVtt_extractsTimedSegments() {
        String vtt = """
                WEBVTT

                00:00:00.000 --> 00:00:02.500
                Hello world

                00:00:02.500 --> 00:00:05.000
                Next <b>line</b>
                """;
        List<TranscriptSegmentResponse> segments = service.parseWebVtt(vtt);
        assertEquals(2, segments.size());
        assertEquals(0.0, segments.get(0).getStartSeconds());
        assertEquals(2.5, segments.get(0).getEndSeconds());
        assertEquals("Hello world", segments.get(0).getText());
        assertEquals("Next line", segments.get(1).getText());
        assertFalse(segments.isEmpty());
    }
}
