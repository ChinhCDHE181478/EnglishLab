package fu.sep490.g23.backend.dto.request.classroom;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.time.Instant;

@Data
public class CompletePracticeRequest {
    @Size(max = 10000, message = "Ghi chú luyện tập không được vượt quá 10000 ký tự")
    private String responseText;

    @Size(max = 50000, message = "Dữ liệu câu trả lời không được vượt quá 50000 ký tự")
    private String answersJson;

    @Min(value = 0, message = "Thời gian làm bài không hợp lệ")
    private Integer durationSeconds;

    private Instant startedAt;
}
