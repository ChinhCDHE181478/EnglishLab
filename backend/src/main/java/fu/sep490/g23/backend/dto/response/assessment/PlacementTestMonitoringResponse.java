package fu.sep490.g23.backend.dto.response.assessment;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Value
@Builder
public class PlacementTestMonitoringResponse {
    String examType;
    long totalAttempts;
    long uniqueParticipants;
    long completedAttempts;
    BigDecimal averageOverallBand;
    BigDecimal averageListeningBand;
    BigDecimal averageReadingBand;
    BigDecimal averageWritingBand;
    BigDecimal averageSpeakingBand;
    List<BandDistributionItem> bandDistribution;
    List<RecentAttempt> recentAttempts;

    @Value
    @Builder
    public static class BandDistributionItem {
        String label;
        long count;
    }

    @Value
    @Builder
    public static class RecentAttempt {
        Long id;
        String examType;
        String learnerName;
        String learnerEmail;
        BigDecimal overallBand;
        BigDecimal listeningBand;
        BigDecimal readingBand;
        BigDecimal writingBand;
        BigDecimal speakingBand;
        String status;
        LocalDateTime submittedAt;
    }
}
