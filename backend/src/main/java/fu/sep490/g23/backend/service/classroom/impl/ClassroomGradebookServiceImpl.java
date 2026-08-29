package fu.sep490.g23.backend.service.classroom.impl;
import fu.sep490.g23.backend.service.classroom.ClassroomMapper;


import fu.sep490.g23.backend.dto.request.classroom.UpdateGradebookRequest;
import fu.sep490.g23.backend.dto.request.classroom.UpdateGradebookHomeworkScoreRequest;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomGradebookHomeworkResponse;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomGradebookResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.classroom.ClassroomGradebookEntry;
import fu.sep490.g23.backend.entity.classroom.ClassroomHomework;
import fu.sep490.g23.backend.entity.classroom.ClassroomHomeworkSubmission;
import fu.sep490.g23.backend.entity.classroom.ClassSection;
import fu.sep490.g23.backend.entity.classroom.enums.GradebookEntryStatus;
import fu.sep490.g23.backend.entity.classroom.enums.HomeworkStatus;
import fu.sep490.g23.backend.entity.classroom.enums.HomeworkSubmissionStatus;
import fu.sep490.g23.backend.repository.classroom.ClassEnrollmentRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomGradebookEntryRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomHomeworkRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomHomeworkSubmissionRepository;
import fu.sep490.g23.backend.repository.classroom.ClassSectionRepository;
import fu.sep490.g23.backend.security.ClassroomAccessHelper;
import fu.sep490.g23.backend.service.classroom.ClassroomGradebookService;
import fu.sep490.g23.backend.service.classroom.ClassroomHomeworkScoreCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ClassroomGradebookServiceImpl implements ClassroomGradebookService {

    private final ClassroomGradebookEntryRepository gradebookEntryRepository;
    private final ClassSectionRepository offeringRepository;
    private final ClassEnrollmentRepository enrollmentRepository;
    private final ClassroomHomeworkRepository homeworkRepository;
    private final ClassroomHomeworkSubmissionRepository submissionRepository;
    private final ClassroomAccessHelper accessHelper;
    private final ClassroomMapper mapper;
    private final ClassroomHomeworkScoreCalculator homeworkScoreCalculator;

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomGradebookResponse> getClassGradebook(Long offeringId) {
        return buildClassGradebookResponses(offeringId, true);
    }

    @Override
    @Transactional(readOnly = true)
    public ClassroomGradebookResponse getMyGradebook(Long offeringId, String learnerEmail) {
        User learner = accessHelper.requireUser(learnerEmail);
        ClassroomGradebookEntry entry = gradebookEntryRepository
                .findByClassSectionIdAndStudentId(offeringId, learner.getId())
                .orElse(null);
        if (entry == null || entry.getStatus() != GradebookEntryStatus.PUBLISHED) {
            return null;
        }
        List<ClassroomHomework> homeworks = homeworkRepository
                .findByClassSectionIdOrderByCreatedAtDesc(offeringId).stream()
                .filter(homework -> homework.getStatus() != HomeworkStatus.DRAFT)
                .toList();
        List<ClassroomHomeworkSubmission> submissions = submissionRepository
                .findAllForStudentGradebook(offeringId, learner.getId());
        return buildResponse(entry, homeworks, submissions);
    }

    @Override
    public ClassroomGradebookResponse updateEntry(Long offeringId, UpdateGradebookRequest request, String updaterEmail) {
        User updater = accessHelper.requireUser(updaterEmail);
        accessHelper.assertTeacher(updater);

        ClassSection offering = offeringRepository.findById(offeringId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp học."));
        User student = enrollmentRepository.findByStudentIdAndClassSectionId(request.getStudentId(), offeringId)
                .map(enrollment -> enrollment.getStudent())
                .orElseThrow(() -> new RuntimeException("Học viên không thuộc lớp này."));

        ClassroomGradebookEntry entry = gradebookEntryRepository
                .findByClassSectionIdAndStudentId(offeringId, student.getId())
                .orElseGet(() -> ClassroomGradebookEntry.builder()
                        .classSection(offering)
                        .student(student)
                        .status(GradebookEntryStatus.PENDING)
                        .build());

        List<ClassroomHomework> homeworks = homeworkRepository
                .findByClassSectionIdOrderByCreatedAtDesc(offeringId);
        List<ClassroomHomeworkSubmission> submissions = new ArrayList<>(submissionRepository
                .findAllForStudentGradebook(offeringId, student.getId()));
        applyHomeworkScoreUpdates(request.getHomeworkScores(), homeworks, submissions, student, updater);
        entry.setHomeworkScore(homeworkScoreCalculator.calculateAverage(homeworks, submissions));
        if (request.getAttendancePercent() != null) {
            entry.setAttendancePercent(request.getAttendancePercent());
        }
        if (request.getFinalResult() != null) {
            entry.setFinalResult(request.getFinalResult());
        }
        if (request.getTeacherComment() != null) {
            entry.setTeacherComment(request.getTeacherComment());
        }
        if (request.getStatus() != null) {
            entry.setStatus(request.getStatus());
        } else if (entry.getStatus() == GradebookEntryStatus.PENDING) {
            entry.setStatus(GradebookEntryStatus.GRADED);
        }
        entry.setUpdatedBy(updater);

        ClassroomGradebookEntry savedEntry = gradebookEntryRepository.save(entry);
        return buildResponse(savedEntry, homeworks, submissions);
    }

    @Override
    public List<ClassroomGradebookResponse> publishGradebook(Long offeringId, String publisherEmail) {
        User publisher = accessHelper.requireUser(publisherEmail);
        accessHelper.assertTeacher(publisher);

        List<ClassroomGradebookEntry> entries = gradebookEntryRepository.findByClassSectionId(offeringId);
        if (entries.isEmpty()) {
            throw new RuntimeException("Chưa có dữ liệu bảng điểm để công bố.");
        }

        entries.forEach(entry -> {
            entry.setStatus(GradebookEntryStatus.PUBLISHED);
            entry.setUpdatedBy(publisher);
        });
        gradebookEntryRepository.saveAll(entries);
        return buildClassGradebookResponses(offeringId, true);
    }

    @Override
    public List<ClassroomGradebookResponse> unpublishGradebook(Long offeringId, String publisherEmail) {
        User publisher = accessHelper.requireUser(publisherEmail);
        accessHelper.assertTeacher(publisher);

        List<ClassroomGradebookEntry> entries = gradebookEntryRepository.findByClassSectionId(offeringId);
        List<ClassroomGradebookEntry> publishedEntries = entries.stream()
                .filter(entry -> entry.getStatus() == GradebookEntryStatus.PUBLISHED)
                .toList();
        if (publishedEntries.isEmpty()) {
            throw new RuntimeException("Bảng điểm chưa được công bố.");
        }

        publishedEntries.forEach(entry -> {
            entry.setStatus(GradebookEntryStatus.GRADED);
            entry.setUpdatedBy(publisher);
        });
        gradebookEntryRepository.saveAll(publishedEntries);
        return buildClassGradebookResponses(offeringId, true);
    }

    private void applyHomeworkScoreUpdates(
            List<UpdateGradebookHomeworkScoreRequest> updates,
            List<ClassroomHomework> homeworks,
            List<ClassroomHomeworkSubmission> submissions,
            User student,
            User grader
    ) {
        if (updates == null || updates.isEmpty()) {
            return;
        }

        Map<Long, ClassroomHomework> homeworkById = homeworks.stream()
                .collect(Collectors.toMap(ClassroomHomework::getId, Function.identity()));
        Map<Long, ClassroomHomeworkSubmission> submissionByHomeworkId = submissions.stream()
                .collect(Collectors.toMap(submission -> submission.getHomework().getId(), Function.identity()));
        Set<Long> updatedHomeworkIds = new HashSet<>();
        List<ClassroomHomeworkSubmission> changedSubmissions = new ArrayList<>();

        for (UpdateGradebookHomeworkScoreRequest update : updates) {
            if (!updatedHomeworkIds.add(update.getHomeworkId())) {
                throw new RuntimeException("Danh sách điểm có bài tập bị trùng lặp.");
            }
            ClassroomHomework homework = homeworkById.get(update.getHomeworkId());
            if (homework == null) {
                throw new RuntimeException("Bài tập không thuộc lớp học này.");
            }

            ClassroomHomeworkSubmission submission = submissionByHomeworkId.get(homework.getId());
            if (update.getScore() == null) {
                if (submission != null && submission.getScore() != null) {
                    clearHomeworkGrade(submission);
                    changedSubmissions.add(submission);
                }
                continue;
            }

            validateHomeworkScore(update.getScore(), homework.getMaxScore());
            if (submission == null) {
                submission = ClassroomHomeworkSubmission.builder()
                        .homework(homework)
                        .student(student)
                        .build();
                submissions.add(submission);
                submissionByHomeworkId.put(homework.getId(), submission);
            }
            submission.setScore(update.getScore());
            submission.setStatus(HomeworkSubmissionStatus.GRADED);
            submission.setGradedAt(LocalDateTime.now());
            submission.setGradedBy(grader);
            changedSubmissions.add(submission);
        }

        if (!changedSubmissions.isEmpty()) {
            submissionRepository.saveAll(changedSubmissions);
        }
    }

    private void clearHomeworkGrade(ClassroomHomeworkSubmission submission) {
        submission.setScore(null);
        submission.setGradedAt(null);
        submission.setGradedBy(null);
        submission.setStatus(submission.getSubmittedAt() == null
                ? HomeworkSubmissionStatus.DRAFT
                : HomeworkSubmissionStatus.SUBMITTED);
    }

    private void validateHomeworkScore(BigDecimal score, BigDecimal maxScore) {
        if (score.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Điểm bài tập không được nhỏ hơn 0.");
        }
        BigDecimal effectiveMaxScore = maxScore == null ? BigDecimal.TEN : maxScore;
        if (score.compareTo(effectiveMaxScore) > 0) {
            throw new RuntimeException("Điểm bài tập không được vượt quá điểm tối đa ("
                    + effectiveMaxScore.stripTrailingZeros().toPlainString() + ").");
        }
    }

    private List<ClassroomGradebookResponse> buildClassGradebookResponses(Long offeringId, boolean includeDraftHomework) {
        List<ClassroomHomework> homeworks = homeworkRepository
                .findByClassSectionIdOrderByCreatedAtDesc(offeringId).stream()
                .filter(homework -> includeDraftHomework || homework.getStatus() != HomeworkStatus.DRAFT)
                .toList();
        Map<Long, List<ClassroomHomeworkSubmission>> submissionsByStudent = submissionRepository
                .findAllForGradebook(offeringId).stream()
                .collect(Collectors.groupingBy(submission -> submission.getStudent().getId()));

        return gradebookEntryRepository.findByClassSectionId(offeringId).stream()
                .map(entry -> buildResponse(
                        entry,
                        homeworks,
                        submissionsByStudent.getOrDefault(entry.getStudent().getId(), List.of())
                ))
                .toList();
    }

    private ClassroomGradebookResponse buildResponse(
            ClassroomGradebookEntry entry,
            List<ClassroomHomework> homeworks,
            List<ClassroomHomeworkSubmission> submissions
    ) {
        Map<Long, ClassroomHomeworkSubmission> submissionByHomeworkId = new HashMap<>();
        submissions.forEach(submission -> submissionByHomeworkId.put(submission.getHomework().getId(), submission));

        BigDecimal homeworkAverage = homeworkScoreCalculator.calculateAverage(homeworks, submissions);
        ClassroomGradebookResponse response = mapper.toGradebookResponse(entry);
        response.setHomeworkAverage(homeworkAverage);
        response.setHomeworks(homeworks.stream()
                .map(homework -> toHomeworkResponse(homework, submissionByHomeworkId.get(homework.getId())))
                .toList());
        return response;
    }

    private ClassroomGradebookHomeworkResponse toHomeworkResponse(
            ClassroomHomework homework,
            ClassroomHomeworkSubmission submission
    ) {
        return ClassroomGradebookHomeworkResponse.builder()
                .id(homework.getId())
                .title(homework.getTitle())
                .score(submission == null ? null : submission.getScore())
                .maxScore(homework.getMaxScore() == null ? BigDecimal.TEN : homework.getMaxScore())
                .status(resolveHomeworkStatus(submission))
                .build();
    }

    private String resolveHomeworkStatus(ClassroomHomeworkSubmission submission) {
        if (submission == null || submission.getStatus() == HomeworkSubmissionStatus.DRAFT) {
            return "NOT_SUBMITTED";
        }
        return submission.getStatus().name();
    }
}
