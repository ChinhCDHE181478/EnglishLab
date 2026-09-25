package fu.sep490.g23.backend.dto.response.home;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class HomeTestimonialResponse {
    private Long id;
    private String learnerName;
    private String courseTitle;
    private int rating;
    private String comment;
    private LocalDateTime reviewedAt;
}
