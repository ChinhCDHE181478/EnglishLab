package fu.sep490.g23.backend.dto.response.home;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class HomeTeacherResponse {
    private Long id;
    private String name;
    private String avatarUrl;
    private String headline;
    private String biography;
    private String specializations;
    private Integer yearsOfExperience;
    private String highestQualification;
    private List<String> badges;
}
