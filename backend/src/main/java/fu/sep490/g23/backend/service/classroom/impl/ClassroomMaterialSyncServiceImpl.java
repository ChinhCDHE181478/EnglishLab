package fu.sep490.g23.backend.service.classroom.impl;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.classroom.CenterMaterialLibraryItem;
import fu.sep490.g23.backend.entity.classroom.ClassroomMaterial;
import fu.sep490.g23.backend.entity.classroom.ClassroomOffering;
import fu.sep490.g23.backend.entity.classroom.enums.ContentReviewStatus;
import fu.sep490.g23.backend.entity.curriculum.CurriculumMaterialRef;
import fu.sep490.g23.backend.entity.curriculum.CurriculumUnit;
import fu.sep490.g23.backend.repository.classroom.ClassroomMaterialRepository;
import fu.sep490.g23.backend.service.classroom.ClassroomMaterialSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ClassroomMaterialSyncServiceImpl implements ClassroomMaterialSyncService {

    private static final Set<String> MANDATORY_SOURCE_TYPES = Set.of(
            "CURRICULUM_LIBRARY"
    );

    private final ClassroomMaterialRepository materialRepository;

    @Override
    public void synchronizeMandatoryMaterials(ClassroomOffering offering, User actor) {
        if (offering == null || offering.getId() == null) {
            return;
        }

        Map<Long, RequiredMaterial> requiredMaterials = collectRequiredMaterials(offering);
        List<ClassroomMaterial> existingMaterials = new ArrayList<>(
                materialRepository.findByClassroomOfferingIdOrderByCreatedAtDesc(offering.getId())
        );
        Map<Long, ClassroomMaterial> existingByCenterMaterialId = existingMaterials.stream()
                .filter(material -> material.getSession() == null)
                .filter(material -> material.getCenterMaterialId() != null)
                .collect(Collectors.toMap(
                        ClassroomMaterial::getCenterMaterialId,
                        material -> material,
                        this::preferMandatoryMaterial,
                        LinkedHashMap::new
                ));

        for (RequiredMaterial required : requiredMaterials.values()) {
            ClassroomMaterial classroomMaterial = existingByCenterMaterialId.get(required.material().getId());
            if (classroomMaterial == null) {
                classroomMaterial = ClassroomMaterial.builder()
                        .classroomOffering(offering)
                        .centerMaterialId(required.material().getId())
                        .uploadedBy(actor)
                        .build();
            }
            applyRequiredMaterial(classroomMaterial, required);
            materialRepository.save(classroomMaterial);
        }

        existingMaterials.stream()
                .filter(this::isMandatory)
                .filter(material -> material.getCenterMaterialId() == null
                        || !requiredMaterials.containsKey(material.getCenterMaterialId()))
                .forEach(materialRepository::delete);
    }

    private Map<Long, RequiredMaterial> collectRequiredMaterials(ClassroomOffering offering) {
        Map<Long, RequiredMaterial> required = new LinkedHashMap<>();
        if (offering.getCurriculumProgram() != null) {
            for (CurriculumUnit unit : offering.getCurriculumProgram().getUnits()) {
                for (CurriculumMaterialRef materialRef : unit.getMaterialRefs()) {
                    CenterMaterialLibraryItem material = materialRef.getMaterial();
                    if (material != null && material.getId() != null) {
                        required.put(material.getId(), new RequiredMaterial(material, unit, "CURRICULUM_LIBRARY"));
                    }
                }
            }
        }
        return required;
    }

    private void applyRequiredMaterial(ClassroomMaterial target, RequiredMaterial required) {
        CenterMaterialLibraryItem source = required.material();
        target.setTitle(source.getTitle());
        target.setFileUrl(source.getFileUrl());
        target.setFileType(source.getFileType());
        target.setDescription(source.getDescription());
        target.setMaterialType(source.getMaterialType());
        target.setProvider(source.getProvider());
        target.setVisibility("LEARNERS_IN_CLASS");
        target.setSourceType(required.sourceType());
        target.setCenterMaterialId(source.getId());
        target.setCurriculumUnit(required.unit());
        target.setSession(null);
        target.setReviewStatus(ContentReviewStatus.APPROVED);
        target.setReviewNote(null);
        target.setSubmittedForReviewAt(null);
    }

    private ClassroomMaterial preferMandatoryMaterial(ClassroomMaterial first, ClassroomMaterial second) {
        if (isMandatory(first)) {
            return first;
        }
        return isMandatory(second) ? second : first;
    }

    private boolean isMandatory(ClassroomMaterial material) {
        return material.getSourceType() != null
                && MANDATORY_SOURCE_TYPES.contains(material.getSourceType().toUpperCase());
    }

    private record RequiredMaterial(
            CenterMaterialLibraryItem material,
            CurriculumUnit unit,
            String sourceType
    ) {
        private RequiredMaterial {
            Objects.requireNonNull(material);
        }
    }
}
