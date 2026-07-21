package fu.sap490.g23.backend.service.course;

import fu.sap490.g23.backend.dto.response.course.TranscriptSegmentResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TranscriptSegmentNormalizerTest {
    @Test
    void clipsRollingCaptionsAtTheNextStartTime() {
        List<TranscriptSegmentResponse> normalized = TranscriptSegmentNormalizer.normalize(List.of(
                segment(0.16, 5.20, "First caption"),
                segment(2.64, 7.52, "Second caption"),
                segment(5.20, 7.52, "Third caption")
        ));

        assertThat(normalized)
                .extracting(TranscriptSegmentResponse::getStartSeconds, TranscriptSegmentResponse::getEndSeconds)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(0.16, 2.64),
                        org.assertj.core.groups.Tuple.tuple(2.64, 5.20),
                        org.assertj.core.groups.Tuple.tuple(5.20, 7.52)
                );
    }

    @Test
    void sortsSegmentsAndKeepsTheMostCompleteDuplicateStart() {
        List<TranscriptSegmentResponse> normalized = TranscriptSegmentNormalizer.normalize(List.of(
                segment(4, 7, "Later"),
                segment(1, 5, "Short"),
                segment(1, 6, "More complete caption")
        ));

        assertThat(normalized).hasSize(2);
        assertThat(normalized.getFirst().getText()).isEqualTo("More complete caption");
        assertThat(normalized.getFirst().getEndSeconds()).isEqualTo(4);
    }

    private TranscriptSegmentResponse segment(double start, double end, String text) {
        return TranscriptSegmentResponse.builder()
                .startSeconds(start)
                .endSeconds(end)
                .text(text)
                .build();
    }
}
