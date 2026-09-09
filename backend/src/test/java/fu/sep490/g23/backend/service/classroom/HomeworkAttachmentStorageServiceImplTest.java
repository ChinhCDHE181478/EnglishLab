package fu.sep490.g23.backend.service.classroom;

import fu.sep490.g23.backend.service.classroom.impl.HomeworkAttachmentStorageServiceImpl;
import fu.sep490.g23.backend.service.storage.LegacyLocalFileReader;
import fu.sep490.g23.backend.service.storage.ObjectStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class HomeworkAttachmentStorageServiceImplTest {

    @TempDir
    Path storageDirectory;

    private ObjectStore objectStore;
    private LegacyLocalFileReader legacyLocalFileReader;
    private HomeworkAttachmentStorageServiceImpl service;

    @BeforeEach
    void setUp() {
        objectStore = mock(ObjectStore.class);
        legacyLocalFileReader = mock(LegacyLocalFileReader.class);
        service = new HomeworkAttachmentStorageServiceImpl(
                objectStore,
                legacyLocalFileReader,
                100L * 1024 * 1024,
                1,
                100L * 1024 * 1024
        );
    }

    @Test
    void store_appliesPerUserHourlyRateLimit() {
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
        assertThrows(IllegalArgumentException.class, () -> service.load("../secret.pdf"));
    }
}
