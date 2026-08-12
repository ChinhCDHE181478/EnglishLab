package fu.sep490.g23.backend.migration;

import fu.sep490.g23.backend.entity.classroom.ClassroomOffering;
import fu.sep490.g23.backend.entity.classroom.TrainingProgram;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import fu.sep490.g23.backend.entity.course.enums.PackageStatus;
import fu.sep490.g23.backend.entity.curriculum.CurriculumProgram;
import fu.sep490.g23.backend.repository.classroom.ClassroomOfferingRepository;
import fu.sep490.g23.backend.repository.classroom.TrainingProgramRepository;
import fu.sep490.g23.backend.repository.curriculum.CurriculumProgramRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class TrainingProgramBackfillMigration implements CommandLineRunner {

    private final CurriculumProgramRepository curriculumProgramRepository;
    private final TrainingProgramRepository trainingProgramRepository;
    private final ClassroomOfferingRepository classroomOfferingRepository;

    @Override
    @Transactional
    public void run(String... args) {
        backfillTrainingPrograms();
        backfillClassroomLinks();
    }

    private void backfillTrainingPrograms() {
        for (CurriculumProgram curriculum : curriculumProgramRepository.findAll(Sort.by(Sort.Direction.ASC, "id"))) {
            ClassroomDeliveryMode mode = curriculum.getDeliveryMode() == null ? ClassroomDeliveryMode.OFFLINE : curriculum.getDeliveryMode();
            boolean exists = trainingProgramRepository
                    .findFirstByCurriculumProgramIdAndDeliveryModeOrderByIdAsc(curriculum.getId(), mode)
                    .isPresent();
            if (exists) {
                continue;
            }

            TrainingProgram program = TrainingProgram.builder()
                    .title(curriculum.getTitle())
                    .code(uniqueCode(curriculum.getCode()))
                    .slug(uniqueSlug(curriculum.getSlug()))
                    .deliveryMode(mode)
                    .curriculumProgram(curriculum)
                    .shortDescription(curriculum.getOutcomes())
                    .price(BigDecimal.ZERO)
                    .duration(curriculum.getTotalSessions() == null || curriculum.getTotalSessions() <= 0 ? null : curriculum.getTotalSessions() + " buổi")
                    .studyMode(mode == ClassroomDeliveryMode.VIRTUAL ? "Virtual" : "Tại trung tâm")
                    .status(toPackageStatus(curriculum.getStatus()))
                    .displayOrder(curriculum.getDisplayOrder() == null ? 0 : curriculum.getDisplayOrder())
                    .featured(false)
                    .build();
            trainingProgramRepository.save(program);
        }
    }

    private void backfillClassroomLinks() {
        for (ClassroomOffering offering : classroomOfferingRepository.findAll()) {
            if (offering.getTrainingProgram() != null || offering.getCurriculumProgram() == null) {
                continue;
            }
            Long curriculumId = offering.getCurriculumProgram().getId();
            ClassroomDeliveryMode mode = offering.getDeliveryMode();
            trainingProgramRepository.findFirstByCurriculumProgramIdAndDeliveryModeOrderByIdAsc(curriculumId, mode)
                    .or(() -> trainingProgramRepository.findFirstByCurriculumProgramIdOrderByIdAsc(curriculumId))
                    .ifPresent(offering::setTrainingProgram);
        }
    }

    private PackageStatus toPackageStatus(String status) {
        if ("PUBLISHED".equalsIgnoreCase(status)) return PackageStatus.PUBLISHED;
        if ("ARCHIVED".equalsIgnoreCase(status)) return PackageStatus.ARCHIVED;
        return PackageStatus.DRAFT;
    }

    private String uniqueCode(String base) {
        String normalized = defaultText(base, "PROGRAM").trim().toUpperCase(Locale.ROOT);
        String candidate = normalized;
        int index = 2;
        while (trainingProgramRepository.existsByCodeIgnoreCase(candidate)) {
            candidate = normalized + "-" + index++;
        }
        return candidate;
    }

    private String uniqueSlug(String base) {
        String normalized = slugify(defaultText(base, "program"));
        String candidate = normalized;
        int index = 2;
        while (trainingProgramRepository.existsBySlug(candidate)) {
            candidate = normalized + "-" + index++;
        }
        return candidate;
    }

    private String slugify(String value) {
        String normalized = Normalizer.normalize(String.valueOf(value), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .replaceAll("[^A-Za-z0-9\\s-]", " ");
        return normalized.trim().replaceAll("\\s+", "-").replaceAll("-+", "-").toLowerCase(Locale.ROOT);
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }
}
