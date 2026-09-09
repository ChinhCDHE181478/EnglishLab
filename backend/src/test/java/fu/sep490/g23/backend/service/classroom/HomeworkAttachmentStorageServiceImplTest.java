package fu.sep490.g23.backend.service.classroom;

import fu.sep490.g23.backend.service.classroom.impl.HomeworkAttachmentStorageServiceImpl;
import fu.sep490.g23.backend.service.storage.LegacyLocalFileReader;
import fu.sep490.g23.backend.service.storage.ObjectStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        when(objectStore.objectKey(anyString(), anyString())).thenCallRealMethod();
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
        when(objectStore.put(anyString(), any(InputStream.class), anyLong(), anyString()))
                .thenAnswer(invocation -> new ObjectStore.StoredObject(
                        invocation.getArgument(0),
                        "https://files.example.test/" + invocation.<String>getArgument(0),
                        invocation.getArgument(2),
                        invocation.getArgument(3)
                ));

        service.store(first, "/api/classroom-homework/attachments", "learner@example.com");

        assertThrows(
                IllegalArgumentException.class,
                () -> service.store(second, "/api/classroom-homework/attachments", "learner@example.com")
        );
    }

    @Test
    void store_returnsTheR2PublicUrlWithoutRewritingIt() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "lesson.png",
                "image/png",
                new byte[]{1, 2, 3}
        );
        String publicUrl = "https://files.example.test/classroom-attachments/homework-test.png";
        when(objectStore.put(anyString(), any(InputStream.class), anyLong(), anyString()))
                .thenAnswer(invocation -> new ObjectStore.StoredObject(
                        invocation.getArgument(0),
                        publicUrl,
                        invocation.getArgument(2),
                        invocation.getArgument(3)
                ));

        var uploaded = service.store(
                file,
                "https://englishlab.io.vn/api/classroom-homework/attachments",
                "manager@englishlab.vn"
        );

        assertThat(uploaded.getUrl()).isEqualTo(publicUrl);
        verify(objectStore).put(
                org.mockito.ArgumentMatchers.startsWith("classroom-attachments/homework-"),
                any(InputStream.class),
                org.mockito.ArgumentMatchers.eq(3L),
                org.mockito.ArgumentMatchers.eq("image/png")
        );
    }

    @Test
    void load_rejectsPathTraversal() {
        assertThrows(IllegalArgumentException.class, () -> service.load("../secret.pdf"));
    }
}
