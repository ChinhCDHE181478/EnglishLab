package fu.sep490.g23.backend.service.classroom;

import fu.sep490.g23.backend.dto.response.classroom.ClassroomHomeworkResponse;
import fu.sep490.g23.backend.entity.classroom.ClassroomHomework;
import fu.sep490.g23.backend.entity.classroom.ClassroomOffering;
import fu.sep490.g23.backend.entity.classroom.enums.HomeworkActivityType;
import fu.sep490.g23.backend.entity.classroom.enums.HomeworkGradingMode;
import fu.sep490.g23.backend.repository.classroom.ClassroomEnrollmentRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomHomeworkSubmissionRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomSessionRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomTeacherAssignmentRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomTuitionPaymentRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ClassroomMapperHomeworkGradingModeTest {

    @Test
    void toHomeworkResponse_UsesPersistedGradingModeInsteadOfInferringItFromContent() {
        ClassroomMapper mapper = new ClassroomMapper(
                new HomeworkTextAnnotationCodec(),
                mock(ClassroomEnrollmentRepository.class),
                mock(ClassroomTeacherAssignmentRepository.class),
                mock(ClassroomHomeworkSubmissionRepository.class),
                mock(ClassroomHomeworkGradingCatalogService.class),
                mock(ClassroomTuitionPaymentRepository.class),
                mock(ClassroomSessionRepository.class),
                mock(VirtualMeetingService.class),
                new ClassroomHomeworkObjectiveGrader()
        );
        ClassroomHomework homework = ClassroomHomework.builder()
                .id(10L)
                .classroomOffering(ClassroomOffering.builder().id(20L).build())
                .activityType(HomeworkActivityType.SKILL_PRACTICE)
                .activityConfigJson("{\"answerKey\":{\"1\":\"A\"}}")
                .gradingMode(HomeworkGradingMode.TEACHER)
                .build();

        ClassroomHomeworkResponse response = mapper.toHomeworkResponse(homework, null);

        assertThat(response.getGradingMode()).isEqualTo(HomeworkGradingMode.TEACHER);
    }
}
