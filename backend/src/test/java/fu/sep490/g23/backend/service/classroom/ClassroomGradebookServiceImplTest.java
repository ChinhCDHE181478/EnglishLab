package fu.sep490.g23.backend.service.classroom;

import fu.sep490.g23.backend.dto.request.classroom.UpdateGradebookRequest;
import fu.sep490.g23.backend.dto.request.classroom.UpdateGradebookHomeworkScoreRequest;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomGradebookResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.classroom.ClassEnrollment;
import fu.sep490.g23.backend.entity.classroom.ClassroomGradebookEntry;
import fu.sep490.g23.backend.entity.classroom.ClassroomHomework;
import fu.sep490.g23.backend.entity.classroom.ClassroomHomeworkSubmission;
import fu.sep490.g23.backend.entity.classroom.ClassSection;
import fu.sep490.g23.backend.entity.classroom.enums.GradebookEntryStatus;
import fu.sep490.g23.backend.entity.classroom.enums.HomeworkSubmissionStatus;
import fu.sep490.g23.backend.entity.enums.RoleEnum;
import fu.sep490.g23.backend.repository.classroom.ClassEnrollmentRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomGradebookEntryRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomHomeworkRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomHomeworkSubmissionRepository;
import fu.sep490.g23.backend.repository.classroom.ClassSectionRepository;
import fu.sep490.g23.backend.security.ClassroomAccessHelper;
import fu.sep490.g23.backend.service.classroom.impl.ClassroomGradebookServiceImpl;
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
    @Mock private ClassSectionRepository offeringRepository;
    @Mock private ClassEnrollmentRepository enrollmentRepository;
    @Mock private ClassroomHomeworkRepository homeworkRepository;
    @Mock private ClassroomHomeworkSubmissionRepository submissionRepository;
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
                homeworkRepository,
                submissionRepository,
                accessHelper,
                mapper,
                new ClassroomHomeworkScoreCalculator()
        );

        ClassSection offering = ClassSection.builder().id(21L).build();
        User student = User.builder().id(31L).fullName("Learner Test").build();
        entry = ClassroomGradebookEntry.builder()
                .id(51L)
                .classSection(offering)
                .student(student)
                .status(GradebookEntryStatus.PENDING)
                .build();

    }

    @Test
    void updateEntry_UpdatesDynamicHomeworkScoresAndMarksPendingEntryAsGraded() {
        when(accessHelper.requireUser("teacher@example.com"))
                .thenReturn(User.builder().id(41L).role(RoleEnum.TEACHER).build());
        when(offeringRepository.findById(21L)).thenReturn(Optional.of(entry.getClassSection()));
        when(enrollmentRepository.findByStudentIdAndClassSectionId(31L, 21L))
                .thenReturn(Optional.of(ClassEnrollment.builder()
                        .classSection(entry.getClassSection())
                        .student(entry.getStudent())
                        .build()));
        when(gradebookEntryRepository.findByClassSectionIdAndStudentId(21L, 31L))
                .thenReturn(Optional.of(entry));
        ClassroomHomework homework = ClassroomHomework.builder()
                .id(61L)
                .classSection(entry.getClassSection())
                .title("Unit 5")
                .maxScore(BigDecimal.TEN)
                .build();
        ClassroomHomeworkSubmission submission = ClassroomHomeworkSubmission.builder()
                .id(71L)
                .homework(homework)
                .student(entry.getStudent())
                .status(HomeworkSubmissionStatus.SUBMITTED)
                .build();
        when(homeworkRepository.findByClassSectionIdOrderByCreatedAtDesc(21L))
                .thenReturn(List.of(homework));
        when(submissionRepository.findAllForStudentGradebook(21L, 31L))
                .thenReturn(List.of(submission));

        UpdateGradebookRequest request = UpdateGradebookRequest.builder()
                .studentId(31L)
                .homeworkScores(List.of(UpdateGradebookHomeworkScoreRequest.builder()
                        .homeworkId(61L)
                        .score(new BigDecimal("8.50"))
                        .build()))
                .attendancePercent(new BigDecimal("95.00"))
                .finalResult(new BigDecimal("8.70"))
                .teacherComment("Tiến bộ tốt")
                .build();

        when(gradebookEntryRepository.save(entry)).thenReturn(entry);
        when(mapper.toGradebookResponse(entry)).thenAnswer(invocation -> ClassroomGradebookResponse.builder()
                .id(entry.getId())
                .studentId(entry.getStudent().getId())
                .homeworkAverage(entry.getHomeworkScore())
                .attendancePercent(entry.getAttendancePercent())
                .finalResult(entry.getFinalResult())
                .teacherComment(entry.getTeacherComment())
                .status(entry.getStatus())
                .build());

        ClassroomGradebookResponse response = service.updateEntry(21L, request, "teacher@example.com");

        assertThat(response.getHomeworkAverage()).isEqualByComparingTo("8.5");
        assertThat(response.getHomeworks()).singleElement().satisfies(item -> {
            assertThat(item.getTitle()).isEqualTo("Unit 5");
            assertThat(item.getScore()).isEqualByComparingTo("8.50");
            assertThat(item.getStatus()).isEqualTo("GRADED");
        });
        assertThat(response.getAttendancePercent()).isEqualByComparingTo("95.00");
        assertThat(response.getFinalResult()).isEqualByComparingTo("8.70");
        assertThat(response.getTeacherComment()).isEqualTo("Tiến bộ tốt");
        assertThat(response.getStatus()).isEqualTo(GradebookEntryStatus.GRADED);
        assertThat(entry.getUpdatedBy()).isNotNull();
        verify(accessHelper).assertTeacher(entry.getUpdatedBy());
        verify(submissionRepository).saveAll(List.of(submission));
    }

    @Test
    void unpublishGradebook_MarksPublishedEntriesAsGraded() {
        when(accessHelper.requireUser("teacher@example.com"))
                .thenReturn(User.builder().id(41L).role(RoleEnum.TEACHER).build());
        entry.setStatus(GradebookEntryStatus.PUBLISHED);
        when(gradebookEntryRepository.findByClassSectionId(21L)).thenReturn(List.of(entry));
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

    @Test
    void getMyGradebook_ReturnsNoContentWhenEntryDoesNotExist() {
        User learner = entry.getStudent();
        when(accessHelper.requireUser("learner@example.com")).thenReturn(learner);
        when(gradebookEntryRepository.findByClassSectionIdAndStudentId(21L, learner.getId()))
                .thenReturn(Optional.empty());

        ClassroomGradebookResponse response = service.getMyGradebook(21L, "learner@example.com");

        assertThat(response).isNull();
    }

    @Test
    void getMyGradebook_ReturnsNoContentWhenEntryIsNotPublished() {
        User learner = entry.getStudent();
        when(accessHelper.requireUser("learner@example.com")).thenReturn(learner);
        when(gradebookEntryRepository.findByClassSectionIdAndStudentId(21L, learner.getId()))
                .thenReturn(Optional.of(entry));

        ClassroomGradebookResponse response = service.getMyGradebook(21L, "learner@example.com");

        assertThat(response).isNull();
    }
}
