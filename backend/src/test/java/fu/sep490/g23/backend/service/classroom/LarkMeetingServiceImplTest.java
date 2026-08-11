package fu.sep490.g23.backend.service.classroom;

import fu.sep490.g23.backend.entity.classroom.enums.LarkMeetingStatus;
import fu.sep490.g23.backend.service.classroom.impl.LarkMeetingServiceImpl;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LarkMeetingServiceImplTest {

    private final LarkMeetingService service = new LarkMeetingServiceImpl(new LarkProperties());

    @Test
    void legacyGeneratedUrlsAreNeverJoinable() {
        String legacyUrl = "https://meet.larksuite.com/s/englishlab-toeic-650-showcase";

        assertThat(service.isDemoUrl(legacyUrl)).isTrue();
        assertThat(service.resolveStatus(legacyUrl)).isEqualTo(LarkMeetingStatus.NOT_CREATED);
        assertThat(service.isJoinable(legacyUrl, LarkMeetingStatus.OPEN)).isFalse();
    }

    @Test
    void larkApiUrlCanBeJoinedWhenMeetingIsOpen() {
        String apiUrl = "https://meetings.larksuite.com/s/abc123xyz";

        assertThat(service.isDemoUrl(apiUrl)).isFalse();
        assertThat(service.isJoinable(apiUrl, LarkMeetingStatus.OPEN)).isTrue();
        assertThat(service.isJoinable(apiUrl, LarkMeetingStatus.SCHEDULED)).isFalse();
    }
}
