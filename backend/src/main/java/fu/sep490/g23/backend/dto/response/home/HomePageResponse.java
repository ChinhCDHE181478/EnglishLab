package fu.sep490.g23.backend.dto.response.home;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class HomePageResponse {
    private List<HomeTeacherResponse> teachers;
    private List<HomeTestimonialResponse> testimonials;
}
