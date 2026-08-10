package fu.sap490.g23.backend.dto.request.support;

import fu.sap490.g23.backend.entity.support.enums.SupportTicketCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateSupportTicketRequest {

    @NotBlank(message = "Chủ đề không được để trống.")
    @Size(min = 5, max = 160, message = "Chủ đề phải có từ 5 đến 160 ký tự.")
    private String subject;

    @NotNull(message = "Vui lòng chọn nhóm hỗ trợ.")
    private SupportTicketCategory category;

    @NotBlank(message = "Nội dung không được để trống.")
    @Size(min = 10, max = 5000, message = "Nội dung phải có từ 10 đến 5000 ký tự.")
    private String message;
}
