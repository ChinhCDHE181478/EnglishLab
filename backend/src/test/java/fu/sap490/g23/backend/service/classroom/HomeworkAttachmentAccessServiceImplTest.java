package fu.sap490.g23.backend.service.classroom;

import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.classroom.ClassroomHomework;
import fu.sap490.g23.backend.entity.classroom.ClassroomHomeworkSubmission;
import fu.sap490.g23.backend.entity.classroom.ClassroomOffering;
import fu.sap490.g23.backend.repository.classroom.CenterMaterialLibraryItemRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomEnrollmentRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomHomeworkRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomHomeworkSubmissionRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomMaterialRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomTeacherAssignmentRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomTuitionPaymentProofRepository;
import fu.sap490.g23.backend.security.ClassroomAccessHelper;
import fu.sap490.g23.backend.service.classroom.impl.HomeworkAttachmentAccessServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HomeworkAttachmentAccessServiceImplTest {

    @Mock private HomeworkAttachmentStorageService storageService;
    @Mock private ClassroomAccessHelper accessHelper;
    @Mock private ClassroomHomeworkRepository homeworkRepository;
    @Mock private ClassroomHomeworkSubmissionRepository submissionRepository;
    @Mock private ClassroomTuitionPaymentProofRepository proofRepository;
    @Mock private ClassroomMaterialRepository materialRepository;
    @Mock private CenterMaterialLibraryItemRepository centerMaterialRepository;
    @Mock private ClassroomEnrollmentRepository enrollmentRepository;
    @Mock private ClassroomTeacherAssignmentRepository teacherAssignmentRepository;
    @Mock private Resource resource;

    private HomeworkAttachmentAccessServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new HomeworkAttachmentAccessServiceImpl(
                storageService,
                accessHelper,
                homeworkRepository,
                submissionRepository,
                proofRepository,
                materialRepository,
                centerMaterialRepository,
                enrollmentRepository,
                teacherAssignmentRepository
        );
    }

    @Test
    void loadAuthorized_allowsSubmissionOwner() {
        User learner = User.builder().id(10L).email("learner@example.com").build();
        ClassroomHomeworkSubmission submission = ClassroomHomeworkSubmission.builder()
                .student(learner)
                .homework(ClassroomHomework.builder()
                        .classroomOffering(ClassroomOffering.builder().id(20L).build())
                        .build())
                .build();
        when(accessHelper.requireUser("learner@example.com")).thenReturn(learner);
        when(proofRepository.findFirstByFileUrlEndingWith("/homework-file.pdf")).thenReturn(Optional.empty());
        when(submissionRepository.findFirstByAttachmentUrlEndingWith("/homework-file.pdf"))
                .thenReturn(Optional.of(submission));
        when(storageService.load("homework-file.pdf")).thenReturn(resource);

        Resource result = service.loadAuthorized("homework-file.pdf", "learner@example.com");

        assertSame(resource, result);
        verify(storageService).load("homework-file.pdf");
    }

    @Test
    void loadAuthorized_hidesAnotherStudentsSubmission() {
        User owner = User.builder().id(10L).email("owner@example.com").build();
        User stranger = User.builder().id(11L).email("stranger@example.com").build();
        ClassroomHomeworkSubmission submission = ClassroomHomeworkSubmission.builder()
                .student(owner)
                .homework(ClassroomHomework.builder()
                        .classroomOffering(ClassroomOffering.builder().id(20L).build())
                        .build())
                .build();
        when(accessHelper.requireUser("stranger@example.com")).thenReturn(stranger);
        when(proofRepository.findFirstByFileUrlEndingWith("/homework-file.pdf")).thenReturn(Optional.empty());
        when(submissionRepository.findFirstByAttachmentUrlEndingWith("/homework-file.pdf"))
                .thenReturn(Optional.of(submission));

        assertThrows(
                ResponseStatusException.class,
                () -> service.loadAuthorized("homework-file.pdf", "stranger@example.com")
        );
        verify(storageService, never()).load("homework-file.pdf");
    }

    @Test
    void loadAuthorized_rejectsUnreferencedFileEvenForOperationsUser() {
        User staff = User.builder().id(30L).email("staff@example.com").build();
        when(accessHelper.requireUser("staff@example.com")).thenReturn(staff);
        when(accessHelper.canManageTrainingOperations(staff)).thenReturn(true);

        assertThrows(
                ResponseStatusException.class,
                () -> service.loadAuthorized("orphan.pdf", "staff@example.com")
        );
        verify(storageService, never()).load("orphan.pdf");
    }
}
