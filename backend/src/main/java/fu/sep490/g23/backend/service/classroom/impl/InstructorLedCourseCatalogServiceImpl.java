package fu.sep490.g23.backend.service.classroom.impl;

import fu.sep490.g23.backend.dto.response.classroom.InstructorLedCourseResponse;
import fu.sep490.g23.backend.entity.course.enums.PackageStatus;
import fu.sep490.g23.backend.entity.course.InstructorLedCourse;
import fu.sep490.g23.backend.repository.course.InstructorLedCourseRepository;
import fu.sep490.g23.backend.service.classroom.InstructorLedCourseCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class InstructorLedCourseCatalogServiceImpl implements InstructorLedCourseCatalogService {
    private final InstructorLedCourseRepository instructorLedCourseRepository;

    @Override
    @Transactional(readOnly = true)
    public List<InstructorLedCourseResponse> listInstructorLedCourses() {
        List<InstructorLedCourse> courses = instructorLedCourseRepository.findAllByOrderByUpdatedAtDescIdDesc();
        return courses.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstructorLedCourseResponse> listPublishedInstructorLedCourses() {
        return listInstructorLedCourses().stream()
                .filter(course -> course.getStatus() == PackageStatus.PUBLISHED)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public InstructorLedCourseResponse getPublishedInstructorLedCourse(String idOrCode) {
        InstructorLedCourse course;
        try {
            course = instructorLedCourseRepository.findById(Long.parseLong(idOrCode))
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học."));
        } catch (NumberFormatException ignored) {
            course = instructorLedCourseRepository.findByCodeIgnoreCase(idOrCode)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học."));
        }
        if (course.getPublicationStatus() != PackageStatus.PUBLISHED) {
            throw new RuntimeException("Khóa học chưa mở nhận đăng ký.");
        }
        return toResponse(course);
    }

    @Override
    @Transactional(readOnly = true)
    public InstructorLedCourseResponse getInstructorLedCourse(Long id) {
        return toResponse(findInstructorLedCourse(id));
    }

    private InstructorLedCourseResponse toResponse(InstructorLedCourse course) {
        return InstructorLedCourseResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .code(course.getCode())
                .instructorLedCourseId(course.getId())
                .instructorLedCourseTitle(course.getTitle())
                .instructorLedCourseCode(course.getCode())
                .instructorLedCourseExamType(course.getExamType())
                .examType(course.getExamType())
                .examCategory(course.getExamType())
                .focusSkills(course.getFocusSkills())
                .instructorLedCourseStatus(course.getPublicationStatus().name())
                .shortDescription(course.getShortDescription())
                .description(course.getDescription())
                .entryLevel(course.getEntryLevel())
                .targetScore(resolveTargetScore(course))
                .targetOutcome(course.getLearningOutcomes())
                .price(course.getBaseTuitionFeeVnd())
                .salePrice(course.getSaleTuitionFeeVnd())
                .duration(course.getDurationLabel())
                .status(course.getPublicationStatus())
                .statusLabel(statusLabel(course.getPublicationStatus()))
                .classroomCount(0)
                .activeClassroomCount(0)
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .build();
    }

    private String resolveTargetScore(InstructorLedCourse curriculum) {
        if (curriculum == null) {
            return null;
        }
        if (curriculum.getTargetBand() != null) {
            return curriculum.getTargetBand().stripTrailingZeros().toPlainString();
        }
        return curriculum.getTargetScore() == null ? null : String.valueOf(curriculum.getTargetScore());
    }

    private InstructorLedCourse findInstructorLedCourse(Long id) {
        return instructorLedCourseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học có giảng viên."));
    }

    private String statusLabel(PackageStatus status) {
        if (status == PackageStatus.PUBLISHED) return "Đã xuất bản";
        if (status == PackageStatus.ARCHIVED) return "Đã lưu trữ";
        if (status == PackageStatus.REJECTED) return "Từ chối";
        if (status == PackageStatus.PENDING_REVIEW) return "Chờ duyệt";
        return "Bản nháp";
    }
}
