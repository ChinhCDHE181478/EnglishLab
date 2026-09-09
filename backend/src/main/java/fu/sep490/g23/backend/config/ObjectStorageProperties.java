package fu.sep490.g23.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the object storage layer (Cloudflare R2 by default, with a local filesystem
 * fallback for development when R2 credentials are not configured).
 *
 * <p>All fields are bound from the {@code englishlab.storage.*} prefix in {@code application.properties},
 * which in turn pulls values from environment variables / {@code .env}. See {@code .env.example}
 * for the full list of supported variables.
 */
@ConfigurationProperties(prefix = "englishlab.storage")
@Getter
@Setter
public class ObjectStorageProperties {

    /**
     * When true, the local filesystem backend is always used, even when R2 credentials
     * are configured. Useful for development or testing without cloud access.
     */
    private boolean localEnabled = false;

    /**
     * Selection of R2 vs local filesystem is automatic via {@code @ConditionalOnProperty}
     * on the implementations themselves: {@code LocalStorageServiceImpl} activates when
     * {@code localEnabled=true} OR when R2 credentials are absent; {@code R2StorageServiceImpl}
     * otherwise.
     */
    /** Base directory for the {@code local} backend. */
    private String localBaseDir = "backend/uploads";

    /**
     * When set, the storage services will first try to read files from this directory before
     * hitting the object store. Useful during the migration window so existing URLs continue
     * to work after the backend switches to R2.
     *
     * <p>Leave blank to disable the fallback. The directory layout mirrors the R2 object
     * prefixes: {@code <base>/avatars/}, {@code <base>/course-thumbnails/},
     * {@code <base>/classroom-attachments/}, {@code <base>/assessment-audio/}.
     */
    private String legacyLocalBaseDir = "";

    // --- R2 / S3 fields ---------------------------------------------------------

    /** S3-compatible endpoint, e.g. {@code https://<account-id>.r2.cloudflarestorage.com}. */
    private String r2Endpoint;

    /** AWS-style region. Cloudflare R2 accepts {@code auto} or {@code us-east-1}. */
    private String r2Region = "auto";

    /** R2 access key id (token). */
    private String r2AccessKey;

    /** R2 secret access key. */
    private String r2SecretKey;

    /** Target bucket name. */
    private String r2Bucket;

    /**
     * Public base URL used to build absolute URLs for objects exposed publicly
     * (avatars, course thumbnails). For Cloudflare R2 + a public bucket this is the
     * {@code .r2.dev} dev URL or a custom domain.
     */
    private String r2PublicBaseUrl;

    /**
     * When true, uploaded objects are served from {@link #r2PublicBaseUrl} and downloads
     * bypass the backend. When false, downloads are streamed through the backend
     * proxy (used for private objects such as homework attachments or audio recordings).
     */
    private boolean r2PublicRead = true;

    /** Path-style addressing is required by R2; do not change unless you know what you are doing. */
    private boolean r2PathStyleAccess = true;

    /**
     * Returns true when R2 is configured with all required fields.
     */
    public boolean hasR2Credentials() {
        return r2Endpoint != null && !r2Endpoint.isBlank()
                && r2AccessKey != null && !r2AccessKey.isBlank()
                && r2SecretKey != null && !r2SecretKey.isBlank()
                && r2Bucket != null && !r2Bucket.isBlank();
    }
}
