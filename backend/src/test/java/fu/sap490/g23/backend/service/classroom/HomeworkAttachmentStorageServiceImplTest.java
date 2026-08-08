package fu.sap490.g23.backend.service.classroom;

import fu.sap490.g23.backend.service.classroom.impl.HomeworkAttachmentStorageServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;

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
}
