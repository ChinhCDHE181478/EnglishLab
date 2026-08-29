package fu.sep490.g23.backend.service.course.impl;
import fu.sep490.g23.backend.entity.course.OnlineCourseEnrollment;
import fu.sep490.g23.backend.entity.course.CourseLessonFlashcardRef;
import fu.sep490.g23.backend.entity.course.OnlineCourseModule;
import fu.sep490.g23.backend.entity.course.VocabularyProgress;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.entity.course.OnlineLesson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fu.sep490.g23.backend.dto.response.course.VocabularyTermResponse;
import fu.sep490.g23.backend.dto.response.course.LessonResponse;
import fu.sep490.g23.backend.dto.response.course.ModuleResponse;
import fu.sep490.g23.backend.dto.response.course.OnlineCourseResponse;
import fu.sep490.g23.backend.dto.response.curriculum.FlashcardSetResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.commerce.CourseListItem;
import fu.sep490.g23.backend.entity.commerce.enums.CourseListType;
import fu.sep490.g23.backend.entity.course.enums.FlashcardPracticeSource;
import fu.sep490.g23.backend.entity.course.enums.VocabularyProgressStatus;
import fu.sep490.g23.backend.entity.course.*;
import fu.sep490.g23.backend.entity.curriculum.ContentBankItem;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.commerce.CourseListItemRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseEnrollmentRepository;
import fu.sep490.g23.backend.repository.course.VocabularyProgressRepository;
import fu.sep490.g23.backend.service.course.FlashcardPracticeService;
import fu.sep490.g23.backend.service.course.OnlineCourseVersionService;
import fu.sep490.g23.backend.service.curriculum.ContentBankPayloadSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FlashcardPracticeServiceImpl implements FlashcardPracticeService {
    private static final Pattern VOCABULARY_HEADING = Pattern.compile("(?m)^###\\s+\\d+\\.\\s+(.+)$");

    private final UserRepository userRepository;
    private final OnlineCourseRepository onlineCourseRepository;
    private final OnlineCourseEnrollmentRepository enrollmentRepository;
    private final CourseListItemRepository courseListItemRepository;
    private final VocabularyProgressRepository progressRepository;
    private final OnlineCourseVersionService onlineCourseVersionService;
    private final ObjectMapper objectMapper = new ObjectMapper();





    private VocabularyTermResponse.VocabularyTermResponseBuilder baseTerm(OnlineCourse course, OnlineCourseModule module, OnlineLesson lesson) {
        return VocabularyTermResponse.builder()
                .courseId(course.getId())
                .courseTitle(course.getTitle())
                .moduleId(module.getId())
                .moduleTitle(module.getTitle())
                .lessonId(lesson.getId())
                .lessonTitle(lesson.getTitle())
                .status(VocabularyProgressStatus.NEW)
                .starred(false)
                .reviewCount(0)
                .correctCount(0)
                .incorrectCount(0);
    }

    private VocabularyTermResponse.VocabularyTermResponseBuilder baseTerm(
            OnlineCourseResponse course,
            ModuleResponse module,
            LessonResponse lesson
    ) {
        return VocabularyTermResponse.builder()
                .courseId(course.getId())
                .courseTitle(course.getTitle())
                .moduleId(module.getId())
                .moduleTitle(module.getTitle())
                .lessonId(lesson.getId())
                .lessonTitle(lesson.getTitle())
                .status(VocabularyProgressStatus.NEW)
                .starred(false)
                .reviewCount(0)
                .correctCount(0)
                .incorrectCount(0);
    }





    private String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = clean(node.path(field).asText(""));
            if (!value.isBlank()) return value;
        }
        return "";
    }

    private String field(String block, String label) {
        var matcher = Pattern.compile("(?m)^\\*\\*" + Pattern.quote(label) + ":\\*\\*\\s*(.+)$", Pattern.CASE_INSENSITIVE).matcher(block);
        return matcher.find() ? clean(matcher.group(1)) : "";
    }

    private String clean(String value) {
        return value == null ? "" : value.replace("**", "").replaceAll("^[\"']|[\"']$", "").trim();
    }

    private String firstNonBlank(String first, String second) {
        return first == null || first.isBlank() ? second : first;
    }

    private String slug(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return normalized.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
    }
}
