package fu.sap490.g23.backend.service.classroom;

import fu.sap490.g23.backend.entity.classroom.CenterMaterialLibraryItem;
import fu.sap490.g23.backend.entity.classroom.ClassroomMaterial;
import fu.sap490.g23.backend.entity.classroom.ClassroomOffering;
import fu.sap490.g23.backend.entity.classroom.enums.ContentReviewStatus;
import fu.sap490.g23.backend.entity.curriculum.CurriculumMaterialRef;
import fu.sap490.g23.backend.entity.curriculum.CurriculumProgram;
import fu.sap490.g23.backend.entity.curriculum.CurriculumUnit;
import fu.sap490.g23.backend.repository.classroom.ClassroomMaterialRepository;
import fu.sap490.g23.backend.service.classroom.impl.ClassroomMaterialSyncServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClassroomMaterialSyncServiceImplTest {

    @Mock
    private ClassroomMaterialRepository materialRepository;

    private ClassroomMaterialSyncServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ClassroomMaterialSyncServiceImpl(materialRepository);
    }

    @Test
    void synchronizesCurriculumMaterialsAsMandatory() {
        CenterMaterialLibraryItem unitMaterial = material(20L, "Unit 1 worksheet");
        CurriculumUnit unit = unit(101L, "Unit 1", unitMaterial);
        ClassroomOffering offering = offering(unit);
        when(materialRepository.findByClassroomOfferingIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

        service.synchronizeMandatoryMaterials(offering, null);

        ArgumentCaptor<ClassroomMaterial> captor = ArgumentCaptor.forClass(ClassroomMaterial.class);
        verify(materialRepository).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(ClassroomMaterial::getSourceType)
                .containsExactly("CURRICULUM_LIBRARY");
        ClassroomMaterial syncedUnitMaterial = captor.getAllValues().stream()
                .filter(item -> "CURRICULUM_LIBRARY".equals(item.getSourceType()))
                .findFirst()
                .orElseThrow();
        assertThat(syncedUnitMaterial.getCurriculumUnit()).isSameAs(unit);
        assertThat(syncedUnitMaterial.getReviewStatus()).isEqualTo(ContentReviewStatus.APPROVED);
        assertThat(syncedUnitMaterial.getVisibility()).isEqualTo("LEARNERS_IN_CLASS");
    }

    @Test
    void upgradesPreviouslyAttachedCenterMaterialWhenItBecomesRequired() {
        CenterMaterialLibraryItem unitMaterial = material(20L, "Tên mới từ giáo trình");
        CurriculumUnit unit = unit(101L, "Unit 1", unitMaterial);
        ClassroomOffering offering = offering(unit);
        ClassroomMaterial existing = ClassroomMaterial.builder()
                .id(301L)
                .classroomOffering(offering)
                .centerMaterialId(20L)
                .title("Tên cũ")
                .sourceType("CENTER_LIBRARY")
                .build();
        when(materialRepository.findByClassroomOfferingIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(existing));

        service.synchronizeMandatoryMaterials(offering, null);

        verify(materialRepository).save(existing);
        assertThat(existing.getTitle()).isEqualTo("Tên mới từ giáo trình");
        assertThat(existing.getSourceType()).isEqualTo("CURRICULUM_LIBRARY");
        assertThat(existing.getCurriculumUnit()).isSameAs(unit);
    }

    @Test
    void removesStaleMandatoryMaterialButKeepsTeacherSupplement() {
        ClassroomOffering offering = offering();
        ClassroomMaterial stale = ClassroomMaterial.builder()
                .id(301L)
                .classroomOffering(offering)
                .centerMaterialId(99L)
                .sourceType("CURRICULUM_LIBRARY")
                .build();
        ClassroomMaterial supplement = ClassroomMaterial.builder()
                .id(302L)
                .classroomOffering(offering)
                .sourceType("CLASSROOM_UPLOAD")
                .build();
        when(materialRepository.findByClassroomOfferingIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(stale, supplement));

        service.synchronizeMandatoryMaterials(offering, null);

        verify(materialRepository).delete(stale);
        verify(materialRepository, never()).delete(supplement);
    }

    private ClassroomOffering offering(CurriculumUnit... units) {
        CurriculumProgram curriculum = CurriculumProgram.builder()
                .units(new ArrayList<>(List.of(units)))
                .build();
        return ClassroomOffering.builder()
                .id(1L)
                .curriculumProgram(curriculum)
                .build();
    }

    private CurriculumUnit unit(Long id, String title, CenterMaterialLibraryItem material) {
        CurriculumUnit unit = CurriculumUnit.builder()
                .id(id)
                .title(title)
                .materialRefs(new ArrayList<>())
                .build();
        unit.getMaterialRefs().add(CurriculumMaterialRef.builder()
                .unit(unit)
                .material(material)
                .displayOrder(0)
                .build());
        return unit;
    }

    private CenterMaterialLibraryItem material(Long id, String title) {
        return CenterMaterialLibraryItem.builder()
                .id(id)
                .title(title)
                .fileUrl("https://example.test/" + id)
                .fileType("PDF")
                .materialType("LESSON_NOTE")
                .provider("EnglishLab")
                .status("PUBLISHED")
                .build();
    }
}
