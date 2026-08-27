package fu.sep490.g23.backend.service.classroom.impl;

import fu.sep490.g23.backend.dto.request.classroom.InstructorLedCourseRequest;
import fu.sep490.g23.backend.dto.response.classroom.InstructorLedCourseResponse;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomOfferingStatus;
import fu.sep490.g23.backend.entity.course.enums.PackageStatus;
import fu.sep490.g23.backend.entity.course.InstructorLedCourse;
import fu.sep490.g23.backend.repository.course.InstructorLedCourseRepository;
import fu.sep490.g23.backend.service.classroom.InstructorLedCourseCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional
public class InstructorLedCourseCatalogServiceImpl implements InstructorLedCourseCatalogService {
    private static final Pattern NON_LATIN = Pattern.compile("[^\\w\\s-]");
    private static final Set<ClassroomOfferingStatus> ACTIVE_CLASS_STATUSES = Set.of(
            ClassroomOfferingStatus.UPCOMING,
            ClassroomOfferingStatus.ACTIVE
    );

    private final InstructorLedCourseRepository programRepository;

    @Override
    @Transactional(readOnly = true)
    public List<InstructorLedCourseResponse> listPrograms(ClassroomDeliveryMode deliveryMode) {
        List<InstructorLedCourse> programs = programRepository.findAllByOrderByDisplayOrderAscUpdatedAtDescIdDesc();
        return programs.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstructorLedCourseResponse> listPublishedPrograms(ClassroomDeliveryMode deliveryMode) {
        return listPrograms(deliveryMode).stream()
                .filter(program -> program.getStatus() == PackageStatus.PUBLISHED)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public InstructorLedCourseResponse getPublishedProgram(String slugOrId) {
        InstructorLedCourse program;
        try {
            program = programRepository.findById(Long.parseLong(slugOrId))
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học."));
        } catch (NumberFormatException ignored) {
            program = programRepository.findBySlug(slugOrId)
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

    @Override
    public InstructorLedCourseResponse createProgram(InstructorLedCourseRequest request) {
        InstructorLedCourse program = InstructorLedCourse.builder()
                .title(request.getTitle().trim())
                .code(uniqueCode(defaultText(request.getCode(), makeCode(request.getTitle(), request.getDeliveryType()))))
                .slug(uniqueSlug(defaultText(request.getSlug(), slugify(request.getTitle()))))
                .examType("IELTS")
                .publicationStatus(request.getStatus() == null ? PackageStatus.DRAFT : request.getStatus())
                .build();
        apply(program, request);
        validatePublishable(program);
        InstructorLedCourse saved = programRepository.save(program);
        return toResponse(saved);
    }

    @Override
    public InstructorLedCourseResponse updateProgram(Long id, InstructorLedCourseRequest request) {
        InstructorLedCourse program = findProgram(id);
        PackageStatus originalStatus = program.getPublicationStatus();
        program.setTitle(request.getTitle().trim());
        program.setCode(uniqueCodeForUpdate(defaultText(request.getCode(), makeCode(request.getTitle(), request.getDeliveryType())), program.getId()));
        program.setSlug(uniqueSlugForUpdate(defaultText(request.getSlug(), slugify(request.getTitle())), program.getId()));
        program.setPublicationStatus(request.getStatus() == null ? PackageStatus.DRAFT : request.getStatus());
        apply(program, request);
        if (countActiveClassrooms(program) > 0
                && originalStatus != program.getPublicationStatus()) {
            throw new IllegalArgumentException(
                    "Không thể đổi chương trình đào tạo hoặc trạng thái của khóa học đang được lớp hoạt động sử dụng."
            );
        }
        validatePublishable(program);
        InstructorLedCourse saved = programRepository.save(program);
        return toResponse(saved);
    }

    @Override
    public InstructorLedCourseResponse cloneProgram(Long id) {
        InstructorLedCourse source = findProgram(id);
        InstructorLedCourse clone = InstructorLedCourse.builder()
                .title(source.getTitle() + " (Bản sao)")
                .code(uniqueCode(source.getCode() + "-COPY"))
                .slug(uniqueSlug(source.getSlug() + "-copy"))
                .examType(source.getExamType())
                .programTrack(source.getProgramTrack())
                .entryLevel(source.getEntryLevel())
                .focusSkills(source.getFocusSkills())
                .learningOutcomes(source.getLearningOutcomes())
                .shortDescription(source.getShortDescription())
                .description(source.getDescription())
                .baseTuitionFeeVnd(source.getBaseTuitionFeeVnd())
                .saleTuitionFeeVnd(source.getSaleTuitionFeeVnd())
                .durationLabel(source.getDurationLabel())
                .thumbnailUrl(source.getThumbnailUrl())
                .publicationStatus(PackageStatus.DRAFT)
                .featured(false)
                .build();
        InstructorLedCourse saved = programRepository.save(clone);
        return toResponse(saved);
    }

    @Override
    public void archiveProgram(Long id) {
        InstructorLedCourse program = findProgram(id);
        if (countActiveClassrooms(program) > 0) {
            throw new RuntimeException("Không thể lưu trữ khóa học đang được lớp sắp khai giảng hoặc đang hoạt động sử dụng.");
        }
        program.setPublicationStatus(PackageStatus.ARCHIVED);
        programRepository.save(program);
    }

    private void apply(InstructorLedCourse program, InstructorLedCourseRequest request) {
        program.setShortDescription(trimOrNull(request.getShortDescription()));
        program.setDescription(trimOrNull(request.getDescription()));
        program.setBaseTuitionFeeVnd(request.getPrice() == null ? BigDecimal.ZERO : request.getPrice());
        program.setSaleTuitionFeeVnd(request.getSalePrice());
        program.setDurationLabel(trimOrNull(request.getDuration()));
        program.setThumbnailUrl(trimOrNull(request.getThumbnailUrl()));
        program.setFeatured(Boolean.TRUE.equals(request.getFeatured()));
    }

    private void validatePublishable(InstructorLedCourse program) {
        if (program.getPublicationStatus() != PackageStatus.PUBLISHED) {
            return;
        }
    }

    private InstructorLedCourseResponse toResponse(InstructorLedCourse program) {
        return InstructorLedCourseResponse.builder()
                .id(program.getId())
                .title(program.getTitle())
                .code(program.getCode())
                .slug(program.getSlug())
                .instructorLedCourseId(program.getId())
                .instructorLedCourseTitle(program.getTitle())
                .instructorLedCourseCode(program.getCode())
                .instructorLedCourseExamType(program.getExamType())
                .examType(program.getExamType())
                .examCategory(program.getExamType())
                .programTrack(program.getProgramTrack())
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
                .thumbnailUrl(program.getThumbnailUrl())
                .status(program.getPublicationStatus())
                .statusLabel(statusLabel(program.getPublicationStatus()))
                .displayOrder(program.getDisplayOrder())
                .featured(program.isFeatured())
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

    private long countActiveClassrooms(InstructorLedCourse program) {
        return 0L;
    }

    private String makeCode(String title, ClassroomDeliveryMode mode) {
        String prefix = mode == ClassroomDeliveryMode.VIRTUAL ? "VIRTUAL" : "OFFLINE";
        String normalized = Normalizer.normalize(String.valueOf(title), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .replaceAll("[^A-Za-z0-9\\s]", " ")
                .trim()
                .replaceAll("\\s+", "-")
                .toUpperCase(Locale.ROOT);
        return (prefix + "-" + normalized).replaceAll("-+", "-");
    }

    private String slugify(String value) {
        String normalized = Normalizer.normalize(String.valueOf(value), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replace('Đ', 'D');
        return NON_LATIN.matcher(normalized)
                .replaceAll(" ")
                .trim()
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .toLowerCase(Locale.ROOT);
    }

    private String uniqueCode(String base) {
        String normalized = defaultText(base, "PROGRAM").trim().toUpperCase(Locale.ROOT);
        String candidate = normalized;
        int index = 2;
        while (programRepository.existsByCodeIgnoreCase(candidate)) {
            candidate = normalized + "-" + index++;
        }
        return candidate;
    }

    private String uniqueCodeForUpdate(String base, Long currentId) {
        String normalized = defaultText(base, "PROGRAM").trim().toUpperCase(Locale.ROOT);
        return programRepository.findAll().stream()
                .filter(program -> !program.getId().equals(currentId))
                .anyMatch(program -> program.getCode().equalsIgnoreCase(normalized))
                ? uniqueCode(normalized)
                : normalized;
    }

    private String uniqueSlug(String base) {
        String normalized = defaultText(slugify(base), "program");
        String candidate = normalized;
        int index = 2;
        while (programRepository.existsBySlug(candidate)) {
            candidate = normalized + "-" + index++;
        }
        return candidate;
    }

    private String uniqueSlugForUpdate(String base, Long currentId) {
        String normalized = defaultText(slugify(base), "program");
        return programRepository.findAll().stream()
                .filter(program -> !program.getId().equals(currentId))
                .anyMatch(program -> program.getSlug().equals(normalized))
                ? uniqueSlug(normalized)
                : normalized;
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String trimOrNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String statusLabel(PackageStatus status) {
        if (status == PackageStatus.PUBLISHED) return "Đã xuất bản";
        if (status == PackageStatus.ARCHIVED) return "Đã lưu trữ";
        if (status == PackageStatus.REJECTED) return "Từ chối";
        if (status == PackageStatus.PENDING_REVIEW) return "Chờ duyệt";
        return "Bản nháp";
    }
}
