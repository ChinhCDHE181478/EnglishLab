package fu.sep490.g23.backend.service.classroom.impl;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.classroom.ClassroomHomework;
import fu.sep490.g23.backend.entity.classroom.ClassSection;
import fu.sep490.g23.backend.entity.classroom.ClassroomTeacherAssignment;
import fu.sep490.g23.backend.entity.classroom.enums.HomeworkStatus;
import fu.sep490.g23.backend.entity.enums.RoleCodes;
import fu.sep490.g23.backend.repository.classroom.CenterMaterialLibraryItemRepository;
import fu.sep490.g23.backend.repository.classroom.ClassEnrollmentRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomHomeworkRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomHomeworkSubmissionRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomMaterialRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomTeacherAssignmentRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomTuitionPaymentProofRepository;
import fu.sep490.g23.backend.security.ClassroomAccessHelper;
import fu.sep490.g23.backend.service.classroom.HomeworkAttachmentAccessService;
import fu.sep490.g23.backend.service.classroom.HomeworkAttachmentStorageService;
import fu.sep490.g23.backend.service.classroom.ClassroomRegistrationSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeworkAttachmentAccessServiceImpl implements HomeworkAttachmentAccessService {

    private final HomeworkAttachmentStorageService storageService;
    private final ClassroomAccessHelper accessHelper;
    private final ClassroomHomeworkRepository homeworkRepository;
    private final ClassroomHomeworkSubmissionRepository submissionRepository;
    private final ClassroomTuitionPaymentProofRepository proofRepository;
    private final ClassroomMaterialRepository materialRepository;
    private final CenterMaterialLibraryItemRepository centerMaterialRepository;
    private final ClassEnrollmentRepository enrollmentRepository;
    private final ClassroomTeacherAssignmentRepository teacherAssignmentRepository;

    @Override
    public Resource loadAuthorized(String fileName, String requesterEmail) {
        User requester = accessHelper.requireUser(requesterEmail);
        String suffix = fileSuffix(fileName);
        if (!canAccessReferencedFile(suffix, requester)) {
            throw hiddenFileException();
        }
        try {
            return storageService.load(fileName);
        } catch (IllegalArgumentException exception) {
            throw hiddenFileException();
        }
    }

    @Override
    public String contentType(String fileName) {
        return storageService.contentType(fileName);
    }

    @Override
    public void assertLearnerUploadAccess(Long homeworkId, String learnerEmail) {
        User learner = accessHelper.requireUser(learnerEmail);
        ClassroomHomework homework = homeworkRepository.findById(homeworkId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy bài tập."));
        boolean enrolled = enrollmentRepository.existsByStudentIdAndClassSectionIdAndRegistrationStatusIn(
                learner.getId(),
                homework.getClassSection().getId(),
                ClassroomRegistrationSupport.HAS_LEARNING_ACCESS
        );
        if (!enrolled) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền nộp tệp cho bài tập này.");
        }
        if (homework.getStatus() != HomeworkStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bài tập chưa mở để nộp tệp.");
        }
        if (homework.getDeadline() != null && homework.getDeadline().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Đã quá hạn nộp tệp cho bài tập này.");
        }
    }

    private boolean canAccessReferencedFile(String suffix, User requester) {
        if (accessHelper.canManageTrainingOperations(requester)) {
            return isReferenced(suffix);
        }

        var proof = proofRepository.findFirstByFileUrlEndingWith(suffix);
        if (proof.isPresent()) {
            return proof.get().getEnrollment().getStudent().getId().equals(requester.getId());
        }

        var submission = submissionRepository.findFirstByAttachmentUrlEndingWith(suffix);
        if (submission.isPresent()) {
            if (submission.get().getStudent().getId().equals(requester.getId())) {
                return true;
            }
            return canTeachClass(requester, submission.get().getHomework().getClassSection());
        }

        var homework = homeworkRepository.findFirstByAttachmentUrlEndingWith(suffix);
        if (homework.isPresent()) {
            return canAccessClassContent(requester, homework.get().getClassSection());
        }

        var material = materialRepository.findFirstByFileUrlEndingWith(suffix);
        if (material.isPresent()) {
            return canAccessClassContent(requester, material.get().getClassSection());
        }

        return requester.hasRole(RoleCodes.CONTENT_MANAGER)
                && centerMaterialRepository.findFirstByFileUrlEndingWith(suffix).isPresent();
    }

    private boolean isReferenced(String suffix) {
        return proofRepository.existsByFileUrlEndingWith(suffix)
                || submissionRepository.existsByAttachmentUrlEndingWith(suffix)
                || homeworkRepository.existsByAttachmentUrlEndingWith(suffix)
                || materialRepository.existsByFileUrlEndingWith(suffix)
                || centerMaterialRepository.existsByFileUrlEndingWith(suffix);
    }

    private boolean canAccessClassContent(User requester, ClassSection offering) {
        if (requester.hasRole(RoleCodes.CONTENT_MANAGER) || canTeachClass(requester, offering)) {
            return true;
        }
        return enrollmentRepository.existsByStudentIdAndClassSectionIdAndRegistrationStatusIn(
                requester.getId(),
                offering.getId(),
                ClassroomRegistrationSupport.HAS_LEARNING_ACCESS
        );
    }

    private boolean canTeachClass(User requester, ClassSection offering) {
        if (!requester.hasRole(RoleCodes.TEACHER)) {
            return false;
        }
        if (offering.getPrimaryTeacher() != null && offering.getPrimaryTeacher().getId().equals(requester.getId())) {
            return true;
        }
        LocalDate today = LocalDate.now();
        return teacherAssignmentRepository
                .findAllByClassSectionIdAndTeacherId(offering.getId(), requester.getId())
                .stream()
                .anyMatch(assignment -> isActive(assignment, today));
    }

    private boolean isActive(ClassroomTeacherAssignment assignment, LocalDate today) {
        return (assignment.getEffectiveFrom() == null || !assignment.getEffectiveFrom().isAfter(today))
                && (assignment.getEffectiveTo() == null || !assignment.getEffectiveTo().isBefore(today));
    }

    private String fileSuffix(String fileName) {
        if (fileName == null || fileName.isBlank() || fileName.contains("/") || fileName.contains("\\")) {
            throw hiddenFileException();
        }
        return "/" + fileName;
    }

    private ResponseStatusException hiddenFileException() {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Không tìm thấy tệp hoặc bạn không có quyền truy cập."
        );
    }
}
