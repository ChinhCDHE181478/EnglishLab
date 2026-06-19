package fu.sap490.g23.backend.repository.classroom;

import fu.sap490.g23.backend.entity.classroom.ClassroomRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClassroomRoomRepository extends JpaRepository<ClassroomRoom, Long> {
    List<ClassroomRoom> findByCampusIdAndActiveTrue(Long campusId);
}
