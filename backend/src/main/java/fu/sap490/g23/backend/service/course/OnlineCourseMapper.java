package fu.sap490.g23.backend.service.course;

import fu.sap490.g23.backend.dto.response.course.LessonResponse;
import fu.sap490.g23.backend.dto.response.course.ModuleResponse;
import fu.sap490.g23.backend.dto.response.course.OnlineCourseResponse;
import fu.sap490.g23.backend.dto.response.course.PackageEnrollmentResponse;
import fu.sap490.g23.backend.entity.course.CourseCategory;
import fu.sap490.g23.backend.entity.course.CourseModule;
import fu.sap490.g23.backend.entity.course.LearningPackage;
import fu.sap490.g23.backend.entity.course.Lesson;
import fu.sap490.g23.backend.entity.course.LessonProgress;
import fu.sap490.g23.backend.entity.course.LessonProgressStatus;
import fu.sap490.g23.backend.entity.course.OnlineCourse;
import fu.sap490.g23.backend.entity.course.PackageEnrollment;
import fu.sap490.g23.backend.repository.course.LessonProgressRepository;
import fu.sap490.g23.backend.repository.course.OnlineCourseRepository;
import fu.sap490.g23.backend.repository.course.PackageEnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OnlineCourseMapper {

    private final OnlineCourseRepository onlineCourseRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final PackageEnrollmentRepository packageEnrollmentRepository;

    public OnlineCourseResponse toResponse(OnlineCourse course) {
        return toResponse(course, false, null, null);
    }

    public OnlineCourseResponse toResponse(OnlineCourse course, boolean registered, Integer progressPercent, Long enrollmentId) {
        LearningPackage learningPackage = course.getLearningPackage();
        CourseCategory category = course.getCategory();
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
                .duration(learningPackage.getDuration())
                .studyMode(learningPackage.getStudyMode())
                .price(learningPackage.getPrice())
                .thumbnailUrl(learningPackage.getThumbnailUrl())
                .totalLessons(course.getTotalLessons())
                .totalHours(course.getTotalHours())
                .displayOrder(learningPackage.getDisplayOrder())
                .featured(learningPackage.isFeatured())
                .registered(registered)
                .progressPercent(progressPercent)
                .enrollmentId(enrollmentId)
                .enrollmentCount(packageEnrollmentRepository.countByLearningPackage(learningPackage))
                .createdAt(learningPackage.getCreatedAt())
                .updatedAt(learningPackage.getUpdatedAt())
                .modules(toModuleResponses(course.getModules()))
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

    private List<ModuleResponse> toModuleResponses(List<CourseModule> modules) {
        return modules.stream()
                .sorted(Comparator.comparing(CourseModule::getDisplayOrder).thenComparing(CourseModule::getId))
                .map(module -> ModuleResponse.builder()
                        .id(module.getId())
                        .title(module.getTitle())
                        .description(module.getDescription())
                        .displayOrder(module.getDisplayOrder())
                        .lessons(toLessonResponses(module.getLessons()))
                        .build())
                .toList();
    }

    private List<LessonResponse> toLessonResponses(List<Lesson> lessons) {
        return lessons.stream()
                .sorted(Comparator.comparing(Lesson::getDisplayOrder).thenComparing(Lesson::getId))
                .map(lesson -> LessonResponse.builder()
                        .id(lesson.getId())
                        .title(lesson.getTitle())
                        .description(lesson.getDescription())
                        .contentType(lesson.getContentType())
                        .contentText(lesson.getContentText())
                        .videoUrl(lesson.getVideoUrl())
                        .bunnyVideoId(lesson.getBunnyVideoId())
                        .bunnyLibraryId(lesson.getBunnyLibraryId())
                        .bunnyCdnUrl(lesson.getBunnyCdnUrl())
                        .materialUrl(lesson.getMaterialUrl())
                        .durationMinutes(lesson.getDurationMinutes())
                        .displayOrder(lesson.getDisplayOrder())
                        .preview(lesson.isPreview())
                        .build())
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
}
