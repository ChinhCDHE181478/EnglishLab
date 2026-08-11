package fu.sep490.g23.backend.service.course;

import fu.sep490.g23.backend.dto.response.course.TranscriptSegmentResponse;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class TranscriptSegmentNormalizer {
    private static final double EPSILON = 0.000001d;

    private TranscriptSegmentNormalizer() {
    }

    /**
     * Converts rolling captions into a sorted, non-overlapping timeline.
     * YouTube commonly returns the next caption before the previous display
     * window ends; the next start time is the stable boundary between cues.
     */
    public static List<TranscriptSegmentResponse> normalize(List<TranscriptSegmentResponse> segments) {
        if (segments == null || segments.isEmpty()) {
            return List.of();
        }

        List<TranscriptSegmentResponse> sorted = segments.stream()
                .filter(TranscriptSegmentNormalizer::isValid)
                .map(TranscriptSegmentNormalizer::copy)
                .sorted(Comparator.comparing(TranscriptSegmentResponse::getStartSeconds)
                        .thenComparing(TranscriptSegmentResponse::getEndSeconds))
                .toList();
        List<TranscriptSegmentResponse> normalized = new ArrayList<>();

        for (TranscriptSegmentResponse current : sorted) {
            if (normalized.isEmpty()) {
                normalized.add(current);
                continue;
            }

            TranscriptSegmentResponse previous = normalized.getLast();
            if (Math.abs(current.getStartSeconds() - previous.getStartSeconds()) <= EPSILON) {
                previous.setEndSeconds(Math.max(previous.getEndSeconds(), current.getEndSeconds()));
                if (current.getText().length() > previous.getText().length()) {
                    previous.setText(current.getText());
                }
                continue;
            }

            if (current.getStartSeconds() < previous.getEndSeconds()) {
                previous.setEndSeconds(current.getStartSeconds());
            }
            normalized.add(current);
        }

        return List.copyOf(normalized);
    }

    private static boolean isValid(TranscriptSegmentResponse segment) {
        return segment != null
                && segment.getText() != null
                && !segment.getText().isBlank()
                && segment.getStartSeconds() != null
                && segment.getEndSeconds() != null
                && Double.isFinite(segment.getStartSeconds())
                && Double.isFinite(segment.getEndSeconds())
                && segment.getStartSeconds() >= 0
                && segment.getEndSeconds() > segment.getStartSeconds();
    }

    private static TranscriptSegmentResponse copy(TranscriptSegmentResponse segment) {
        return TranscriptSegmentResponse.builder()
                .startSeconds(segment.getStartSeconds())
                .endSeconds(segment.getEndSeconds())
                .text(segment.getText().trim())
                .build();
    }
}
