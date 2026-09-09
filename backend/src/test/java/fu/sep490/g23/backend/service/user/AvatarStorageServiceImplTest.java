package fu.sep490.g23.backend.service.user;

import fu.sep490.g23.backend.service.storage.LegacyLocalFileReader;
import fu.sep490.g23.backend.service.storage.ObjectStore;
import fu.sep490.g23.backend.service.user.impl.AvatarStorageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class AvatarStorageServiceImplTest {

    @TempDir
    Path tempDirectory;

    private ObjectStore objectStore;
    private LegacyLocalFileReader legacyLocalFileReader;
    private AvatarStorageServiceImpl service;

    @BeforeEach
    void setUp() {
        objectStore = mock(ObjectStore.class);
        legacyLocalFileReader = mock(LegacyLocalFileReader.class);
        service = new AvatarStorageServiceImpl(objectStore, legacyLocalFileReader);
    }

    @Test
    void store_WithValidPng_PersistsAndLoadsImage() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "profile.png", "image/png", validPng());

        String fileName = service.store(file);

        assertTrue(fileName.startsWith("avatar-"));
    }

    @Test
    void store_WithSpoofedImage_RejectsFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "profile.png",
                "image/png",
                "not-an-image".getBytes()
        );

        assertThrows(IllegalArgumentException.class, () -> service.store(file));
    }

    @Test
    void load_WithTraversalFileName_RejectsPath() {
        assertThrows(IllegalArgumentException.class, () -> service.load("avatar-../secret.png"));
    }

    private byte[] validPng() throws Exception {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, Color.RED.getRGB());
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }
}
