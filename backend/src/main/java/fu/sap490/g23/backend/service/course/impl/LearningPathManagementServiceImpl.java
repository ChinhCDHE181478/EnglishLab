package fu.sap490.g23.backend.service.course.impl;

import fu.sap490.g23.backend.dto.request.course.LearningPathCoursesRequest;
import fu.sap490.g23.backend.dto.request.course.LearningPathRequest;
import fu.sap490.g23.backend.dto.response.course.LearnerLearningPathCourseResponse;
import fu.sap490.g23.backend.dto.response.course.LearnerLearningPathResponse;
import fu.sap490.g23.backend.dto.response.course.LearningPathCourseResponse;
import fu.sap490.g23.backend.dto.response.course.LearningPathResponse;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.course.LearningPath;
import fu.sap490.g23.backend.entity.course.LearningPathCourse;
import fu.sap490.g23.backend.entity.course.OnlineCourse;
import fu.sap490.g23.backend.entity.course.PackageEnrollment;
import fu.sap490.g23.backend.entity.course.enums.EnrollmentStatus;
import fu.sap490.g23.backend.entity.course.enums.PackageStatus;
import fu.sap490.g23.backend.repository.UserRepository;
import fu.sap490.g23.backend.repository.course.LearningPathCourseRepository;
import fu.sap490.g23.backend.repository.course.LearningPathRepository;
import fu.sap490.g23.backend.repository.course.OnlineCourseRepository;
import fu.sap490.g23.backend.repository.course.PackageEnrollmentRepository;
import fu.sap490.g23.backend.service.course.LearningPathManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class LearningPathManagementServiceImpl implements LearningPathManagementService {
    private final LearningPathRepository learningPathRepository;
    private final LearningPathCourseRepository learningPathCourseRepository;
    private final OnlineCourseRepository onlineCourseRepository;
    private final UserRepository userRepository;
    private final PackageEnrollmentRepository enrollmentRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<LearningPathResponse> getManagedPaths(Pageable pageable) {
        return learningPathRepository.findAll(pageable).map(this::toResponse);
    }

    @Override
    public LearningPathResponse createPath(LearningPathRequest request) {
        String code = required(request.getCode(), "Mã lộ trình");
        if (learningPathRepository.existsByCodeIgnoreCase(code)) {
            throw new RuntimeException("Mã lộ trình đã tồn tại.");
        }
        LearningPath path = learningPathRepository.save(LearningPath.builder()
                .code(code)
                .name(required(request.getName(), "Tên lộ trình"))
                .build());
        return toResponse(path);
    }

    @Override
    public LearningPathResponse updatePath(Long pathId, LearningPathRequest request) {
        LearningPath path = findPath(pathId);
        String code = required(request.getCode(), "Mã lộ trình");
        learningPathRepository.findByCodeIgnoreCase(code)
                .filter(existing -> !existing.getId().equals(pathId))
                .ifPresent(existing -> { throw new RuntimeException("Mã lộ trình đã tồn tại."); });
        path.setCode(code);
        path.setName(required(request.getName(), "Tên lộ trình"));
        return toResponse(learningPathRepository.save(path));
    }

    @Override
    public LearningPathResponse addCourses(Long pathId, LearningPathCoursesRequest request) {
        LearningPath path = findPath(pathId);
        Set<Long> ids = new LinkedHashSet<>(request.getCourseIds());
        List<LearningPathCourse> existing = learningPathCourseRepository
                .findByLearningPathIdOrderByDisplayOrderAscIdAsc(pathId);
        int nextOrder = existing.size() + 1;
        for (Long courseId : ids) {
            if (learningPathCourseRepository.existsByLearningPathIdAndOnlineCourseId(pathId, courseId)) {
                continue;
            }
            OnlineCourse course = onlineCourseRepository.findById(courseId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học."));
            learningPathCourseRepository.save(LearningPathCourse.builder()
                    .learningPath(path)
                    .onlineCourse(course)
                    .displayOrder(nextOrder++)
                    .build());
        }
        return toResponse(path);
    }

    @Override
    public LearningPathResponse reorderCourses(Long pathId, LearningPathCoursesRequest request) {
        List<LearningPathCourse> existing = learningPathCourseRepository
                .findByLearningPathIdOrderByDisplayOrderAscIdAsc(pathId);
        Set<Long> requestedIds = new LinkedHashSet<>(request.getCourseIds());
        Set<Long> existingIds = existing.stream()
                .map(ref -> ref.getOnlineCourse().getId())
                .collect(java.util.stream.Collectors.toSet());
        if (requestedIds.size() != existingIds.size() || !requestedIds.equals(existingIds)) {
            throw new RuntimeException("Danh sách sắp xếp không hợp lệ.");
        }
        Map<Long, LearningPathCourse> refsByCourseId = existing.stream()
                .collect(java.util.stream.Collectors.toMap(ref -> ref.getOnlineCourse().getId(), ref -> ref));
        int order = 1;
        for (Long courseId : requestedIds) {
            refsByCourseId.get(courseId).setDisplayOrder(order++);
        }
        learningPathCourseRepository.saveAll(existing);
        return toResponse(findPath(pathId));
    }

    @Override
    public void deletePath(Long pathId) {
        learningPathRepository.delete(findPath(pathId));
    }

    @Override
    @Transactional(readOnly = true)
    public LearnerLearningPathResponse getMyLearningPath(String studentEmail) {
        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy học viên."));
        Map<Long, PackageEnrollment> enrollmentsByPackageId = enrollmentRepository
                .findByStudentOrderByRegisteredAtDesc(student)
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        enrollment -> enrollment.getLearningPackage().getId(), enrollment -> enrollment, (first, ignored) -> first));

        List<LearnerLearningPathResponse.PathOverview> allPaths = learningPathRepository.findAll().stream()
                .map(path -> toLearnerPath(path, enrollmentsByPackageId))
                .filter(path -> !path.getCourses().isEmpty())
                .sorted(Comparator.comparing(LearnerLearningPathResponse.PathOverview::getCode))
                .toList();
        List<LearnerLearningPathResponse.PathOverview> paths = allPaths.stream()
                .filter(path -> path.getCourses().stream().anyMatch(course -> !"NOT_ENROLLED".equals(course.getEnrollmentStatus())))
                .findFirst()
                .map(List::of)
                .orElseGet(() -> allPaths.isEmpty() ? List.of() : List.of(allPaths.getFirst()));
        return LearnerLearningPathResponse.builder()
                .currentBand(student.getCurrentBand())
                .targetExam(student.getTargetExam())
                .targetScore(student.getTargetScore())
                .paths(paths)
                .build();
    }

    private LearnerLearningPathResponse.PathOverview toLearnerPath(
            LearningPath path, Map<Long, PackageEnrollment> enrollmentsByPackageId) {
        List<LearningPathCourse> refs = learningPathCourseRepository.findByLearningPathIdOrderByDisplayOrderAscIdAsc(path.getId())
                .stream()
                .filter(ref -> ref.getOnlineCourse().getLearningPackage().getStatus() == PackageStatus.PUBLISHED
                        && !ref.getOnlineCourse().getLearningPackage().isDeleted())
                .toList();
        boolean prerequisiteCompleted = true;
        List<LearnerLearningPathCourseResponse> courses = new ArrayList<>();
        Long currentStepCourseId = null;
        Long nextCourseId = null;
        for (LearningPathCourse ref : refs) {
            OnlineCourse course = ref.getOnlineCourse();
            PackageEnrollment enrollment = activeEnrollment(enrollmentsByPackageId.get(course.getLearningPackage().getId()));
            boolean completed = enrollment != null && (enrollment.getStatus() == EnrollmentStatus.COMPLETED
                    || defaultInt(enrollment.getProgressPercent()) >= 100);
            boolean accessible = enrollment != null || prerequisiteCompleted;
            if (currentStepCourseId == null && accessible && !completed) {
                currentStepCourseId = course.getId();
                nextCourseId = enrollment == null ? course.getId() : null;
            }
            courses.add(LearnerLearningPathCourseResponse.builder()
                    .courseId(course.getId())
                    .slug(course.getLearningPackage().getSlug())
                    .title(course.getLearningPackage().getTitle())
                    .thumbnailUrl(course.getLearningPackage().getThumbnailUrl())
                    .learningPathOrder(ref.getDisplayOrder())
                    .enrollmentStatus(enrollment == null ? "NOT_ENROLLED" : enrollment.getStatus().name())
                    .progressPercent(enrollment == null ? 0 : defaultInt(enrollment.getProgressPercent()))
                    .completed(completed)
                    .lockedReason(accessible ? null : "Hoàn thành khóa học trước để mở bước này.")
                    .build());
            prerequisiteCompleted = prerequisiteCompleted && completed;
        }
        return LearnerLearningPathResponse.PathOverview.builder()
                .code(path.getCode())
                .name(path.getName())
                .totalCourses(courses.size())
                .completedCourses((int) courses.stream().filter(LearnerLearningPathCourseResponse::isCompleted).count())
                .currentStepCourseId(currentStepCourseId)
                .nextCourseId(nextCourseId)
                .courses(courses)
                .build();
    }

    private LearningPath findPath(Long pathId) {
        return learningPathRepository.findById(pathId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lộ trình."));
    }

    private LearningPathResponse toResponse(LearningPath path) {
        List<LearningPathCourseResponse> courses = learningPathCourseRepository
                .findByLearningPathIdOrderByDisplayOrderAscIdAsc(path.getId()).stream()
                .map(ref -> LearningPathCourseResponse.builder()
                        .courseId(ref.getOnlineCourse().getId())
                        .slug(ref.getOnlineCourse().getLearningPackage().getSlug())
                        .title(ref.getOnlineCourse().getLearningPackage().getTitle())
                        .thumbnailUrl(ref.getOnlineCourse().getLearningPackage().getThumbnailUrl())
                        .targetOutcome(ref.getOnlineCourse().getTargetOutcome())
                        .displayOrder(ref.getDisplayOrder())
                        .build())
                .toList();
        return LearningPathResponse.builder().id(path.getId()).code(path.getCode()).name(path.getName()).courses(courses).build();
    }

    private PackageEnrollment activeEnrollment(PackageEnrollment enrollment) {
        return enrollment == null || enrollment.getStatus() == EnrollmentStatus.CANCELLED ? null : enrollment;
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String required(String value, String field) {
        String result = value == null ? "" : value.trim();
        if (result.isEmpty()) throw new RuntimeException(field + " không được để trống.");
        return result;
    }
}
