package fu.sap490.g23.backend.service.course;

import fu.sap490.g23.backend.dto.request.course.LessonRequest;
import fu.sap490.g23.backend.dto.request.course.ModuleRequest;
import fu.sap490.g23.backend.dto.request.course.OnlineCourseRequest;
import fu.sap490.g23.backend.dto.response.course.CourseStatsResponse;
import fu.sap490.g23.backend.dto.response.course.OnlineCourseResponse;
import fu.sap490.g23.backend.dto.response.course.PackageEnrollmentResponse;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.course.*;
import fu.sap490.g23.backend.repository.UserRepository;
import fu.sap490.g23.backend.repository.course.*;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional
public class OnlineCourseServiceImpl implements OnlineCourseService {

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    private final OnlineCourseRepository onlineCourseRepository;
    private final LearningPackageRepository learningPackageRepository;
    private final PackageTypeRepository packageTypeRepository;
    private final CourseCategoryRepository courseCategoryRepository;
    private final PackageEnrollmentRepository enrollmentRepository;
    private final LessonRepository lessonRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final UserRepository userRepository;
    private final OnlineCourseMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Page<OnlineCourseResponse> getPublicCourses(String keyword, CourseCategoryCode category, Pageable pageable) {
        return onlineCourseRepository.findAll(courseSpec(clean(keyword), category, PackageStatus.PUBLISHED), pageable)
                .map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public OnlineCourseResponse getPublicCourse(String slugOrId) {
        OnlineCourse course = findPublicCourse(slugOrId);
        return mapper.toResponse(course);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OnlineCourseResponse> getManagerCourses(String keyword, CourseCategoryCode category, PackageStatus status, Pageable pageable) {
        return onlineCourseRepository.findAll(courseSpec(clean(keyword), category, status), pageable)
                .map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseStatsResponse getStats() {
        return CourseStatsResponse.builder()
                .totalCourses(learningPackageRepository.countByDeletedFalse())
                .publishedCourses(learningPackageRepository.countByDeletedFalseAndStatus(PackageStatus.PUBLISHED))
                .draftCourses(learningPackageRepository.countByDeletedFalseAndStatus(PackageStatus.DRAFT))
                .totalEnrollments(enrollmentRepository.count())
                .build();
    }

    @Override
    public OnlineCourseResponse createCourse(OnlineCourseRequest request, String creatorEmail) {
        User creator = userRepository.findByEmail(creatorEmail).orElse(null);
        PackageType packageType = packageTypeRepository.findByCode(PackageTypeCode.ONLINE_COURSE)
                .orElseThrow(() -> new RuntimeException("ONLINE_COURSE package type is missing"));
        CourseCategory category = courseCategoryRepository.findByCode(request.getCategory())
                .orElseThrow(() -> new RuntimeException("Course category not found"));

        LearningPackage learningPackage = LearningPackage.builder()
                .packageType(packageType)
                .title(request.getTitle().trim())
                .slug(generateUniqueSlug(request.getTitle()))
                .shortDescription(request.getShortDescription())
                .description(request.getDescription())
                .targetScore(request.getTargetScore())
                .duration(request.getDuration())
                .studyMode(request.getStudyMode())
                .price(defaultBigDecimal(request.getPrice()))
                .thumbnailUrl(request.getThumbnailUrl())
                .status(request.getStatus() == null ? PackageStatus.DRAFT : request.getStatus())
                .displayOrder(defaultInt(request.getDisplayOrder()))
                .featured(Boolean.TRUE.equals(request.getFeatured()))
                .createdBy(creator)
                .build();

        OnlineCourse course = OnlineCourse.builder()
                .learningPackage(learningPackage)
                .category(category)
                .level(request.getLevel())
                .totalLessons(defaultInt(request.getTotalLessons()))
                .totalHours(defaultInt(request.getTotalHours()))
                .build();
        rebuildModules(course, request.getModules());
        return mapper.toResponse(onlineCourseRepository.save(course));
    }

    @Override
    public OnlineCourseResponse updateCourse(Long id, OnlineCourseRequest request) {
        OnlineCourse course = findCourse(id);
        LearningPackage learningPackage = course.getLearningPackage();
        CourseCategory category = courseCategoryRepository.findByCode(request.getCategory())
                .orElseThrow(() -> new RuntimeException("Course category not found"));

        learningPackage.setTitle(request.getTitle().trim());
        learningPackage.setShortDescription(request.getShortDescription());
        learningPackage.setDescription(request.getDescription());
        learningPackage.setTargetScore(request.getTargetScore());
        learningPackage.setDuration(request.getDuration());
        learningPackage.setStudyMode(request.getStudyMode());
        learningPackage.setPrice(defaultBigDecimal(request.getPrice()));
        learningPackage.setThumbnailUrl(request.getThumbnailUrl());
        learningPackage.setStatus(request.getStatus() == null ? learningPackage.getStatus() : request.getStatus());
        learningPackage.setDisplayOrder(defaultInt(request.getDisplayOrder()));
        learningPackage.setFeatured(Boolean.TRUE.equals(request.getFeatured()));

        course.setCategory(category);
        course.setLevel(request.getLevel());
        course.setTotalLessons(defaultInt(request.getTotalLessons()));
        course.setTotalHours(defaultInt(request.getTotalHours()));
        course.getModules().clear();
        rebuildModules(course, request.getModules());
        return mapper.toResponse(onlineCourseRepository.save(course));
    }

    @Override
    public OnlineCourseResponse publishCourse(Long id) {
        OnlineCourse course = findCourse(id);
        course.getLearningPackage().setStatus(PackageStatus.PUBLISHED);
        return mapper.toResponse(course);
    }

    @Override
    public OnlineCourseResponse archiveCourse(Long id) {
        OnlineCourse course = findCourse(id);
        course.getLearningPackage().setStatus(PackageStatus.ARCHIVED);
        return mapper.toResponse(course);
    }

    @Override
    public void deleteCourse(Long id) {
        OnlineCourse course = findCourse(id);
        course.getLearningPackage().setDeleted(true);
        course.getLearningPackage().setStatus(PackageStatus.ARCHIVED);
    }

    @Override
    public OnlineCourseResponse registerCourse(Long courseId, String studentEmail) {
        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        OnlineCourse course = findCourse(courseId);
        if (!course.getLearningPackage().isPublished()) {
            throw new RuntimeException("This online course is not available for enrollment");
        }
        PackageEnrollment enrollment = enrollmentRepository.findByStudentAndLearningPackage(student, course.getLearningPackage())
                .orElseGet(() -> enrollmentRepository.save(PackageEnrollment.builder()
                        .student(student)
                        .learningPackage(course.getLearningPackage())
                        .status(EnrollmentStatus.ACTIVE)
                        .progressPercent(0)
                        .build()));
        return mapper.toResponse(course, true, enrollment.getProgressPercent(), enrollment.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PackageEnrollmentResponse> getMyEnrollments(String studentEmail) {
        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        return enrollmentRepository.findByStudentOrderByRegisteredAtDesc(student).stream()
                .map(mapper::toEnrollmentResponse)
                .toList();
    }

    @Override
    public PackageEnrollmentResponse updateLessonProgress(Long courseId, Long lessonId, boolean completed, String studentEmail) {
        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        OnlineCourse course = findCourse(courseId);
        PackageEnrollment enrollment = enrollmentRepository.findByStudentAndLearningPackage(student, course.getLearningPackage())
                .orElseThrow(() -> new RuntimeException("You are not enrolled in this course"));
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Lesson not found"));

        if (lesson.getModule() == null
                || lesson.getModule().getOnlineCourse() == null
                || !course.getId().equals(lesson.getModule().getOnlineCourse().getId())) {
            throw new RuntimeException("Lesson does not belong to this course");
        }

        LessonProgress progress = lessonProgressRepository.findByStudentAndLesson(student, lesson)
                .orElseGet(() -> LessonProgress.builder()
                        .student(student)
                        .lesson(lesson)
                        .enrollment(enrollment)
                        .build());

        progress.setLastAccessedAt(LocalDateTime.now());
        if (completed) {
            progress.setStatus(LessonProgressStatus.COMPLETED);
            progress.setProgressPercent(100);
            if (progress.getCompletedAt() == null) {
                progress.setCompletedAt(LocalDateTime.now());
            }
        } else {
            progress.setStatus(LessonProgressStatus.IN_PROGRESS);
            progress.setProgressPercent(0);
            progress.setCompletedAt(null);
        }
        lessonProgressRepository.save(progress);

        int totalLessons = course.getModules().stream()
                .mapToInt(module -> module.getLessons().size())
                .sum();
        long completedLessons = lessonProgressRepository.countByEnrollmentAndStatus(enrollment, LessonProgressStatus.COMPLETED);
        enrollment.setProgressPercent(totalLessons == 0 ? 0 : (int) Math.round((completedLessons * 100.0) / totalLessons));
        PackageEnrollment savedEnrollment = enrollmentRepository.save(enrollment);
        return mapper.toEnrollmentResponse(savedEnrollment);
    }

    private OnlineCourse findCourse(Long id) {
        OnlineCourse course = onlineCourseRepository.findWithModulesById(id)
                .filter(foundCourse -> !foundCourse.getLearningPackage().isDeleted())
                .orElseThrow(() -> new RuntimeException("Course not found"));
        initializeModules(course);
        return course;
    }

    private OnlineCourse findPublicCourse(String slugOrId) {
        OnlineCourse course;
        try {
            Long numericId = Long.parseLong(slugOrId);
            course = onlineCourseRepository.findWithModulesById(numericId)
                    .orElseGet(() -> {
                        LearningPackage learningPackage = learningPackageRepository.findByIdAndDeletedFalse(numericId)
                                .orElseThrow(() -> new RuntimeException("Course not found"));
                        return onlineCourseRepository.findByLearningPackage(learningPackage)
                                .orElseThrow(() -> new RuntimeException("Course not found"));
                    });
        } catch (NumberFormatException ex) {
            LearningPackage learningPackage = learningPackageRepository.findBySlugAndDeletedFalse(slugOrId)
                    .orElseThrow(() -> new RuntimeException("Course not found"));
            course = onlineCourseRepository.findByLearningPackage(learningPackage)
                    .orElseThrow(() -> new RuntimeException("Course not found"));
        }
        if (!course.getLearningPackage().isPublished()) {
            throw new RuntimeException("Course not found");
        }
        initializeModules(course);
        return course;
    }

    private void initializeModules(OnlineCourse course) {
        course.getModules().forEach(module -> module.getLessons().size());
    }

    private void rebuildModules(OnlineCourse course, List<ModuleRequest> modules) {
        if (modules == null) return;
        for (ModuleRequest moduleRequest : modules) {
            CourseModule module = CourseModule.builder()
                    .title(moduleRequest.getTitle())
                    .description(moduleRequest.getDescription())
                    .displayOrder(defaultInt(moduleRequest.getDisplayOrder()))
                    .build();
            if (moduleRequest.getLessons() != null) {
                for (LessonRequest lessonRequest : moduleRequest.getLessons()) {
                    module.addLesson(Lesson.builder()
                            .title(lessonRequest.getTitle())
                            .description(lessonRequest.getDescription())
                            .videoUrl(lessonRequest.getVideoUrl())
                            .materialUrl(lessonRequest.getMaterialUrl())
                            .durationMinutes(defaultInt(lessonRequest.getDurationMinutes()))
                            .displayOrder(defaultInt(lessonRequest.getDisplayOrder()))
                            .preview(Boolean.TRUE.equals(lessonRequest.getPreview()))
                            .build());
                }
            }
            course.addModule(module);
        }
    }

    private Specification<OnlineCourse> courseSpec(String keyword, CourseCategoryCode category, PackageStatus status) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<OnlineCourse, LearningPackage> learningPackage = root.join("learningPackage");
            Join<OnlineCourse, CourseCategory> categoryJoin = root.join("category");
            predicates.add(criteriaBuilder.isFalse(learningPackage.get("deleted")));

            if (status != null) {
                predicates.add(criteriaBuilder.equal(learningPackage.get("status"), status));
            }
            if (category != null) {
                predicates.add(criteriaBuilder.equal(categoryJoin.get("code"), category));
            }
            if (keyword != null) {
                String pattern = "%" + keyword.toLowerCase(Locale.ROOT) + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(learningPackage.get("title")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(learningPackage.get("shortDescription")), pattern)
                ));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private String generateUniqueSlug(String title) {
        String baseSlug = toSlug(title);
        String slug = baseSlug;
        int index = 2;
        while (learningPackageRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + index++;
        }
        return slug;
    }

    private String toSlug(String input) {
        String nowhitespace = WHITESPACE.matcher(input.trim()).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        slug = slug.replaceAll("-+", "-").toLowerCase(Locale.ENGLISH);
        return slug.isBlank() ? "online-course" : slug;
    }

    private String clean(String keyword) {
        return keyword == null || keyword.isBlank() ? null : keyword.trim();
    }

    private Integer defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private BigDecimal defaultBigDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
