package fu.sep490.g23.backend.security;

import fu.sep490.g23.backend.entity.enums.RoleCodes;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class ClassroomAccessHelper {

    private static final Set<String> MANAGER_ROLES = Set.of(RoleCodes.STAFF, RoleCodes.MANAGER, RoleCodes.ADMIN);
    private static final Set<String> TEACHER_ROLES = Set.of(RoleCodes.TEACHER, RoleCodes.MANAGER, RoleCodes.ADMIN);
    private static final Set<String> CONTENT_MANAGER_ROLES = Set.of(RoleCodes.CONTENT_MANAGER, RoleCodes.MANAGER, RoleCodes.ADMIN);

    private final UserRepository userRepository;

    public User requireUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng."));
    }

    public boolean canManageClassroom(User user) {
        return user.hasAnyRoleCodes(MANAGER_ROLES) || user.hasAnyRoleCodes(CONTENT_MANAGER_ROLES);
    }

    public boolean canTeach(User user) {
        return user.hasAnyRoleCodes(TEACHER_ROLES);
    }

    public boolean canApproveRequests(User user) {
        return TrainingRolePolicy.canOperate(user);
    }

    public boolean canManageTrainingOperations(User user) {
        return TrainingRolePolicy.canOperate(user);
    }

    public void assertStaffOperator(User user) {
        if (!canManageTrainingOperations(user)) {
            throw new RuntimeException("Bạn không có quyền vận hành đào tạo.");
        }
    }

    public void assertManager(User user) {
        if (!canManageTrainingOperations(user)) {
            throw new RuntimeException("Bạn không có quyền truy cập nội dung này.");
        }
    }

    public void assertTeacher(User user) {
        if (!canTeach(user)) {
            throw new RuntimeException("Bạn không có quyền truy cập nội dung này.");
        }
    }
}
