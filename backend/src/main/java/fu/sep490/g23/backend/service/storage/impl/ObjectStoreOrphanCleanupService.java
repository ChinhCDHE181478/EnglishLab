package fu.sep490.g23.backend.service.storage.impl;

import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.assessment.AssessmentSubmissionRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseRepository;
import fu.sep490.g23.backend.service.storage.ObjectStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Periodic janitor that walks the {@code avatars/}, {@code course-thumbnails/} and
 * {@code assessment-audio/} prefixes in the object store and deletes any object that is
 * no longer referenced by the corresponding database column.
 *
 * <p>This is the R2-side analogue of {@code HomeworkAttachmentCleanupServiceImpl} – homework
 * attachments already have their own cleanup because of quota bookkeeping, so they are not
 * touched here.
 *
 * <p>Activation: enable via {@code englishlab.storage.orphan-cleanup.enabled=true}.
 * Retention delay is configurable; the default is 7 days, which gives admin/UI flows enough
 * time to undo accidental deletes before the file is reclaimed.
 */
@Service
@ConditionalOnProperty(name = "englishlab.storage.orphan-cleanup.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class ObjectStoreOrphanCleanupService {

    private static final String AVATAR_PREFIX = "avatars";
    private static final String THUMBNAIL_PREFIX = "course-thumbnails";
    private static final String AUDIO_PREFIX = "assessment-audio";

    private final ObjectStore objectStore;
    private final UserRepository userRepository;
    private final OnlineCourseRepository onlineCourseRepository;
    private final AssessmentSubmissionRepository assessmentSubmissionRepository;

    @Value("${englishlab.storage.orphan-cleanup.retention-hours:168}")
    private long retentionHours;

    /**
     * Runs once every {@code englishlab.storage.orphan-cleanup.delay-ms} ms (default 7 days).
     * An initial delay of 15 minutes lets the application finish booting and warms the JPA
     * repositories before the first scan.
     */
    @Scheduled(
            fixedDelayString = "${englishlab.storage.orphan-cleanup.delay-ms:604800000}",
            initialDelayString = "${englishlab.storage.orphan-cleanup.initial-delay-ms:900000}"
    )
    @Transactional(readOnly = true)
    public void cleanupOrphans() {
        Instant cutoff = Instant.now().minus(Duration.ofHours(Math.max(1L, retentionHours)));

        // Project only the URLs we need from each table — no full entity hydration.
        Set<String> referencedKeys;
        try {
            referencedKeys = collectReferencedKeys();
        } catch (RuntimeException exception) {
            log.warn("Orphan cleanup skipped — failed to collect referenced keys: {}",
                    exception.getMessage());
            return;
        }

        int removed = 0;
        for (String prefix : new String[]{AVATAR_PREFIX, THUMBNAIL_PREFIX, AUDIO_PREFIX}) {
            try {
                removed += cleanupPrefix(prefix, referencedKeys, cutoff);
            } catch (RuntimeException exception) {
                log.warn("Orphan cleanup failed for prefix {}: {}", prefix, exception.getMessage());
            }
        }

        if (removed > 0) {
            log.info("Object-store orphan cleanup removed {} file(s).", removed);
        } else {
            log.debug("Object-store orphan cleanup scanned prefixes – no orphans found.");
        }
    }

    private Set<String> collectReferencedKeys() {
        Set<String> keys = new HashSet<>();
        // Avatar URLs
        userRepository.findAllNonEmptyAvatarUrls().forEach(url ->
                keys.add(extractObjectKey(url, AVATAR_PREFIX)));
        // Course thumbnail URLs
        onlineCourseRepository.findAllNonEmptyThumbnailUrls().forEach(url ->
                keys.add(extractObjectKey(url, THUMBNAIL_PREFIX)));
        // Assessment audio URLs
        assessmentSubmissionRepository.findAllNonEmptyAudioUrls().forEach(url ->
                keys.add(extractObjectKey(url, AUDIO_PREFIX)));
        return keys;
    }

    private int cleanupPrefix(String prefix, Set<String> referencedKeys, Instant cutoff) {
        // Normalize the prefix so we generate identical keys to referencedKeys.
        String normalizedPrefix = normalizePrefix(prefix);
        List<String> candidateKeys = objectStore.listKeysOlderThan(prefix, cutoff);
        int removed = 0;
        for (String key : candidateKeys) {
            String keyForLookup = stripLeadingSlash(key);
            if (referencedKeys.contains(keyForLookup)) {
                continue;
            }
            try {
                objectStore.delete(key);
                removed++;
                log.debug("Deleted orphan object store key: {}", key);
            } catch (RuntimeException exception) {
                log.warn("Failed to delete orphan object store key {}: {}", key, exception.getMessage());
            }
        }
        return removed;
    }

    // ---------- Helpers ----------

    /** Matches the same normalization logic as {@link ObjectStore#objectKey(String, String)}. */
    static String normalizePrefix(String prefix) {
        if (prefix == null) {
            return "";
        }
        return prefix.replaceAll("^/+", "").replaceAll("/+$", "");
    }

    /** Strips a single leading slash (matches what ObjectStore.objectKey does). */
    static String stripLeadingSlash(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("^/+", "");
    }

    /**
     * Extracts the canonical {@code prefix/fileName} key from any URL form (R2 public URL,
     * legacy {@code /api/...} path, or absolute URL with a custom domain).
     *
     * <p>The result mirrors exactly what {@link ObjectStore#objectKey(String, String)} would
     * produce, so orphan detection never mismatches because of URL quirks.
     *
     * <p>If the URL does not contain the expected prefix segment (i.e. the column stores a
     * URL from a different storage location), the method returns an empty string and the
     * URL is excluded from the referenced-keys set — it will NOT be deleted because it is
     * not found in the candidate list anyway.
     */
    static String extractObjectKey(String url, String prefix) {
        if (url == null || url.isBlank()) {
            return "";
        }
        String normalized = url.trim().replace('\\', '/');
        int query = normalized.indexOf('?');
        if (query >= 0) {
            normalized = normalized.substring(0, query);
        }
        // Verify the URL actually contains "/<prefix>/" – if not, we have a misconfigured
        // URL and must skip it rather than risk deleting the wrong file.
        String normalizedPrefix = normalizePrefix(prefix).toLowerCase(java.util.Locale.ROOT);
        if (normalizedPrefix.isEmpty()) {
            return "";
        }
        // Use case-insensitive prefix check so URLs with different casing still match.
        Pattern prefixPattern = Pattern.compile("/" + Pattern.quote(normalizedPrefix) + "/",
                Pattern.CASE_INSENSITIVE);
        if (!prefixPattern.matcher(normalized).find()) {
            return "";
        }
        // Extract the file name from the last path segment, after URL-decoding.
        int lastSlash = normalized.lastIndexOf('/');
        if (lastSlash < 0 || lastSlash == normalized.length() - 1) {
            return "";
        }
        String fileName = normalized.substring(lastSlash + 1);
        try {
            fileName = URLDecoder.decode(fileName, StandardCharsets.UTF_8).trim();
        } catch (IllegalArgumentException ignored) {
            // Malformed encoding – keep the raw file name.
        }
        // Reject file names that still contain characters R2 / object stores cannot safely
        // key on. This includes spaces and shell metacharacters that would otherwise cause
        // a mismatch between the extracted key and the actual R2 key.
        if (fileName.isBlank()
                || fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")
                || fileName.contains(" ") || fileName.contains("\0") || fileName.contains("?")) {
            return "";
        }
        return stripLeadingSlash(normalizedPrefix + "/" + fileName);
    }
}
