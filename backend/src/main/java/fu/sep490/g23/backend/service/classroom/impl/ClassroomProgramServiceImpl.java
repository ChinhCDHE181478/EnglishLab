package fu.sep490.g23.backend.service.classroom.impl;

import fu.sep490.g23.backend.dto.request.classroom.UpdateClassroomProgramRequest;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomOfferingResponse;
import fu.sep490.g23.backend.entity.classroom.ClassSection;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import fu.sep490.g23.backend.repository.classroom.ClassSectionRepository;
import fu.sep490.g23.backend.service.classroom.ClassroomMapper;
import fu.sep490.g23.backend.service.classroom.ClassroomProgramService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ClassroomProgramServiceImpl implements ClassroomProgramService {

    private final ClassSectionRepository offeringRepository;
    private final ClassroomMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomOfferingResponse> listPrograms(ClassroomDeliveryMode deliveryMode) {
        return offeringRepository.findAll().stream()
                .filter(offering -> offering.getInstructorLedCourse() != null && !offering.isDeleted())
                .filter(offering -> deliveryMode == null || offering.getDeliveryMode() == deliveryMode)
                .map(offering -> mapper.toOfferingResponse(offering, true, null, null, true))
                .toList();
    }

    @Override
    public ClassroomOfferingResponse updateProgramProfile(Long offeringId, UpdateClassroomProgramRequest request) {
        ClassSection offering = offeringRepository.findById(offeringId)
                .filter(item -> item.getInstructorLedCourse() != null && !item.isDeleted())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp học."));

        if (StringUtils.hasText(request.getEntryLevel())) {
            offering.setEntryLevel(request.getEntryLevel().trim());
        }
        if (request.getTargetOutcome() != null) {
            offering.setTargetOutcome(trimOrNull(request.getTargetOutcome()));
        }
        if (request.getProgramOutcomes() != null) {
            offering.setProgramOutcomes(trimOrNull(request.getProgramOutcomes()));
        }
        if (request.getTeacherGuide() != null) {
            offering.setTeacherGuide(trimOrNull(request.getTeacherGuide()));
        }
        if (request.getInteractionActivities() != null) {
            offering.setInteractionActivities(trimOrNull(request.getInteractionActivities()));
        }
        if (request.getSyllabusSummary() != null) {
            offering.setSyllabusSummary(trimOrNull(request.getSyllabusSummary()));
        }
        if (request.getDeliveryMode() != null) {
            offering.setDeliveryMode(request.getDeliveryMode());
        }

        return mapper.toOfferingResponse(offeringRepository.save(offering), true, null, null, true);
    }

    private String trimOrNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
