package fu.sep490.g23.backend.service.user;

import fu.sep490.g23.backend.service.user.impl.AvatarStorageServiceImpl;
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

class AvatarStorageServiceImplTest {

    @TempDir
    Path tempDirectory;

    @Test
    void store_WithValidPng_PersistsAndLoadsImage() throws Exception {
        AvatarStorageServiceImpl service = new AvatarStorageServiceImpl(tempDirectory.toString());
        MockMultipartFile file = new MockMultipartFile("file", "profile.png", "image/png", validPng());

        String fileName = service.store(file);

        assertTrue(fileName.startsWith("avatar-"));
        assertTrue(service.load(fileName).exists());
        service.delete(fileName);
        assertThrows(IllegalArgumentException.class, () -> service.load(fileName));
    }

    @Test
    void store_WithSpoofedImage_RejectsFile() {
        AvatarStorageServiceImpl service = new AvatarStorageServiceImpl(tempDirectory.toString());
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
        AvatarStorageServiceImpl service = new AvatarStorageServiceImpl(tempDirectory.toString());

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
