package fu.sep490.g23.backend.service.classroom.impl;

import fu.sep490.g23.backend.dto.request.classroom.UpdateRecordingRequest;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomSessionResponse;
import fu.sep490.g23.backend.entity.classroom.ClassSchedule;
import fu.sep490.g23.backend.entity.classroom.enums.RecordingSyncStatus;
import fu.sep490.g23.backend.repository.classroom.ClassSectionRepository;
import fu.sep490.g23.backend.repository.classroom.ClassScheduleRepository;
import fu.sep490.g23.backend.service.classroom.ClassroomMapper;
import fu.sep490.g23.backend.service.classroom.ClassroomRecordingService;
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
    private final ClassSectionRepository classSectionRepository;
    private final ClassScheduleRepository classScheduleRepository;
    private final ClassroomMapper mapper;
    private final VirtualMeetingService virtualMeetingService;
    private final GoogleMeetProperties googleMeetProperties;

    @Override
    public ClassroomSessionResponse updateSessionRecording(Long scheduleId, UpdateRecordingRequest request) {
        ClassSchedule schedule = requireSchedule(scheduleId);
        if (request.getRecordingUrl() != null) {
            String url = trimOrNull(request.getRecordingUrl());
            validateRecordingUrl(url);
            schedule.setRecordingUrl(url);
            if (url == null) {
                clearRecording(schedule);
            } else {
                schedule.setRecordingStatus(RecordingSyncStatus.READY);
                schedule.setRecordingSyncedAt(LocalDateTime.now());
                schedule.setRecordingSyncError(null);
            }
        }
        if (request.getRecordingVisible() != null) {
            if (request.getRecordingVisible() && !StringUtils.hasText(schedule.getRecordingUrl())) {
                throw new IllegalArgumentException("Chưa có đường dẫn ghi hình hợp lệ để công bố.");
            }
            schedule.setRecordingVisible(request.getRecordingVisible());
        }
        return mapper.toManagerSessionResponse(classScheduleRepository.save(schedule));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomSessionResponse> listManagerSessions(Long classSectionId) {
        if (!classSectionRepository.existsById(classSectionId)) {
            throw new IllegalArgumentException("Không tìm thấy lớp học.");
        }
        return classScheduleRepository.findByClassSectionIdOrderBySessionDateAscStartTimeAsc(classSectionId)
                .stream().map(mapper::toManagerSessionResponse).toList();
    }

    @Override
    public ClassroomSessionResponse syncRecording(Long scheduleId) {
        ClassSchedule schedule = requireSchedule(scheduleId);
        syncGoogleMeetRecording(schedule);
        return mapper.toManagerSessionResponse(classScheduleRepository.save(schedule));
    }

    @Scheduled(
            fixedDelayString = "${englishlab.google-meet.recording-sync-delay-ms:60000}",
            initialDelayString = "${englishlab.google-meet.recording-sync-delay-ms:60000}"
    )
    public void reconcilePendingRecordings() {
        if (!virtualMeetingService.isEnabled()) return;
        LocalDateTime retryBefore = LocalDateTime.now().minusSeconds(
                Math.max(30, googleMeetProperties.getRecordingSyncDelayMs() / 1000));
        classScheduleRepository.findGoogleMeetRecordingsPendingSync(
                        EnumSet.of(RecordingSyncStatus.PROCESSING, RecordingSyncStatus.FAILED),
                        Math.max(1, googleMeetProperties.getRecordingMaxSyncAttempts()),
                        retryBefore)
                .stream()
                .filter(schedule -> schedule.getSessionDate() == null
                        || !schedule.getSessionDate().isAfter(LocalDate.now()))
                .limit(50)
                .forEach(schedule -> {
                    syncGoogleMeetRecording(schedule);
                    classScheduleRepository.save(schedule);
                });
    }

    private void syncGoogleMeetRecording(ClassSchedule schedule) {
        schedule.setRecordingLastAttemptAt(LocalDateTime.now());
        schedule.setRecordingSyncAttempts((schedule.getRecordingSyncAttempts() == null
                ? 0 : schedule.getRecordingSyncAttempts()) + 1);
        try {
            VirtualMeetingRecordingInfo recording = virtualMeetingService.getRecording(schedule);
            validateRecordingUrl(recording.url());
            schedule.setRecordingUrl(recording.url());
            schedule.setRecordingStatus(RecordingSyncStatus.READY);
            schedule.setRecordingSyncedAt(LocalDateTime.now());
            schedule.setRecordingSyncError(null);
            if (googleMeetProperties.isRecordingAutoPublish()) schedule.setRecordingVisible(true);
        } catch (RuntimeException exception) {
            boolean processing = exception.getMessage() != null
                    && (exception.getMessage().contains("đang xử lý")
                    || exception.getMessage().contains("chưa có bản ghi"));
            schedule.setRecordingStatus(processing
                    ? RecordingSyncStatus.PROCESSING : RecordingSyncStatus.FAILED);
            schedule.setRecordingSyncError(processing
                    ? "Google Meet đang xử lý file ghi hình. Hệ thống sẽ tự thử lại."
                    : limit(exception.getMessage(), 1000));
        }
    }

    private ClassSchedule requireSchedule(Long id) {
        return classScheduleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy buổi học."));
    }

    private void clearRecording(ClassSchedule schedule) {
        schedule.setRecordingVisible(false);
        schedule.setRecordingStatus(RecordingSyncStatus.NOT_AVAILABLE);
        schedule.setRecordingSyncedAt(null);
        schedule.setRecordingLastAttemptAt(null);
        schedule.setRecordingSyncError(null);
        schedule.setRecordingSyncAttempts(0);
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
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Đường dẫn ghi hình phải là HTTPS hợp lệ.");
        }
    }

    private String limit(String value, int maxLength) {
        if (!StringUtils.hasText(value)) return "Không đồng bộ được bản ghi từ Google Meet.";
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
