package fu.sep490.g23.backend.service.course;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fu.sep490.g23.backend.dto.response.curriculum.FlashcardSetResponse;
import fu.sep490.g23.backend.dto.response.course.LessonResponse;
import fu.sep490.g23.backend.dto.response.course.ModuleResponse;
import fu.sep490.g23.backend.dto.response.course.OnlineCourseResponse;
import fu.sep490.g23.backend.dto.response.course.OnlineCourseEnrollmentResponse;
import fu.sep490.g23.backend.dto.response.course.TranscriptSegmentResponse;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sep490.g23.backend.entity.course.CourseCategory;
import fu.sep490.g23.backend.entity.course.OnlineCourseModule;
import fu.sep490.g23.backend.entity.course.OnlineLesson;
import fu.sep490.g23.backend.entity.course.LessonProgress;
import fu.sep490.g23.backend.entity.course.enums.LessonProgressStatus;
import fu.sep490.g23.backend.entity.curriculum.ContentBankItem;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.entity.course.OnlineCourseEnrollment;
import fu.sep490.g23.backend.entity.course.OnlineCourseVersion;
import fu.sep490.g23.backend.entity.course.enums.CourseVersionStatus;
import fu.sep490.g23.backend.repository.assessment.CourseAssessmentRepository;
import fu.sep490.g23.backend.repository.course.LessonProgressRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseEnrollmentRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseVersionRepository;
import fu.sep490.g23.backend.service.curriculum.ContentBankPayloadSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class OnlineCourseMapper {

    private final OnlineCourseRepository onlineCourseRepository;
    private final OnlineCourseVersionRepository onlineCourseVersionRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final OnlineCourseEnrollmentRepository packageEnrollmentRepository;
    private final CourseAssessmentRepository courseAssessmentRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OnlineCourseResponse toResponse(OnlineCourse course) {
        return toResponse(course, false, null, null, true, resolveWorkingModules(course));
    }

    public OnlineCourseResponse toResponse(OnlineCourse course, List<OnlineCourseModule> modulesOverride) {
        return toResponse(course, false, null, null, true, modulesOverride);
    }

    public OnlineCourseResponse toPublicResponse(OnlineCourse course) {
        return toResponse(course, false, null, null, false, resolveWorkingModules(course));
    }

    public OnlineCourseResponse toResponse(OnlineCourse course, boolean registered, Integer progressPercent, Long enrollmentId) {
        return toResponse(course, registered, progressPercent, enrollmentId, true, resolveWorkingModules(course));
    }

    private OnlineCourseResponse toResponse(
            OnlineCourse course,
            boolean registered,
            Integer progressPercent,
            Long enrollmentId,
            boolean includeLessonContent,
            List<OnlineCourseModule> modules
    ) {
        CourseCategory category = course.getCategory();
        BigDecimal originalPrice = safePrice(course.getPrice());
        BigDecimal salePrice = resolveSalePrice(course);
        Long packageId = null == null ? null : course.getId();
        List<OnlineCourseModule> effectiveModules = modules == null ? List.of() : modules;
        return OnlineCourseResponse.builder()
                .id(course.getId())
                .packageId(packageId)
                .title(course.getTitle())
                .slug(course.getSlug())
                .shortDescription(course.getShortDescription())
                .description(course.getDescription())
                .category(category == null ? null : category.getCode())
                .categoryName(category == null ? null : category.getName())
                .level(course.getLevel())
                .status(course.getStatus())
                .targetScore(course.getTargetScore())
                .recommendedCurrentBandMin(course.getRecommendedCurrentBandMin())
                .targetBand(course.getTargetBand())
                .targetOutcome(course.getTargetOutcome())
                .duration(course.getDuration())
                .price(originalPrice)
                .originalPrice(originalPrice)
                .salePrice(salePrice)
                .discountPercent(resolveDiscountPercent(originalPrice, salePrice))
                .thumbnailUrl(course.getThumbnailUrl())
                .totalLessons(course.getTotalLessons())
                .totalHours(course.getTotalHours())
                .featured(course.isFeatured())
                .registered(registered)
                .progressPercent(progressPercent)
                .enrollmentId(enrollmentId)
                .enrollmentCount(packageEnrollmentRepository.countByOnlineCourse(course))
                .averageRating(resolveAverageRating(course))
                .reviewCount(packageEnrollmentRepository.countByOnlineCourseAndReviewRatingIsNotNull(course))
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .focusSkills(resolveFocusSkills(course, effectiveModules))
                .modules(toModuleResponses(effectiveModules, includeLessonContent))
                .build();
    }

    public OnlineCourseEnrollmentResponse toEnrollmentResponse(OnlineCourseEnrollment enrollment) {
        OnlineCourse course = enrollment.getOnlineCourse() != null
                ? enrollment.getOnlineCourse()
                : enrollment.getOnlineCourse();
        Long courseId = course == null ? null : course.getId();
        String title = course != null ? course.getTitle() : enrollment.getOnlineCourse() != null ? enrollment.getOnlineCourse().getTitle() : null;
        String slug = course != null ? course.getSlug() : enrollment.getOnlineCourse() != null ? enrollment.getOnlineCourse().getSlug() : null;
        String thumbnail = course != null ? course.getThumbnailUrl() : enrollment.getOnlineCourse() != null ? enrollment.getOnlineCourse().getThumbnailUrl() : null;
        List<LessonProgress> completedProgress = lessonProgressRepository.findByEnrollmentAndStatusOrderByCompletedAtDesc(enrollment, LessonProgressStatus.COMPLETED);
        return OnlineCourseEnrollmentResponse.builder()
                .id(enrollment.getId())
                .packageId(enrollment.getId())
                .courseId(courseId)
                .courseVersionId(enrollment.getCourseVersion() == null ? null : enrollment.getCourseVersion().getId())
                .courseVersionNumber(enrollment.getCourseVersion() == null ? null : enrollment.getCourseVersion().getVersionNumber())
                .courseTitle(title)
                .courseSlug(slug)
                .thumbnailUrl(thumbnail)
                .status(enrollment.getStatus())
                .progressPercent(enrollment.getProgressPercent())
                .streakDays(calculateStreakDays(completedProgress))
                .registeredAt(enrollment.getRegisteredAt())
                .completedLessonIds(completedProgress.stream()
                        .map(progress -> progress.getLesson().getId())
                        .toList())
                .build();
    }

    private List<ModuleResponse> toModuleResponses(List<OnlineCourseModule> modules, boolean includeLessonContent) {
        return modules.stream()
                .sorted(Comparator.comparing(OnlineCourseModule::getSequenceNumber).thenComparing(OnlineCourseModule::getId))
                .map(module -> ModuleResponse.builder()
                        .id(module.getId())
                        .title(module.getTitle())
                        .description(module.getDescription())
                        .displayOrder(module.getSequenceNumber())
                        .lessons(toLessonResponses(module.getLessons(), includeLessonContent))
                        .build())
                .toList();
    }

    private List<LessonResponse> toLessonResponses(List<OnlineLesson> lessons, boolean includeLessonContent) {
        return lessons.stream()
                .sorted(Comparator.comparing(OnlineLesson::getSequenceNumber).thenComparing(OnlineLesson::getId))
                .map(lesson -> {
                    boolean exposeContent = includeLessonContent || lesson.isPreview();
                    return LessonResponse.builder()
                        .id(lesson.getId())
                        .lessonKey(lesson.getLessonKey())
                        .title(lesson.getTitle())
                        .description(lesson.getDescription())
                        .contentType(lesson.getContentType())
                        .contentText(exposeContent ? lesson.getContentText() : null)
                        .videoUrl(exposeContent ? lesson.getVideoUrl() : null)
                        .materialUrl(exposeContent ? lesson.getMaterialUrl() : null)
                        .transcriptSegments(exposeContent ? parseTranscriptSegments(lesson.getTranscriptSegmentsJson()) : List.of())
                        .flashcardSets(exposeContent ? toFlashcardSetResponses(lesson) : List.of())
                        .durationMinutes(lesson.getDurationMinutes())
                        .displayOrder(lesson.getSequenceNumber())
                        .preview(lesson.isPreview())
                        .build();
                })
                .toList();
    }

    private int calculateStreakDays(List<LessonProgress> completedProgress) {
        List<LocalDate> completedDates = completedProgress.stream()
                .filter(progress -> progress.getCompletedAt() != null)
                .map(progress -> progress.getCompletedAt().toLocalDate())
                .distinct()
                .sorted(Comparator.reverseOrder())
                .toList();

        if (completedDates.isEmpty()) {
            return 0;
        }

        LocalDate cursor = LocalDate.now();
        if (!completedDates.contains(cursor)) {
            cursor = cursor.minusDays(1);
            if (!completedDates.contains(cursor)) {
                return 0;
            }
        }

        int streak = 0;
        for (LocalDate completedDate : completedDates) {
            if (completedDate.equals(cursor)) {
                streak++;
                cursor = cursor.minusDays(1);
            } else if (completedDate.isBefore(cursor)) {
                break;
            }
        }
        return streak;
    }

    private List<String> resolveFocusSkills(OnlineCourse course, List<OnlineCourseModule> modules) {
        Set<String> skills = new LinkedHashSet<>();

        courseAssessmentRepository.findByOnlineCourseAndActiveTrueOrderByDisplayOrderAscIdAsc(course).stream()
                .map(assessment -> {
                    AssessmentSkill skill = assessment.getAssessmentBankItem() == null
                            ? assessment.getSkill()
                            : assessment.getAssessmentBankItem().getSkill();
                    return skill == null ? null : skill.name();
                })
                .filter(skill -> skill != null && !skill.isBlank())
                .forEach(skills::add);

        if (skills.isEmpty() && modules != null) {
            modules.stream()
                    .flatMap(module -> module.getLessons().stream())
                    .forEach(lesson -> inferSkillsFromLesson(lesson, skills));
        }

        return List.copyOf(skills);
    }

    private List<OnlineCourseModule> resolveWorkingModules(OnlineCourse course) {
        if (course == null) {
            return List.of();
        }
        if (course.getId() != null) {
            List<OnlineCourseVersion> versions = onlineCourseVersionRepository.findByOnlineCourseOrderByVersionNumberDesc(course);
            for (CourseVersionStatus status : List.of(
                    CourseVersionStatus.DRAFT,
                    CourseVersionStatus.PENDING_REVIEW,
                    CourseVersionStatus.PUBLISHED
            )) {
                OnlineCourseVersion match = versions.stream()
                        .filter(version -> version.getStatus() == status)
                        .findFirst()
                        .orElse(null);
                if (match != null) {
                    List<OnlineCourseModule> modules = match.getModules();
                    if (modules != null) {
                        modules.forEach(module -> module.getLessons().size());
                        return modules;
                    }
                }
            }
        }
        return course.getLatestModules();
    }

    private void inferSkillsFromLesson(OnlineLesson lesson, Set<String> skills) {
        String content = String.join(" ",
                safe(lesson.getTitle()),
                safe(lesson.getDescription()),
                safe(lesson.getContentText())
        ).toLowerCase(Locale.ROOT);

        for (AssessmentSkill skill : AssessmentSkill.values()) {
            String token = skill.name().toLowerCase(Locale.ROOT);
            if (content.contains(token)) {
                skills.add(skill.name());
            }
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private BigDecimal safePrice(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Double resolveAverageRating(OnlineCourse course) {
        Double average = packageEnrollmentRepository.findAverageReviewRatingByOnlineCourse(course);
        return average == null ? 0D : BigDecimal.valueOf(average).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private BigDecimal resolveSalePrice(OnlineCourse course) {
        BigDecimal originalPrice = safePrice(course.getPrice());
        BigDecimal salePrice = course.getSalePrice();
        if (salePrice == null || salePrice.compareTo(BigDecimal.ZERO) < 0 || salePrice.compareTo(originalPrice) >= 0) {
            return originalPrice;
        }
        return salePrice;
    }

    private Integer resolveDiscountPercent(BigDecimal originalPrice, BigDecimal salePrice) {
        if (originalPrice == null || salePrice == null || originalPrice.compareTo(BigDecimal.ZERO) <= 0 || salePrice.compareTo(originalPrice) >= 0) {
            return 0;
        }
        return originalPrice.subtract(salePrice)
                .multiply(BigDecimal.valueOf(100))
                .divide(originalPrice, 0, RoundingMode.HALF_UP)
                .intValue();
    }

    private List<TranscriptSegmentResponse> parseTranscriptSegments(String transcriptSegmentsJson) {
        if (transcriptSegmentsJson == null || transcriptSegmentsJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(transcriptSegmentsJson, new TypeReference<>() {
            });
        } catch (Exception ex) {
            return List.of();
        }
    }

    private List<FlashcardSetResponse> toFlashcardSetResponses(OnlineLesson lesson) {
        if (lesson.getFlashcardRefs() == null) {
            return List.of();
        }
        return lesson.getFlashcardRefs().stream()
                .map(ref -> toFlashcardSetResponse(ref.getContentBankItem()))
                .filter(response -> response != null)
                .toList();
    }

    private FlashcardSetResponse toFlashcardSetResponse(ContentBankItem item) {
        if (item == null) {
            return null;
        }
        return FlashcardSetResponse.builder()
                .id(item.getId())
                .title(item.getTitle())
                .description(item.getDescription())
                .examCategory(item.getExamCategory())
                .skill(item.getSkill())
                .tags(item.getTags())
                .cardsJson(ContentBankPayloadSupport.cardsJsonFromPayload(item.getContentData()))
                .status(item.getStatus())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }
}
