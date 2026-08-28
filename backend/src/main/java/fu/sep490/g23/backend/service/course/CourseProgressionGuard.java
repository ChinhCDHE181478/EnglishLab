package fu.sep490.g23.backend.service.course;
import fu.sep490.g23.backend.entity.course.LessonProgress;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.assessment.CourseAssessment;
import fu.sep490.g23.backend.entity.assessment.enums.SubmissionStatus;
import fu.sep490.g23.backend.entity.course.OnlineCourseModule;
import fu.sep490.g23.backend.entity.course.OnlineLesson;
import fu.sep490.g23.backend.entity.course.enums.LessonProgressStatus;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.repository.assessment.AssessmentSubmissionRepository;
import fu.sep490.g23.backend.repository.assessment.CourseAssessmentRepository;
import fu.sep490.g23.backend.repository.course.LessonProgressRepository;
import fu.sep490.g23.backend.service.assessment.AssessmentPassingThresholdResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class CourseProgressionGuard {

    private final LessonProgressRepository lessonProgressRepository;
    private final CourseAssessmentRepository courseAssessmentRepository;
    private final AssessmentSubmissionRepository assessmentSubmissionRepository;
    private final AssessmentPassingThresholdResolver passingThresholdResolver;

    public void ensureLessonCanBeCompleted(User student, OnlineCourse course, OnlineLesson lesson) {
        List<OrderedLesson> orderedLessons = orderedLessons(course);
        int lessonIndex = indexOfLesson(orderedLessons, lesson.getId());
        if (lessonIndex < 0) {
            throw new RuntimeException("Bài học không thuộc khóa học hiện tại.");
        }

        OrderedLesson currentLesson = orderedLessons.get(lessonIndex);
        if (currentLesson.moduleIndex() > 0) {
            OnlineCourseModule previousModule = orderedModules(course).get(currentLesson.moduleIndex() - 1);
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

    public void ensureAssessmentCanBeSubmitted(User student, CourseAssessment assessment) {
        OnlineCourse course = assessment.getOnlineCourse();
        OnlineCourseModule module = assessment.getModule();
        if (module == null) {
            ensureAllLessonsCompleted(student, course);
            ensureAllModuleChecksPassed(student, course);
            return;
        }

        List<OnlineCourseModule> orderedModules = orderedModules(course);
        int moduleIndex = indexOfModule(orderedModules, module.getId());
        if (moduleIndex < 0) {
            throw new RuntimeException("Mô-đun không thuộc khóa học hiện tại.");
        }

        if (moduleIndex > 0) {
            OnlineCourseModule previousModule = orderedModules.get(moduleIndex - 1);
            if (!canAdvancePastModule(student, previousModule)) {
                throw new RuntimeException("Bạn cần hoàn thành và đạt yêu cầu ở bài đánh giá cuối mô-đun trước đó trước khi làm bài đánh giá này.");
            }
        }

        ensureModuleLessonsCompleted(student, module);
    }

    public boolean canAdvancePastModule(User student, OnlineCourseModule module) {
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
        var latestSubmission = assessment.getProgressKey() == null || assessment.getProgressKey().isBlank()
                ? assessmentSubmissionRepository.findTopByAssessmentAndStudentOrderBySubmittedAtDesc(assessment, student)
                : assessmentSubmissionRepository.findTopByAssessmentProgressKeyAndStudentOrderBySubmittedAtDesc(
                        assessment.getProgressKey(),
                        student
                );
        return latestSubmission
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

    private void ensureModuleLessonsCompleted(User student, OnlineCourseModule module) {
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
        for (OnlineCourseModule module : orderedModules(course)) {
            if (!canAdvancePastModule(student, module)) {
                throw new RuntimeException("Bạn cần hoàn thành và đạt yêu cầu ở các bài đánh giá cuối mô-đun trước khi làm bài đánh giá cuối khóa.");
            }
        }
    }

    private boolean areModuleLessonsCompleted(User student, OnlineCourseModule module) {
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

    private List<OnlineCourseModule> orderedModules(OnlineCourse course) {
        List<OnlineCourseModule> modules = new ArrayList<>(course.getPublishedModules());
        modules.sort(Comparator.comparing(OnlineCourseModule::getSequenceNumber).thenComparing(module -> module.getId() == null ? Long.MAX_VALUE : module.getId()));
        return modules;
    }

    private List<OnlineLesson> orderedLessonsOfModule(OnlineCourseModule module) {
        List<OnlineLesson> lessons = new ArrayList<>(module.getLessons() == null ? List.of() : module.getLessons());
        lessons.sort(Comparator.comparing(OnlineLesson::getSequenceNumber).thenComparing(lesson -> lesson.getId() == null ? Long.MAX_VALUE : lesson.getId()));
        return lessons;
    }

    private List<OrderedLesson> orderedLessons(OnlineCourse course) {
        List<OrderedLesson> orderedLessons = new ArrayList<>();
        List<OnlineCourseModule> modules = orderedModules(course);
        for (int moduleIndex = 0; moduleIndex < modules.size(); moduleIndex++) {
            OnlineCourseModule module = modules.get(moduleIndex);
            List<OnlineLesson> lessons = orderedLessonsOfModule(module);
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

    private int indexOfModule(List<OnlineCourseModule> modules, Long moduleId) {
        for (int index = 0; index < modules.size(); index++) {
            if (moduleId.equals(modules.get(index).getId())) {
                return index;
            }
        }
        return -1;
    }

    private record OrderedLesson(int moduleIndex, int lessonIndex, OnlineCourseModule module, OnlineLesson lesson) {
    }
}
