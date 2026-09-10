package fu.sep490.g23.backend.service.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Read-only accessor for files that were uploaded before the Cloudflare R2 migration.
 *
 * <p>The pre-migration backend served avatars, course thumbnails, homework attachments and
 * assessment audio directly from disk. After the migration, new uploads go to R2 but we still
 * need to be able to serve previously-stored files using the same URLs – this helper loads
 * them from a legacy directory tree such as {@code backend/uploads/avatars/}.
 *
 * <p>Behaviour:
 * <ul>
 *   <li>When the legacy base dir is not configured (null/blank) the helper is a no-op.</li>
 *   <li>All paths are normalised and confined to the base directory to prevent traversal.</li>
 *   <li>Files that no longer exist on disk return {@link Optional#empty()} – callers should
 *       then fall back to the object store.</li>
 * </ul>
 */
@Component
public class LegacyLocalFileReader {

    private final Path baseDirectory;

    public LegacyLocalFileReader(
            @Value("${englishlab.storage.legacy-local-base-dir:}") String baseDir
    ) {
        this.baseDirectory = (baseDir == null || baseDir.isBlank())
                ? null
                : Paths.get(baseDir).toAbsolutePath().normalize();
    }

    /**
     * Attempts to load a file from {@code <legacyBase>/<subDir>/<fileName>}.
     *
     * @param subDir   logical prefix that mirrors the R2 object prefix (e.g. {@code avatars})
     * @param fileName the file name as it appears in the legacy URL (e.g. {@code avatar-uuid.jpg})
     */
    public Optional<Resource> tryLoad(String subDir, String fileName) {
        if (baseDirectory == null || fileName == null || fileName.isBlank()) {
            return Optional.empty();
        }
        if (fileName.contains("/") || fileName.contains("\\") || fileName.contains("..")) {
            return Optional.empty();
        }
        String prefix = subDir == null ? "" : subDir.replaceAll("^/+", "").replaceAll("/+$", "");
        Path target;
        if (prefix.isEmpty()) {
            target = baseDirectory.resolve(fileName).normalize();
        } else {
            target = baseDirectory.resolve(prefix).resolve(fileName).normalize();
        }
        if (!target.startsWith(baseDirectory) || !Files.isRegularFile(target)) {
            return Optional.empty();
        }
        return Optional.of(new FileSystemResource(target));
    }

    /** Probe content type via the JDK's {@link Files#probeContentType} – falls back to application/octet-stream. */
    public String probeContentType(String subDir, String fileName) {
        if (baseDirectory == null || fileName == null || fileName.isBlank()) {
            return "application/octet-stream";
        }
        if (fileName.contains("/") || fileName.contains("\\") || fileName.contains("..")) {
            return "application/octet-stream";
        }
        String prefix = subDir == null ? "" : subDir.replaceAll("^/+", "").replaceAll("/+$", "");
        Path target;
        if (prefix.isEmpty()) {
            target = baseDirectory.resolve(fileName).normalize();
        } else {
            target = baseDirectory.resolve(prefix).resolve(fileName).normalize();
        }
        if (!target.startsWith(baseDirectory)) {
            return "application/octet-stream";
        }
        try {
            String detected = Files.probeContentType(target);
            return detected == null ? "application/octet-stream" : detected;
        } catch (java.io.IOException exception) {
            return "application/octet-stream";
        }
    }

    /** Returns true if a legacy local reader has been configured. */
    public boolean isEnabled() {
        return baseDirectory != null;
    }
}
