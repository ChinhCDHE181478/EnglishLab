package fu.sep490.g23.backend.seed;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.classroom.ClassSection;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomRegistrationStatus;
import fu.sep490.g23.backend.entity.classroom.enums.HomeworkGradingMode;
import fu.sep490.g23.backend.entity.classroom.enums.HomeworkSubmissionStatus;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.classroom.ClassEnrollmentRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomHomeworkRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomHomeworkSubmissionRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomMaterialRepository;
import fu.sep490.g23.backend.repository.classroom.ClassSectionRepository;
import fu.sep490.g23.backend.repository.classroom.ClassScheduleRepository;
import fu.sep490.g23.backend.repository.course.CourseUnitRepository;
import fu.sep490.g23.backend.service.classroom.ClassroomMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ToeicShowcaseClassroomSeederIntegrationTest {

    private static final Set<String> SEEDED_UNIT_TITLES = Set.of(
            "Unit 1 - Photographs & mô tả hành động",
            "Unit 2 - Question-Response",
            "Unit 3 - Conversations",
            "Unit 4 - Short Talks",
            "Unit 5 - Incomplete Sentences",
            "Unit 6 - Text Completion",
            "Unit 7 - Reading Comprehension",
            "Unit 8 - Full Test Strategy"
    );

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClassSectionRepository offeringRepository;

    @Autowired
    private ClassEnrollmentRepository enrollmentRepository;

    @Autowired
    private ClassScheduleRepository sessionRepository;

    @Autowired
    private ClassroomHomeworkRepository homeworkRepository;

    @Autowired
    private ClassroomHomeworkSubmissionRepository homeworkSubmissionRepository;

    @Autowired
    private ClassroomMaterialRepository materialRepository;

    @Autowired
    private CourseUnitRepository courseUnitRepository;

    @Autowired
    private ClassroomMapper classroomMapper;

    @Test
    void createsCompleteToeicClassForRequestedLearner() {
        User learner = userRepository.findByEmail("0386852628z@gmail.com").orElseThrow();
        ClassSection offering = offeringRepository
                .findByInstructorLedCourseSlugOrCode("toeic-650-showcase-class-0386852628z")
                .orElseThrow();

        assertThat(offering.getInstructorLedCourse()).isNotNull();
        var seededUnits = courseUnitRepository.findByInstructorLedCourseIdOrderBySequenceNumberAscIdAsc(
                        offering.getInstructorLedCourse().getId()).stream()
                .filter(unit -> SEEDED_UNIT_TITLES.contains(unit.getTitle()))
                .toList();
        assertThat(seededUnits).hasSize(8);
        var learnerResponse = classroomMapper.toOfferingResponse(offering, true, learner.getId(), null, true);
        var seededUnitResponses = learnerResponse.getInstructorLedCourse().getUnits().stream()
                .filter(unit -> SEEDED_UNIT_TITLES.contains(unit.getTitle()))
                .toList();
        assertThat(seededUnitResponses).hasSize(8)
                .allSatisfy(unit -> {
                    assertThat(unit.getMaterials()).hasSizeGreaterThanOrEqualTo(1);
                    assertThat(unit.getExercises()).hasSizeGreaterThanOrEqualTo(1);
                    assertThat(unit.getFlashcards()).hasSize(1);
                    assertThat(unit.getFlashcards().getFirst().getContentJson()).contains("\"front\"");
                });
        assertThat(seededUnitResponses.get(4).getAssessments())
                .hasSizeGreaterThanOrEqualTo(1);
        assertThat(seededUnitResponses.get(4).getAssessments())
                .anySatisfy(assessment -> {
                    assertThat(assessment.getTitle()).isEqualTo("TOEIC 650 Unit 5 Progress Check - Incomplete Sentences");
                    assertThat(assessment.getSubtitle()).isEqualTo("QUIZ");
                })
                .noneSatisfy(assessment -> assertThat(assessment.getSubtitle()).isEqualTo("MODULE_TEST"));
        assertThat(seededUnitResponses.getFirst().getFlashcards().getFirst().getContentJson())
                .isNotEqualTo(seededUnitResponses.get(1).getFlashcards().getFirst().getContentJson());
        assertThat(sessionRepository.findByClassSectionIdOrderBySessionDateAscStartTimeAsc(
                offering.getId())).hasSize(8).allSatisfy(session -> {
                    assertThat(session.getRecordingUrl()).isNull();
                    assertThat(session.getRecordingVisible()).isFalse();
                });
        assertThat(materialRepository.findByClassSectionIdOrderByCreatedAtDesc(
                offering.getId())).hasSizeGreaterThanOrEqualTo(8);
        var classroomHomework = homeworkRepository.findByClassSectionIdOrderByCreatedAtDesc(offering.getId());
        assertThat(classroomHomework).hasSizeGreaterThanOrEqualTo(8).extracting("title").contains(
                "Unit 1 Quiz - Photographs",
                "Unit 2 Worksheet - Nộp file",
                "Unit 3 Flashcard Review",
                "Unit 4 Short Talks - Listening Summary",
                "Unit 5 Progress Check - Incomplete Sentences",
                "Unit 6 Text Completion - System Practice",
                "Unit 7 Error Log - Reading",
                "Unit 8 Full Test Strategy - Nộp kế hoạch"
        );
        assertThat(classroomHomework).extracting(item -> item.getCourseUnit().getSequenceNumber())
                .contains(1, 2, 3, 4, 5, 6, 7, 8);
        assertThat(classroomHomework.stream()
                .filter(item -> "Unit 5 Progress Check - Incomplete Sentences".equals(item.getTitle()))
                .findFirst()).get()
                .satisfies(item -> {
                    assertThat(item.getActivityConfigJson()).contains("\"parts\"");
                    assertThat(item.getInstruction()).contains("kiểm tra tiến độ bắt buộc");
                    assertThat(item.getCourseUnit().getSequenceNumber()).isEqualTo(5);
                    assertThat(item.getGradingMode()).isEqualTo(HomeworkGradingMode.AUTO);
                });
        var quiz = classroomHomework.stream()
                .filter(item -> "Unit 1 Quiz - Photographs".equals(item.getTitle()))
                .findFirst()
                .orElseThrow();
        assertThat(quiz.getGradingMode()).isEqualTo(HomeworkGradingMode.AUTO);
        assertThat(homeworkSubmissionRepository.findByHomeworkId(quiz.getId()).stream()
                .filter(submission -> Set.of(
                        "classroom.learner2@englishlab.vn",
                        "classroom.learner3@englishlab.vn"
                ).contains(submission.getStudent().getEmail()))
                .toList())
                .hasSize(2)
                .allSatisfy(submission -> {
                    assertThat(submission.getStatus()).isEqualTo(HomeworkSubmissionStatus.GRADED);
                    assertThat(submission.getScore()).isNotNull();
                });

        var writing = classroomHomework.stream()
                .filter(item -> "Unit 7 Error Log - Reading".equals(item.getTitle()))
                .findFirst()
                .orElseThrow();
        assertThat(homeworkSubmissionRepository.findByHomeworkId(writing.getId()).stream()
                .filter(submission -> Set.of(
                        "classroom.learner2@englishlab.vn",
                        "classroom.learner3@englishlab.vn",
                        "classroom.learner4@englishlab.vn"
                ).contains(submission.getStudent().getEmail()))
                .toList())
                .hasSize(3)
                .anySatisfy(submission -> assertThat(submission.getSubmittedAt()).isAfter(writing.getDeadline()))
                .anySatisfy(submission -> assertThat(submission.getStatus()).isEqualTo(HomeworkSubmissionStatus.SUBMITTED))
                .anySatisfy(submission -> assertThat(submission.getStatus()).isEqualTo(HomeworkSubmissionStatus.GRADED));

        var speaking = classroomHomework.stream()
                .filter(item -> "Unit 3 Speaking Retell - Conversations".equals(item.getTitle()))
                .findFirst()
                .orElseThrow();
        assertThat(homeworkSubmissionRepository.findByHomeworkId(speaking.getId()).stream()
                .filter(submission -> "classroom.learner2@englishlab.vn".equals(submission.getStudent().getEmail()))
                .toList())
                .singleElement()
                .satisfies(submission -> {
                    assertThat(submission.getStatus()).isEqualTo(HomeworkSubmissionStatus.SUBMITTED);
                    assertThat(submission.getAttachmentUrl()).endsWith("unit-3-speaking-submission.wav");
                });
        assertThat(enrollmentRepository.findByStudentIdAndClassSectionId(
                learner.getId(), offering.getId())).get()
                .extracting("registrationStatus")
                .isEqualTo(ClassroomRegistrationStatus.ASSIGNED);
    }
}
