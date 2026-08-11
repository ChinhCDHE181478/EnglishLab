package fu.sep490.g23.backend.service.classroom;

import fu.sep490.g23.backend.dto.response.classroom.ClassroomHomeworkSubmissionResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.classroom.ClassroomHomework;
import fu.sep490.g23.backend.entity.classroom.ClassroomHomeworkSubmission;
import fu.sep490.g23.backend.entity.classroom.enums.HomeworkSubmissionStatus;
import fu.sep490.g23.backend.entity.classroom.enums.HomeworkSubmissionTiming;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ClassroomMapperHomeworkSubmissionTest {

    private final ClassroomMapper mapper = new ClassroomMapper(
            new HomeworkTextAnnotationCodec(),
            null, null, null, null, null, null, null
    );

    @Test
    void toHomeworkSubmissionResponse_DistinguishesOnTimeLateAndMissingSubmissions() {
        LocalDateTime deadline = LocalDateTime.of(2026, 8, 8, 20, 0);
        ClassroomHomework homework = ClassroomHomework.builder().id(10L).deadline(deadline).build();
        User onTimeStudent = student(1L, "An Nguyễn");
        User lateStudent = student(2L, "Bình Trần");
        User missingStudent = student(3L, "Chi Lê");

        ClassroomHomeworkSubmissionResponse onTime = mapper.toHomeworkSubmissionResponse(
                homework,
                onTimeStudent,
                submission(homework, onTimeStudent, deadline.minusMinutes(1))
        );
        ClassroomHomeworkSubmissionResponse late = mapper.toHomeworkSubmissionResponse(
                homework,
                lateStudent,
                submission(homework, lateStudent, deadline.plusMinutes(1))
        );
        ClassroomHomeworkSubmissionResponse missing = mapper.toHomeworkSubmissionResponse(homework, missingStudent, null);

        assertThat(onTime.getSubmissionTiming()).isEqualTo(HomeworkSubmissionTiming.ON_TIME);
        assertThat(late.getSubmissionTiming()).isEqualTo(HomeworkSubmissionTiming.LATE);
        assertThat(missing.getSubmissionTiming()).isEqualTo(HomeworkSubmissionTiming.NOT_SUBMITTED);
        assertThat(missing.isSubmitted()).isFalse();
        assertThat(missing.getStudentEmail()).isEqualTo("chi@example.com");
    }

    private User student(Long id, String name) {
        return User.builder()
                .id(id)
                .fullName(name)
                .email(name.startsWith("An") ? "an@example.com" : name.startsWith("Bình") ? "binh@example.com" : "chi@example.com")
                .build();
    }

    private ClassroomHomeworkSubmission submission(
            ClassroomHomework homework,
            User student,
            LocalDateTime submittedAt
    ) {
        return ClassroomHomeworkSubmission.builder()
                .homework(homework)
                .student(student)
                .submittedAt(submittedAt)
                .status(HomeworkSubmissionStatus.SUBMITTED)
                .build();
    }
}
