package fu.sap490.g23.backend.dto.response.course;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CourseRatingResponse {
    private Long courseId;
    private Double averageRating;
    private Long reviewCount;
    private Integer myRating;
    private String myComment;
    private LocalDateTime updatedAt;
}
