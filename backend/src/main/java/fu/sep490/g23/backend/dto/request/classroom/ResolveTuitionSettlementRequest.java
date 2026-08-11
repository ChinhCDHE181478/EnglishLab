package fu.sep490.g23.backend.dto.request.classroom;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ResolveTuitionSettlementRequest {

    /** APPROVE_REFUND | REJECT_REFUND */
    @NotBlank(message = "Vui long chon thao tac xu ly settlement.")
    @Size(max = 40)
    private String action;

    @Size(max = 700, message = "Ghi chu toi da 700 ky tu.")
    private String note;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
