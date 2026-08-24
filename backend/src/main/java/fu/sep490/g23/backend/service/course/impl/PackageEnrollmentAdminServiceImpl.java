package fu.sep490.g23.backend.service.course.impl;

import fu.sep490.g23.backend.dto.request.course.UpdatePackageEnrollmentRequest;
import fu.sep490.g23.backend.dto.response.course.PackageEnrollmentAdminResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.course.LearningPackage;
import fu.sep490.g23.backend.entity.course.PackageEnrollment;
import fu.sep490.g23.backend.entity.course.enums.EnrollmentStatus;
import fu.sep490.g23.backend.entity.course.enums.PackageTypeCode;
import fu.sep490.g23.backend.repository.course.PackageEnrollmentRepository;
import fu.sep490.g23.backend.security.ClassroomAccessHelper;
import fu.sep490.g23.backend.service.course.PackageEnrollmentAdminService;
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
public class PackageEnrollmentAdminServiceImpl implements PackageEnrollmentAdminService {

    private final PackageEnrollmentRepository enrollmentRepository;
    private final ClassroomAccessHelper accessHelper;

    @Override
    @Transactional(readOnly = true)
    public List<PackageEnrollmentAdminResponse> listEnrollments(EnrollmentStatus status, String keyword) {
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim().toLowerCase() : null;
        return enrollmentRepository.findAll().stream()
                .filter(enrollment -> isOnlinePackage(enrollment.getLearningPackage()))
                .filter(enrollment -> status == null || enrollment.getStatus() == status)
                .filter(enrollment -> matchesKeyword(enrollment, normalizedKeyword))
                .sorted(Comparator.comparing(PackageEnrollment::getRegisteredAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PackageEnrollmentAdminResponse> pageEnrollments(EnrollmentStatus status, String keyword, Pageable pageable) {
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim().toLowerCase() : null;
        Specification<PackageEnrollment> specification = (root, query, criteriaBuilder) -> {
            var learningPackage = root.join("learningPackage", JoinType.INNER);
            var packageType = learningPackage.join("packageType", JoinType.INNER);
            var student = root.join("student", JoinType.LEFT);
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(criteriaBuilder.equal(packageType.get("code"), PackageTypeCode.ONLINE_COURSE));
            predicates.add(criteriaBuilder.isFalse(learningPackage.get("deleted")));
            if (status != null) predicates.add(criteriaBuilder.equal(root.get("status"), status));
            if (normalizedKeyword != null) {
                String pattern = "%" + normalizedKeyword + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(student.get("fullName")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(student.get("email")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(learningPackage.get("title")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(learningPackage.get("slug")), pattern)
                ));
            }
            return criteriaBuilder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        return enrollmentRepository.findAll(specification, pageable).map(this::toResponse);
    }

    @Override
    public PackageEnrollmentAdminResponse updateEnrollment(
            Long enrollmentId,
            UpdatePackageEnrollmentRequest request,
            String managerEmail
    ) {
        User manager = accessHelper.requireUser(managerEmail);
        accessHelper.assertManager(manager);
        PackageEnrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ghi danh."));
        if (!isOnlinePackage(enrollment.getLearningPackage())) {
            throw new IllegalArgumentException("Ghi danh này không thuộc khóa học online.");
        }
        enrollment.setStatus(request.getStatus());
        return toResponse(enrollmentRepository.save(enrollment));
    }

    private boolean isOnlinePackage(LearningPackage learningPackage) {
        return learningPackage != null
                && learningPackage.getPackageType() != null
                && learningPackage.getPackageType().getCode() == PackageTypeCode.ONLINE_COURSE
                && !learningPackage.isDeleted();
    }

    private boolean matchesKeyword(PackageEnrollment enrollment, String keyword) {
        if (keyword == null) {
            return true;
        }
        User student = enrollment.getStudent();
        LearningPackage learningPackage = enrollment.getLearningPackage();
        return (student != null && (
                contains(student.getFullName(), keyword)
                        || contains(student.getEmail(), keyword)))
                || (learningPackage != null && (
                contains(learningPackage.getTitle(), keyword)
                        || contains(learningPackage.getSlug(), keyword)));
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword);
    }

    private PackageEnrollmentAdminResponse toResponse(PackageEnrollment enrollment) {
        LearningPackage learningPackage = enrollment.getLearningPackage();
        User student = enrollment.getStudent();
        return PackageEnrollmentAdminResponse.builder()
                .id(enrollment.getId())
                .studentId(student == null ? null : student.getId())
                .studentName(student == null ? null : student.getFullName())
                .studentEmail(student == null ? null : student.getEmail())
                .packageId(learningPackage == null ? null : learningPackage.getId())
                .packageTitle(learningPackage == null ? null : learningPackage.getTitle())
                .packageSlug(learningPackage == null ? null : learningPackage.getSlug())
                .status(enrollment.getStatus())
                .progressPercent(enrollment.getProgressPercent())
                .registeredAt(enrollment.getRegisteredAt())
                .updatedAt(enrollment.getUpdatedAt())
                .build();
    }
}
