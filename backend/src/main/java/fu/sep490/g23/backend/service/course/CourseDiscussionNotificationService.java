package fu.sep490.g23.backend.service.course;

import fu.sep490.g23.backend.entity.course.CourseDiscussionReply;
import fu.sep490.g23.backend.entity.course.CourseDiscussionThread;

public interface CourseDiscussionNotificationService {

    void notifyQuestionSent(CourseDiscussionThread thread);

    void notifyNewReply(CourseDiscussionReply reply);
}
