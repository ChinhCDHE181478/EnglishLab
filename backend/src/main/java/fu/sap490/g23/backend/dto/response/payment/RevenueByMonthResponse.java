package fu.sap490.g23.backend.dto.response.payment;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RevenueByMonthResponse {
    private String month;
    private long revenueVnd;
    private long orderCount;
}
