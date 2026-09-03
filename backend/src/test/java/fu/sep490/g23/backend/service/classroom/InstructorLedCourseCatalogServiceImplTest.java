package fu.sep490.g23.backend.service.classroom;

import fu.sep490.g23.backend.dto.response.classroom.InstructorLedCourseResponse;
import fu.sep490.g23.backend.entity.course.enums.PackageStatus;
import fu.sep490.g23.backend.entity.course.InstructorLedCourse;
import fu.sep490.g23.backend.repository.course.InstructorLedCourseRepository;
import fu.sep490.g23.backend.service.classroom.impl.InstructorLedCourseCatalogServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InstructorLedCourseCatalogServiceImplTest {

    @Mock
    private InstructorLedCourseRepository programRepository;


    private InstructorLedCourseCatalogServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new InstructorLedCourseCatalogServiceImpl(programRepository);
    }

    @Test
    void returnsCanonicalInstructorLedCourseSummary() {
        InstructorLedCourse program = InstructorLedCourse.builder()
                .id(21L)
                .title("TOEIC 650")
                .code("TOEIC-650")
                .examType("TOEIC")
                .entryLevel("TOEIC 350+")
                .targetScore(650)
                .learningOutcomes("Hoàn thành đủ 7 Part TOEIC.")
                .publicationStatus(PackageStatus.DRAFT)
                .build();
        when(programRepository.findById(21L)).thenReturn(Optional.of(program));

        InstructorLedCourseResponse response = service.getProgram(21L);

        assertThat(response.getEntryLevel()).isEqualTo("TOEIC 350+");
        assertThat(response.getTargetScore()).isEqualTo("650");
        assertThat(response.getTargetOutcome()).isEqualTo("Hoàn thành đủ 7 Part TOEIC.");
    }
}
