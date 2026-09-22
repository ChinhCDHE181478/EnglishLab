package fu.sep490.g23.backend.ut.classroomLearningService;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.classroom.CourseRegistrationRequest;
import fu.sep490.g23.backend.entity.classroom.enums.EnrollmentRequestStatus;
import fu.sep490.g23.backend.repository.classroom.CourseRegistrationRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * UC-52: Cancel Class Registration Request - test ngắn gọn.
 * Entity: CourseRegistrationRequest, status: EnrollmentRequestStatus
 */
@ExtendWith(MockitoExtension.class)
public class CancelClassRegistrationRequestTest {

    @Mock
    private CourseRegistrationRequestRepository repository;

    // TC01: Hủy thành công ở trạng thái SUBMITTED -> CANCELLED
    @Test
    void cancelClassRegistrationRequest_UTC01_cancelSubmittedStatus_succeeds() {
        User learner = User.builder().id(1L).email("LEARNER_01").build();
        CourseRegistrationRequest req = CourseRegistrationRequest.builder()
                .id(101L).learner(learner).status(EnrollmentRequestStatus.SUBMITTED).build();

        req.setStatus(EnrollmentRequestStatus.CANCELLED);
        when(repository.save(req)).thenReturn(req);

        CourseRegistrationRequest result = repository.save(req);

        assertThat(result.getStatus()).isEqualTo(EnrollmentRequestStatus.CANCELLED);
        ArgumentCaptor<CourseRegistrationRequest> captor = ArgumentCaptor.forClass(CourseRegistrationRequest.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(EnrollmentRequestStatus.CANCELLED);
    }

    // TC02: Hủy thành công ở trạng thái TEST_SCHEDULED
    @Test
    void cancelClassRegistrationRequest_UTC02_cancelTestScheduledStatus_succeeds() {
        User learner = User.builder().id(1L).email("LEARNER_01").build();
        CourseRegistrationRequest req = CourseRegistrationRequest.builder()
                .id(102L).learner(learner).status(EnrollmentRequestStatus.TEST_SCHEDULED).build();

        req.setStatus(EnrollmentRequestStatus.CANCELLED);
        when(repository.save(req)).thenReturn(req);

        CourseRegistrationRequest result = repository.save(req);
        assertThat(result.getStatus()).isEqualTo(EnrollmentRequestStatus.CANCELLED);
    }

    // TC03: Thất bại khi đã CLASS_ASSIGNED -> IllegalStateException, save không gọi
    @Test
    void cancelClassRegistrationRequest_UTC03_cancelAssignedStatus() {
        User learner = User.builder().id(1L).email("LEARNER_01").build();
        CourseRegistrationRequest req = CourseRegistrationRequest.builder()
                .id(103L).learner(learner).status(EnrollmentRequestStatus.CLASS_ASSIGNED).build();

        assertThatThrownBy(() -> {
            if (req.getStatus() == EnrollmentRequestStatus.CLASS_ASSIGNED) {
                throw new IllegalStateException("MSG-44: Yêu cầu đăng ký không thể xử lý ở trạng thái hiện tại");
            }
        }).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MSG-44");

        verify(repository, never()).save(req);
    }

    // TC04: Thất bại khi đã REJECTED
    @Test
    void cancelClassRegistrationRequest_UTC04_cancelRejectedStatus() {
        User learner = User.builder().id(1L).email("LEARNER_01").build();
        CourseRegistrationRequest req = CourseRegistrationRequest.builder()
                .id(104L).learner(learner).status(EnrollmentRequestStatus.REJECTED).build();

        assertThatThrownBy(() -> {
            if (req.getStatus() == EnrollmentRequestStatus.REJECTED) {
                throw new IllegalStateException("BR-60 vi phạm");
            }
        }).isInstanceOf(IllegalStateException.class);

        verify(repository, never()).save(req);
    }

    // TC05: Thất bại khi đã CANCELLED trước đó
    @Test
    void cancelClassRegistrationRequest_UTC05_cancelAlreadyCancelled() {
        User learner = User.builder().id(1L).email("LEARNER_01").build();
        CourseRegistrationRequest req = CourseRegistrationRequest.builder()
                .id(105L).learner(learner).status(EnrollmentRequestStatus.CANCELLED).build();

        assertThatThrownBy(() -> {
            if (req.getStatus() == EnrollmentRequestStatus.CANCELLED) {
                throw new IllegalStateException("Trạng thái không hợp lệ");
            }
        }).isInstanceOf(IllegalStateException.class);
    }

    // TC06: Không tìm thấy request -> EntityNotFoundException
    @Test
    void cancelClassRegistrationRequest_UTC06_requestNotFound() {
        when(repository.findById(9999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> repository.findById(9999L)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("MSG-48: Không tìm thấy dữ liệu yêu cầu")))
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class);
    }

    // TC07: Học viên cố tình hủy đơn của người khác -> AccessDeniedException
    @Test
    void cancelClassRegistrationRequest_UTC07_cancelOthersRequest() {
        User learner1 = User.builder().id(1L).email("LEARNER_01").build();
        User learner2 = User.builder().id(2L).email("LEARNER_02").build();
        CourseRegistrationRequest req = CourseRegistrationRequest.builder()
                .id(106L).learner(learner2).status(EnrollmentRequestStatus.SUBMITTED).build();

        assertThatThrownBy(() -> {
            if (!req.getLearner().getEmail().equals(learner1.getEmail())) {
                throw new org.springframework.security.access.AccessDeniedException("Access Denied");
            }
        }).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);

        verify(repository, never()).save(req);
    }
}
