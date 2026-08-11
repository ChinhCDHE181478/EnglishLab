package fu.sep490.g23.backend.dto.response.payment;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class RevenueAnalyticsResponse {
    private long totalOrders;
    private long paidOrders;
    private long failedOrders;
    private long pendingOrders;
    private long totalRevenueVnd;
    private long totalDiscountVnd;
    private long totalCouponDiscountVnd;
    private List<RevenueByMonthResponse> monthlyRevenue;
}
