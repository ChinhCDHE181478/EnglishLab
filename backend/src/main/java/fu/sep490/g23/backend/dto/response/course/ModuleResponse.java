package fu.sep490.g23.backend.dto.response.course;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModuleResponse {
    private Long id;
    private String title;
    private String description;
    private Integer displayOrder;

    @Builder.Default
    private List<LessonResponse> lessons = new ArrayList<>();
}
