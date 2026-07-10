package fu.sap490.g23.backend.dto.request.classroom;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class GenerateSessionsFromTemplateRequest {
    @NotNull(message = "Vui lòng chọn mẫu lịch.")
    private Long templateId;
    @NotNull(message = "Vui lòng chọn ngày bắt đầu.")
    private LocalDate startDate;
    @NotNull(message = "Vui lòng nhập số tuần.")
    @Min(value = 1, message = "Số tuần phải lớn hơn 0.")
    private Integer weeks;
}
