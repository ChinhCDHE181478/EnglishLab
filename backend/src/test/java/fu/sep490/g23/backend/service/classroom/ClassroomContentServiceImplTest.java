package fu.sep490.g23.backend.service.classroom;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.classroom.ClassroomMaterial;
import fu.sep490.g23.backend.entity.classroom.ClassroomOffering;
import fu.sep490.g23.backend.entity.course.LearningPackage;
import fu.sep490.g23.backend.repository.classroom.ClassroomAnnouncementRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomEnrollmentRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomMaterialRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomOfferingRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomSessionRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomSyllabusItemRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomTeacherAssignmentRepository;
import fu.sep490.g23.backend.security.ClassroomAccessHelper;
import fu.sep490.g23.backend.service.classroom.impl.ClassroomContentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClassroomContentServiceImplTest {

    @Mock private ClassroomMaterialRepository materialRepository;
    @Mock private ClassroomAnnouncementRepository announcementRepository;
    @Mock private ClassroomSyllabusItemRepository syllabusItemRepository;
    @Mock private ClassroomOfferingRepository offeringRepository;
    @Mock private ClassroomSessionRepository sessionRepository;
    @Mock private ClassroomEnrollmentRepository enrollmentRepository;
    @Mock private ClassroomTeacherAssignmentRepository teacherAssignmentRepository;
    @Mock private ClassroomAccessHelper accessHelper;
    @Mock private ClassroomMapper mapper;
    @Mock private ClassroomMaterialSyncService materialSyncService;

    private ClassroomContentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ClassroomContentServiceImpl(
                materialRepository,
                announcementRepository,
                syllabusItemRepository,
                offeringRepository,
                sessionRepository,
                enrollmentRepository,
                teacherAssignmentRepository,
                accessHelper,
                mapper,
                materialSyncService
        );
    }

    @Test
    void mandatoryProgramMaterialCannotBeDeletedDirectly() {
        User manager = User.builder().id(7L).email("manager@englishlab.test").build();
        ClassroomMaterial material = ClassroomMaterial.builder()
                .id(9L)
                .classroomOffering(offering())
                .sourceType("CURRICULUM_LIBRARY")
                .build();
        when(accessHelper.requireUser(manager.getEmail())).thenReturn(manager);
        when(accessHelper.canManageTrainingOperations(manager)).thenReturn(true);
        when(materialRepository.findById(9L)).thenReturn(Optional.of(material));

        assertThatThrownBy(() -> service.deleteMaterial(9L, manager.getEmail()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Học liệu bắt buộc");
        verify(materialRepository, never()).delete(material);
    }

    @Test
    void teacherCannotReadMaterialsOfAnUnassignedClass() {
        User teacher = User.builder().id(8L).email("teacher@englishlab.test").build();
        ClassroomOffering offering = offering();
        when(accessHelper.requireUser(teacher.getEmail())).thenReturn(teacher);
        when(accessHelper.canManageTrainingOperations(teacher)).thenReturn(false);
        when(offeringRepository.findById(1L)).thenReturn(Optional.of(offering));
        when(teacherAssignmentRepository.findByClassroomOfferingIdAndTeacherId(1L, 8L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getTeacherMaterials(1L, teacher.getEmail()))
                .hasMessageContaining("không được phân công");
        verify(materialSyncService, never()).synchronizeMandatoryMaterials(offering, null);
    }

    private ClassroomOffering offering() {
        return ClassroomOffering.builder()
                .id(1L)
                .learningPackage(LearningPackage.builder().deleted(false).build())
                .build();
    }
}
