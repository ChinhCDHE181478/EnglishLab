package fu.sap490.g23.backend.service.classroom.impl;

import fu.sap490.g23.backend.service.classroom.*;


import fu.sap490.g23.backend.dto.request.classroom.CenterMaterialLibraryUpsertRequest;
import fu.sap490.g23.backend.dto.response.classroom.CenterMaterialLibraryItemResponse;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.classroom.CenterMaterialLibraryItem;
import fu.sap490.g23.backend.repository.classroom.CenterMaterialLibraryItemRepository;
import fu.sap490.g23.backend.repository.classroom.TrainingProgramMaterialRepository;
import fu.sap490.g23.backend.repository.curriculum.CurriculumMaterialRefRepository;
import fu.sap490.g23.backend.security.ClassroomAccessHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class CenterMaterialLibraryServiceImpl implements CenterMaterialLibraryService {

    private final CenterMaterialLibraryItemRepository repository;
    private final TrainingProgramMaterialRepository trainingProgramMaterialRepository;
    private final CurriculumMaterialRefRepository curriculumMaterialRefRepository;
    private final ClassroomAccessHelper accessHelper;

    @Override
    @Transactional(readOnly = true)
    public List<CenterMaterialLibraryItemResponse> listForContentManager() {
        return repository.findAllByOrderByUpdatedAtDescIdDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public CenterMaterialLibraryItemResponse create(CenterMaterialLibraryUpsertRequest request, String actorEmail) {
        User actor = accessHelper.requireUser(actorEmail);
        assertMaterialLibraryManagement(actor);
        validateRequest(request);

        CenterMaterialLibraryItem item = CenterMaterialLibraryItem.builder()
                .createdBy(actor)
                .updatedBy(actor)
                .build();
        applyRequest(item, request, actor);
        return toResponse(repository.save(item));
    }

    @Override
    public CenterMaterialLibraryItemResponse update(Long materialId, CenterMaterialLibraryUpsertRequest request, String actorEmail) {
        User actor = accessHelper.requireUser(actorEmail);
        assertMaterialLibraryManagement(actor);
        validateRequest(request);

        CenterMaterialLibraryItem item = repository.findById(materialId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy học liệu trung tâm."));
        applyRequest(item, request, actor);
        return toResponse(repository.save(item));
    }

    @Override
    public void delete(Long materialId, String actorEmail) {
        User actor = accessHelper.requireUser(actorEmail);
        assertMaterialLibraryManagement(actor);

        CenterMaterialLibraryItem item = repository.findById(materialId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy học liệu trung tâm."));
        if (trainingProgramMaterialRepository.existsByMaterialId(materialId)
                || curriculumMaterialRefRepository.existsByMaterialId(materialId)) {
            throw new IllegalArgumentException(
                    "Học liệu đang được sử dụng trong chương trình hoặc giáo trình. Hãy gỡ liên kết trước khi xóa."
            );
        }
        repository.delete(item);
    }

    private void applyRequest(CenterMaterialLibraryItem item, CenterMaterialLibraryUpsertRequest request, User actor) {
        item.setTitle(request.getTitle().trim());
        item.setDescription(normalize(request.getDescription()));
        item.setFileUrl(request.getFileUrl().trim());
        item.setFileType(upperOrNull(request.getFileType()));
        item.setMaterialType(normalize(request.getMaterialType()));
        item.setProvider(normalize(request.getProvider()));
        item.setExamCategory(upperOrNull(request.getExamCategory()));
        item.setIeltsBandMin(normalizeBand(request.getIeltsBandMin()));
        item.setIeltsBandMax(normalizeBand(request.getIeltsBandMax()));
        item.setToeicScoreMin(request.getToeicScoreMin());
        item.setToeicScoreMax(request.getToeicScoreMax());
        item.setSkill(normalize(request.getSkill()));
        item.setTags(normalize(request.getTags()));
        item.setStatus(upperOrDefault(request.getStatus(), "PUBLISHED"));
        item.setUpdatedBy(actor);
    }

    private void validateRequest(CenterMaterialLibraryUpsertRequest request) {
        if (!StringUtils.hasText(request.getFileUrl())) {
            throw new IllegalArgumentException("Cần cung cấp tệp hoặc liên kết học liệu.");
        }
        if (request.getIeltsBandMin() != null && request.getIeltsBandMax() != null
                && request.getIeltsBandMin().compareTo(request.getIeltsBandMax()) > 0) {
            throw new IllegalArgumentException("Band IELTS tối thiểu không được lớn hơn band tối đa.");
        }
        if (request.getToeicScoreMin() != null && request.getToeicScoreMax() != null
                && request.getToeicScoreMin() > request.getToeicScoreMax()) {
            throw new IllegalArgumentException("Điểm TOEIC tối thiểu không được lớn hơn điểm tối đa.");
        }
    }

    private void assertMaterialLibraryManagement(User actor) {
        if (!accessHelper.canManageClassroom(actor)) {
            throw new RuntimeException("Bạn không có quyền quản lý thư viện học liệu trung tâm.");
        }
    }

    private CenterMaterialLibraryItemResponse toResponse(CenterMaterialLibraryItem item) {
        return CenterMaterialLibraryItemResponse.builder()
                .id(item.getId())
                .title(item.getTitle())
                .description(item.getDescription())
                .fileUrl(item.getFileUrl())
                .fileType(item.getFileType())
                .materialType(item.getMaterialType())
                .provider(item.getProvider())
                .examCategory(item.getExamCategory())
                .ieltsBandMin(item.getIeltsBandMin())
                .ieltsBandMax(item.getIeltsBandMax())
                .toeicScoreMin(item.getToeicScoreMin())
                .toeicScoreMax(item.getToeicScoreMax())
                .skill(item.getSkill())
                .tags(item.getTags())
                .status(item.getStatus())
                .createdByName(item.getCreatedBy() == null ? null : item.getCreatedBy().getFullName())
                .updatedByName(item.getUpdatedBy() == null ? null : item.getUpdatedBy().getFullName())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String upperOrNull(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : null;
    }

    private String upperOrDefault(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : fallback;
    }

    private BigDecimal normalizeBand(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros();
    }
}
