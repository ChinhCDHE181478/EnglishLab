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
    private final InstructorLedCourseRepository programRepository;

    @Override
    @Transactional(readOnly = true)
    public List<InstructorLedCourseResponse> listPrograms() {
        List<InstructorLedCourse> programs = programRepository.findAllByOrderByUpdatedAtDescIdDesc();
        return programs.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstructorLedCourseResponse> listPublishedPrograms() {
        return listPrograms().stream()
                .filter(program -> program.getStatus() == PackageStatus.PUBLISHED)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public InstructorLedCourseResponse getPublishedProgram(String idOrCode) {
        InstructorLedCourse program;
        try {
            program = programRepository.findById(Long.parseLong(idOrCode))
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học."));
        } catch (NumberFormatException ignored) {
            program = programRepository.findByCodeIgnoreCase(idOrCode)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học."));
        }
        if (program.getPublicationStatus() != PackageStatus.PUBLISHED) {
            throw new RuntimeException("Khóa học chưa mở nhận đăng ký.");
        }
        return toResponse(program);
    }

    @Override
    @Transactional(readOnly = true)
    public InstructorLedCourseResponse getProgram(Long id) {
        return toResponse(findProgram(id));
    }

    private InstructorLedCourseResponse toResponse(InstructorLedCourse program) {
        return InstructorLedCourseResponse.builder()
                .id(program.getId())
                .title(program.getTitle())
                .code(program.getCode())
                .instructorLedCourseId(program.getId())
                .instructorLedCourseTitle(program.getTitle())
                .instructorLedCourseCode(program.getCode())
                .instructorLedCourseExamType(program.getExamType())
                .examType(program.getExamType())
                .examCategory(program.getExamType())
                .focusSkills(program.getFocusSkills())
                .instructorLedCourseStatus(program.getPublicationStatus().name())
                .shortDescription(program.getShortDescription())
                .description(program.getDescription())
                .entryLevel(program.getEntryLevel())
                .targetScore(resolveTargetScore(program))
                .targetOutcome(program.getLearningOutcomes())
                .price(program.getBaseTuitionFeeVnd())
                .salePrice(program.getSaleTuitionFeeVnd())
                .duration(program.getDurationLabel())
                .status(program.getPublicationStatus())
                .statusLabel(statusLabel(program.getPublicationStatus()))
                .classroomCount(0)
                .activeClassroomCount(0)
                .createdAt(program.getCreatedAt())
                .updatedAt(program.getUpdatedAt())
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

    private InstructorLedCourse findProgram(Long id) {
        return programRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học theo lịch."));
    }

    private String statusLabel(PackageStatus status) {
        if (status == PackageStatus.PUBLISHED) return "Đã xuất bản";
        if (status == PackageStatus.ARCHIVED) return "Đã lưu trữ";
        if (status == PackageStatus.REJECTED) return "Từ chối";
        if (status == PackageStatus.PENDING_REVIEW) return "Chờ duyệt";
        return "Bản nháp";
    }
}
