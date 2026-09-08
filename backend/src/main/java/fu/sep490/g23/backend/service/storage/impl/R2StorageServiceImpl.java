package fu.sep490.g23.backend.service.storage.impl;

import fu.sep490.g23.backend.config.ConditionalOnR2Enabled;
import fu.sep490.g23.backend.config.ObjectStorageProperties;
import fu.sep490.g23.backend.service.storage.ObjectStore;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Cloudflare R2 implementation of {@link ObjectStore}. R2 is S3-compatible, so we use the
 * official AWS SDK v2 S3 client with a custom endpoint. Path-style addressing and the
 * {@code auto} region are required by R2.
 */
@Service
@ConditionalOnR2Enabled
@Slf4j
public class R2StorageServiceImpl implements ObjectStore {

    private final ObjectStorageProperties properties;
    private S3Client client;

    public R2StorageServiceImpl(ObjectStorageProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void init() {
        validate();
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                require("englishlab.storage.r2-access-key", properties.getR2AccessKey()),
                require("englishlab.storage.r2-secret-key", properties.getR2SecretKey())
        );
        this.client = S3Client.builder()
                .endpointOverride(URI.create(require("englishlab.storage.r2-endpoint", properties.getR2Endpoint())))
                .region(Region.of(properties.getR2Region() == null ? "auto" : properties.getR2Region()))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(properties.isR2PathStyleAccess())
                        .build())
                .build();
        log.info("Initialized R2 storage client: endpoint={}, bucket={}, publicRead={}",
                properties.getR2Endpoint(), properties.getR2Bucket(), properties.isR2PublicRead());
    }

    @Override
    public StoredObject put(String objectKey, java.io.InputStream content, long size, String contentType) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(properties.getR2Bucket())
                    .key(objectKey)
                    .contentType(contentType == null ? "application/octet-stream" : contentType)
                    .contentLength(size)
                    .build();
            client.putObject(request, RequestBody.fromInputStream(content, size));
            return new StoredObject(objectKey, publicUrl(objectKey), size, contentType);
        } catch (SdkException exception) {
            throw new StorageException("Không thể tải tệp lên R2: " + objectKey, exception);
        }
    }

    @Override
    public Optional<byte[]> getBytes(String objectKey) {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(properties.getR2Bucket())
                    .key(objectKey)
                    .build();
            return Optional.of(client.getObject(request, ResponseTransformer.toBytes()).asByteArray());
        } catch (NoSuchKeyException exception) {
            return Optional.empty();
        } catch (SdkException exception) {
            throw new StorageException("Không thể tải tệp từ R2: " + objectKey, exception);
        }
    }

    @Override
    public Optional<Instant> getLastModified(String objectKey) {
        try {
            HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(properties.getR2Bucket())
                    .key(objectKey)
                    .build();
            HeadObjectResponse response = client.headObject(request);
            return Optional.of(response.lastModified());
        } catch (NoSuchKeyException exception) {
            return Optional.empty();
        } catch (SdkException exception) {
            throw new StorageException("Không thể kiểm tra tệp trên R2: " + objectKey, exception);
        }
    }

    @Override
    public Optional<Long> getSize(String objectKey) {
        try {
            HeadObjectResponse response = client.headObject(HeadObjectRequest.builder()
                    .bucket(properties.getR2Bucket())
                    .key(objectKey)
                    .build());
            return Optional.of(response.contentLength());
        } catch (NoSuchKeyException exception) {
            return Optional.empty();
        } catch (SdkException exception) {
            throw new StorageException("Không thể kiểm tra kích thước tệp trên R2: " + objectKey, exception);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(properties.getR2Bucket())
                    .key(objectKey)
                    .build();
            client.deleteObject(request);
        } catch (SdkException exception) {
            throw new StorageException("Không thể xóa tệp trên R2: " + objectKey, exception);
        }
    }

    @Override
    public List<String> listKeysOlderThan(String prefix, Instant cutoff) {
        List<String> result = new ArrayList<>();
        String continuationToken = null;
        do {
            ListObjectsV2Request.Builder builder = ListObjectsV2Request.builder()
                    .bucket(properties.getR2Bucket())
                    .prefix(prefix == null ? "" : prefix);
            if (continuationToken != null) {
                builder.continuationToken(continuationToken);
            }
            ListObjectsV2Response response = client.listObjectsV2(builder.build());
            for (S3Object object : response.contents()) {
                if (object.lastModified() != null && object.lastModified().isBefore(cutoff)) {
                    result.add(object.key());
                }
            }
            continuationToken = response.isTruncated() ? response.nextContinuationToken() : null;
        } while (continuationToken != null);
        return result;
    }

    @Override
    public String probeContentType(String objectKey) {
        String detected = probe(objectKey);
        return detected == null ? "application/octet-stream" : detected;
    }

    @Override
    public String publicUrl(String objectKey) {
        // Defensive normalization: objectKey should never start with a slash (objectKey()
        // strips it), but be tolerant of callers that pass one anyway. Never produce "//"
        // which most CDNs reject.
        String normalizedKey = (objectKey == null ? "" : objectKey).replaceAll("^/+", "");
        if (normalizedKey.isBlank()) {
            return null;
        }
        String base = properties.getR2PublicBaseUrl();
        if (base == null || base.isBlank()) {
            // Fall back to the canonical S3 URL form if no public base was configured.
            return String.format("%s/%s/%s", properties.getR2Endpoint(), properties.getR2Bucket(), normalizedKey);
        }
        String trimmed = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        return trimmed + "/" + normalizedKey;
    }

    @Override
    public boolean isPublic() {
        return properties.isR2PublicRead();
    }

    // --- helpers ------------------------------------------------------------------

    private void validate() {
        require("englishlab.storage.r2-endpoint", properties.getR2Endpoint());
        require("englishlab.storage.r2-access-key", properties.getR2AccessKey());
        require("englishlab.storage.r2-secret-key", properties.getR2SecretKey());
        require("englishlab.storage.r2-bucket", properties.getR2Bucket());
    }

    private static String require(String key, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Thiếu cấu hình R2: '" + key + "'. Hãy thiết lập biến môi trường tương ứng trong backend/.env.");
        }
        return value;
    }

    private String probe(String objectKey) {
        try {
            HeadObjectResponse head = client.headObject(HeadObjectRequest.builder()
                    .bucket(properties.getR2Bucket())
                    .key(objectKey)
                    .build());
            if (head.contentType() != null && !head.contentType().isBlank()) {
                return head.contentType();
            }
        } catch (SdkException ignored) {
            // fall through to extension-based guess
        }
        int dot = objectKey.lastIndexOf('.');
        if (dot < 0 || dot == objectKey.length() - 1) {
            return "application/octet-stream";
        }
        String extension = objectKey.substring(dot + 1).toLowerCase(Locale.ROOT);
        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "pdf" -> "application/pdf";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "ppt" -> "application/vnd.ms-powerpoint";
            case "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "txt" -> "text/plain";
            case "zip" -> "application/zip";
            case "rar" -> "application/vnd.rar";
            case "mp3" -> "audio/mpeg";
            case "m4a", "mp4" -> "audio/mp4";
            case "wav" -> "audio/wav";
            case "ogg", "oga" -> "audio/ogg";
            case "webm" -> "audio/webm";
            default -> "application/octet-stream";
        };
    }
}
