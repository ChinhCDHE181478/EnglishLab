package fu.sep490.g23.backend.service.course;

import fu.sep490.g23.backend.dto.response.assessment.CourseAssessmentResponse;
import fu.sep490.g23.backend.dto.response.course.LessonResponse;
import fu.sep490.g23.backend.dto.response.course.ModuleResponse;
import fu.sep490.g23.backend.dto.response.course.OnlineCoursePreviewWarningResponse;
import fu.sep490.g23.backend.dto.response.course.OnlineCourseResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class OnlineCoursePreviewValidator {

    public List<OnlineCoursePreviewWarningResponse> validate(
            OnlineCourseResponse course,
            List<CourseAssessmentResponse> assessments
    ) {
        List<OnlineCoursePreviewWarningResponse> warnings = new ArrayList<>();

        if (isBlank(course.getThumbnailUrl())) {
            warnings.add(warning(
                    "MISSING_THUMBNAIL",
                    "WARNING",
                    "course.thumbnailUrl",
                    "Khóa học chưa có ảnh bìa."
            ));
        }
        if (course.getPrice() == null || course.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            warnings.add(warning(
                    "INVALID_PRICE",
                    "ERROR",
                    "course.price",
                    "Học phí phải là một giá trị không âm."
            ));
        }
        if (isBlank(course.getDescription()) && isBlank(course.getShortDescription())) {
            warnings.add(warning(
                    "MISSING_DESCRIPTION",
                    "WARNING",
                    "course.description",
                    "Khóa học chưa có mô tả để Content Manager kiểm tra trải nghiệm giới thiệu."
            ));
        }

        List<ModuleResponse> modules = course.getModules() == null ? List.of() : course.getModules();
        if (modules.isEmpty()) {
            warnings.add(warning(
                    "NO_MODULES",
                    "ERROR",
                    "course.modules",
                    "Khóa học chưa có mô-đun nào."
            ));
        }
        for (int moduleIndex = 0; moduleIndex < modules.size(); moduleIndex++) {
            ModuleResponse module = modules.get(moduleIndex);
            List<LessonResponse> lessons = module.getLessons() == null ? List.of() : module.getLessons();
            String moduleLocation = "modules[" + moduleIndex + "]";
            if (lessons.isEmpty()) {
                warnings.add(warning(
                        "EMPTY_MODULE",
                        "ERROR",
                        moduleLocation,
                        "Mô-đun \"" + safeTitle(module.getTitle(), moduleIndex + 1) + "\" chưa có bài học."
                ));
                continue;
            }
            for (int lessonIndex = 0; lessonIndex < lessons.size(); lessonIndex++) {
                validateLesson(warnings, lessons.get(lessonIndex), module, moduleIndex, lessonIndex);
            }
        }

        List<CourseAssessmentResponse> safeAssessments = assessments == null ? List.of() : assessments;
        if (safeAssessments.isEmpty()) {
            warnings.add(warning(
                    "NO_ASSESSMENTS",
                    "WARNING",
                    "course.assessments",
                    "Khóa học chưa cấu hình bài đánh giá nào."
            ));
        }
        for (int index = 0; index < safeAssessments.size(); index++) {
            CourseAssessmentResponse assessment = safeAssessments.get(index);
            if (isBlank(assessment.getTitle())
                    || assessment.getType() == null
                    || assessment.getSkill() == null
                    || assessment.getMaxScore() == null
                    || assessment.getMaxScore().compareTo(BigDecimal.ZERO) <= 0) {
                warnings.add(warning(
                        "INCOMPLETE_ASSESSMENT",
                        "ERROR",
                        "assessments[" + index + "]",
                        "Bài đánh giá \"" + safeAssessmentTitle(assessment, index + 1)
                                + "\" chưa có đủ loại, kỹ năng hoặc thang điểm hợp lệ."
                ));
            }
        }

        return List.copyOf(warnings);
    }

    private void validateLesson(
            List<OnlineCoursePreviewWarningResponse> warnings,
            LessonResponse lesson,
            ModuleResponse module,
            int moduleIndex,
            int lessonIndex
    ) {
        String location = "modules[" + moduleIndex + "].lessons[" + lessonIndex + "]";
        String lessonTitle = isBlank(lesson.getTitle()) ? "Bài học " + (lessonIndex + 1) : lesson.getTitle();
        boolean hasText = !isBlank(lesson.getContentText());
        boolean hasVideo = !isBlank(lesson.getVideoUrl()) || !isBlank(lesson.getBunnyCdnUrl());
        boolean hasMaterial = !isBlank(lesson.getMaterialUrl());
        boolean hasTranscript = lesson.getTranscriptSegments() != null && !lesson.getTranscriptSegments().isEmpty();
        boolean hasFlashcards = lesson.getFlashcardSets() != null && !lesson.getFlashcardSets().isEmpty();

        if (!hasText && !hasVideo && !hasMaterial && !hasTranscript && !hasFlashcards) {
            warnings.add(warning(
                    "LESSON_WITHOUT_CONTENT",
                    "ERROR",
                    location,
                    "Bài học \"" + lessonTitle + "\" trong mô-đun \""
                            + safeTitle(module.getTitle(), moduleIndex + 1) + "\" chưa có nội dung."
            ));
        }

        String contentType = String.valueOf(lesson.getContentType()).toUpperCase(Locale.ROOT);
        if (contentType.contains("VIDEO")) {
            String videoSource = !isBlank(lesson.getBunnyCdnUrl()) ? lesson.getBunnyCdnUrl() : lesson.getVideoUrl();
            if (isBlank(videoSource) || !isHttpUrl(videoSource)) {
                warnings.add(warning(
                        "INVALID_VIDEO_SOURCE",
                        "ERROR",
                        location + ".videoUrl",
                        "Bài học video \"" + lessonTitle + "\" chưa có nguồn video HTTP(S) hợp lệ."
                ));
            }
        }
    }

    private boolean isHttpUrl(String value) {
        try {
            URI uri = URI.create(value.trim());
            return "http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme());
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private OnlineCoursePreviewWarningResponse warning(String code, String severity, String location, String message) {
        return OnlineCoursePreviewWarningResponse.builder()
                .code(code)
                .severity(severity)
                .location(location)
                .message(message)
                .build();
    }

    private String safeTitle(String value, int fallbackIndex) {
        return isBlank(value) ? "Mô-đun " + fallbackIndex : value;
    }

    private String safeAssessmentTitle(CourseAssessmentResponse assessment, int fallbackIndex) {
        return isBlank(assessment.getTitle()) ? "Bài đánh giá " + fallbackIndex : assessment.getTitle();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
