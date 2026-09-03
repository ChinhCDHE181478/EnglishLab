package fu.sep490.g23.backend.service.classroom;

import fu.sep490.g23.backend.entity.classroom.ClassSchedule;
import fu.sep490.g23.backend.entity.classroom.ClassSection;
import fu.sep490.g23.backend.entity.classroom.enums.GoogleMeetStatus;
import fu.sep490.g23.backend.repository.classroom.ClassSectionRepository;
import fu.sep490.g23.backend.service.classroom.impl.GoogleMeetServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class GoogleMeetServiceImplTest {

    @Mock private TeacherGoogleMeetConnectionService connectionService;
    @Mock private ClassSectionRepository classSectionRepository;

    private GoogleMeetProperties properties;
    private GoogleMeetServiceImpl service;

    @BeforeEach
    void setUp() {
        properties = new GoogleMeetProperties();
        service = new GoogleMeetServiceImpl(properties, connectionService, classSectionRepository);
    }

    @Test
    void acceptsOnlyGoogleMeetHttpsUrls() {
        assertThat(service.isGoogleMeetUrl("https://meet.google.com/abc-defg-hij")).isTrue();
        assertThat(service.isGoogleMeetUrl("https://meet.google.com/englishlab-sheet-class-1")).isFalse();
        assertThat(service.isGoogleMeetUrl("https://meet.google.com/_meet/whoops?sc=232")).isFalse();
        assertThat(service.isGoogleMeetUrl("https://meet.larksuite.com/abc")).isFalse();
        assertThat(service.isGoogleMeetUrl("http://meet.google.com/abc")).isFalse();
        assertThat(service.isGoogleMeetUrl(null)).isFalse();
    }

    @Test
    void classRoomIsJoinableOnlyWhenReady() {
        ClassSection section = ClassSection.builder()
                .googleMeetStatus(GoogleMeetStatus.READY)
                .googleMeetUrl("https://meet.google.com/abc-defg-hij")
                .build();

        assertThat(service.isJoinable(section)).isTrue();
        section.setGoogleMeetStatus(GoogleMeetStatus.FAILED);
        assertThat(service.isJoinable(section)).isFalse();
    }

    @Test
    void recordingRequiresEnabledIntegration() {
        ClassSchedule schedule = ClassSchedule.builder()
                .classSection(ClassSection.builder().build())
                .build();

        assertThatThrownBy(() -> service.getRecording(schedule))
                .isInstanceOf(IllegalStateException.class);
    }
}
