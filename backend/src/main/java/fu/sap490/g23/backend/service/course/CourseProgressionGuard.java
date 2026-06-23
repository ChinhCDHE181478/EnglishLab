package fu.sap490.g23.backend.service.course;

import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.assessment.CourseAssessment;
import fu.sap490.g23.backend.entity.assessment.enums.SubmissionStatus;
import fu.sap490.g23.backend.entity.course.CourseModule;
import fu.sap490.g23.backend.entity.course.Lesson;
import fu.sap490.g23.backend.entity.course.LessonProgress;
import fu.sap490.g23.backend.entity.course.enums.LessonProgressStatus;
import fu.sap490.g23.backend.entity.course.OnlineCourse;
import fu.sap490.g23.backend.repository.assessment.AssessmentSubmissionRepository;
import fu.sap490.g23.backend.repository.assessment.CourseAssessmentRepository;
import fu.sap490.g23.backend.repository.course.LessonProgressRepository;
import fu.sap490.g23.backend.service.assessment.AssessmentPassingThresholdResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class CourseProgressionGuard {

    private final LessonProgressRepository lessonProgressRepository;
    private final CourseAssessmentRepository courseAssessmentRepository;
    private final AssessmentSubmissionRepository assessmentSubmissionRepository;
    private final AssessmentPassingThresholdResolver passingThresholdResolver;

    public void ensureLessonCanBeCompleted(User student, OnlineCourse course, Lesson lesson) {
        List<OrderedLesson> orderedLessons = orderedLessons(course);
        int lessonIndex = indexOfLesson(orderedLessons, lesson.getId());
        if (lessonIndex < 0) {
            throw new RuntimeException("Bài học không thuộc khóa học hiện tại.");
        }

        OrderedLesson currentLesson = orderedLessons.get(lessonIndex);
        if (currentLesson.moduleIndex() > 0) {
            CourseModule previousModule = orderedModules(course).get(currentLesson.moduleIndex() - 1);
            if (!canAdvancePastModule(student, previousModule)) {
                throw new RuntimeException("Bạn cần hoàn thành và đạt yêu cầu ở bài đánh giá cuối mô-đun trước khi mở mô-đun tiếp theo.");
            }
        }

        if (lessonIndex == 0) {
            return;
        }

        OrderedLesson previousLesson = orderedLessons.get(lessonIndex - 1);
        if (!isLessonCompleted(student, previousLesson.lesson().getId())) {
            throw new RuntimeException("Bạn cần hoàn thành bài học trước đó trước khi tiếp tục.");
        }
    }

    public void ensureLessonCanBeMarkedIncomplete(User student, OnlineCourse course, Lesson lesson) {
        List<OrderedLesson> orderedLessons = orderedLessons(course);
        int lessonIndex = indexOfLesson(orderedLessons, lesson.getId());
        if (lessonIndex < 0) {
            throw new RuntimeException("Bài học không thuộc khóa học hiện tại.");
        }

        Set<Long> laterLessonIds = new HashSet<>();
        Set<Long> affectedModuleIds = new HashSet<>();
        for (int index = lessonIndex + 1; index < orderedLessons.size(); index++) {
            OrderedLesson laterLesson = orderedLessons.get(index);
            laterLessonIds.add(laterLesson.lesson().getId());
            affectedModuleIds.add(laterLesson.module().getId());
        }
        affectedModuleIds.add(orderedLessons.get(lessonIndex).module().getId());

        if (!laterLessonIds.isEmpty()) {
            List<LessonProgress> completedLaterLessons = lessonProgressRepository.findByStudentAndLessonIdInAndStatus(
                    student,
                    laterLessonIds,
                    LessonProgressStatus.COMPLETED
            );
            if (!completedLaterLessons.isEmpty()) {
                throw new RuntimeException("Không thể bỏ hoàn thành bài này vì bạn đã học xong các bài phía sau.");
            }
        }

        List<CourseAssessment> dependentAssessments = courseAssessmentRepository.findByOnlineCourseAndActiveTrueOrderByDisplayOrderAscIdAsc(course).stream()
                .filter(assessment -> assessment.getModule() == null || affectedModuleIds.contains(assessment.getModule().getId()))
                .toList();
        if (!dependentAssessments.isEmpty() && assessmentSubmissionRepository.existsByAssessmentInAndStudent(dependentAssessments, student)) {
            throw new RuntimeException("Không thể bỏ hoàn thành bài này vì đã có bài đánh giá liên quan được nộp.");
        }
    }

    public void ensureAssessmentCanBeSubmitted(User student, CourseAssessment assessment) {
        OnlineCourse course = assessment.getOnlineCourse();
        CourseModule module = assessment.getModule();
        if (module == null) {
            ensureAllLessonsCompleted(student, course);
            ensureAllModuleChecksPassed(student, course);
            return;
        }

        List<CourseModule> orderedModules = orderedModules(course);
        int moduleIndex = indexOfModule(orderedModules, module.getId());
        if (moduleIndex < 0) {
            throw new RuntimeException("Mô-đun không thuộc khóa học hiện tại.");
        }

        if (moduleIndex > 0) {
            CourseModule previousModule = orderedModules.get(moduleIndex - 1);
            if (!canAdvancePastModule(student, previousModule)) {
                throw new RuntimeException("Bạn cần hoàn thành và đạt yêu cầu ở bài đánh giá cuối mô-đun trước đó trước khi làm bài đánh giá này.");
            }
        }

        ensureModuleLessonsCompleted(student, module);
    }

    public boolean canAdvancePastModule(User student, CourseModule module) {
        if (module == null) {
            return true;
        }
        if (!areModuleLessonsCompleted(student, module)) {
            return false;
        }

        List<CourseAssessment> moduleAssessments = courseAssessmentRepository.findByModuleAndActiveTrueOrderByDisplayOrderAscIdAsc(module);
        if (moduleAssessments.isEmpty()) {
            return true;
        }

        return moduleAssessments.stream().allMatch(assessment -> isAssessmentPassed(student, assessment));
    }

    public boolean isAssessmentPassed(User student, CourseAssessment assessment) {
        return assessmentSubmissionRepository.findTopByAssessmentAndStudentOrderBySubmittedAtDesc(assessment, student)
                .map(submission -> {
                    SubmissionStatus status = submission.getStatus();
                    if (status == SubmissionStatus.PASSED) {
                        return true;
                    }
                    if (status == SubmissionStatus.NEEDS_IMPROVEMENT) {
                        return false;
                    }
                    BigDecimal score = submission.getAiScore();
                    if (score == null) {
                        return status == SubmissionStatus.AI_EVALUATED
                                && passingThresholdResolver.resolve(assessment) == null;
                    }
                    return passingThresholdResolver.isScorePassing(score, assessment);
                })
                .orElse(false);
    }

    private void ensureModuleLessonsCompleted(User student, CourseModule module) {
        if (!areModuleLessonsCompleted(student, module)) {
            throw new RuntimeException("Bạn cần hoàn thành toàn bộ bài học trong mô-đun trước khi làm bài đánh giá cuối mô-đun.");
        }
    }

    private void ensureAllLessonsCompleted(User student, OnlineCourse course) {
        List<OrderedLesson> orderedLessons = orderedLessons(course);
        for (OrderedLesson orderedLesson : orderedLessons) {
            if (!isLessonCompleted(student, orderedLesson.lesson().getId())) {
                throw new RuntimeException("Bạn cần hoàn thành toàn bộ bài học trước khi làm bài đánh giá cuối khóa.");
            }
        }
    }

    private void ensureAllModuleChecksPassed(User student, OnlineCourse course) {
        for (CourseModule module : orderedModules(course)) {
            if (!canAdvancePastModule(student, module)) {
                throw new RuntimeException("Bạn cần hoàn thành và đạt yêu cầu ở các bài đánh giá cuối mô-đun trước khi làm bài đánh giá cuối khóa.");
            }
        }
    }

    private boolean areModuleLessonsCompleted(User student, CourseModule module) {
        return orderedLessonsOfModule(module).stream()
                .allMatch(lesson -> isLessonCompleted(student, lesson.getId()));
    }

    private boolean isLessonCompleted(User student, Long lessonId) {
        if (lessonId == null) {
            return false;
        }
        Set<Long> lessonIds = Set.of(lessonId);
        return !lessonProgressRepository.findByStudentAndLessonIdInAndStatus(student, lessonIds, LessonProgressStatus.COMPLETED).isEmpty();
    }

    private List<CourseModule> orderedModules(OnlineCourse course) {
        List<CourseModule> modules = new ArrayList<>(course.getModules() == null ? List.of() : course.getModules());
        modules.sort(Comparator.comparing(CourseModule::getDisplayOrder).thenComparing(module -> module.getId() == null ? Long.MAX_VALUE : module.getId()));
        return modules;
    }

    private List<Lesson> orderedLessonsOfModule(CourseModule module) {
        List<Lesson> lessons = new ArrayList<>(module.getLessons() == null ? List.of() : module.getLessons());
        lessons.sort(Comparator.comparing(Lesson::getDisplayOrder).thenComparing(lesson -> lesson.getId() == null ? Long.MAX_VALUE : lesson.getId()));
        return lessons;
    }

    private List<OrderedLesson> orderedLessons(OnlineCourse course) {
        List<OrderedLesson> orderedLessons = new ArrayList<>();
        List<CourseModule> modules = orderedModules(course);
        for (int moduleIndex = 0; moduleIndex < modules.size(); moduleIndex++) {
            CourseModule module = modules.get(moduleIndex);
            List<Lesson> lessons = orderedLessonsOfModule(module);
            for (int lessonIndex = 0; lessonIndex < lessons.size(); lessonIndex++) {
                orderedLessons.add(new OrderedLesson(moduleIndex, lessonIndex, module, lessons.get(lessonIndex)));
            }
        }
        return orderedLessons;
    }

    private int indexOfLesson(List<OrderedLesson> orderedLessons, Long lessonId) {
        for (int index = 0; index < orderedLessons.size(); index++) {
            if (lessonId.equals(orderedLessons.get(index).lesson().getId())) {
                return index;
            }
        }
        return -1;
    }

    private int indexOfModule(List<CourseModule> modules, Long moduleId) {
        for (int index = 0; index < modules.size(); index++) {
            if (moduleId.equals(modules.get(index).getId())) {
                return index;
            }
        }
        return -1;
    }

    private record OrderedLesson(int moduleIndex, int lessonIndex, CourseModule module, Lesson lesson) {
    }
}
