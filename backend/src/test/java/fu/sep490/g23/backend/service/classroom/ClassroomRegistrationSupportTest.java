package fu.sep490.g23.backend.service.classroom;

import fu.sep490.g23.backend.entity.classroom.ClassEnrollment;
import fu.sep490.g23.backend.entity.classroom.ClassSection;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassroomRegistrationSupportTest {

    @Test
    void regularRegistration_requiresBalanceSevenDaysBeforeClass() {
        LocalDate startDate = LocalDate.of(2026, 10, 20);
        ClassEnrollment enrollment = enrollment(startDate, LocalDateTime.of(2026, 10, 1, 9, 0));

        assertEquals(
                LocalDateTime.of(startDate.minusDays(7), LocalTime.MAX),
                ClassroomRegistrationSupport.tuitionPaymentDeadline(enrollment)
        );
        assertFalse(ClassroomRegistrationSupport.requiresFullTuitionPayment(enrollment));
    }

    @Test
    void lateApproval_requiresFullPaymentWithinTwentyFourHours() {
        LocalDateTime invitedAt = LocalDateTime.of(2026, 10, 15, 9, 30);
        ClassEnrollment enrollment = enrollment(LocalDate.of(2026, 10, 20), invitedAt);

        assertTrue(ClassroomRegistrationSupport.requiresFullTuitionPayment(enrollment));
        assertEquals(
                invitedAt.plusHours(ClassroomRegistrationSupport.LATE_APPROVAL_PAYMENT_HOURS),
                ClassroomRegistrationSupport.tuitionPaymentDeadline(enrollment)
        );
    }

    @Test
    void unpaidBalance_isOverdueAfterComputedDeadline() {
        ClassEnrollment enrollment = enrollment(
                LocalDate.of(2026, 10, 20),
                LocalDateTime.of(2026, 10, 1, 9, 0)
        );

        assertTrue(ClassroomRegistrationSupport.isTuitionPaymentOverdue(
                enrollment,
                LocalDateTime.of(2026, 10, 14, 0, 0)
        ));
    }

    private ClassEnrollment enrollment(LocalDate startDate, LocalDateTime enrolledAt) {
        return ClassEnrollment.builder()
                .classSection(ClassSection.builder().startDate(startDate).build())
                .enrolledAt(enrolledAt)
                .tuitionAmountDue(new BigDecimal("5000000"))
                .tuitionAmountPaid(BigDecimal.ZERO)
                .build();
    }
}
