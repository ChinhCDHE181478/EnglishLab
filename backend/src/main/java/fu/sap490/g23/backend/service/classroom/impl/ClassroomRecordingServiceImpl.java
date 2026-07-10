package fu.sap490.g23.backend.service.classroom.impl;

import fu.sap490.g23.backend.dto.request.classroom.UpdateRecordingRequest;
import fu.sap490.g23.backend.dto.response.classroom.ClassroomOfferingResponse;
import fu.sap490.g23.backend.dto.response.classroom.ClassroomSessionResponse;
import fu.sap490.g23.backend.entity.classroom.ClassroomOffering;
import fu.sap490.g23.backend.entity.classroom.ClassroomSession;
import fu.sap490.g23.backend.repository.classroom.ClassroomOfferingRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomSessionRepository;
import fu.sap490.g23.backend.service.classroom.ClassroomMapper;
import fu.sap490.g23.backend.service.classroom.ClassroomRecordingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional
public class ClassroomRecordingServiceImpl implements ClassroomRecordingService {

    private final ClassroomOfferingRepository offeringRepository;
    private final ClassroomSessionRepository sessionRepository;
    private final ClassroomMapper mapper;

    @Override
    public ClassroomOfferingResponse updateOfferingRecording(Long offeringId, UpdateRecordingRequest request) {
        ClassroomOffering offering = offeringRepository.findById(offeringId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp học."));
        if (request.getRecordingUrl() != null) {
            offering.setRecordingUrl(trimOrNull(request.getRecordingUrl()));
        }
        if (request.getRecordingVisible() != null) {
            offering.setRecordingVisible(request.getRecordingVisible());
        }
        return mapper.toOfferingResponse(offeringRepository.save(offering), true, null, null, true);
    }

    @Override
    public ClassroomSessionResponse updateSessionRecording(Long sessionId, UpdateRecordingRequest request) {
        ClassroomSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy buổi học."));
        if (request.getRecordingUrl() != null) {
            session.setRecordingUrl(trimOrNull(request.getRecordingUrl()));
        }
        if (request.getRecordingVisible() != null) {
            session.setRecordingVisible(request.getRecordingVisible());
        }
        return mapper.toSessionResponse(sessionRepository.save(session));
    }

    private String trimOrNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
