package fu.sap490.g23.backend.service.classroom;

import fu.sap490.g23.backend.dto.request.classroom.UpdateGradebookRequest;
import fu.sap490.g23.backend.dto.response.classroom.ClassroomGradebookResponse;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.classroom.ClassroomEnrollment;
import fu.sap490.g23.backend.entity.classroom.ClassroomGradebookEntry;
import fu.sap490.g23.backend.entity.classroom.ClassroomOffering;
import fu.sap490.g23.backend.entity.classroom.enums.GradebookEntryStatus;
import fu.sap490.g23.backend.entity.enums.RoleEnum;
import fu.sap490.g23.backend.repository.classroom.ClassroomEnrollmentRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomGradebookEntryRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomOfferingRepository;
import fu.sap490.g23.backend.security.ClassroomAccessHelper;
import fu.sap490.g23.backend.service.classroom.impl.ClassroomGradebookServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClassroomGradebookServiceImplTest {

    @Mock private ClassroomGradebookEntryRepository gradebookEntryRepository;
    @Mock private ClassroomOfferingRepository offeringRepository;
    @Mock private ClassroomEnrollmentRepository enrollmentRepository;
    @Mock private ClassroomAccessHelper accessHelper;
    @Mock private ClassroomMapper mapper;

    private ClassroomGradebookServiceImpl service;
    private ClassroomGradebookEntry entry;

    @BeforeEach
    void setUp() {
        service = new ClassroomGradebookServiceImpl(
                gradebookEntryRepository,
                offeringRepository,
                enrollmentRepository,
                accessHelper,
                mapper
        );

        ClassroomOffering offering = ClassroomOffering.builder().id(21L).build();
        User student = User.builder().id(31L).fullName("Learner Test").build();
        User teacher = User.builder().id(41L).role(RoleEnum.TEACHER).build();
        entry = ClassroomGradebookEntry.builder()
                .id(51L)
                .classroomOffering(offering)
                .student(student)
                .status(GradebookEntryStatus.PENDING)
                .build();

        when(accessHelper.requireUser("teacher@example.com")).thenReturn(teacher);
    }

    @Test
    void updateEntry_UpdatesManualScoresAndMarksPendingEntryAsGraded() {
        when(offeringRepository.findById(21L)).thenReturn(Optional.of(entry.getClassroomOffering()));
        when(enrollmentRepository.findByStudentIdAndClassroomOfferingId(31L, 21L))
                .thenReturn(Optional.of(ClassroomEnrollment.builder()
                        .classroomOffering(entry.getClassroomOffering())
                        .student(entry.getStudent())
                        .build()));
        when(gradebookEntryRepository.findByClassroomOfferingIdAndStudentId(21L, 31L))
                .thenReturn(Optional.of(entry));
        UpdateGradebookRequest request = UpdateGradebookRequest.builder()
                .studentId(31L)
                .homeworkScore(new BigDecimal("8.50"))
                .quizScore(new BigDecimal("9.00"))
                .attendancePercent(new BigDecimal("95.00"))
                .participationScore(new BigDecimal("8.00"))
                .finalResult(new BigDecimal("8.70"))
                .teacherComment("Tiến bộ tốt")
                .build();

        when(gradebookEntryRepository.save(entry)).thenReturn(entry);
        when(mapper.toGradebookResponse(entry)).thenAnswer(invocation -> ClassroomGradebookResponse.builder()
                .id(entry.getId())
                .studentId(entry.getStudent().getId())
                .homeworkScore(entry.getHomeworkScore())
                .quizScore(entry.getQuizScore())
                .attendancePercent(entry.getAttendancePercent())
                .participationScore(entry.getParticipationScore())
                .finalResult(entry.getFinalResult())
                .teacherComment(entry.getTeacherComment())
                .status(entry.getStatus())
                .build());

        ClassroomGradebookResponse response = service.updateEntry(21L, request, "teacher@example.com");

        assertThat(response.getHomeworkScore()).isEqualByComparingTo("8.50");
        assertThat(response.getQuizScore()).isEqualByComparingTo("9.00");
        assertThat(response.getAttendancePercent()).isEqualByComparingTo("95.00");
        assertThat(response.getFinalResult()).isEqualByComparingTo("8.70");
        assertThat(response.getTeacherComment()).isEqualTo("Tiến bộ tốt");
        assertThat(response.getStatus()).isEqualTo(GradebookEntryStatus.GRADED);
        assertThat(entry.getUpdatedBy()).isNotNull();
        verify(accessHelper).assertTeacher(entry.getUpdatedBy());
    }

    @Test
    void unpublishGradebook_MarksPublishedEntriesAsGraded() {
        entry.setStatus(GradebookEntryStatus.PUBLISHED);
        when(gradebookEntryRepository.findByClassroomOfferingId(21L)).thenReturn(List.of(entry));
        when(gradebookEntryRepository.saveAll(List.of(entry))).thenReturn(List.of(entry));
        when(mapper.toGradebookResponse(entry)).thenAnswer(invocation -> ClassroomGradebookResponse.builder()
                .id(entry.getId())
                .studentId(entry.getStudent().getId())
                .status(entry.getStatus())
                .build());

        List<ClassroomGradebookResponse> responses =
                service.unpublishGradebook(21L, "teacher@example.com");

        assertThat(entry.getStatus()).isEqualTo(GradebookEntryStatus.GRADED);
        assertThat(entry.getUpdatedBy()).isNotNull();
        assertThat(responses).singleElement()
                .extracting(ClassroomGradebookResponse::getStatus)
                .isEqualTo(GradebookEntryStatus.GRADED);
        verify(accessHelper).assertTeacher(entry.getUpdatedBy());
        verify(gradebookEntryRepository).saveAll(List.of(entry));
    }
}
