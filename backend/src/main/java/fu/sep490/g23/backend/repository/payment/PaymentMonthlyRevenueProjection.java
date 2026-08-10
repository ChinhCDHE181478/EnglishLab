package fu.sap490.g23.backend.repository.payment;

public interface PaymentMonthlyRevenueProjection {

    Integer getYearValue();

    Integer getMonthValue();

    Long getRevenueVnd();

    Long getOrderCount();
}
