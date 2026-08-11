package fu.sep490.g23.backend.dto.response.admin;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminDashboardResponse {
    private long totalUsers;
    private long learners;
    private long teachers;
    private long staffAndAdmins;
}
