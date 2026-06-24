package fu.sap490.g23.backend.service.classroom.impl;

import fu.sap490.g23.backend.service.classroom.*;


import fu.sap490.g23.backend.dto.request.classroom.UpdateGradebookRequest;
import fu.sap490.g23.backend.dto.response.classroom.ClassroomGradebookResponse;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.classroom.ClassroomGradebookEntry;
import fu.sap490.g23.backend.entity.classroom.ClassroomOffering;
import fu.sap490.g23.backend.entity.classroom.enums.GradebookEntryStatus;
import fu.sap490.g23.backend.repository.classroom.ClassroomEnrollmentRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomGradebookEntryRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomOfferingRepository;
import fu.sap490.g23.backend.security.ClassroomAccessHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ClassroomGradebookServiceImpl implements ClassroomGradebookService {

    private final ClassroomGradebookEntryRepository gradebookEntryRepository;
    private final ClassroomOfferingRepository offeringRepository;
    private final ClassroomEnrollmentRepository enrollmentRepository;
    private final ClassroomAccessHelper accessHelper;
    private final ClassroomMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomGradebookResponse> getClassGradebook(Long offeringId) {
        return gradebookEntryRepository.findByClassroomOfferingId(offeringId).stream()
                .map(mapper::toGradebookResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ClassroomGradebookResponse getMyGradebook(Long offeringId, String learnerEmail) {
        User learner = accessHelper.requireUser(learnerEmail);
        ClassroomGradebookEntry entry = gradebookEntryRepository
                .findByClassroomOfferingIdAndStudentId(offeringId, learner.getId())
                .orElseThrow(() -> new RuntimeException("Chưa có bảng điểm cho học viên này."));
        if (entry.getStatus() != GradebookEntryStatus.PUBLISHED) {
            throw new RuntimeException("Bảng điểm chưa được công bố.");
        }
        return mapper.toGradebookResponse(entry);
    }

    @Override
    public ClassroomGradebookResponse updateEntry(Long offeringId, UpdateGradebookRequest request, String updaterEmail) {
        User updater = accessHelper.requireUser(updaterEmail);
        accessHelper.assertTeacher(updater);

        ClassroomOffering offering = offeringRepository.findById(offeringId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp học."));
        User student = enrollmentRepository.findByStudentIdAndClassroomOfferingId(request.getStudentId(), offeringId)
                .map(enrollment -> enrollment.getStudent())
                .orElseThrow(() -> new RuntimeException("Học viên không thuộc lớp này."));

        ClassroomGradebookEntry entry = gradebookEntryRepository
                .findByClassroomOfferingIdAndStudentId(offeringId, student.getId())
                .orElseGet(() -> ClassroomGradebookEntry.builder()
                        .classroomOffering(offering)
                        .student(student)
                        .status(GradebookEntryStatus.PENDING)
                        .build());

        if (request.getHomeworkScore() != null) {
            entry.setHomeworkScore(request.getHomeworkScore());
        }
        if (request.getQuizScore() != null) {
            entry.setQuizScore(request.getQuizScore());
        }
        if (request.getAttendancePercent() != null) {
            entry.setAttendancePercent(request.getAttendancePercent());
        }
        if (request.getParticipationScore() != null) {
            entry.setParticipationScore(request.getParticipationScore());
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

        return mapper.toGradebookResponse(gradebookEntryRepository.save(entry));
    }

    @Override
    public List<ClassroomGradebookResponse> publishGradebook(Long offeringId, String publisherEmail) {
        User publisher = accessHelper.requireUser(publisherEmail);
        accessHelper.assertTeacher(publisher);

        List<ClassroomGradebookEntry> entries = gradebookEntryRepository.findByClassroomOfferingId(offeringId);
        if (entries.isEmpty()) {
            throw new RuntimeException("Chưa có dữ liệu bảng điểm để công bố.");
        }

        entries.forEach(entry -> {
            entry.setStatus(GradebookEntryStatus.PUBLISHED);
            entry.setUpdatedBy(publisher);
        });
        return gradebookEntryRepository.saveAll(entries).stream()
                .map(mapper::toGradebookResponse)
                .toList();
    }
}
