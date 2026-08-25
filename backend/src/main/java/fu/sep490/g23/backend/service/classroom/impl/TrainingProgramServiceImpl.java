package fu.sep490.g23.backend.service.classroom.impl;

import fu.sep490.g23.backend.dto.request.classroom.TrainingProgramRequest;
import fu.sep490.g23.backend.dto.response.classroom.TrainingProgramResponse;
import fu.sep490.g23.backend.entity.classroom.TrainingProgram;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomOfferingStatus;
import fu.sep490.g23.backend.entity.course.enums.PackageStatus;
import fu.sep490.g23.backend.entity.curriculum.CurriculumProgram;
import fu.sep490.g23.backend.repository.classroom.TrainingProgramRepository;
import fu.sep490.g23.backend.repository.curriculum.CurriculumProgramRepository;
import fu.sep490.g23.backend.service.classroom.TrainingProgramService;
import fu.sep490.g23.backend.service.course.InstructorLedCourseSync;
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
public class TrainingProgramServiceImpl implements TrainingProgramService {
    private static final Pattern NON_LATIN = Pattern.compile("[^\\w\\s-]");
    private static final Set<ClassroomOfferingStatus> ACTIVE_CLASS_STATUSES = Set.of(
            ClassroomOfferingStatus.UPCOMING,
            ClassroomOfferingStatus.ACTIVE
    );

    private final TrainingProgramRepository programRepository;
    private final CurriculumProgramRepository curriculumProgramRepository;
    private final InstructorLedCourseSync instructorLedCourseSync;

