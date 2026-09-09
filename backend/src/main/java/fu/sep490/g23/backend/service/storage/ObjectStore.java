package fu.sep490.g23.backend.service.storage;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Unified abstraction over the object storage layer used by the application.
 *
 * <p>Two implementations are provided out of the box:
 * <ul>
 *   <li>{@link R2StorageServiceImpl} – Cloudflare R2 (S3-compatible) for production.</li>
 *   <li>{@link LocalStorageServiceImpl} – Filesystem fallback used when R2 credentials
 *       are not configured (e.g. during local development or CI).</li>
 * </ul>
 *
 * <p>All operations are keyed by {@link #objectKey(String)} which combines a logical prefix
 * (e.g. {@code avatars/}) with the storage layer's own file name. Implementations decide
 * whether to expose objects publicly (via {@code publicBaseUrl}) or stream them via a backend
 * proxy (see {@link StoredObject}).
 */
public interface ObjectStore {

    /**
     * Uploads the given content under {@code objectKey}. Overwrites any existing object with
     * the same key. The returned {@link StoredObject} describes how the object should be
     * addressed by clients.
     */
    StoredObject put(String objectKey, InputStream content, long size, String contentType);

    /**
     * Downloads the object's bytes. Returns {@link Optional#empty()} if the object does not
     * exist.
     */
    Optional<byte[]> getBytes(String objectKey);

    /**
     * Returns the last-modified timestamp of the object, or empty if the object does not exist.
     */
    Optional<Instant> getLastModified(String objectKey);

    /**
     * Returns the size in bytes of the object, or empty if the object does not exist.
     */
    Optional<Long> getSize(String objectKey);

    /**
     * Deletes the object. Missing objects are not treated as errors.
     */
    void delete(String objectKey);

    /**
     * Returns every object key under {@code prefix} whose last-modified time is strictly
     * before {@code cutoff}. Used by the orphan-file cleanup scheduled job.
     */
    List<String> listKeysOlderThan(String prefix, Instant cutoff);

    /**
     * Best-effort content-type probe (extension + magic bytes) used when serving proxy
     * downloads. Implementations may return {@code application/octet-stream} when uncertain.
     */
    String probeContentType(String objectKey);

    /**
     * Composes the absolute URL exposed to clients for a public object.
     * For non-public buckets or when {@link ObjectStorageProperties#isR2PublicRead()} is
     * {@code false}, implementations should return the backend proxy URL instead so that
     * access checks can run.
     */
    String publicUrl(String objectKey);

    /**
     * Whether objects uploaded via this store are directly addressable (no auth) by clients.
     */
    boolean isPublic();

    /** Convenience for composing {@code prefix/filename} without worrying about double slashes. */
    default String objectKey(String prefix, String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("Tên tệp không được để trống.");
        }
        String normalizedPrefix = prefix == null ? "" : prefix.replaceAll("^/+", "").replaceAll("/+$", "");
        String normalizedName = fileName.replaceAll("^/+", "");
        if (normalizedPrefix.isEmpty()) {
            return normalizedName;
        }
        return normalizedPrefix + "/" + normalizedName;
    }

    /**
     * Result of a successful {@link #put(String, InputStream, long, String)} call.
     *
     * @param objectKey  storage key (including prefix)
     * @param publicUrl  URL that clients should use to fetch the object (may be the public
     *                   R2 URL or a backend proxy URL – implementations decide based on
     *                   {@link ObjectStore#isPublic()})
     * @param size       object size in bytes
     * @param contentType content type stored alongside the object
     */
    record StoredObject(String objectKey, String publicUrl, long size, String contentType) {
    }

    /** Carrier for download responses (kept here to avoid leaking framework types). */
    record DownloadedObject(String contentType, byte[] bytes) {
    }

    /** Thrown when an upload or other write to the underlying store fails irrecoverably. */
    class StorageException extends RuntimeException {
        public StorageException(String message) {
            super(message);
        }
        public StorageException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /** Sentinel for missing objects – mirrors AWS SDK behaviour but kept here for clarity. */
    static Optional<Duration> NO_MAX_AGE = Optional.empty();
}
