package fu.sep490.g23.backend.service.course;

import fu.sep490.g23.backend.dto.response.assessment.CourseAssessmentResponse;
import fu.sep490.g23.backend.dto.response.course.LessonResponse;
import fu.sep490.g23.backend.dto.response.course.ModuleResponse;
import fu.sep490.g23.backend.dto.response.course.OnlineCoursePreviewWarningResponse;
import fu.sep490.g23.backend.dto.response.course.OnlineCourseResponse;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OnlineCoursePreviewValidatorTest {

    private final OnlineCoursePreviewValidator validator = new OnlineCoursePreviewValidator();

    @Test
    void completeCourseHasNoValidationWarnings() {
        LessonResponse lesson = LessonResponse.builder()
                .title("Listening foundation")
                .contentType("VIDEO")
                .videoUrl("https://www.youtube.com/watch?v=example")
                .build();
        OnlineCourseResponse course = OnlineCourseResponse.builder()
                .title("IELTS Foundation")
                .description("Nội dung giới thiệu")
                .thumbnailUrl("https://cdn.example.com/course.jpg")
                .price(BigDecimal.valueOf(1_000_000))
                .modules(List.of(ModuleResponse.builder()
                        .title("Nền tảng")
                        .lessons(List.of(lesson))
                        .build()))
                .build();
        CourseAssessmentResponse assessment = CourseAssessmentResponse.builder()
                .title("Kiểm tra cuối mô-đun")
                .type(AssessmentType.MODULE_TEST)
                .skill(AssessmentSkill.LISTENING)
                .maxScore(BigDecimal.TEN)
                .build();

        assertThat(validator.validate(course, List.of(assessment))).isEmpty();
    }

    @Test
    void reportsMissingAndInvalidContentWithStableCodes() {
        OnlineCourseResponse course = OnlineCourseResponse.builder()
                .title("Draft course")
                .price(BigDecimal.ZERO)
                .modules(List.of(
                        ModuleResponse.builder().title("Mô-đun rỗng").lessons(List.of()).build(),
                        ModuleResponse.builder()
                                .title("Video")
                                .lessons(List.of(
                                        LessonResponse.builder()
                                                .title("Video lỗi")
                                                .contentType("VIDEO")
                                                .videoUrl("not-a-url")
                                                .build(),
                                        LessonResponse.builder()
                                                .title("Bài đọc trống")
                                                .contentType("ARTICLE")
                                                .build()
                                ))
                                .build()
                ))
                .build();
        CourseAssessmentResponse assessment = CourseAssessmentResponse.builder()
                .title("Assessment chưa hoàn chỉnh")
                .maxScore(BigDecimal.ZERO)
                .build();

        List<String> codes = validator.validate(course, List.of(assessment)).stream()
                .map(OnlineCoursePreviewWarningResponse::getCode)
                .toList();

        assertThat(codes).contains(
                "MISSING_THUMBNAIL",
                "MISSING_DESCRIPTION",
                "EMPTY_MODULE",
                "LESSON_WITHOUT_CONTENT",
                "INVALID_VIDEO_SOURCE",
                "INCOMPLETE_ASSESSMENT"
        );
    }

    @Test
    void reportsCourseWithoutModulesOrAssessments() {
        OnlineCourseResponse course = OnlineCourseResponse.builder()
                .thumbnailUrl("https://cdn.example.com/course.jpg")
                .description("Draft")
                .price(BigDecimal.ZERO)
                .modules(List.of())
                .build();

        List<String> codes = validator.validate(course, List.of()).stream()
                .map(OnlineCoursePreviewWarningResponse::getCode)
                .toList();

        assertThat(codes).containsExactlyInAnyOrder("NO_MODULES", "NO_ASSESSMENTS");
    }

    @Test
    void quizLessonRequiresAssessmentLinkedToThatLesson() {
        LessonResponse lesson = LessonResponse.builder()
                .id(77L)
                .title("Vocabulary quiz")
                .contentType("QUIZ")
                .build();
        OnlineCourseResponse course = OnlineCourseResponse.builder()
                .thumbnailUrl("https://cdn.example.com/course.jpg")
                .description("Draft")
                .price(BigDecimal.ZERO)
                .modules(List.of(ModuleResponse.builder().title("Vocabulary").lessons(List.of(lesson)).build()))
                .build();
        CourseAssessmentResponse unrelated = CourseAssessmentResponse.builder()
                .title("Module test")
                .type(AssessmentType.MODULE_TEST)
                .skill(AssessmentSkill.VOCABULARY)
                .maxScore(BigDecimal.TEN)
                .build();

        assertThat(validator.validate(course, List.of(unrelated))).extracting(OnlineCoursePreviewWarningResponse::getCode)
                .contains("LESSON_WITHOUT_ASSESSMENT");

        unrelated.setLessonId(77L);
        assertThat(validator.validate(course, List.of(unrelated))).extracting(OnlineCoursePreviewWarningResponse::getCode)
                .doesNotContain("LESSON_WITHOUT_ASSESSMENT", "LESSON_WITHOUT_CONTENT");
    }
}