    @Override
    @Transactional(readOnly = true)
    public List<TrainingProgramResponse> listPrograms(ClassroomDeliveryMode deliveryMode) {
        List<TrainingProgram> programs = deliveryMode == null
                ? programRepository.findAllByOrderByDisplayOrderAscUpdatedAtDescIdDesc()
                : programRepository.findByDeliveryModeOrderByDisplayOrderAscUpdatedAtDescIdDesc(deliveryMode);
        return programs.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrainingProgramResponse> listPublishedPrograms(ClassroomDeliveryMode deliveryMode) {
        return listPrograms(deliveryMode).stream()
                .filter(program -> program.getStatus() == PackageStatus.PUBLISHED)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TrainingProgramResponse getPublishedProgram(String slugOrId) {
        TrainingProgram program;
        try {
            program = programRepository.findById(Long.parseLong(slugOrId))
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học."));
        } catch (NumberFormatException ignored) {
            program = programRepository.findBySlug(slugOrId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học."));
        }
        if (program.getStatus() != PackageStatus.PUBLISHED) {
            throw new RuntimeException("Khóa học chưa mở nhận đăng ký.");
        }
        return toResponse(program);
    }

    @Override
    @Transactional(readOnly = true)
    public TrainingProgramResponse getProgram(Long id) {
        return toResponse(findProgram(id));
    }

    @Override
    public TrainingProgramResponse createProgram(TrainingProgramRequest request) {
        CurriculumProgram curriculum = resolveCurriculum(request.getCurriculumProgramId());
        TrainingProgram program = TrainingProgram.builder()
                .title(request.getTitle().trim())
                .code(uniqueCode(defaultText(request.getCode(), makeCode(request.getTitle(), request.getDeliveryType()))))
                .slug(uniqueSlug(defaultText(request.getSlug(), slugify(request.getTitle()))))
                .deliveryMode(request.getDeliveryType())
                .curriculumProgram(curriculum)
                .status(request.getStatus() == null ? PackageStatus.DRAFT : request.getStatus())
                .build();
        apply(program, request);
        validatePublishable(program);
        TrainingProgram saved = programRepository.save(program);
        instructorLedCourseSync.syncFromTrainingProgram(saved);
        return toResponse(saved);
    }

    @Override
    public TrainingProgramResponse updateProgram(Long id, TrainingProgramRequest request) {
        TrainingProgram program = findProgram(id);
        Long originalCurriculumId = program.getCurriculumProgram() == null ? null : program.getCurriculumProgram().getId();
        PackageStatus originalStatus = program.getStatus();
        program.setTitle(request.getTitle().trim());
        program.setCode(uniqueCodeForUpdate(defaultText(request.getCode(), makeCode(request.getTitle(), request.getDeliveryType())), program.getId()));
        program.setSlug(uniqueSlugForUpdate(defaultText(request.getSlug(), slugify(request.getTitle())), program.getId()));
        program.setDeliveryMode(request.getDeliveryType());
        program.setCurriculumProgram(resolveCurriculum(request.getCurriculumProgramId()));
        program.setStatus(request.getStatus() == null ? PackageStatus.DRAFT : request.getStatus());
        apply(program, request);
        if (countActiveClassrooms(program) > 0
                && (!java.util.Objects.equals(originalCurriculumId, program.getCurriculumProgram().getId())
                || originalStatus != program.getStatus())) {
            throw new IllegalArgumentException(
                    "Không thể đổi chương trình đào tạo hoặc trạng thái của khóa học đang được lớp hoạt động sử dụng."
            );
        }
        validatePublishable(program);
        TrainingProgram saved = programRepository.save(program);
        instructorLedCourseSync.syncFromTrainingProgram(saved);
        return toResponse(saved);
    }

    @Override
    public TrainingProgramResponse cloneProgram(Long id) {
        TrainingProgram source = findProgram(id);
        TrainingProgram clone = TrainingProgram.builder()
                .title(source.getTitle() + " (Bản sao)")
                .code(uniqueCode(source.getCode() + "-COPY"))
                .slug(uniqueSlug(source.getSlug() + "-copy"))
                .deliveryMode(source.getDeliveryMode())
                .curriculumProgram(source.getCurriculumProgram())
                .shortDescription(source.getShortDescription())
                .description(source.getDescription())
                .price(source.getPrice())
                .salePrice(source.getSalePrice())
                .duration(source.getDuration())
                .studyMode(source.getStudyMode())
                .thumbnailUrl(source.getThumbnailUrl())
                .status(PackageStatus.DRAFT)
                .featured(false)
                .build();
        TrainingProgram saved = programRepository.save(clone);
        instructorLedCourseSync.syncFromTrainingProgram(saved);
        return toResponse(saved);
    }

    @Override
    public void archiveProgram(Long id) {
        TrainingProgram program = findProgram(id);
        if (countActiveClassrooms(program) > 0) {
            throw new RuntimeException("Không thể lưu trữ khóa học đang được lớp sắp khai giảng hoặc đang hoạt động sử dụng.");
        }
        program.setStatus(PackageStatus.ARCHIVED);
        TrainingProgram saved = programRepository.save(program);
        instructorLedCourseSync.syncFromTrainingProgram(saved);
    }

    private void apply(TrainingProgram program, TrainingProgramRequest request) {
        program.setShortDescription(trimOrNull(request.getShortDescription()));
        program.setDescription(trimOrNull(request.getDescription()));
        program.setPrice(request.getPrice() == null ? BigDecimal.ZERO : request.getPrice());
        program.setSalePrice(request.getSalePrice());
        program.setDuration(trimOrNull(request.getDuration()));
        program.setStudyMode(trimOrNull(request.getStudyMode()));
        program.setThumbnailUrl(trimOrNull(request.getThumbnailUrl()));
        program.setFeatured(Boolean.TRUE.equals(request.getFeatured()));
    }

    private void validatePublishable(TrainingProgram program) {
        if (program.getStatus() != PackageStatus.PUBLISHED) {
            return;
        }
        if (program.getCurriculumProgram() == null
                || !"PUBLISHED".equalsIgnoreCase(program.getCurriculumProgram().getStatus())) {
            throw new IllegalArgumentException("Chỉ có thể xuất bản khóa học khi chương trình đào tạo gốc đã được duyệt.");
        }
    }

    private TrainingProgramResponse toResponse(TrainingProgram program) {
        CurriculumProgram curriculum = program.getCurriculumProgram();
        return TrainingProgramResponse.builder()
                .id(program.getId())
                .title(program.getTitle())
                .code(program.getCode())
                .slug(program.getSlug())
                .deliveryType(program.getDeliveryMode())
                .deliveryMode(program.getDeliveryMode())
                .deliveryModeLabel(deliveryModeLabel(program.getDeliveryMode()))
                .curriculumProgramId(curriculum == null ? null : curriculum.getId())
                .curriculumProgramTitle(curriculum == null ? null : curriculum.getTitle())
                .curriculumProgramCode(curriculum == null ? null : curriculum.getCode())
                .curriculumProgramExamCategory(curriculum == null ? null : curriculum.getExamCategory())
                .programTrack(curriculum == null ? null : curriculum.getProgramTrack())
                .focusSkills(curriculum == null ? null : curriculum.getFocusSkills())
                .curriculumProgramStatus(curriculum == null ? null : curriculum.getStatus())
                .shortDescription(program.getShortDescription())
                .description(program.getDescription())
                .entryLevel(curriculum == null ? null : curriculum.getEntryLevel())
                .targetScore(resolveTargetScore(curriculum))
                .targetOutcome(curriculum == null ? null : curriculum.getOutcomes())
                .price(program.getPrice())
                .salePrice(program.getSalePrice())
                .duration(program.getDuration())
                .studyMode(program.getStudyMode())
                .thumbnailUrl(program.getThumbnailUrl())
                .status(program.getStatus())
                .statusLabel(statusLabel(program.getStatus()))
                .displayOrder(program.getDisplayOrder())
                .featured(program.isFeatured())
                .classroomCount(program.getClassSections().size())
                .activeClassroomCount((int) countActiveClassrooms(program))
                .createdAt(program.getCreatedAt())
                .updatedAt(program.getUpdatedAt())
                .build();
    }

    private String resolveTargetScore(CurriculumProgram curriculum) {
        if (curriculum == null) {
            return null;
        }
        if (curriculum.getTargetBand() != null) {
            return curriculum.getTargetBand().stripTrailingZeros().toPlainString();
        }
        return curriculum.getTargetScore() == null ? null : String.valueOf(curriculum.getTargetScore());
    }

    private TrainingProgram findProgram(Long id) {
        return programRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học theo lịch."));
    }

    private CurriculumProgram resolveCurriculum(Long id) {
        return curriculumProgramRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chương trình đào tạo."));
    }

    private long countActiveClassrooms(TrainingProgram program) {
        return program.getClassSections().stream()
                .filter(offering -> ACTIVE_CLASS_STATUSES.contains(offering.getStatus()))
                .count();
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

    private String deliveryModeLabel(ClassroomDeliveryMode mode) {
        return mode == ClassroomDeliveryMode.VIRTUAL ? "Virtual" : "Offline";
    }

    private String statusLabel(PackageStatus status) {
        if (status == PackageStatus.PUBLISHED) return "Đã xuất bản";
        if (status == PackageStatus.ARCHIVED) return "Đã lưu trữ";
        if (status == PackageStatus.REJECTED) return "Từ chối";
        if (status == PackageStatus.PENDING_REVIEW) return "Chờ duyệt";
        return "Bản nháp";
    }
}
