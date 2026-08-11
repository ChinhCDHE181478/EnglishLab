package fu.sep490.g23.backend.repository.classroom;

import fu.sep490.g23.backend.entity.classroom.ClassroomAnnouncement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClassroomAnnouncementRepository extends JpaRepository<ClassroomAnnouncement, Long> {
    List<ClassroomAnnouncement> findByClassroomOfferingIdOrderByCreatedAtDesc(Long classroomOfferingId);
}
