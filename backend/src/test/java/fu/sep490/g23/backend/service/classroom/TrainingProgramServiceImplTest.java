package fu.sep490.g23.backend.service.classroom;

import fu.sep490.g23.backend.dto.response.classroom.TrainingProgramResponse;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import fu.sep490.g23.backend.entity.course.enums.PackageStatus;
import fu.sep490.g23.backend.entity.course.InstructorLedCourse;
import fu.sep490.g23.backend.repository.course.InstructorLedCourseRepository;
import fu.sep490.g23.backend.service.classroom.impl.TrainingProgramServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainingProgramServiceImplTest {

    @Mock
    private InstructorLedCourseRepository programRepository;


    private TrainingProgramServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TrainingProgramServiceImpl(programRepository);
    }

    @Test
    void returnsAcademicSummaryFromLinkedCurriculum() {
        InstructorLedCourse curriculum = InstructorLedCourse.builder()
                .id(11L)
                .title("TOEIC 650")
                .code("TOEIC-650")
                .examType("TOEIC")
                .entryLevel("TOEIC 350+")
                .targetScore(650)
                .learningOutcomes("Hoàn thành đủ 7 Part TOEIC.")
                .publicationStatus(PackageStatus.PUBLISHED)
                .build();
        InstructorLedCourse program = InstructorLedCourse.builder()
                .id(21L)
                .title("TOEIC 650 Offline")
                .code("OFFLINE-TOEIC-650")
                .slug("toeic-650-offline")
                .examType(curriculum.getExamType())
                .entryLevel(curriculum.getEntryLevel())
                .targetScore(curriculum.getTargetScore())
                .learningOutcomes(curriculum.getLearningOutcomes())
                .publicationStatus(PackageStatus.DRAFT)
                .build();
        when(programRepository.findById(21L)).thenReturn(Optional.of(program));

        TrainingProgramResponse response = service.getProgram(21L);

        assertThat(response.getDeliveryType()).isNull();
        assertThat(response.getDeliveryMode()).isNull();
        assertThat(response.getEntryLevel()).isEqualTo(curriculum.getEntryLevel());
        assertThat(response.getTargetScore()).isEqualTo("650");
        assertThat(response.getTargetOutcome()).isEqualTo(curriculum.getLearningOutcomes());
    }
}
