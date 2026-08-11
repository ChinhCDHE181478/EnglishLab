package fu.sep490.g23.backend.service.classroom;

public interface TeacherClassroomAuthorizationService {

    void assertClassroomAccess(Long classroomId, String actorEmail);

    void assertSessionAccess(Long sessionId, String actorEmail);

    void assertHomeworkAccess(Long homeworkId, String actorEmail);

    void assertMaterialAccess(Long materialId, String actorEmail);
}
