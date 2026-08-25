package fu.sep490.g23.backend.service.classroom.impl;

import fu.sep490.g23.backend.dto.request.classroom.UpdateRecordingRequest;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomOfferingResponse;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomSessionResponse;
import fu.sep490.g23.backend.entity.classroom.ClassSection;
import fu.sep490.g23.backend.entity.classroom.ClassSchedule;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomSessionStatus;
import fu.sep490.g23.backend.entity.classroom.enums.RecordingSyncStatus;
import fu.sep490.g23.backend.repository.classroom.ClassSectionRepository;
import fu.sep490.g23.backend.repository.classroom.ClassScheduleRepository;
import fu.sep490.g23.backend.service.classroom.ClassroomMapper;
import fu.sep490.g23.backend.service.classroom.ClassroomRecordingService;
import fu.sep490.g23.backend.service.classroom.LarkMeetingService;
import fu.sep490.g23.backend.service.classroom.LarkProperties;
import fu.sep490.g23.backend.service.classroom.LarkRecordingInfo;
import fu.sep490.g23.backend.service.classroom.GoogleMeetProperties;
import fu.sep490.g23.backend.service.classroom.VirtualMeetingRecordingInfo;
import fu.sep490.g23.backend.service.classroom.VirtualMeetingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ClassroomRecordingServiceImpl implements ClassroomRecordingService {

    private final ClassSectionRepository offeringRepository;
    private final ClassScheduleRepository sessionRepository;
    private final ClassroomMapper mapper;
    private final LarkMeetingService larkMeetingService;
    private final LarkProperties larkProperties;
    private final VirtualMeetingService virtualMeetingService;
    private final GoogleMeetProperties googleMeetProperties;

    @Override
    public ClassroomOfferingResponse updateOfferingRecording(Long offeringId, UpdateRecordingRequest request) {
        ClassSection offering = offeringRepository.findById(offeringId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp học."));
        if (request.getRecordingUrl() != null) {
            String recordingUrl = trimOrNull(request.getRecordingUrl());
            validateRecordingUrl(recordingUrl);
            offering.setRecordingUrl(recordingUrl);
        }
        if (request.getRecordingVisible() != null) {
            if (request.getRecordingVisible() && !StringUtils.hasText(offering.getRecordingUrl())) {
                throw new RuntimeException("Chưa có đường dẫn recording hợp lệ để công bố cho học viên.");
            }
            offering.setRecordingVisible(request.getRecordingVisible());
        }
        return mapper.toOfferingResponse(offeringRepository.save(offering), true, null, null, true);
    }

    @Override
    public ClassroomSessionResponse updateSessionRecording(Long sessionId, UpdateRecordingRequest request) {
        ClassSchedule session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy buổi học."));
        if (request.getRecordingUrl() != null) {
            String recordingUrl = trimOrNull(request.getRecordingUrl());
            validateRecordingUrl(recordingUrl);
            session.setRecordingUrl(recordingUrl);
            if (recordingUrl == null) {
                clearRecordingMetadata(session);
            } else {
                session.setRecordingProvider("MANUAL");
                session.setRecordingSyncStatus(RecordingSyncStatus.READY);
                session.setRecordingSyncedAt(LocalDateTime.now());
                session.setRecordingSyncError(null);
                setExpiry(session);
            }
        }
        if (request.getRecordingVisible() != null) {
            if (request.getRecordingVisible() && !StringUtils.hasText(session.getRecordingUrl())) {
                throw new RuntimeException("Chưa có đường dẫn recording hợp lệ để công bố cho học viên.");
            }
            session.setRecordingVisible(request.getRecordingVisible());
            session.setRecordingPublishedAt(request.getRecordingVisible() ? LocalDateTime.now() : null);
        }
        return mapper.toManagerSessionResponse(sessionRepository.save(session));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomSessionResponse> listManagerSessions(Long offeringId) {
        if (!offeringRepository.existsById(offeringId)) {
            throw new RuntimeException("Không tìm thấy lớp học.");
        }
        return sessionRepository.findByClassSectionIdOrderBySessionDateAscStartTimeAsc(offeringId).stream()
                .map(mapper::toManagerSessionResponse)
                .toList();
    }

    @Override
    public ClassroomSessionResponse syncRecording(Long sessionId) {
        ClassSchedule session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy buổi học."));
        if (isGoogleMeetSession(session)) {
            syncGoogleMeetRecording(session);
        } else {
            syncLarkRecording(session);
        }
        return mapper.toManagerSessionResponse(sessionRepository.save(session));
    }

    @Override
    public ClassroomSessionResponse syncLarkRecording(Long sessionId) {
        return syncRecording(sessionId);
    }

    @Scheduled(
            fixedDelayString = "${englishlab.lark.recording-sync-delay-ms:60000}",
            initialDelayString = "${englishlab.lark.recording-sync-delay-ms:60000}"
    )
    public void reconcilePendingRecordings() {
        if (!larkMeetingService.isEnabled()) {
            return;
        }
        LocalDateTime retryBefore = LocalDateTime.now().minusSeconds(
                Math.max(30, larkProperties.getRecordingSyncDelayMs() / 1000)
        );
        List<ClassSchedule> pending = sessionRepository.findRecordingsPendingSync(
                EnumSet.of(RecordingSyncStatus.PROCESSING, RecordingSyncStatus.FAILED),
                Math.max(1, larkProperties.getRecordingMaxSyncAttempts()),
                retryBefore
        );
        pending.stream().limit(50).forEach(session -> {
            try {
                if (isGoogleMeetSession(session)) {
                    return;
                }
                syncLarkRecording(session);
                sessionRepository.save(session);
            } catch (RuntimeException ex) {
                log.warn("Không đồng bộ được recording Lark cho sessionId={}: {}", session.getId(), ex.getMessage());
            }
        });
    }

    @Scheduled(
            fixedDelayString = "${englishlab.google-meet.recording-sync-delay-ms:60000}",
            initialDelayString = "${englishlab.google-meet.recording-sync-delay-ms:60000}"
    )
    public void reconcilePendingGoogleMeetRecordings() {
        if (!virtualMeetingService.isEnabled()) {
            return;
        }
        LocalDateTime retryBefore = LocalDateTime.now().minusSeconds(
                Math.max(30, googleMeetProperties.getRecordingSyncDelayMs() / 1000)
        );
        List<ClassSchedule> pending = sessionRepository.findGoogleMeetRecordingsPendingSync(
                EnumSet.of(
                        RecordingSyncStatus.SCHEDULED,
                        RecordingSyncStatus.RECORDING,
                        RecordingSyncStatus.PROCESSING,
                        RecordingSyncStatus.FAILED
                ),
                Math.max(1, googleMeetProperties.getRecordingMaxSyncAttempts()),
                retryBefore
        );
        pending.stream()
                .filter(session -> session.getSessionDate() == null || !session.getSessionDate().isAfter(LocalDate.now()))
                .limit(50)
                .forEach(session -> {
                    syncGoogleMeetRecording(session);
                    sessionRepository.save(session);
                });
    }

    @Scheduled(fixedDelayString = "${englishlab.lark.recording-expiry-check-delay-ms:300000}")
    public void unpublishExpiredRecordings() {
        List<ClassSchedule> expired = sessionRepository
                .findByRecordingVisibleTrueAndRecordingExpiresAtBefore(LocalDateTime.now());
        expired.forEach(session -> {
            session.setRecordingVisible(false);
            session.setRecordingPublishedAt(null);
        });
        if (!expired.isEmpty()) {
            sessionRepository.saveAll(expired);
        }
    }

    @Scheduled(
            fixedDelayString = "${englishlab.lark.reserve-reconciliation-delay-ms:3600000}",
            initialDelayString = "${englishlab.lark.reserve-reconciliation-initial-delay-ms:120000}"
    )
    public void reconcileUpcomingAutoRecordMeetings() {
        if (!larkMeetingService.isEnabled() || !larkProperties.isAutoRecord()) {
            return;
        }
        LocalDate today = LocalDate.now();
        List<ClassSchedule> candidates = sessionRepository
                .findByDeliveryModeAndSessionDateBetweenOrderBySessionDateAscStartTimeAsc(
                        ClassroomDeliveryMode.VIRTUAL,
                        today,
                        today.plusDays(29)
                );
        candidates.stream()
                .filter(session -> session.getStatus() != ClassroomSessionStatus.CANCELLED)
                .filter(session -> !StringUtils.hasText(session.getLarkReserveId())
                        || "FAILED".equals(session.getLarkSyncStatus()))
                .limit(50)
                .forEach(session -> {
                    try {
                        larkMeetingService.syncMeeting(session);
                    } catch (RuntimeException ex) {
                        session.setLarkSyncStatus("FAILED");
                        session.setLarkSyncError(limit(ex.getMessage(), 1000));
                        log.warn("Không tạo được phòng Lark tự ghi hình cho sessionId={}: {}",
                                session.getId(), ex.getMessage());
                    }
                    sessionRepository.save(session);
                });
    }

    private void syncLarkRecording(ClassSchedule session) {
        session.setRecordingLastAttemptAt(LocalDateTime.now());
        session.setRecordingSyncAttempts((session.getRecordingSyncAttempts() == null ? 0 : session.getRecordingSyncAttempts()) + 1);
        try {
            LarkRecordingInfo recording = larkMeetingService.getRecording(session);
            validateRecordingUrl(recording.url());
            session.setRecordingUrl(recording.url());
            session.setRecordingDurationMs(recording.durationMs());
            session.setRecordingProvider("LARK");
            session.setRecordingSyncStatus(RecordingSyncStatus.READY);
            session.setRecordingSyncedAt(LocalDateTime.now());
            session.setRecordingSyncError(null);
            setExpiry(session);
            if (larkProperties.isRecordingAutoPublish()) {
                session.setRecordingVisible(true);
                session.setRecordingPublishedAt(LocalDateTime.now());
            }
        } catch (RuntimeException ex) {
            boolean stillProcessing = ex.getMessage() != null && ex.getMessage().contains("124002");
            session.setRecordingSyncStatus(stillProcessing ? RecordingSyncStatus.PROCESSING : RecordingSyncStatus.FAILED);
            session.setRecordingSyncError(stillProcessing
                    ? "Lark đang xử lý file recording. Hệ thống sẽ tự thử lại."
                    : limit(ex.getMessage(), 1000));
        }
    }

    private void syncGoogleMeetRecording(ClassSchedule session) {
        session.setRecordingLastAttemptAt(LocalDateTime.now());
        session.setRecordingSyncAttempts((session.getRecordingSyncAttempts() == null ? 0 : session.getRecordingSyncAttempts()) + 1);
        try {
            VirtualMeetingRecordingInfo recording = virtualMeetingService.getRecording(session);
            validateRecordingUrl(recording.url());
            session.setRecordingUrl(recording.url());
            session.setRecordingDurationMs(recording.durationMs());
            session.setRecordingProvider("GOOGLE_MEET");
            session.setRecordingSyncStatus(RecordingSyncStatus.READY);
            session.setRecordingSyncedAt(LocalDateTime.now());
            session.setRecordingSyncError(null);
            setExpiry(session);
            if (googleMeetProperties.isRecordingAutoPublish()) {
                session.setRecordingVisible(true);
                session.setRecordingPublishedAt(LocalDateTime.now());
            }
        } catch (RuntimeException ex) {
            boolean stillProcessing = ex.getMessage() != null && (
                    ex.getMessage().contains("đang xử lý")
                            || ex.getMessage().contains("chưa có bản ghi hội nghị")
            );
            session.setRecordingSyncStatus(stillProcessing ? RecordingSyncStatus.PROCESSING : RecordingSyncStatus.FAILED);
            session.setRecordingSyncError(stillProcessing
                    ? "Google Meet đang xử lý file ghi hình. Hệ thống sẽ tự thử lại."
                    : limit(ex.getMessage(), 1000));
        }
    }

    private boolean isGoogleMeetSession(ClassSchedule session) {
        return StringUtils.hasText(session.getLarkMeetingId())
                && session.getLarkMeetingId().startsWith("spaces/");
    }

    private String trimOrNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void validateRecordingUrl(String value) {
        if (value == null) return;
        try {
            URI uri = URI.create(value);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || !StringUtils.hasText(uri.getHost())) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("Recording phải là đường dẫn HTTPS hợp lệ.");
        }
    }

    private void clearRecordingMetadata(ClassSchedule session) {
        session.setRecordingVisible(false);
        session.setRecordingProvider(null);
        session.setRecordingDurationMs(null);
        session.setRecordingSyncStatus(RecordingSyncStatus.NOT_AVAILABLE);
        session.setRecordingSyncedAt(null);
        session.setRecordingSyncError(null);
        session.setRecordingSyncAttempts(0);
        session.setRecordingPublishedAt(null);
        session.setRecordingExpiresAt(null);
    }

    private void setExpiry(ClassSchedule session) {
        Integer availableDays = session.getClassSection().getCurriculumProgram() == null
                ? null
                : session.getClassSection().getCurriculumProgram().getRecordingAvailableDays();
        int days = availableDays == null || availableDays <= 0 ? 30 : availableDays;
        session.setRecordingExpiresAt(LocalDateTime.now().plusDays(days));
    }

    private String limit(String value, int maxLength) {
        if (!StringUtils.hasText(value)) return "Không đồng bộ được recording từ Lark.";
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
