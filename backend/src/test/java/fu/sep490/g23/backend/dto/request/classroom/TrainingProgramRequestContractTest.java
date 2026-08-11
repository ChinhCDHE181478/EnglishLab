package fu.sep490.g23.backend.dto.request.classroom;

import com.fasterxml.jackson.databind.ObjectMapper;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TrainingProgramRequestContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void readsCanonicalDeliveryType() throws Exception {
        TrainingProgramRequest request = objectMapper.readValue(
                "{\"title\":\"IELTS Foundation Offline\",\"deliveryType\":\"OFFLINE\",\"curriculumProgramId\":1}",
                TrainingProgramRequest.class
        );

        assertThat(request.getDeliveryType()).isEqualTo(ClassroomDeliveryMode.OFFLINE);
    }

    @Test
    void readsLegacyDeliveryModeDuringCompatibilityWindow() throws Exception {
        TrainingProgramRequest request = objectMapper.readValue(
                "{\"title\":\"IELTS Foundation Virtual\",\"deliveryMode\":\"VIRTUAL\",\"curriculumProgramId\":1}",
                TrainingProgramRequest.class
        );

        assertThat(request.getDeliveryType()).isEqualTo(ClassroomDeliveryMode.VIRTUAL);
    }
}
