package fu.sep490.g23.backend.dto.request.classroom;

import fu.sep490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ClassroomRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void sessionRejectsEndTimeNotAfterStartTime() {
        CreateClassroomSessionRequest request = CreateClassroomSessionRequest.builder()
                .sessionDate(LocalDate.of(2026, 8, 10))
                .startTime(LocalTime.of(20, 0))
                .endTime(LocalTime.of(18, 0))
                .build();

        assertThat(messages(validator.validate(request)))
                .contains("Giờ kết thúc phải sau giờ bắt đầu");
    }

    @Test
    void offeringRejectsInvalidDateAndPriceRanges() {
        CreateClassroomOfferingRequest request = CreateClassroomOfferingRequest.builder()
                .title("IELTS Evening")
                .deliveryMode(ClassroomDeliveryMode.OFFLINE)
                .maxCapacity(20)
                .startDate(LocalDate.of(2026, 9, 1))
                .endDate(LocalDate.of(2026, 8, 31))
                .price(new BigDecimal("1000000"))
                .salePrice(new BigDecimal("1500000"))
                .build();

        assertThat(messages(validator.validate(request)))
                .contains(
                        "Ngày kết thúc phải từ ngày bắt đầu trở đi",
                        "Giá ưu đãi không được lớn hơn học phí gốc"
                );
    }

    @Test
    void conflictCheckRejectsPartialOrInvertedScheduleWindow() {
        ConflictCheckRequest partial = ConflictCheckRequest.builder()
                .sessionDate(LocalDate.of(2026, 8, 10))
                .build();
        ConflictCheckRequest inverted = ConflictCheckRequest.builder()
                .sessionDate(LocalDate.of(2026, 8, 10))
                .startTime(LocalTime.of(20, 0))
                .endTime(LocalTime.of(18, 0))
                .build();

        assertThat(messages(validator.validate(partial)))
                .contains("Cần cung cấp đầy đủ ngày, giờ bắt đầu và giờ kết thúc khi kiểm tra lịch");
        assertThat(messages(validator.validate(inverted)))
                .contains("Giờ kết thúc phải sau giờ bắt đầu");
    }

    @Test
    void proposalRejectsInvalidDateAndTimeRanges() {
        CreateClassroomProposalRequest request = new CreateClassroomProposalRequest();
        request.setTitle("IELTS Evening");
        request.setCourseOfferingId(1L);
        request.setCapacity(20);
        request.setPlannedStartDate(LocalDate.of(2026, 9, 1));
        request.setPlannedEndDate(LocalDate.of(2026, 8, 31));
        request.setWeekdays(List.of(DayOfWeek.MONDAY));
        request.setSessionStartTime(LocalTime.of(20, 0));
        request.setSessionEndTime(LocalTime.of(18, 0));

        assertThat(messages(validator.validate(request)))
                .contains(
                        "Ngày kết thúc phải từ ngày bắt đầu trở đi",
                        "Giờ kết thúc phải sau giờ bắt đầu"
                );
    }

    private static Set<String> messages(Set<? extends ConstraintViolation<?>> violations) {
        return violations.stream()
                .map(ConstraintViolation::getMessage)
                .collect(java.util.stream.Collectors.toSet());
    }
}
