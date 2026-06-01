package fu.sap490.g23.backend.service.course;

import fu.sap490.g23.backend.dto.request.course.LessonRequest;
import fu.sap490.g23.backend.dto.request.course.ModuleRequest;
import fu.sap490.g23.backend.dto.request.course.OnlineCourseRequest;
import fu.sap490.g23.backend.dto.response.course.BunnyVideoUploadResponse;
import fu.sap490.g23.backend.dto.response.course.CourseStatsResponse;
import fu.sap490.g23.backend.dto.response.course.LessonResponse;
import fu.sap490.g23.backend.dto.response.course.OnlineCourseResponse;
import fu.sap490.g23.backend.dto.response.course.PackageEnrollmentResponse;
import fu.sap490.g23.backend.dto.response.course.VocabularyTermResponse;
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
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional
public class OnlineCourseServiceImpl implements OnlineCourseService {

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
    private static final Pattern VOCABULARY_HEADING = Pattern.compile("(?m)^###\\s+\\d+\\.\\s+(.+)$");

    private final OnlineCourseRepository onlineCourseRepository;
    private final LearningPackageRepository learningPackageRepository;
    private final PackageTypeRepository packageTypeRepository;
    private final CourseCategoryRepository courseCategoryRepository;
    private final PackageEnrollmentRepository enrollmentRepository;
    private final LessonRepository lessonRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final VocabularyProgressRepository vocabularyProgressRepository;
    private final UserRepository userRepository;
    private final OnlineCourseMapper mapper;
    private final BunnyStreamService bunnyStreamService;

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
    public OnlineCourseResponse getManagerCourse(String slugOrId) {
        return mapper.toResponse(findManagerCourse(slugOrId));
    }

