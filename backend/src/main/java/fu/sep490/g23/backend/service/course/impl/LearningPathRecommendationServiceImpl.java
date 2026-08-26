package fu.sep490.g23.backend.service.course.impl;

import fu.sep490.g23.backend.dto.response.course.LearnerLearningPathCourseResponse;
import fu.sep490.g23.backend.dto.response.course.LearnerLearningPathResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.course.LearningPath;
import fu.sep490.g23.backend.entity.course.LearningPathCourse;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.entity.course.OnlineCourseEnrollment;
import fu.sep490.g23.backend.entity.course.enums.CourseLevel;
import fu.sep490.g23.backend.entity.course.enums.EnrollmentStatus;
import fu.sep490.g23.backend.entity.course.enums.PackageStatus;
import fu.sep490.g23.backend.repository.course.LearningPathCourseRepository;
import fu.sep490.g23.backend.repository.course.LearningPathRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseEnrollmentRepository;
import fu.sep490.g23.backend.service.assessment.PlacementRecommendationContext;
import fu.sep490.g23.backend.service.course.LearningPathRecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LearningPathRecommendationServiceImpl implements LearningPathRecommendationService {
    private final LearningPathRepository pathRepository;
    private final LearningPathCourseRepository pathCourseRepository;
    private final OnlineCourseEnrollmentRepository enrollmentRepository;

    @Override
    public LearnerLearningPathResponse.PathOverview recommend(
            User learner,
            PlacementRecommendationContext context,
            boolean preserveActivePath
    ) {
        Map<Long, OnlineCourseEnrollment> enrollments = enrollmentRepository.findByStudentOrderByRegisteredAtDesc(learner)
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        enrollment -> enrollment.getOnlineCourse().getId(),
                        enrollment -> enrollment,
                        (first, ignored) -> first
                ));
        List<PathCandidate> candidates = pathRepository.findAll().stream()
                .map(path -> new PathCandidate(path, refs(path), score(path, context)))
                .filter(candidate -> !candidate.refs().isEmpty())
                .toList();
        if (candidates.isEmpty()) return null;

        PathCandidate selected = preserveActivePath
                ? candidates.stream().filter(candidate -> hasActiveEnrollment(candidate, enrollments)).findFirst().orElse(null)
                : null;
        if (selected == null) {
            selected = candidates.stream()
                    .filter(candidate -> examCompatible(candidate.path(), context))
                    .sorted(Comparator.comparingDouble(PathCandidate::score).reversed()
                            .thenComparing(candidate -> candidate.path().getCode())
                            .thenComparing(candidate -> candidate.path().getId()))
                    .findFirst()
                    .orElseGet(() -> candidates.stream()
                            .sorted(Comparator.comparing(candidate -> candidate.path().getCode()))
                            .findFirst().orElse(null));
        }
        return selected == null ? null : toOverview(selected, enrollments, context);
    }

    private LearnerLearningPathResponse.PathOverview toOverview(
            PathCandidate candidate,
            Map<Long, OnlineCourseEnrollment> enrollments,
            PlacementRecommendationContext context
    ) {
        List<LearningPathCourse> refs = candidate.refs();
        int startIndex = resolveStartIndex(refs, enrollments, context);
        List<LearnerLearningPathCourseResponse> courses = new java.util.ArrayList<>();
        for (int index = 0; index < refs.size(); index++) {
            LearningPathCourse ref = refs.get(index);
            OnlineCourse course = ref.getOnlineCourse();
            OnlineCourseEnrollment enrollment = activeEnrollment(enrollments.get(course.getId()));
            boolean completed = enrollment != null && (enrollment.getStatus() == EnrollmentStatus.COMPLETED
                    || defaultInt(enrollment.getProgressPercent()) >= 100);
            String stepStatus;
            if (completed) {
                stepStatus = "COMPLETED";
            } else if (enrollment != null && enrollment.getStatus() != EnrollmentStatus.CANCELLED) {
                stepStatus = "CURRENT";
            } else if (index < startIndex) {
                stepStatus = "PLACEMENT_WAIVED";
            } else if (index == startIndex) {
                stepStatus = "CURRENT";
            } else if (index == startIndex + 1) {
                stepStatus = "NEXT";
            } else {
                stepStatus = "LOCKED";
            }
            courses.add(LearnerLearningPathCourseResponse.builder()
                    .courseId(course.getId())
                    .slug(course.getSlug())
                    .title(course.getTitle())
                    .thumbnailUrl(course.getThumbnailUrl())
                    .learningPathOrder(ref.getDisplayOrder())
                    .enrollmentStatus(enrollment == null ? "NOT_ENROLLED" : enrollment.getStatus().name())
                    .progressPercent(enrollment == null ? 0 : defaultInt(enrollment.getProgressPercent()))
                    .completed(completed)
                    .lockedReason("LOCKED".equals(stepStatus) ? "Hoàn thành giai đoạn hiện tại để mở khóa học này." : null)
                    .stepStatus(stepStatus)
                    .build());
        }

        LearnerLearningPathCourseResponse start = courses.isEmpty() ? null : courses.get(Math.min(startIndex, courses.size() - 1));
        return LearnerLearningPathResponse.PathOverview.builder()
                .id(candidate.path().getId())
                .code(candidate.path().getCode())
                .name(candidate.path().getName())
                .examCategory(candidate.path().getExamCategory())
                .targetBand(candidate.path().getTargetBand())
                .targetScore(candidate.path().getTargetScore())
                .totalCourses(courses.size())
                .completedCourses((int) courses.stream().filter(LearnerLearningPathCourseResponse::isCompleted).count())
                .waivedCourses((int) courses.stream().filter(course -> "PLACEMENT_WAIVED".equals(course.getStepStatus())).count())
                .currentStepCourseId(start == null ? null : start.getCourseId())
                .nextCourseId(start == null || !"NOT_ENROLLED".equals(start.getEnrollmentStatus()) ? null : start.getCourseId())
                .recommendedStartCourseId(start == null ? null : start.getCourseId())
                .recommendedStartOrder(start == null ? null : start.getLearningPathOrder())
                .recommendationReason(buildReason(context, start))
                .courses(courses)
                .build();
    }

    private int resolveStartIndex(
            List<LearningPathCourse> refs,
            Map<Long, OnlineCourseEnrollment> enrollments,
            PlacementRecommendationContext context
    ) {
        for (int index = 0; index < refs.size(); index++) {
            OnlineCourseEnrollment enrollment = activeEnrollment(enrollments.get(refs.get(index).getOnlineCourse().getId()));
            if (enrollment != null && enrollment.getStatus() != EnrollmentStatus.COMPLETED
                    && defaultInt(enrollment.getProgressPercent()) < 100) return index;
        }

        if (context != null && context.getRecommendedLevel() != null) {
            CourseLevel desired = CourseLevel.valueOf(context.getRecommendedLevel().name());
            Integer desiredIndex = null;
            if ("IELTS".equals(context.getExamType()) && context.getOverallScore() != null) {
                double current = context.getOverallScore().doubleValue();
                for (int index = 0; index < refs.size(); index++) {
                    OnlineCourse course = refs.get(index).getOnlineCourse();
                    if (course.getLevel() == desired && course.getRecommendedCurrentBandMin() != null
                            && current >= course.getRecommendedCurrentBandMin()) {
                        desiredIndex = index;
                        break;
                    }
                }
            }
            if (desiredIndex == null) {
                for (int index = 0; index < refs.size(); index++) {
                    if (refs.get(index).getOnlineCourse().getLevel() == desired) {
                        desiredIndex = index;
                        break;
                    }
                }
            }
            if (desiredIndex != null) {
                int start = desiredIndex;
                while (start < refs.size() - 1 && isCompleted(refs.get(start), enrollments)) start++;
                return start;
            }
        }

        for (int index = 0; index < refs.size(); index++) {
            OnlineCourseEnrollment enrollment = activeEnrollment(enrollments.get(refs.get(index).getOnlineCourse().getId()));
            if (enrollment == null || enrollment.getStatus() != EnrollmentStatus.COMPLETED) return index;
        }
        return Math.max(0, refs.size() - 1);
    }

    private double score(LearningPath path, PlacementRecommendationContext context) {
        if (context == null) return path.getExamCategory() == null ? 1 : 0;
        double score = 0;
        String exam = normalize(path.getExamCategory());
        if (exam.equals(context.getExamType())) score += 30;
        else if (!exam.isBlank()) return -1000;
        BigDecimal target = context.getTargetScore();
        if (target != null && "IELTS".equals(context.getExamType()) && path.getTargetBand() != null) {
            score += Math.max(0, 20 - path.getTargetBand().subtract(target).abs().doubleValue() * 10);
        }
        if (target != null && "TOEIC".equals(context.getExamType()) && path.getTargetScore() != null) {
            score += Math.max(0, 20 - Math.abs(path.getTargetScore() - target.intValue()) / 25D);
        }
        if (target == null && exam.equals(context.getExamType())) score += path.getTargetBand() == null && path.getTargetScore() == null ? 8 : 3;
        return score;
    }

    private boolean examCompatible(LearningPath path, PlacementRecommendationContext context) {
        if (context == null) return true;
        String exam = normalize(path.getExamCategory());
        return exam.isBlank() || exam.equals(context.getExamType());
    }

    private boolean hasActiveEnrollment(PathCandidate candidate, Map<Long, OnlineCourseEnrollment> enrollments) {
        return candidate.refs().stream().anyMatch(ref -> {
            OnlineCourseEnrollment enrollment = activeEnrollment(enrollments.get(ref.getOnlineCourse().getId()));
            return enrollment != null && enrollment.getStatus() == EnrollmentStatus.ACTIVE;
        });
    }

    private List<LearningPathCourse> refs(LearningPath path) {
        return pathCourseRepository.findByLearningPathIdOrderByDisplayOrderAscIdAsc(path.getId()).stream()
                .filter(ref -> ref.getOnlineCourse().getStatus() == PackageStatus.PUBLISHED
                        && !ref.getOnlineCourse().isDeleted())
                .toList();
    }

    private String buildReason(PlacementRecommendationContext context, LearnerLearningPathCourseResponse start) {
        if (context == null || start == null) return "Lộ trình phù hợp nhất với hồ sơ học tập hiện tại của bạn.";
        if (context.getRecommendedLevel() == null) {
            return "Lộ trình được chọn theo kỳ thi và mục tiêu trong hồ sơ của bạn.";
        }
        return "Với trình độ " + context.getRecommendedLevel().name().toLowerCase(Locale.ROOT)
                + ", bạn nên bắt đầu từ giai đoạn “" + start.getTitle() + "”.";
    }

    private OnlineCourseEnrollment activeEnrollment(OnlineCourseEnrollment enrollment) {
        return enrollment == null || enrollment.getStatus() == EnrollmentStatus.CANCELLED ? null : enrollment;
    }

    private boolean isCompleted(LearningPathCourse ref, Map<Long, OnlineCourseEnrollment> enrollments) {
        OnlineCourseEnrollment enrollment = activeEnrollment(enrollments.get(ref.getOnlineCourse().getId()));
        return enrollment != null && (enrollment.getStatus() == EnrollmentStatus.COMPLETED
                || defaultInt(enrollment.getProgressPercent()) >= 100);
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private record PathCandidate(LearningPath path, List<LearningPathCourse> refs, double score) {}
}
