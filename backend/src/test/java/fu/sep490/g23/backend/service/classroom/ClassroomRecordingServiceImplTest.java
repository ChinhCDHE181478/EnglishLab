package fu.sep490.g23.backend.service.classroom;

import fu.sep490.g23.backend.dto.request.classroom.UpdateRecordingRequest;
import fu.sep490.g23.backend.entity.classroom.ClassSchedule;
import fu.sep490.g23.backend.entity.classroom.ClassSection;
import fu.sep490.g23.backend.entity.classroom.enums.RecordingSyncStatus;
import fu.sep490.g23.backend.repository.classroom.ClassScheduleRepository;
import fu.sep490.g23.backend.repository.classroom.ClassSectionRepository;
import fu.sep490.g23.backend.service.classroom.impl.ClassroomRecordingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClassroomRecordingServiceImplTest {

    @Mock private ClassSectionRepository classSectionRepository;
    @Mock private ClassScheduleRepository classScheduleRepository;
    @Mock private ClassroomMapper mapper;
    @Mock private VirtualMeetingService virtualMeetingService;

    private ClassroomRecordingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ClassroomRecordingServiceImpl(
                classSectionRepository,
                classScheduleRepository,
                mapper,
                virtualMeetingService,
                new GoogleMeetProperties()
        );
    }

    @Test
    void manualRecordingUrlBecomesReadyAndStaysPrivateUntilPublished() {
        ClassSchedule schedule = ClassSchedule.builder()
                .id(11L)
                .classSection(ClassSection.builder().build())
                .build();
        when(classScheduleRepository.findById(11L)).thenReturn(Optional.of(schedule));
        UpdateRecordingRequest request = new UpdateRecordingRequest();
        request.setRecordingUrl("https://drive.google.com/file/d/recording");
        request.setRecordingVisible(false);
        service.updateSessionRecording(11L, request);

        assertThat(schedule.getRecordingStatus()).isEqualTo(RecordingSyncStatus.READY);
        assertThat(schedule.getRecordingVisible()).isFalse();
        assertThat(schedule.getRecordingUrl()).contains("drive.google.com");
    }
}
