package fu.sep490.g23.backend.service.classroom;

import fu.sep490.g23.backend.entity.classroom.CenterMaterialLibraryItem;
import fu.sep490.g23.backend.entity.classroom.ClassroomMaterial;
import fu.sep490.g23.backend.entity.classroom.ClassSection;
import fu.sep490.g23.backend.entity.classroom.enums.ContentReviewStatus;
import fu.sep490.g23.backend.entity.course.CourseUnit;
import fu.sep490.g23.backend.entity.course.CourseUnitContentRef;
import fu.sep490.g23.backend.entity.course.InstructorLedCourse;
import fu.sep490.g23.backend.entity.course.enums.CourseUnitContentType;
import fu.sep490.g23.backend.repository.classroom.ClassroomMaterialRepository;
import fu.sep490.g23.backend.repository.course.CourseUnitRepository;
import fu.sep490.g23.backend.service.classroom.impl.ClassroomMaterialSyncServiceImpl;
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

    @Mock
    private CourseUnitRepository courseUnitRepository;

    private ClassroomMaterialSyncServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ClassroomMaterialSyncServiceImpl(materialRepository, courseUnitRepository);
    }

    @Test
    void synchronizesCurriculumMaterialsAsMandatory() {
        CenterMaterialLibraryItem unitMaterial = material(20L, "Unit 1 worksheet");
        CourseUnit unit = unit(101L, "Unit 1", unitMaterial);
        ClassSection offering = offering(unit);
        when(materialRepository.findByClassSectionIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

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
        assertThat(syncedUnitMaterial.getCourseUnit()).isSameAs(unit);
        assertThat(syncedUnitMaterial.getReviewStatus()).isEqualTo(ContentReviewStatus.APPROVED);
        assertThat(syncedUnitMaterial.getVisibility()).isEqualTo("LEARNERS_IN_CLASS");
    }

    @Test
    void upgradesPreviouslyAttachedCenterMaterialWhenItBecomesRequired() {
        CenterMaterialLibraryItem unitMaterial = material(20L, "Tên mới từ giáo trình");
        CourseUnit unit = unit(101L, "Unit 1", unitMaterial);
        ClassSection offering = offering(unit);
        ClassroomMaterial existing = ClassroomMaterial.builder()
                .id(301L)
                .classSection(offering)
                .centerMaterialId(20L)
                .title("Tên cũ")
                .sourceType("CENTER_LIBRARY")
                .build();
        when(materialRepository.findByClassSectionIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(existing));

        service.synchronizeMandatoryMaterials(offering, null);

        verify(materialRepository).save(existing);
        assertThat(existing.getTitle()).isEqualTo("Tên mới từ giáo trình");
        assertThat(existing.getSourceType()).isEqualTo("CURRICULUM_LIBRARY");
        assertThat(existing.getCourseUnit()).isSameAs(unit);
    }

    @Test
    void removesStaleMandatoryMaterialButKeepsTeacherSupplement() {
        ClassSection offering = offering();
        ClassroomMaterial stale = ClassroomMaterial.builder()
                .id(301L)
                .classSection(offering)
                .centerMaterialId(99L)
                .sourceType("CURRICULUM_LIBRARY")
                .build();
        ClassroomMaterial supplement = ClassroomMaterial.builder()
                .id(302L)
                .classSection(offering)
                .sourceType("CLASSROOM_UPLOAD")
                .build();
        when(materialRepository.findByClassSectionIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(stale, supplement));

        service.synchronizeMandatoryMaterials(offering, null);

        verify(materialRepository).delete(stale);
        verify(materialRepository, never()).delete(supplement);
    }

    private ClassSection offering(CourseUnit... units) {
        InstructorLedCourse course = InstructorLedCourse.builder().id(10L).build();
        for (CourseUnit unit : units) {
            unit.setInstructorLedCourse(course);
        }
        when(courseUnitRepository.findByInstructorLedCourseIdOrderBySequenceNumberAscIdAsc(10L))
                .thenReturn(List.of(units));
        return ClassSection.builder()
                .id(1L)
                .instructorLedCourse(course)
                .build();
    }

    private CourseUnit unit(Long id, String title, CenterMaterialLibraryItem material) {
        CourseUnit unit = CourseUnit.builder()
                .id(id)
                .title(title)
                .contentRefs(new ArrayList<>())
                .build();
        unit.getContentRefs().add(CourseUnitContentRef.builder()
                .courseUnit(unit)
                .contentType(CourseUnitContentType.MATERIAL)
                .learningResource(material)
                .sequenceNumber(0)
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
