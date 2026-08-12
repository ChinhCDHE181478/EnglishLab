package fu.sep490.g23.backend.dto.request.classroom;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveHomeworkAnnotationsRequest {

    @Valid
    @Size(max = 100, message = "Mỗi bài làm chỉ được có tối đa 100 nhận xét")
    private List<HomeworkTextAnnotationRequest> annotations;
}
