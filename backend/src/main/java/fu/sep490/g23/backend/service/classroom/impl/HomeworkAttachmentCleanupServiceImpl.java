package fu.sap490.g23.backend.service.classroom.impl;

import fu.sap490.g23.backend.repository.classroom.CenterMaterialLibraryItemRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomHomeworkRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomHomeworkSubmissionRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomMaterialRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomTuitionPaymentProofRepository;
import fu.sap490.g23.backend.service.classroom.HomeworkAttachmentCleanupService;
import fu.sap490.g23.backend.service.classroom.HomeworkAttachmentStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class HomeworkAttachmentCleanupServiceImpl implements HomeworkAttachmentCleanupService {

    private final HomeworkAttachmentStorageService storageService;
    private final ClassroomHomeworkRepository homeworkRepository;
    private final ClassroomHomeworkSubmissionRepository submissionRepository;
    private final ClassroomTuitionPaymentProofRepository proofRepository;
    private final ClassroomMaterialRepository materialRepository;
    private final CenterMaterialLibraryItemRepository centerMaterialRepository;

    @Value("${englishlab.homework-attachments.orphan-retention-hours:24}")
    private long orphanRetentionHours;

    @Override
    @Transactional(readOnly = true)
    public int cleanupOrphanFiles() {
        int removed = 0;
        Duration retention = Duration.ofHours(Math.max(1L, orphanRetentionHours));
        for (String fileName : storageService.findStoredFileNamesOlderThan(retention)) {
            String suffix = "/" + fileName;
            if (isReferenced(suffix)) {
                continue;
            }
            try {
                storageService.delete(fileName);
                removed++;
            } catch (RuntimeException exception) {
                log.warn("Không thể xóa tệp đính kèm mồ côi {}: {}", fileName, exception.getMessage());
            }
        }
        if (removed > 0) {
            log.info("Đã xóa {} tệp đính kèm mồ côi.", removed);
        }
        return removed;
    }

    @Scheduled(
            fixedDelayString = "${englishlab.homework-attachments.cleanup-delay-ms:86400000}",
            initialDelayString = "${englishlab.homework-attachments.cleanup-initial-delay-ms:1800000}"
    )
    public void scheduledCleanup() {
        cleanupOrphanFiles();
    }

    private boolean isReferenced(String suffix) {
        return homeworkRepository.existsByAttachmentUrlEndingWith(suffix)
                || submissionRepository.existsByAttachmentUrlEndingWith(suffix)
                || proofRepository.existsByFileUrlEndingWith(suffix)
                || materialRepository.existsByFileUrlEndingWith(suffix)
                || centerMaterialRepository.existsByFileUrlEndingWith(suffix);
    }
}
