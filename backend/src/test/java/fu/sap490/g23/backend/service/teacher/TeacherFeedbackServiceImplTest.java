package fu.sap490.g23.backend.service.teacher;

import fu.sap490.g23.backend.dto.request.teacher.UpsertTeacherCourseFeedbackRequest;
import fu.sap490.g23.backend.dto.response.teacher.TeacherFeedbackAggregateResponse;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.classroom.ClassroomEnrollment;
import fu.sap490.g23.backend.entity.classroom.ClassroomOffering;
import fu.sap490.g23.backend.entity.classroom.enums.ClassroomRegistrationStatus;
import fu.sap490.g23.backend.entity.course.LearningPackage;
import fu.sap490.g23.backend.entity.teacher.TeacherCourseFeedback;
import fu.sap490.g23.backend.entity.teacher.enums.TeacherFeedbackPace;
import fu.sap490.g23.backend.repository.UserRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomEnrollmentRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomTeacherAssignmentRepository;
import fu.sap490.g23.backend.repository.teacher.TeacherCourseFeedbackRepository;
import fu.sap490.g23.backend.service.admin.AuditLogService;
import fu.sap490.g23.backend.service.teacher.impl.TeacherFeedbackServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeacherFeedbackServiceImplTest {
    @Mock private UserRepository userRepository;
    @Mock private ClassroomEnrollmentRepository enrollmentRepository;
    @Mock private ClassroomTeacherAssignmentRepository assignmentRepository;
    @Mock private TeacherCourseFeedbackRepository feedbackRepository;
    @Mock private AuditLogService auditLogService;

    private TeacherFeedbackServiceImpl service;
    private User learner;
    private User teacher;
    private ClassroomOffering classroom;
    private ClassroomEnrollment enrollment;

    @BeforeEach
    void setUp() {
        service = new TeacherFeedbackServiceImpl(
                userRepository, enrollmentRepository, assignmentRepository, feedbackRepository, auditLogService
        );
        ReflectionTestUtils.setField(service, "opensDaysBeforeEnd", 7);
        ReflectionTestUtils.setField(service, "closesDaysAfterEnd", 14);
        ReflectionTestUtils.setField(service, "anonymityThreshold", 3);
        learner = User.builder().id(1L).email("learner@example.com").fullName("Học viên").build();
        teacher = User.builder().id(2L).email("teacher@example.com").fullName("Giáo viên").build();
        classroom = ClassroomOffering.builder()
                .id(10L)
                .learningPackage(LearningPackage.builder().title("IELTS Foundation").build())
                .primaryTeacher(teacher)
                .startDate(LocalDate.now().minusMonths(1))
                .endDate(LocalDate.now().plusDays(3))
                .build();
        enrollment = ClassroomEnrollment.builder()
                .id(20L)
                .student(learner)
                .classroomOffering(classroom)
                .registrationStatus(ClassroomRegistrationStatus.ASSIGNED)
                .build();
    }

    @Test
    void learnerCanSubmitAndUpdateDuringWindowWithoutCreatingSecondRecord() {
        UpsertTeacherCourseFeedbackRequest request = validRequest();
        TeacherCourseFeedback existing = feedback(99L, 4);
        when(userRepository.findByEmail(learner.getEmail())).thenReturn(Optional.of(learner));
        when(enrollmentRepository.findByStudentIdAndClassroomOfferingId(1L, 10L)).thenReturn(Optional.of(enrollment));
        when(assignmentRepository.findByClassroomOfferingId(10L)).thenReturn(List.of());
        when(feedbackRepository.findByEnrollmentIdAndTeacherId(20L, 2L)).thenReturn(Optional.of(existing));
        when(feedbackRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.saveLearnerFeedback(10L, 2L, learner.getEmail(), request);

        assertThat(response.isSubmitted()).isTrue();
        assertThat(response.isEditable()).isTrue();
        assertThat(response.getStrengths()).isEqualTo(request.getStrengths());
        verify(feedbackRepository).save(existing);
        verify(auditLogService).record(
                learner.getEmail(), "TEACHER_FEEDBACK_UPDATED", "TEACHER_COURSE_FEEDBACK", "99",
                "Học viên cập nhật đánh giá ẩn danh cho lớp #10"
        );
    }

    @Test
    void learnerCannotSubmitBeforeWindowOpens() {
        classroom.setEndDate(LocalDate.now().plusDays(20));
        when(userRepository.findByEmail(learner.getEmail())).thenReturn(Optional.of(learner));
        when(enrollmentRepository.findByStudentIdAndClassroomOfferingId(1L, 10L)).thenReturn(Optional.of(enrollment));

        assertThatThrownBy(() -> service.saveLearnerFeedback(10L, 2L, learner.getEmail(), validRequest()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Phiếu đánh giá mở từ ngày");
        verifyNoInteractions(feedbackRepository);
    }

    @Test
    void teacherOnlySeesAggregateAfterAnonymityThresholdAndNeverReceivesComments() {
        when(userRepository.findByEmail(teacher.getEmail())).thenReturn(Optional.of(teacher));
        when(feedbackRepository.findByTeacherIdOrderBySubmittedAtDesc(2L))
                .thenReturn(List.of(feedback(1L, 5), feedback(2L, 4)));

        TeacherFeedbackAggregateResponse protectedResult = service.getTeacherSummary(teacher.getEmail());

        assertThat(protectedResult.isProtectedByAnonymity()).isTrue();
        assertThat(protectedResult.getResponseCount()).isEqualTo(2);
        assertThat(protectedResult.getOverallScore()).isNull();
        assertThat(protectedResult.getClassrooms()).isEmpty();
    }

    private TeacherCourseFeedback feedback(Long id, int rating) {
        return TeacherCourseFeedback.builder()
                .id(id).enrollment(enrollment).classroomOffering(classroom).teacher(teacher)
                .clarityScore(rating).engagementScore(rating).learnerSupportScore(rating)
                .feedbackTimelinessScore(rating).professionalismScore(rating)
                .pace(TeacherFeedbackPace.JUST_RIGHT).wouldRecommend(true)
                .strengths("Giảng giải rõ ràng và có ví dụ thực tế.")
                .improvementSuggestions("Cần thêm thời gian chữa bài tập khó.")
                .submittedAt(LocalDateTime.now()).build();
    }

    private UpsertTeacherCourseFeedbackRequest validRequest() {
        UpsertTeacherCourseFeedbackRequest request = new UpsertTeacherCourseFeedbackRequest();
        request.setClarityScore(5);
        request.setEngagementScore(4);
        request.setLearnerSupportScore(5);
        request.setFeedbackTimelinessScore(4);
        request.setProfessionalismScore(5);
        request.setPace(TeacherFeedbackPace.JUST_RIGHT);
        request.setWouldRecommend(true);
        request.setStrengths("Giảng giải rõ ràng và có ví dụ thực tế.");
        request.setImprovementSuggestions("Nên dành thêm thời gian chữa bài tập khó.");
        return request;
    }
}
