package fu.sap490.g23.backend.service.course.impl;

import fu.sap490.g23.backend.dto.request.course.UpdatePackageEnrollmentRequest;
import fu.sap490.g23.backend.dto.response.course.PackageEnrollmentAdminResponse;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.course.LearningPackage;
import fu.sap490.g23.backend.entity.course.PackageEnrollment;
import fu.sap490.g23.backend.entity.course.enums.EnrollmentStatus;
import fu.sap490.g23.backend.entity.course.enums.PackageTypeCode;
import fu.sap490.g23.backend.repository.course.PackageEnrollmentRepository;
import fu.sap490.g23.backend.security.ClassroomAccessHelper;
import fu.sap490.g23.backend.service.course.PackageEnrollmentAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
