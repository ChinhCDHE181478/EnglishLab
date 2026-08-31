package fu.sep490.g23.backend.service.classroom;

import fu.sep490.g23.backend.service.classroom.impl.HomeworkAttachmentStorageServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.assertj.core.api.Assertions.assertThat;

class HomeworkAttachmentStorageServiceImplTest {

    @TempDir
    Path storageDirectory;

    @Test
    void store_appliesPerUserHourlyRateLimit() {
        HomeworkAttachmentStorageServiceImpl service = new HomeworkAttachmentStorageServiceImpl(
                storageDirectory.toString(),
                100L * 1024 * 1024,
                1,
                100L * 1024 * 1024
        );
        MockMultipartFile first = new MockMultipartFile("file", "first.pdf", "application/pdf", new byte[]{1});
        MockMultipartFile second = new MockMultipartFile("file", "second.pdf", "application/pdf", new byte[]{2});

        service.store(first, "/api/classroom-homework/attachments", "learner@example.com");

        assertThrows(
                IllegalArgumentException.class,
                () -> service.store(second, "/api/classroom-homework/attachments", "learner@example.com")
        );
    }

    @Test
    void load_rejectsPathTraversal() {
        HomeworkAttachmentStorageServiceImpl service = new HomeworkAttachmentStorageServiceImpl(
                storageDirectory.toString(),
                100L * 1024 * 1024,
                10,
                100L * 1024 * 1024
        );

        assertThrows(IllegalArgumentException.class, () -> service.load("../secret.pdf"));
    }

    @Test
    void loadStoredAttachmentFromUrl_loadsUploadedAudioWithAudioMimeType() {
        HomeworkAttachmentStorageServiceImpl service = new HomeworkAttachmentStorageServiceImpl(
                storageDirectory.toString(),
                100L * 1024 * 1024,
                10,
                100L * 1024 * 1024
        );
        byte[] audioBytes = new byte[]{1, 2, 3, 4};
        MockMultipartFile audio = new MockMultipartFile(
                "file", "answer.webm", "video/webm", audioBytes
        );
        String url = service.store(
                audio,
                "/api/classroom-homework/attachments",
                "learner@example.com"
        ).getUrl();

        HomeworkAttachmentStorageService.StoredHomeworkAttachment stored = service
                .loadStoredAttachmentFromUrl(url)
                .orElseThrow();

        assertThat(stored.contentType()).isEqualTo("audio/webm");
        assertThat(stored.bytes()).containsExactly(audioBytes);
        assertThat(service.loadStoredAttachmentFromUrl("https://example.com/answer.webm")).isEmpty();
    }
}
