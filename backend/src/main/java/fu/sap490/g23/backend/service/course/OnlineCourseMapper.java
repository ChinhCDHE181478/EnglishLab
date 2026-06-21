package fu.sap490.g23.backend.service.course;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fu.sap490.g23.backend.dto.response.course.LessonResponse;
import fu.sap490.g23.backend.dto.response.course.ModuleResponse;
import fu.sap490.g23.backend.dto.response.course.OnlineCourseResponse;
import fu.sap490.g23.backend.dto.response.course.PackageEnrollmentResponse;
import fu.sap490.g23.backend.dto.response.course.TranscriptSegmentResponse;
import fu.sap490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sap490.g23.backend.entity.course.CourseCategory;
import fu.sap490.g23.backend.entity.course.CourseModule;
import fu.sap490.g23.backend.entity.course.LearningPackage;
import fu.sap490.g23.backend.entity.course.Lesson;
import fu.sap490.g23.backend.entity.course.LessonProgress;
import fu.sap490.g23.backend.entity.course.enums.LessonProgressStatus;
import fu.sap490.g23.backend.entity.course.OnlineCourse;
import fu.sap490.g23.backend.entity.course.PackageEnrollment;
import fu.sap490.g23.backend.repository.assessment.CourseAssessmentRepository;
import fu.sap490.g23.backend.repository.course.LessonProgressRepository;
import fu.sap490.g23.backend.repository.course.OnlineCourseRepository;
import fu.sap490.g23.backend.repository.course.PackageEnrollmentRepository;
import fu.sap490.g23.backend.repository.course.CourseReviewRepository;
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
    private final LessonProgressRepository lessonProgressRepository;
    private final PackageEnrollmentRepository packageEnrollmentRepository;
    private final CourseReviewRepository courseReviewRepository;
    private final CourseAssessmentRepository courseAssessmentRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OnlineCourseResponse toResponse(OnlineCourse course) {
        return toResponse(course, false, null, null, true);
    }

    public OnlineCourseResponse toPublicResponse(OnlineCourse course) {
        return toResponse(course, false, null, null, false);
    }

    public OnlineCourseResponse toResponse(OnlineCourse course, boolean registered, Integer progressPercent, Long enrollmentId) {
        return toResponse(course, registered, progressPercent, enrollmentId, true);
    }

    private OnlineCourseResponse toResponse(OnlineCourse course, boolean registered, Integer progressPercent, Long enrollmentId, boolean includeLessonContent) {
        LearningPackage learningPackage = course.getLearningPackage();
        CourseCategory category = course.getCategory();
        BigDecimal originalPrice = safePrice(learningPackage.getPrice());
        BigDecimal salePrice = resolveSalePrice(learningPackage);
        return OnlineCourseResponse.builder()
                .id(course.getId())
                .packageId(learningPackage.getId())
                .title(learningPackage.getTitle())
                .slug(learningPackage.getSlug())
                .shortDescription(learningPackage.getShortDescription())
                .description(learningPackage.getDescription())
                .category(category == null ? null : category.getCode())
                .categoryName(category == null ? null : category.getName())
                .level(course.getLevel())
                .status(learningPackage.getStatus())
                .targetScore(learningPackage.getTargetScore())
                .recommendedCurrentBandMin(course.getRecommendedCurrentBandMin())
                .recommendedCurrentBandMax(course.getRecommendedCurrentBandMax())
                .targetBand(course.getTargetBand())
                .learningPathCode(course.getLearningPathCode())
                .learningPathName(course.getLearningPathName())
                .learningPathOrder(course.getLearningPathOrder())
                .targetOutcome(course.getTargetOutcome())
                .recommendedNextCourseSlug(course.getRecommendedNextCourseSlug())
                .duration(learningPackage.getDuration())
                .studyMode(learningPackage.getStudyMode())
                .price(originalPrice)
                .originalPrice(originalPrice)
                .salePrice(salePrice)
                .discountPercent(resolveDiscountPercent(originalPrice, salePrice))
                .thumbnailUrl(learningPackage.getThumbnailUrl())
                .totalLessons(course.getTotalLessons())
                .totalHours(course.getTotalHours())
                .displayOrder(learningPackage.getDisplayOrder())
                .featured(learningPackage.isFeatured())
                .registered(registered)
                .progressPercent(progressPercent)
                .enrollmentId(enrollmentId)
                .enrollmentCount(packageEnrollmentRepository.countByLearningPackage(learningPackage))
                .averageRating(resolveAverageRating(course))
                .reviewCount(courseReviewRepository.countByCourse(course))
                .createdAt(learningPackage.getCreatedAt())
                .updatedAt(learningPackage.getUpdatedAt())
                .focusSkills(resolveFocusSkills(course))
                .modules(toModuleResponses(course.getModules(), includeLessonContent))
                .build();
    }

    public PackageEnrollmentResponse toEnrollmentResponse(PackageEnrollment enrollment) {
        LearningPackage learningPackage = enrollment.getLearningPackage();
        Long courseId = onlineCourseRepository.findByLearningPackage(learningPackage)
                .map(OnlineCourse::getId)
                .orElse(null);
        List<LessonProgress> completedProgress = lessonProgressRepository.findByEnrollmentAndStatusOrderByCompletedAtDesc(enrollment, LessonProgressStatus.COMPLETED);
        return PackageEnrollmentResponse.builder()
                .id(enrollment.getId())
                .packageId(learningPackage.getId())
                .courseId(courseId)
                .courseTitle(learningPackage.getTitle())
                .courseSlug(learningPackage.getSlug())
                .thumbnailUrl(learningPackage.getThumbnailUrl())
                .status(enrollment.getStatus())
                .progressPercent(enrollment.getProgressPercent())
                .streakDays(calculateStreakDays(completedProgress))
                .registeredAt(enrollment.getRegisteredAt())
                .completedLessonIds(completedProgress.stream()
                        .map(progress -> progress.getLesson().getId())
                        .toList())
                .build();
    }

    private List<ModuleResponse> toModuleResponses(List<CourseModule> modules, boolean includeLessonContent) {
        return modules.stream()
                .sorted(Comparator.comparing(CourseModule::getDisplayOrder).thenComparing(CourseModule::getId))
                .map(module -> ModuleResponse.builder()
                        .id(module.getId())
                        .title(module.getTitle())
                        .description(module.getDescription())
                        .displayOrder(module.getDisplayOrder())
                        .lessons(toLessonResponses(module.getLessons(), includeLessonContent))
                        .build())
                .toList();
    }

    private List<LessonResponse> toLessonResponses(List<Lesson> lessons, boolean includeLessonContent) {
        return lessons.stream()
                .sorted(Comparator.comparing(Lesson::getDisplayOrder).thenComparing(Lesson::getId))
                .map(lesson -> {
                    boolean exposeContent = includeLessonContent || lesson.isPreview();
                    return LessonResponse.builder()
                        .id(lesson.getId())
                        .title(lesson.getTitle())
                        .description(lesson.getDescription())
                        .contentType(lesson.getContentType())
                        .contentText(exposeContent ? lesson.getContentText() : null)
                        .videoUrl(exposeContent ? lesson.getVideoUrl() : null)
                        .bunnyVideoId(exposeContent ? lesson.getBunnyVideoId() : null)
                        .bunnyLibraryId(includeLessonContent ? lesson.getBunnyLibraryId() : null)
                        .bunnyCdnUrl(exposeContent ? lesson.getBunnyCdnUrl() : null)
                        .materialUrl(exposeContent ? lesson.getMaterialUrl() : null)
                        .transcriptSegments(exposeContent ? parseTranscriptSegments(lesson.getTranscriptSegmentsJson()) : List.of())
                        .durationMinutes(lesson.getDurationMinutes())
                        .displayOrder(lesson.getDisplayOrder())
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

    private List<String> resolveFocusSkills(OnlineCourse course) {
        Set<String> skills = new LinkedHashSet<>();

        courseAssessmentRepository.findByOnlineCourseAndActiveTrueOrderByDisplayOrderAscIdAsc(course).stream()
                .map(assessment -> assessment.getSkill() == null ? null : assessment.getSkill().name())
                .filter(skill -> skill != null && !skill.isBlank())
                .forEach(skills::add);

        if (skills.isEmpty()) {
            course.getModules().stream()
                    .flatMap(module -> module.getLessons().stream())
                    .forEach(lesson -> inferSkillsFromLesson(lesson, skills));
        }

        return List.copyOf(skills);
    }

    private void inferSkillsFromLesson(Lesson lesson, Set<String> skills) {
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
        Double average = courseReviewRepository.findAverageRatingByCourse(course);
        return average == null ? 0D : BigDecimal.valueOf(average).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private BigDecimal resolveSalePrice(LearningPackage learningPackage) {
        BigDecimal originalPrice = safePrice(learningPackage.getPrice());
        BigDecimal salePrice = learningPackage.getSalePrice();
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
}
