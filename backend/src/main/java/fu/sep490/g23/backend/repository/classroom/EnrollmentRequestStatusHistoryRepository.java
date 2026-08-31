package fu.sep490.g23.backend.repository.classroom;

import fu.sep490.g23.backend.entity.classroom.EnrollmentRequestStatusHistory;
import com.fasterxml.jackson.databind.ObjectMapper;
import fu.sep490.g23.backend.entity.admin.AuditLog;
import fu.sep490.g23.backend.entity.classroom.enums.EnrollmentRequestStatus;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.admin.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class EnrollmentRequestStatusHistoryRepository {
    private static final String TARGET_TYPE = "COURSE_REGISTRATION_REQUEST";

    private final AuditLogRepository auditLogRepository;
    private final CourseRegistrationRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public EnrollmentRequestStatusHistory save(EnrollmentRequestStatusHistory history) {
        try {
            AuditLog saved = auditLogRepository.save(AuditLog.builder()
                    .actorEmail(history.getActor() == null ? "system@englishlab.local" : history.getActor().getEmail())
                    .action("REGISTRATION_STATUS_CHANGED")
                    .targetType(TARGET_TYPE)
                    .targetId(String.valueOf(history.getCourseRegistrationRequest().getId()))
                    .detail(objectMapper.writeValueAsString(new HistoryDetail(
                            history.getFromStatus(), history.getToStatus(), history.getReason())))
                    .build());
            return fromAudit(saved, history.getCourseRegistrationRequest());
        } catch (Exception ex) {
            throw new IllegalStateException("Không thể lưu lịch sử đăng ký khóa học.", ex);
        }
    }

    public List<EnrollmentRequestStatusHistory> findByCourseRegistrationRequestIdOrderByCreatedAtAscIdAsc(Long requestId) {
        var request = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy yêu cầu đăng ký."));
        return auditLogRepository.findByTargetTypeAndTargetIdOrderByCreatedAtAscIdAsc(TARGET_TYPE, String.valueOf(requestId))
                .stream().map(audit -> fromAudit(audit, request)).toList();
    }

    private EnrollmentRequestStatusHistory fromAudit(AuditLog audit, fu.sep490.g23.backend.entity.classroom.CourseRegistrationRequest request) {
        try {
            HistoryDetail detail = objectMapper.readValue(audit.getDetail(), HistoryDetail.class);
            return EnrollmentRequestStatusHistory.builder()
                    .id(audit.getId())
                    .courseRegistrationRequest(request)
                    .fromStatus(detail.fromStatus())
                    .toStatus(detail.toStatus())
                    .reason(detail.reason())
                    .actor(userRepository.findByEmail(audit.getActorEmail()).orElse(null))
                    .createdAt(audit.getCreatedAt())
                    .build();
        } catch (Exception ex) {
            throw new IllegalStateException("Không thể đọc lịch sử đăng ký khóa học.", ex);
        }
    }

    private record HistoryDetail(EnrollmentRequestStatus fromStatus, EnrollmentRequestStatus toStatus, String reason) {}
}
