package fu.sap490.g23.backend.seed;

import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.classroom.ClassroomOffering;
import fu.sap490.g23.backend.entity.classroom.enums.ClassroomRegistrationStatus;
import fu.sap490.g23.backend.repository.UserRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomEnrollmentRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomHomeworkRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomMaterialRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomOfferingRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomSessionRepository;
import fu.sap490.g23.backend.repository.curriculum.CurriculumUnitRepository;
import fu.sap490.g23.backend.service.classroom.ClassroomMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ToeicShowcaseClassroomSeederIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClassroomOfferingRepository offeringRepository;

    @Autowired
    private ClassroomEnrollmentRepository enrollmentRepository;

    @Autowired
    private ClassroomSessionRepository sessionRepository;

    @Autowired
    private ClassroomHomeworkRepository homeworkRepository;

    @Autowired
    private ClassroomMaterialRepository materialRepository;

    @Autowired
    private CurriculumUnitRepository curriculumUnitRepository;

    @Autowired
    private ClassroomMapper classroomMapper;

    @Test
    void createsCompleteToeicClassForRequestedLearner() {
        User learner = userRepository.findByEmail("0386852628z@gmail.com").orElseThrow();
        ClassroomOffering offering = offeringRepository
                .findByLearningPackageSlug("toeic-650-showcase-class-0386852628z")
                .orElseThrow();

        assertThat(offering.getCurriculumProgram()).isNotNull();
        assertThat(offering.getTrainingProgram()).isNotNull();
        assertThat(curriculumUnitRepository.findByProgramIdOrderByDisplayOrderAscIdAsc(
                offering.getCurriculumProgram().getId())).hasSize(8);
        var learnerResponse = classroomMapper.toOfferingResponse(offering, true, learner.getId(), null, true);
        assertThat(learnerResponse.getCurriculumProgram().getUnits()).hasSize(8)
                .allSatisfy(unit -> {
                    assertThat(unit.getMaterials()).hasSizeGreaterThanOrEqualTo(1);
                    assertThat(unit.getExercises()).hasSizeGreaterThanOrEqualTo(1);
                    assertThat(unit.getFlashcards()).hasSize(1);
                    assertThat(unit.getFlashcards().getFirst().getContentJson()).contains("\"front\"");
                });
        assertThat(learnerResponse.getCurriculumProgram().getUnits().get(4).getAssessments())
                .hasSizeGreaterThanOrEqualTo(1);
        assertThat(learnerResponse.getCurriculumProgram().getUnits().getFirst().getFlashcards().getFirst().getContentJson())
                .isNotEqualTo(learnerResponse.getCurriculumProgram().getUnits().get(1).getFlashcards().getFirst().getContentJson());
        assertThat(sessionRepository.findByClassroomOfferingIdOrderBySessionDateAscStartTimeAsc(
                offering.getId())).hasSize(8).allSatisfy(session -> {
                    assertThat(session.getRecordingUrl()).isNull();
                    assertThat(session.getRecordingVisible()).isFalse();
                });
        assertThat(materialRepository.findByClassroomOfferingIdOrderByCreatedAtDesc(
                offering.getId())).hasSizeGreaterThanOrEqualTo(8);
        var classroomHomework = homeworkRepository.findByClassroomOfferingIdOrderByCreatedAtDesc(offering.getId());
        assertThat(classroomHomework).hasSizeGreaterThanOrEqualTo(8).extracting("title").contains(
                "Unit 1 Quiz - Photographs",
                "Unit 2 Worksheet - Nộp file",
                "Unit 3 Flashcard Review",
                "Unit 4 Short Talks - Listening Summary",
                "Unit 5 Module Test - Incomplete Sentences",
                "Unit 6 Text Completion - System Practice",
                "Unit 7 Error Log - Reading",
                "Unit 8 Full Test Strategy - Nộp kế hoạch"
        );
        assertThat(classroomHomework).extracting(item -> item.getCurriculumUnit().getDisplayOrder())
                .contains(1, 2, 3, 4, 5, 6, 7, 8);
        assertThat(classroomHomework.stream()
                .filter(item -> "Unit 5 Module Test - Incomplete Sentences".equals(item.getTitle()))
                .findFirst()).get()
                .satisfies(item -> {
                    assertThat(item.getActivityConfigJson()).contains("\"parts\"");
                    assertThat(item.getInstruction()).contains("Module Test");
                    assertThat(item.getCurriculumUnit().getDisplayOrder()).isEqualTo(5);
                });
        assertThat(enrollmentRepository.findByStudentIdAndClassroomOfferingId(
                learner.getId(), offering.getId())).get()
                .extracting("registrationStatus")
                .isEqualTo(ClassroomRegistrationStatus.ASSIGNED);
    }
}
