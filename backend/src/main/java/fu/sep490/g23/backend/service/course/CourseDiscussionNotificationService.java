package fu.sep490.g23.backend.service.course;

import fu.sep490.g23.backend.entity.course.CourseDiscussionPost;

public interface CourseDiscussionNotificationService {

    void notifyQuestionSent(CourseDiscussionPost thread);

    void notifyNewReply(CourseDiscussionPost reply);
}
