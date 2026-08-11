package fu.sep490.g23.backend.service.classroom;

import fu.sep490.g23.backend.dto.response.classroom.AvailableRoomOptionResponse;
import fu.sep490.g23.backend.dto.response.classroom.AvailableTeacherOptionResponse;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface ClassroomScheduleAvailabilityService {

    List<AvailableRoomOptionResponse> listAvailableRooms(LocalDate sessionDate,
            LocalTime startTime,
            LocalTime endTime,
            Long excludeSessionId);
    List<AvailableTeacherOptionResponse> listAvailableTeachers(LocalDate sessionDate,
            LocalTime startTime,
            LocalTime endTime,
            Long excludeSessionId);
}
