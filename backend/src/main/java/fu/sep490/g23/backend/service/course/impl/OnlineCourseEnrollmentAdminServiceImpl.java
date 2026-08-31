package fu.sep490.g23.backend.service.course.impl;

import fu.sep490.g23.backend.dto.request.course.UpdateOnlineCourseEnrollmentRequest;
import fu.sep490.g23.backend.dto.response.course.OnlineCourseEnrollmentAdminResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.course.OnlineCourseEnrollment;
import fu.sep490.g23.backend.entity.course.enums.EnrollmentStatus;
import fu.sep490.g23.backend.repository.course.OnlineCourseEnrollmentRepository;
import fu.sep490.g23.backend.security.ClassroomAccessHelper;
import fu.sep490.g23.backend.service.course.OnlineCourseEnrollmentAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.JoinType;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class OnlineCourseEnrollmentAdminServiceImpl implements OnlineCourseEnrollmentAdminService {

    private final OnlineCourseEnrollmentRepository enrollmentRepository;
    private final ClassroomAccessHelper accessHelper;

    @Override
    @Transactional(readOnly = true)
    public List<OnlineCourseEnrollmentAdminResponse> listEnrollments(EnrollmentStatus status, String keyword) {
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim().toLowerCase() : null;
        return enrollmentRepository.findAll().stream()
                .filter(enrollment -> enrollment.getOnlineCourse() != null)
                .filter(enrollment -> status == null || enrollment.getStatus() == status)
                .filter(enrollment -> matchesKeyword(enrollment, normalizedKeyword))
                .sorted(Comparator.comparing(OnlineCourseEnrollment::getRegisteredAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OnlineCourseEnrollmentAdminResponse> pageEnrollments(EnrollmentStatus status, String keyword, Pageable pageable) {
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim().toLowerCase() : null;
        Specification<OnlineCourseEnrollment> specification = (root, query, criteriaBuilder) -> {
            var course = root.join("onlineCourse", JoinType.INNER);
            var student = root.join("student", JoinType.LEFT);
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(criteriaBuilder.isFalse(course.get("deleted")));
            if (status != null) predicates.add(criteriaBuilder.equal(root.get("status"), status));
            if (normalizedKeyword != null) {
                String pattern = "%" + normalizedKeyword + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(student.get("fullName")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(student.get("email")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(course.get("title")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(course.get("slug")), pattern)
                ));
            }
            return criteriaBuilder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        return enrollmentRepository.findAll(specification, pageable).map(this::toResponse);
    }

    @Override
    public OnlineCourseEnrollmentAdminResponse updateEnrollment(
            Long enrollmentId,
            UpdateOnlineCourseEnrollmentRequest request,
            String managerEmail
    ) {
        User manager = accessHelper.requireUser(managerEmail);
        accessHelper.assertManager(manager);
        OnlineCourseEnrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ghi danh."));
        if (enrollment.getOnlineCourse() == null) {
            throw new IllegalArgumentException("Ghi danh này không thuộc khóa học online.");
        }
        enrollment.setStatus(request.getStatus());
        return toResponse(enrollmentRepository.save(enrollment));
    }

    

    private boolean matchesKeyword(OnlineCourseEnrollment enrollment, String keyword) {
        if (keyword == null) {
            return true;
        }
        User student = enrollment.getStudent();
        fu.sep490.g23.backend.entity.course.OnlineCourse course = enrollment.getOnlineCourse();
        return (student != null && (
                contains(student.getFullName(), keyword)
                        || contains(student.getEmail(), keyword)))
                || (course != null && (
                contains(course.getTitle(), keyword)
                        || contains(course.getSlug(), keyword)));
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword);
    }

    private OnlineCourseEnrollmentAdminResponse toResponse(OnlineCourseEnrollment enrollment) {
        fu.sep490.g23.backend.entity.course.OnlineCourse course = enrollment.getOnlineCourse();
        User student = enrollment.getStudent();
        return OnlineCourseEnrollmentAdminResponse.builder()
                .id(enrollment.getId())
                .studentId(student == null ? null : student.getId())
                .studentName(student == null ? null : student.getFullName())
                .studentEmail(student == null ? null : student.getEmail())
                .packageId(course == null ? null : course.getId())
                .packageTitle(course == null ? null : course.getTitle())
                .packageSlug(course == null ? null : course.getSlug())
                .status(enrollment.getStatus())
                .progressPercent(enrollment.getProgressPercent())
                .registeredAt(enrollment.getRegisteredAt())
                .updatedAt(enrollment.getUpdatedAt())
                .build();
    }
}
