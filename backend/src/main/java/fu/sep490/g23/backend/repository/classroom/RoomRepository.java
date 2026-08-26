package fu.sep490.g23.backend.repository.classroom;

import fu.sep490.g23.backend.entity.classroom.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findByActiveTrue();
    List<Room> findByActiveTrueOrderByNameAsc();
}