    @Override
    @Transactional(readOnly = true)
    public CourseStatsResponse getStats() {
        return CourseStatsResponse.builder()
                .totalCourses(learningPackageRepository.countByDeletedFalse())
                .publishedCourses(learningPackageRepository.countByDeletedFalseAndStatus(PackageStatus.PUBLISHED))
                .draftCourses(learningPackageRepository.countByDeletedFalseAndStatus(PackageStatus.DRAFT))
                .archivedCourses(learningPackageRepository.countByDeletedFalseAndStatus(PackageStatus.ARCHIVED))
                .totalLessons(lessonRepository.countActiveLessons())
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
        synchronizeModules(course, request.getModules());
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
    public BunnyVideoUploadResponse uploadLessonVideo(Long courseId, Long lessonId, String title, MultipartFile file) {
        OnlineCourse course = findCourse(courseId);
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Lesson not found"));

        if (lesson.getModule() == null
                || lesson.getModule().getOnlineCourse() == null
                || !course.getId().equals(lesson.getModule().getOnlineCourse().getId())) {
            throw new RuntimeException("Lesson does not belong to this course");
        }

        BunnyVideoUploadResponse upload = bunnyStreamService.uploadVideo(file, title == null || title.isBlank() ? lesson.getTitle() : title);
        lesson.setVideoUrl(upload.getEmbedUrl());
        lesson.setBunnyVideoId(upload.getVideoId());
        lesson.setBunnyLibraryId(upload.getLibraryId());
        lesson.setBunnyCdnUrl(upload.getCdnUrl());
        lesson.setContentType("video");

        Lesson savedLesson = lessonRepository.save(lesson);
        upload.setLesson(toLessonResponse(savedLesson));
        return upload;
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

    @Override
    @Transactional(readOnly = true)
    public List<VocabularyTermResponse> getVocabularyTerms(Long courseId, String studentEmail) {
        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        OnlineCourse course = findCourse(courseId);
        ensureEnrolled(student, course);

        List<VocabularyProgress> progressItems = vocabularyProgressRepository.findByStudentAndCourse(student, course);
        return extractVocabularyTerms(course).stream()
                .map(term -> applyVocabularyProgress(term, progressItems))
                .toList();
    }

    @Override
    public VocabularyTermResponse updateVocabularyProgress(Long courseId, String termKey, VocabularyProgressStatus status, Boolean starred, String studentEmail) {
        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        OnlineCourse course = findCourse(courseId);
        ensureEnrolled(student, course);

        VocabularyTermResponse term = extractVocabularyTerms(course).stream()
                .filter(item -> item.getTermKey().equals(termKey))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Vocabulary term not found"));

        VocabularyProgress progress = vocabularyProgressRepository.findByStudentAndCourseAndTermKey(student, course, termKey)
                .orElseGet(() -> VocabularyProgress.builder()
                        .student(student)
                        .course(course)
                        .termKey(termKey)
                        .build());

        if (status != null) {
            progress.setStatus(status);
        }
        if (starred != null) {
            progress.setStarred(starred);
        }
        progress.setLastReviewedAt(LocalDateTime.now());
        VocabularyProgress savedProgress = vocabularyProgressRepository.save(progress);

        term.setStatus(savedProgress.getStatus());
        term.setStarred(savedProgress.isStarred());
        return term;
    }

    private OnlineCourse findCourse(Long id) {
        OnlineCourse course = onlineCourseRepository.findWithModulesById(id)
                .filter(foundCourse -> !foundCourse.getLearningPackage().isDeleted())
                .orElseThrow(() -> new RuntimeException("Course not found"));
        initializeModules(course);
        return course;
    }

    private OnlineCourse findPublicCourse(String slugOrId) {
        OnlineCourse course = findManagerCourse(slugOrId);
        if (!course.getLearningPackage().isPublished()) {
            throw new RuntimeException("Course not found");
        }
        return course;
    }

    private OnlineCourse findManagerCourse(String slugOrId) {
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

        if (course.getLearningPackage().isDeleted()) {
            throw new RuntimeException("Course not found");
        }
        initializeModules(course);
        return course;
    }

    private void initializeModules(OnlineCourse course) {
        course.getModules().forEach(module -> module.getLessons().size());
    }

    private void ensureEnrolled(User student, OnlineCourse course) {
        enrollmentRepository.findByStudentAndLearningPackage(student, course.getLearningPackage())
                .orElseThrow(() -> new RuntimeException("You are not enrolled in this course"));
    }

    private List<VocabularyTermResponse> extractVocabularyTerms(OnlineCourse course) {
        List<VocabularyTermResponse> terms = new ArrayList<>();
        for (CourseModule module : course.getModules()) {
            for (Lesson lesson : module.getLessons()) {
                terms.addAll(extractVocabularyTerms(module, lesson));
            }
        }
        return terms;
    }

    private List<VocabularyTermResponse> extractVocabularyTerms(CourseModule module, Lesson lesson) {
        String content = lesson.getContentText();
        if (content == null || !content.contains("### ")) {
            return List.of();
        }

        java.util.regex.Matcher matcher = VOCABULARY_HEADING.matcher(content);
        List<java.util.regex.MatchResult> headings = matcher.results().toList();
        List<VocabularyTermResponse> terms = new ArrayList<>();

        for (int index = 0; index < headings.size(); index++) {
            java.util.regex.MatchResult heading = headings.get(index);
            int start = heading.end();
            int end = index + 1 < headings.size() ? headings.get(index + 1).start() : content.length();
            String block = content.substring(start, end);
            String meaning = findVocabularyField(block, "Meaning");
            if (meaning == null || meaning.isBlank()) {
                continue;
            }

            String term = cleanMarkdown(heading.group(1));
            terms.add(VocabularyTermResponse.builder()
                    .termKey(toTermKey(module, lesson, term))
                    .term(term)
                    .meaning(meaning)
                    .example(firstNonBlank(findVocabularyField(block, "IELTS example"), findVocabularyField(block, "Example")))
                    .commonError(firstNonBlank(findVocabularyField(block, "Common error to avoid"), findVocabularyField(block, "Common error")))
                    .lessonId(lesson.getId())
                    .lessonTitle(lesson.getTitle())
                    .moduleId(module.getId())
                    .moduleTitle(module.getTitle())
                    .status(VocabularyProgressStatus.NEW)
                    .starred(false)
                    .build());
        }

        return terms;
    }

    private VocabularyTermResponse applyVocabularyProgress(VocabularyTermResponse term, List<VocabularyProgress> progressItems) {
        progressItems.stream()
                .filter(progress -> progress.getTermKey().equals(term.getTermKey()))
                .findFirst()
                .ifPresent(progress -> {
                    term.setStatus(progress.getStatus());
                    term.setStarred(progress.isStarred());
                });
        return term;
    }

    private String findVocabularyField(String block, String label) {
        Pattern fieldPattern = Pattern.compile("(?m)^\\*\\*" + Pattern.quote(label) + ":\\*\\*\\s*(.+)$", Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = fieldPattern.matcher(block);
        return matcher.find() ? cleanMarkdown(matcher.group(1)) : null;
    }

    private String cleanMarkdown(String value) {
        return value == null ? "" : value.replace("**", "").replaceAll("^['\"]|['\"]$", "").trim();
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private String toTermKey(CourseModule module, Lesson lesson, String term) {
        return "%s-%s-%s".formatted(module.getId(), lesson.getId(), toSlug(term));
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
                            .contentType(lessonRequest.getContentType())
                            .contentText(lessonRequest.getContentText())
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

    private void synchronizeModules(OnlineCourse course, List<ModuleRequest> modules) {
        if (modules == null) {
            return;
        }

        List<CourseModule> existingModules = course.getModules();
        Set<Long> incomingModuleIds = new HashSet<>();
        List<CourseModule> nextModules = new ArrayList<>();

        for (int moduleIndex = 0; moduleIndex < modules.size(); moduleIndex++) {
            ModuleRequest moduleRequest = modules.get(moduleIndex);
            CourseModule module = findExistingModule(existingModules, moduleRequest.getId());

            if (module == null) {
                module = CourseModule.builder().build();
                module.setOnlineCourse(course);
            } else if (module.getId() != null) {
                incomingModuleIds.add(module.getId());
            }

            module.setTitle(moduleRequest.getTitle());
            module.setDescription(moduleRequest.getDescription());
            module.setDisplayOrder(defaultInt(moduleRequest.getDisplayOrder()));
            synchronizeLessons(module, moduleRequest.getLessons());
            nextModules.add(module);
        }

        for (CourseModule existingModule : new ArrayList<>(existingModules)) {
            if (existingModule.getId() != null && !incomingModuleIds.contains(existingModule.getId())) {
                ensureModuleCanBeRemoved(existingModule);
            }
        }

        existingModules.clear();
        nextModules.forEach(course::addModule);
    }

    private void synchronizeLessons(CourseModule module, List<LessonRequest> lessons) {
        List<Lesson> existingLessons = module.getLessons();
        Set<Long> incomingLessonIds = new HashSet<>();
        List<Lesson> nextLessons = new ArrayList<>();

        if (lessons != null) {
            for (LessonRequest lessonRequest : lessons) {
                Lesson lesson = findExistingLesson(existingLessons, lessonRequest.getId());

                if (lesson == null) {
                    lesson = Lesson.builder().build();
                    lesson.setModule(module);
                } else if (lesson.getId() != null) {
                    incomingLessonIds.add(lesson.getId());
                }

                lesson.setTitle(lessonRequest.getTitle());
                lesson.setDescription(lessonRequest.getDescription());
                lesson.setContentType(lessonRequest.getContentType());
                lesson.setContentText(lessonRequest.getContentText());
                lesson.setVideoUrl(lessonRequest.getVideoUrl());
                lesson.setMaterialUrl(lessonRequest.getMaterialUrl());
                lesson.setDurationMinutes(defaultInt(lessonRequest.getDurationMinutes()));
                lesson.setDisplayOrder(defaultInt(lessonRequest.getDisplayOrder()));
                lesson.setPreview(Boolean.TRUE.equals(lessonRequest.getPreview()));
                nextLessons.add(lesson);
            }
        }

        for (Lesson existingLesson : new ArrayList<>(existingLessons)) {
            if (existingLesson.getId() != null && !incomingLessonIds.contains(existingLesson.getId())) {
                ensureLessonCanBeRemoved(existingLesson);
            }
        }

        existingLessons.clear();
        nextLessons.forEach(module::addLesson);
    }

    private CourseModule findExistingModule(List<CourseModule> modules, Long moduleId) {
        if (moduleId == null) {
            return null;
        }
        return modules.stream()
                .filter(module -> moduleId.equals(module.getId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Module not found in this course"));
    }

    private Lesson findExistingLesson(List<Lesson> lessons, Long lessonId) {
        if (lessonId == null) {
            return null;
        }
        return lessons.stream()
                .filter(lesson -> lessonId.equals(lesson.getId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Lesson not found in this module"));
    }

    private void ensureModuleCanBeRemoved(CourseModule module) {
        for (Lesson lesson : module.getLessons()) {
            ensureLessonCanBeRemoved(lesson);
        }
    }

    private void ensureLessonCanBeRemoved(Lesson lesson) {
        if (lesson.getId() != null && lessonProgressRepository.existsByLessonId(lesson.getId())) {
            throw new RuntimeException("Cannot remove lesson \"" + lesson.getTitle() + "\" because learner progress already exists.");
        }
    }

    private LessonResponse toLessonResponse(Lesson lesson) {
        return LessonResponse.builder()
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
                .build();
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
